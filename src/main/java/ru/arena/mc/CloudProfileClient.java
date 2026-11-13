package ru.arena.mc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import ru.arena.ui.ClientState;

/** Загружает публичную статистику игрока из облака. Секретные ключи здесь не используются. */
public final class CloudProfileClient {
    private static final String API = "https://feyxldpkwomajbykwysz.supabase.co/functions/v1/arena-api";
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private CloudProfileClient() {}

    public static void load(String uuidText) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (uuidText == null || uuidText.isBlank()) {
            client.execute(() -> ClientState.I.setCloudStatus(null, "UUID недоступен"));
            return;
        }

        final String uuid;
        try {
            uuid = UUID.fromString(uuidText).toString();
        } catch (IllegalArgumentException ex) {
            client.execute(() -> ClientState.I.setCloudStatus(null, "неверный UUID"));
            return;
        }

        client.execute(() -> ClientState.I.setCloudStatus(uuid, "загрузка"));
        CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API + "?player_uuid=" + uuid))
                        .timeout(Duration.ofSeconds(12))
                        .GET()
                        .build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("HTTP " + response.statusCode());
                }

                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonObject profile = root.getAsJsonObject("profile");
                int trophies = profile.get("trophies").getAsInt();
                int wins = profile.get("wins").getAsInt();
                int losses = profile.get("losses").getAsInt();
                int draws = profile.get("draws").getAsInt();

                client.execute(() -> ClientState.I.setCloudProfile(uuid, trophies, wins, losses, draws));
            } catch (Exception ex) {
                client.execute(() -> ClientState.I.setCloudStatus(uuid, "ошибка загрузки"));
            }
        });
    }
}
