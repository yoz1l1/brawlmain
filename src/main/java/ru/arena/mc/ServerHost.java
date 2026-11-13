package ru.arena.mc;

import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import ru.arena.net.Proto;
import ru.arena.server.Host;

/** Реализация связи комнат с Minecraft-сервером. */
public final class ServerHost implements Host {
    private final MinecraftServer server;

    public ServerHost(MinecraftServer server) { this.server = server; }

    private ServerPlayerEntity player(UUID id) { return server.getPlayerManager().getPlayer(id); }

    private boolean send(UUID to, Identifier channel, byte[] data) {
        ServerPlayerEntity p = player(to);
        if (p == null || !ServerPlayNetworking.canSend(p, channel)) return false;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByteArray(data);
        ServerPlayNetworking.send(p, channel, buf);
        return true;
    }

    @Override public void sendLobby(UUID to, byte[] data) { send(to, Ids.LOBBY, data); }
    @Override public void sendStart(UUID to, byte[] data) { send(to, Ids.START, data); }
    @Override public void sendSnap(UUID to, byte[] data) { send(to, Ids.SNAP, data); }
    @Override public void sendEnd(UUID to) { send(to, Ids.END, new byte[0]); }
    @Override public void sendToast(UUID to, String msg) { send(to, Ids.TOAST, Proto.writeString(msg)); }

    @Override
    public boolean sendInvite(UUID to, String fromName, int roomId) {
        return send(to, Ids.INVITE, Proto.writeInvite(fromName, roomId));
    }

    @Override
    public UUID findPlayer(String name) {
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(name);
        return p == null ? null : p.getUuid();
    }

    @Override
    public String nameOf(UUID id) {
        ServerPlayerEntity p = player(id);
        return p == null ? "?" : p.getGameProfile().getName();
    }

    @Override
    public void setProtected(UUID id, boolean on) {
        ServerPlayerEntity p = player(id);
        if (p != null) p.setInvulnerable(on);
    }

    @Override
    public void recordMatch(UUID matchId, List<Host.MatchResult> results) {
        CloudProfiles.recordMatch(matchId, results);
    }
}
