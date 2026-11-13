package ru.arena.mc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import ru.arena.net.Proto;
import ru.arena.ui.ClientState;

/** Клиентская часть: клавиша открытия меню и приём пакетов от сервера. */
public final class ArenaClient implements ClientModInitializer {
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.arenamod.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.arenamod"));
        ClientState.I.net = new ClientNet();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null && client.player != null) client.setScreen(new ArenaScreen());
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            java.util.UUID accountUuid = client.getSession().getUuidOrNull();
            String uuid = accountUuid == null ? null : accountUuid.toString();
            CloudProfileClient.load(uuid);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientState.I.reset());

        ClientPlayNetworking.registerGlobalReceiver(Ids.LOBBY, (client, handler, buf, sender) -> {
            byte[] d = buf.readByteArray();
            client.execute(() -> ClientState.I.onLobby(d));
        });

        ClientPlayNetworking.registerGlobalReceiver(Ids.INVITE, (client, handler, buf, sender) -> {
            byte[] d = buf.readByteArray();
            client.execute(() -> {
                ClientState.I.onInvite(d);
                Object[] iv = Proto.readInvite(d);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§6[Боевая Арена] §f" + iv[0] + " зовёт вас в бой! Откройте меню мода (клавиша B по умолчанию), чтобы принять приглашение."), false);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Ids.START, (client, handler, buf, sender) -> {
            byte[] d = buf.readByteArray();
            client.execute(() -> {
                ClientState.I.onStart(d);
                if (client.currentScreen == null && client.player != null) client.setScreen(new ArenaScreen());
                else if (!(client.currentScreen instanceof ArenaScreen) && client.player != null) {
                    client.player.sendMessage(Text.literal("§6[Боевая Арена] §fБой начался! Нажмите B, чтобы вернуться в игру."), false);
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(Ids.SNAP, (client, handler, buf, sender) -> {
            byte[] d = buf.readByteArray();
            client.execute(() -> ClientState.I.onSnap(d));
        });

        ClientPlayNetworking.registerGlobalReceiver(Ids.END, (client, handler, buf, sender) -> {
            buf.readByteArray();
            client.execute(() -> ClientState.I.onEnd());
        });

        ClientPlayNetworking.registerGlobalReceiver(Ids.TOAST, (client, handler, buf, sender) -> {
            byte[] d = buf.readByteArray();
            client.execute(() -> ClientState.I.toast(Proto.readString(d)));
        });
    }
}
