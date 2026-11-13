// Supabase Edge Function: arena-api
// Deploy with: supabase functions deploy arena-api
// Required secret: ARENA_SERVER_KEY (server-side only; POST only).
// SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are provided by Supabase.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-arena-server-key",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};

// Initial trophy rules. Change these in the trusted server implementation
// when the game's final progression/balance rules are decided.
const TROPHY_DELTA: Record<string, number> = { win: 8, loss: -5, draw: 0 };

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { ...cors, "Content-Type": "application/json" } });

const isUuid = (value: unknown): value is string =>
  typeof value === "string" &&
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(value);

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "GET" && req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  // Only writes require the private server key. GET exposes only public profile statistics.
  if (req.method === "POST") {
    const expected = Deno.env.get("ARENA_SERVER_KEY");
    if (!expected || req.headers.get("x-arena-server-key") !== expected) {
      return json({ error: "unauthorized" }, 401);
    }
  }

  const url = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !serviceKey) return json({ error: "server_not_configured" }, 500);
  const db = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });

  if (req.method === "GET") {
    const playerUuid = new URL(req.url).searchParams.get("player_uuid");
    if (!isUuid(playerUuid)) return json({ error: "invalid_player_uuid" }, 400);

    const [{ data: profile, error: profileError }, { data: brawlers, error: brawlerError }] = await Promise.all([
      db.from("arena_profiles").select("player_uuid,player_name,trophies,wins,losses,draws").eq("player_uuid", playerUuid).maybeSingle(),
      db.from("arena_brawler_progress").select("brawler_id,level,upgrade_points").eq("player_uuid", playerUuid).order("brawler_id"),
    ]);
    if (profileError || brawlerError) return json({ error: "database_error" }, 500);
    return json({
      profile: profile ?? { player_uuid: playerUuid, player_name: "", trophies: 0, wins: 0, losses: 0, draws: 0 },
      brawlers: brawlers ?? [],
    });
  }

  let body: any;
  try { body = await req.json(); } catch { return json({ error: "invalid_json" }, 400); }
  if (!body || !Array.isArray(body.players) || body.players.length < 1 || body.players.length > 6 || !isUuid(body.match_id)) {
    return json({ error: "invalid_match_payload" }, 400);
  }

  // Validate the entire payload before writing anything.
  const seenPlayers = new Set<string>();
  for (const p of body.players) {
    if (!p || !isUuid(p.player_uuid) || seenPlayers.has(p.player_uuid.toLowerCase()) ||
        !["win", "loss", "draw"].includes(p.outcome) ||
        !Number.isInteger(p.brawler_id) || p.brawler_id < 0 || p.brawler_id > 255 ||
        !Number.isInteger(p.kills) || p.kills < 0 || p.kills > 100 ||
        !Number.isInteger(p.deaths) || p.deaths < 0 || p.deaths > 100 ||
        !Number.isInteger(p.damage) || p.damage < 0 || p.damage > 10000000) {
      return json({ error: "invalid_player_result" }, 400);
    }
    seenPlayers.add(p.player_uuid.toLowerCase());
  }

  const accepted = [];
  for (const p of body.players) {
    const { data, error } = await db.rpc("arena_record_match", {
      p_match_id: body.match_id,
      p_player_uuid: p.player_uuid,
      p_player_name: String(p.player_name ?? "").slice(0, 32),
      p_brawler_id: p.brawler_id,
      p_outcome: p.outcome,
      // Never accept a caller-supplied trophy delta.
      p_trophies_delta: TROPHY_DELTA[p.outcome],
      p_kills: p.kills,
      p_deaths: p.deaths,
      p_damage: p.damage,
    });
    if (error) return json({ error: "could_not_record_match" }, 500);
    accepted.push({ player_uuid: p.player_uuid, recorded: data === true });
  }
  return json({ ok: true, results: accepted });
});
