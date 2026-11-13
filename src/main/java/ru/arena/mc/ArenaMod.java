package ru.arena.mc;

import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.arena.net.Proto;
import ru.arena.server.RoomManager;

/** Серверная (общая) часть мода: комнаты, матчи, приём действий и ввода игроков. */
public final class ArenaMod implements ModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("BoevayaArena");
    public static volatile RoomManager rooms;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> rooms = new RoomManager(new ServerHost(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> rooms = null);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            RoomManager r = rooms;
            if (r == null) return;
            try { r.tick(); } catch (RuntimeException e) { LOG.error("Ошибка в такте арены", e); }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            RoomManager r = rooms;
            if (r != null) {
                try { r.disconnect(handler.player.getUuid()); } catch (RuntimeException e) { LOG.error("Ошибка выхода игрока", e); }
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(Ids.ACTION, (server, player, handler, buf, sender) -> {
            byte[] data = buf.readByteArray();
            UUID id = player.getUuid();
            server.execute(() -> {
                RoomManager r = rooms;
                if (r == null) return;
                try {
                    Proto.Action a = Proto.readAction(data);
                    r.handle(id, a.action, a.arg, a.str);
                } catch (RuntimeException e) { LOG.warn("Плохое действие от игрока", e); }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(Ids.INPUT, (server, player, handler, buf, sender) -> {
            byte[] data = buf.readByteArray();
            UUID id = player.getUuid();
            server.execute(() -> {
                RoomManager r = rooms;
                if (r == null) return;
                try {
                    float[] in = Proto.readInput(data);
                    r.input(id, in[0], in[1], in[2], in[3], (int) in[4]);
                } catch (RuntimeException e) { LOG.warn("Плохой ввод от игрока", e); }
            });
        });
    }
}
