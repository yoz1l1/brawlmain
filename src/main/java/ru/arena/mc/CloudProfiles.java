package ru.arena.mc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.arena.server.Host;

/**
 * Server-side cloud match reporting.
 * The private API key is read only from the server process environment and is never bundled.
 */
public final class CloudProfiles {
    private static final Logger LOGGER = Logger.getLogger("boevaya-arena-cloud");
    private static final String API = "https://feyxldpkwomajbykwysz.supabase.co/functions/v1/arena-api";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private CloudProfiles() {}

    public static void recordMatch(UUID matchId, List<Host.MatchResult> results) {
        String key = System.getenv("ARENA_SERVER_KEY");
        if (key == null || key.isBlank()) {
            LOGGER.fine("Cloud match reporting is disabled: ARENA_SERVER_KEY is not configured.");
            return;
        }
        if (matchId == null || results == null || results.isEmpty() || results.size() > 6) {
            LOGGER.warning("Cloud match reporting skipped: invalid match result list.");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("match_id", matchId.toString());
        JsonArray players = new JsonArray();
        for (Host.MatchResult result : results) {
            if (result == null || result.playerUuid == null) {
                LOGGER.warning("Cloud match reporting skipped: a player UUID is missing.");
                return;
            }
            JsonObject player = new JsonObject();
            player.addProperty("player_uuid", result.playerUuid.toString());
            player.addProperty("player_name", result.playerName == null ? "" : result.playerName);
            player.addProperty("brawler_id", result.brawlerId);
            player.addProperty("outcome", result.outcome);
            player.addProperty("kills", result.kills);
            player.addProperty("deaths", result.deaths);
            player.addProperty("damage", result.damage);
            players.add(player);
        }
        payload.add("players", players);

        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .header("x-arena-server-key", key)
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    LOGGER.warning("Cloud match reporting failed with HTTP " + response.statusCode());
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Could not report match to cloud service.", ex);
            }
        });
    }
}
