package ru.arena.server;

import java.util.List;
import java.util.UUID;

/** Связь комнат с «внешним миром» (в моде — Minecraft). */
public interface Host {
    final class MatchResult {
        public final UUID playerUuid;
        public final String playerName;
        public final int brawlerId;
        public final String outcome;
        public final int kills, deaths, damage;

        public MatchResult(UUID playerUuid, String playerName, int brawlerId, String outcome,
                           int kills, int deaths, int damage) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.brawlerId = brawlerId;
            this.outcome = outcome;
            this.kills = kills;
            this.deaths = deaths;
            this.damage = damage;
        }
    }

    void sendLobby(UUID to, byte[] data);
    void sendStart(UUID to, byte[] data);
    void sendSnap(UUID to, byte[] data);
    void sendEnd(UUID to);
    void sendToast(UUID to, String msg);
    /** @return false, если игрок не в сети или у него нет мода. */
    boolean sendInvite(UUID to, String fromName, int roomId);
    UUID findPlayer(String name);
    String nameOf(UUID id);
    /** Защита игрока в мире Minecraft на время матча. */
    void setProtected(UUID id, boolean on);
    /** Отправляет итог завершённого матча в облако с доверенной серверной стороны. */
    void recordMatch(UUID matchId, List<MatchResult> results);
}
