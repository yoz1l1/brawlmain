package ru.arena.mc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import ru.arena.net.Proto;
import ru.arena.ui.ClientState;

/** Клиентская сторона связи: отправка пакетов, чтение клавиш, звуки и файл друзей. */
public final class ClientNet implements ClientState.Net {
    private Path friendsFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("boevaya_arena_friends.txt");
    }

    private boolean send(Identifier ch, byte[] data, boolean warn) {
        if (!ClientPlayNetworking.canSend(ch)) {
            if (warn) ClientState.I.toast("На сервере не установлен мод «Боевая Арена»");
            return false;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByteArray(data);
        ClientPlayNetworking.send(ch, buf);
        return true;
    }

    @Override public void action(int action, int arg, String str) { send(Ids.ACTION, Proto.writeAction(action, arg, str), true); }

    @Override public void input(float mx, float my, float adx, float ady, int flags) {
        send(Ids.INPUT, Proto.writeInput(mx, my, adx, ady, flags), false);
    }

    @Override
    public boolean keyDown(int code) {
        return InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow().getHandle(), code);
    }

    @Override
    public List<String> online() {
        List<String> names = new ArrayList<>();
        ClientPlayNetworkHandler h = MinecraftClient.getInstance().getNetworkHandler();
        if (h == null) return names;
        for (PlayerListEntry e : h.getPlayerList()) names.add(e.getProfile().getName());
        return names;
    }

    @Override
    public String myName() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player != null) return c.player.getGameProfile().getName();
        return c.getSession().getUsername();
    }

    @Override public void sound(String key) { Sfx.event(key); }

    @Override
    public List<String> loadFriends() {
        List<String> out = new ArrayList<>();
        try {
            Path p = friendsFile();
            if (Files.exists(p)) for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) if (!line.trim().isEmpty()) out.add(line.trim());
        } catch (IOException ignored) { }
        return out;
    }

    @Override
    public void saveFriends(List<String> friends) {
        try { Files.write(friendsFile(), friends, StandardCharsets.UTF_8); } catch (IOException ignored) { }
    }
}
