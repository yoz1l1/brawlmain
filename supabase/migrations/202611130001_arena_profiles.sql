-- Boevaya Arena cloud profiles (Supabase)
-- Apply in Supabase SQL Editor. Writes are intentionally server-only.
create table if not exists public.arena_profiles (
  player_uuid uuid primary key,
  player_name text not null default '',
  trophies integer not null default 0 check (trophies >= 0),
  wins integer not null default 0 check (wins >= 0),
  losses integer not null default 0 check (losses >= 0),
  draws integer not null default 0 check (draws >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.arena_brawler_progress (
  player_uuid uuid not null references public.arena_profiles(player_uuid) on delete cascade,
  brawler_id integer not null check (brawler_id between 0 and 255),
  level integer not null default 1 check (level between 1 and 100),
  upgrade_points integer not null default 0 check (upgrade_points >= 0),
  primary key (player_uuid, brawler_id)
);

create table if not exists public.arena_match_results (
  match_id uuid not null,
  player_uuid uuid not null references public.arena_profiles(player_uuid) on delete cascade,
  brawler_id integer not null check (brawler_id between 0 and 255),
  outcome text not null check (outcome in ('win','loss','draw')),
  trophies_delta integer not null default 0,
  kills integer not null default 0 check (kills >= 0),
  deaths integer not null default 0 check (deaths >= 0),
  damage integer not null default 0 check (damage >= 0),
  created_at timestamptz not null default now(),
  primary key (match_id, player_uuid)
);

create index if not exists arena_match_results_player_time
  on public.arena_match_results(player_uuid, created_at desc);

-- No client-side writes. The Edge Function uses the service-role key server-side.
alter table public.arena_profiles enable row level security;
alter table public.arena_brawler_progress enable row level security;
alter table public.arena_match_results enable row level security;

drop policy if exists "deny client access to arena profiles" on public.arena_profiles;
drop policy if exists "deny client access to brawler progress" on public.arena_brawler_progress;
drop policy if exists "deny client access to match results" on public.arena_match_results;

create policy "deny client access to arena profiles"
  on public.arena_profiles for all to anon, authenticated using (false) with check (false);
create policy "deny client access to brawler progress"
  on public.arena_brawler_progress for all to anon, authenticated using (false) with check (false);
create policy "deny client access to match results"
  on public.arena_match_results for all to anon, authenticated using (false) with check (false);

-- Idempotent award function. Only service-role callers can execute it.
create or replace function public.arena_record_match(
  p_match_id uuid,
  p_player_uuid uuid,
  p_player_name text,
  p_brawler_id integer,
  p_outcome text,
  p_trophies_delta integer,
  p_kills integer,
  p_deaths integer,
  p_damage integer
) returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  inserted_count integer;
begin
  if p_outcome not in ('win','loss','draw')
     or p_trophies_delta < -100 or p_trophies_delta > 100
     or p_kills < 0 or p_deaths < 0 or p_damage < 0
     or p_brawler_id < 0 or p_brawler_id > 255 then
    raise exception 'invalid match result';
  end if;

  insert into arena_profiles(player_uuid, player_name)
  values (p_player_uuid, left(coalesce(p_player_name, ''), 32))
  on conflict (player_uuid) do update
    set player_name = excluded.player_name, updated_at = now();

  insert into arena_match_results(match_id, player_uuid, brawler_id, outcome,
      trophies_delta, kills, deaths, damage)
  values (p_match_id, p_player_uuid, p_brawler_id, p_outcome,
      p_trophies_delta, p_kills, p_deaths, p_damage)
  on conflict do nothing;

  get diagnostics inserted_count = row_count;
  if inserted_count = 0 then
    return false;
  end if;

  update arena_profiles
  set trophies = greatest(0, trophies + p_trophies_delta),
      wins = wins + case when p_outcome = 'win' then 1 else 0 end,
      losses = losses + case when p_outcome = 'loss' then 1 else 0 end,
      draws = draws + case when p_outcome = 'draw' then 1 else 0 end,
      updated_at = now()
  where player_uuid = p_player_uuid;

  insert into arena_brawler_progress(player_uuid, brawler_id)
  values (p_player_uuid, p_brawler_id)
  on conflict (player_uuid, brawler_id) do nothing;

  return true;
end;
$$;

revoke all on function public.arena_record_match(uuid, uuid, text, integer, text, integer, integer, integer, integer) from public, anon, authenticated;
grant execute on function public.arena_record_match(uuid, uuid, text, integer, text, integer, integer, integer, integer) to service_role;
