package ru.arena.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/** Константы и (де)сериализация пакетов между клиентом и сервером. Не зависит от Minecraft. */
public final class Proto {
    public static final String NS = "arenamod";

    // клиент -> сервер: действия в лобби
    public static final int A_CREATE = 0, A_JOIN = 1, A_LEAVE = 2, A_SELECT = 3, A_INVITE = 4,
            A_START = 5, A_PRACTICE = 6, A_MAP = 7, A_BOTS = 8, A_SYNC = 9, A_TEAM = 10;

    public static final int IN_ATTACK = 1, IN_SUPER = 2, IN_DASH = 4;

    private Proto() {}

    // ---------------- состояние лобби (сервер -> клиент)
    public static final class Lobby {
        public boolean inRoom, inMatch, bots = true;
        public int roomId, map;
        public String host = "";
        public final List<String> names = new ArrayList<>();
        public final List<Integer> brawlers = new ArrayList<>();
        public final List<Integer> teams = new ArrayList<>();

        public byte[] write() {
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                DataOutputStream o = new DataOutputStream(bo);
                o.writeBoolean(inRoom);
                if (inRoom) {
                    o.writeBoolean(inMatch);
                    o.writeBoolean(bots);
                    o.writeInt(roomId);
                    o.writeByte(map);
                    o.writeUTF(host);
                    o.writeByte(names.size());
                    for (int i = 0; i < names.size(); i++) { o.writeUTF(names.get(i)); o.writeByte(brawlers.get(i)); o.writeByte(i < teams.size() ? teams.get(i) : (i % 2)); }
                }
                return bo.toByteArray();
            } catch (IOException e) { throw new UncheckedIOException(e); }
        }

        public static Lobby read(byte[] data) {
            try {
                DataInputStream i = new DataInputStream(new ByteArrayInputStream(data));
                Lobby l = new Lobby();
                l.inRoom = i.readBoolean();
                if (l.inRoom) {
                    l.inMatch = i.readBoolean();
                    l.bots = i.readBoolean();
                    l.roomId = i.readInt();
                    l.map = i.readUnsignedByte();
                    l.host = i.readUTF();
                    int n = i.readUnsignedByte();
                    for (int k = 0; k < n; k++) { l.names.add(i.readUTF()); l.brawlers.add(i.readUnsignedByte()); l.teams.add(i.readUnsignedByte() & 1); }
                }
                return l;
            } catch (IOException e) { throw new UncheckedIOException(e); }
        }
    }

    // ---------------- старт матча (сервер -> клиент)
    public static final class Meta {
        public int id, team, brawler;
        public String name;
        public boolean bot;
    }

    public static final class Start {
        public int map, myId;
        public final List<Meta> fighters = new ArrayList<>();

        public byte[] write() {
            try {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                DataOutputStream o = new DataOutputStream(bo);
                o.writeByte(map);
                o.writeByte(myId);
                o.writeByte(fighters.size());
                for (Meta m : fighters) {
                    o.writeByte(m.id); o.writeByte(m.team); o.writeByte(m.brawler); o.writeBoolean(m.bot); o.writeUTF(m.name);
                }
                return bo.toByteArray();
            } catch (IOException e) { throw new UncheckedIOException(e); }
        }

        public static Start read(byte[] data) {
            try {
                DataInputStream i = new DataInputStream(new ByteArrayInputStream(data));
                Start s = new Start();
                s.map = i.readUnsignedByte();
                s.myId = i.readUnsignedByte();
                int n = i.readUnsignedByte();
                for (int k = 0; k < n; k++) {
                    Meta m = new Meta();
                    m.id = i.readUnsignedByte(); m.team = i.readUnsignedByte(); m.brawler = i.readUnsignedByte();
                    m.bot = i.readBoolean(); m.name = i.readUTF();
                    s.fighters.add(m);
                }
                return s;
            } catch (IOException e) { throw new UncheckedIOException(e); }
        }
    }

    // ---------------- действие лобби (клиент -> сервер)
    public static byte[] writeAction(int action, int arg, String str) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream o = new DataOutputStream(bo);
            o.writeByte(action); o.writeInt(arg); o.writeUTF(str == null ? "" : str);
            return bo.toByteArray();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static final class Action {
        public int action, arg; public String str;
    }

    public static Action readAction(byte[] data) {
        try {
            DataInputStream i = new DataInputStream(new ByteArrayInputStream(data));
            Action a = new Action();
            a.action = i.readUnsignedByte(); a.arg = i.readInt(); a.str = i.readUTF();
            return a;
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    // ---------------- ввод игрока (клиент -> сервер)
    public static byte[] writeInput(float mx, float my, float adx, float ady, int flags) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(24);
            DataOutputStream o = new DataOutputStream(bo);
            o.writeFloat(mx); o.writeFloat(my); o.writeFloat(adx); o.writeFloat(ady); o.writeByte(flags);
            return bo.toByteArray();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static float[] readInput(byte[] data) {
        try {
            DataInputStream i = new DataInputStream(new ByteArrayInputStream(data));
            return new float[]{i.readFloat(), i.readFloat(), i.readFloat(), i.readFloat(), i.readUnsignedByte()};
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static byte[] writeInvite(String from, int roomId) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream o = new DataOutputStream(bo);
            o.writeUTF(from); o.writeInt(roomId);
            return bo.toByteArray();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static Object[] readInvite(byte[] data) {
        try {
            DataInputStream i = new DataInputStream(new ByteArrayInputStream(data));
            return new Object[]{i.readUTF(), i.readInt()};
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static byte[] writeString(String s) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            new DataOutputStream(bo).writeUTF(s);
            return bo.toByteArray();
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }

    public static String readString(byte[] data) {
        try { return new DataInputStream(new ByteArrayInputStream(data)).readUTF(); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
}
