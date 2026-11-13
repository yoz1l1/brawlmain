// Supabase Edge Function: arena-api
// Deploy with: supabase functions deploy arena-api
// Required secrets: SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY, ARENA_SERVER_KEY
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-arena-server-key",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
};

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { ...cors, "Content-Type": "application/json" } });

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method !== "GET" && req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const expected = Deno.env.get("ARENA_SERVER_KEY");
  if (!expected || req.headers.get("x-arena-server-key") !== expected) {
    return json({ error: "unauthorized" }, 401);
  }

  const url = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!url || !serviceKey) return json({ error: "server_not_configured" }, 500);
  const db = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });

  if (req.method === "GET") {
    const playerUuid = new URL(req.url).searchParams.get("player_uuid");
    if (!playerUuid || !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(playerUuid)) {
      return json({ error: "invalid_player_uuid" }, 400);
    }
    const [{ data: profile, error: profileError }, { data: brawlers, error: brawlerError }] = await Promise.all([
      db.from("arena_profiles").select("player_uuid,player_name,trophies,wins,losses,draws").eq("player_uuid", playerUuid).maybeSingle(),
      db.from("arena_brawler_progress").select("brawler_id,level,upgrade_points").eq("player_uuid", playerUuid).order("brawler_id"),
    ]);
    if (profileError || brawlerError) return json({ error: "database_error" }, 500);
    return json({ profile: profile ?? { player_uuid: playerUuid, player_name: "", trophies: 0, wins: 0, losses: 0, draws: 0 }, brawlers: brawlers ?? [] });
  }

  let body: any;
  try { body = await req.json(); } catch { return json({ error: "invalid_json" }, 400); }
  if (!body || !Array.isArray(body.players) || body.players.length < 1 || body.players.length > 6 ||
      typeof body.match_id !== "string" ||
      !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(body.match_id)) {
    return json({ error: "invalid_match_payload" }, 400);
  }

  const accepted = [];
  for (const p of body.players) {
    if (!p || typeof p.player_uuid !== "string" ||
        !/^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(p.player_uuid) ||
        !["win", "loss", "draw"].includes(p.outcome) ||
        !Number.isInteger(p.brawler_id) || p.brawler_id < 0 || p.brawler_id > 255 ||
        !Number.isInteger(p.trophies_delta) || Math.abs(p.trophies_delta) > 100 ||
        !Number.isInteger(p.kills) || p.kills < 0 || p.kills > 100 ||
        !Number.isInteger(p.deaths) || p.deaths < 0 || p.deaths > 100 ||
        !Number.isInteger(p.damage) || p.damage < 0 || p.damage > 10000000) {
      return json({ error: "invalid_player_result" }, 400);
    }
    const { data, error } = await db.rpc("arena_record_match", {
      p_match_id: body.match_id,
      p_player_uuid: p.player_uuid,
      p_player_name: String(p.player_name ?? "").slice(0, 32),
      p_brawler_id: p.brawler_id,
      p_outcome: p.outcome,
      p_trophies_delta: p.trophies_delta,
      p_kills: p.kills,
      p_deaths: p.deaths,
      p_damage: p.damage,
    });
    if (error) return json({ error: "could_not_record_match" }, 500);
    accepted.push({ player_uuid: p.player_uuid, recorded: data === true });
  }
  return json({ ok: true, results: accepted });
});
