package ru.arena.server;

import java.util.UUID;

/** Связь комнат с «внешним миром» (в моде — Minecraft). */
public interface Host {
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
}
