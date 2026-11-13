package ru.arena.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import ru.arena.game.BrawlerDef;
import ru.arena.game.Brawlers;
import ru.arena.game.Fighter;
import ru.arena.game.GameMap;
import ru.arena.game.Match;
import ru.arena.game.Snap;
import ru.arena.net.Proto;

/** Комнаты, лобби и запуск матчей. Всё, что нужно от Minecraft, приходит через {@link Host}. */
public final class RoomManager {
    public static final int MAX_PLAYERS = 6;
    private static final String[] BOT_NAMES = {"Бот Гоша", "Бот Ника", "Бот Рекс", "Бот Лиса", "Бот Тигр", "Бот Дым"};

    public static final class Room {
        public int id;
        public UUID host;
        public final LinkedHashMap<UUID, Integer> brawler = new LinkedHashMap<>();
        public final LinkedHashMap<UUID, Integer> teams = new LinkedHashMap<>();
        public int map;
        public boolean bots = true;
        public Match match;
        public final Map<UUID, Integer> fid = new HashMap<>();
    }

    private final Host host;
    private final Random rnd = new Random();
    private final Map<Integer, Room> rooms = new HashMap<>();
    private final Map<UUID, Room> byPlayer = new HashMap<>();

    public RoomManager(Host host) { this.host = host; }

    public Room roomOf(UUID p) { return byPlayer.get(p); }

    // ------------------------------------------------------------ действия

    public void handle(UUID p, int action, int arg, String str) {
        switch (action) {
            case Proto.A_CREATE: leave(p, false); create(p); break;
            case Proto.A_JOIN: join(p, arg); break;
            case Proto.A_LEAVE: leave(p, true); break;
            case Proto.A_SELECT: select(p, arg); break;
            case Proto.A_INVITE: invite(p, str); break;
            case Proto.A_START: start(p); break;
            case Proto.A_PRACTICE: practice(p); break;
            case Proto.A_MAP: {
                Room r = byPlayer.get(p);
                if (r != null && r.host.equals(p) && r.match == null) { r.map = ((arg % GameMap.COUNT) + GameMap.COUNT) % GameMap.COUNT; broadcastLobby(r); }
                break;
            }
            case Proto.A_BOTS: {
                Room r = byPlayer.get(p);
                if (r != null && r.host.equals(p) && r.match == null) { r.bots = !r.bots; broadcastLobby(r); }
                break;
            }
            case Proto.A_SYNC: sendLobby(p); break;
            case Proto.A_TEAM: switchTeam(p); break;
            default: break;
        }
    }

    private Room create(UUID p) {
        Room r = new Room();
        do { r.id = 1000 + rnd.nextInt(9000); } while (rooms.containsKey(r.id));
        r.host = p;
        r.brawler.put(p, 0);
        r.teams.put(p, 0);
        r.map = rnd.nextInt(GameMap.COUNT);
        rooms.put(r.id, r);
        byPlayer.put(p, r);
        broadcastLobby(r);
        return r;
    }

    private void join(UUID p, int id) {
        Room cur = byPlayer.get(p);
        if (cur != null && cur.id == id) { sendLobby(p); return; }
        Room r = rooms.get(id);
        if (r == null) { host.sendToast(p, "Комната #" + id + " не найдена"); return; }
        if (r.match != null) { host.sendToast(p, "В этой комнате уже идёт матч"); return; }
        if (r.brawler.size() >= MAX_PLAYERS) { host.sendToast(p, "Комната заполнена"); return; }
        leave(p, false);
        int[] counts = teamCounts(r);
        int team = counts[0] <= counts[1] ? 0 : 1;
        if (counts[team] >= 3) team = 1 - team;
        if (counts[team] >= 3) { host.sendToast(p, "Обе команды заполнены"); return; }
        r.brawler.put(p, 0);
        r.teams.put(p, team);
        byPlayer.put(p, r);
        broadcastLobby(r);
        for (UUID u : r.brawler.keySet()) if (!u.equals(p)) host.sendToast(u, host.nameOf(p) + " зашёл в комнату");
    }

    public void leave(UUID p, boolean notify) {
        Room r = byPlayer.remove(p);
        if (r == null) { if (notify) sendLobby(p); return; }
        r.brawler.remove(p);
        r.teams.remove(p);
        Integer id = r.fid.remove(p);
        if (r.match != null) {
            host.setProtected(p, false);
            if (id != null) r.match.fighters.get(id).bot = true;
            host.sendEnd(p);
        }
        if (r.brawler.isEmpty()) { rooms.remove(r.id); }
        else {
            if (r.host.equals(p)) r.host = r.brawler.keySet().iterator().next();
            broadcastLobby(r);
        }
        sendLobby(p);
    }

    public void disconnect(UUID p) {
        Room r = byPlayer.get(p);
        if (r == null) return;
        leave(p, false);
    }

    private void select(UUID p, int b) {
        Room r = byPlayer.get(p);
        if (r == null || r.match != null) return;
        int count = Brawlers.ALL.length;
        r.brawler.put(p, ((b % count) + count) % count);
        broadcastLobby(r);
    }

    private int[] teamCounts(Room r) {
        int[] counts = {0, 0};
        for (Integer team : r.teams.values()) if (team != null && team >= 0 && team <= 1) counts[team]++;
        return counts;
    }

    private void switchTeam(UUID p) {
        Room r = byPlayer.get(p);
        if (r == null || r.match != null) return;
        Integer current = r.teams.get(p);
        if (current == null) return;
        int other = 1 - current;
        int[] counts = teamCounts(r);
        if (counts[other] >= 3) { host.sendToast(p, "В другой команде уже 3 игрока"); return; }
        r.teams.put(p, other);
        broadcastLobby(r);
    }

    private void invite(UUID p, String nick) {
        Room r = byPlayer.get(p);
        if (r == null) { host.sendToast(p, "Сначала создайте комнату"); return; }
        if (nick == null || nick.trim().isEmpty()) return;
        UUID target = host.findPlayer(nick.trim());
        if (target == null) { host.sendToast(p, "Игрок " + nick.trim() + " не в сети"); return; }
        if (target.equals(p)) { host.sendToast(p, "Это вы сами :)"); return; }
        if (!host.sendInvite(target, host.nameOf(p), r.id)) { host.sendToast(p, "У игрока " + nick.trim() + " не установлен мод"); return; }
        host.sendToast(p, "Приглашение отправлено: " + host.nameOf(target));
    }

    private void practice(UUID p) {
        Room r = byPlayer.get(p);
        if (r == null) r = create(p);
        if (r.match != null) return;
        if (!r.host.equals(p)) { host.sendToast(p, "Начать бой может только хозяин комнаты"); return; }
        r.bots = true;
        startMatch(r);
    }

    private void start(UUID p) {
        Room r = byPlayer.get(p);
        if (r == null || r.match != null) return;
        if (!r.host.equals(p)) { host.sendToast(p, "Начать бой может только хозяин комнаты"); return; }
        int[] counts = teamCounts(r);
        if (!r.bots && (counts[0] == 0 || counts[1] == 0)) { host.sendToast(p, "Для боя без ботов нужны игроки в обеих командах"); return; }
        startMatch(r);
    }

    private void startMatch(Room r) {
        List<Match.Slot> slots = new ArrayList<>();
        List<UUID> humans = new ArrayList<>(r.brawler.keySet());
        int[] teamCount = {0, 0};
        for (int i = 0; i < humans.size(); i++) {
            UUID u = humans.get(i);
            int team = r.teams.getOrDefault(u, i % 2);
            teamCount[team]++;
            slots.add(new Match.Slot(host.nameOf(u), team, r.brawler.get(u), false));
        }
        if (r.bots) {
            int bi = rnd.nextInt(BOT_NAMES.length);
            for (int team = 0; team < 2; team++) {
                while (teamCount[team] < 3) {
                    slots.add(new Match.Slot(BOT_NAMES[bi++ % BOT_NAMES.length], team, rnd.nextInt(8), true));
                    teamCount[team]++;
                }
            }
        }
        Match m = new Match(r.map, rnd.nextLong(), slots);
        r.match = m;
        r.fid.clear();
        for (int i = 0; i < humans.size(); i++) r.fid.put(humans.get(i), i);
        for (UUID u : humans) {
            host.setProtected(u, true);
            Proto.Start st = new Proto.Start();
            st.map = r.map; st.myId = r.fid.get(u);
            for (Fighter f : m.fighters) {
                Proto.Meta mt = new Proto.Meta();
                mt.id = f.id; mt.team = f.team; mt.brawler = f.def.id; mt.name = f.name; mt.bot = f.bot;
                st.fighters.add(mt);
            }
            host.sendStart(u, st.write());
        }
        broadcastLobby(r);
    }

    // --------------------------------------------------------------- ввод

    public void input(UUID p, float mx, float my, float adx, float ady, int flags) {
        Room r = byPlayer.get(p);
        if (r == null || r.match == null) return;
        Integer id = r.fid.get(p);
        if (id == null) return;
        Fighter f = r.match.fighters.get(id);
        if (f.bot) return;
        float len = (float) Math.hypot(mx, my);
        if (len > 1f) { mx /= len; my /= len; }
        if (Float.isNaN(mx) || Float.isNaN(my)) { mx = 0; my = 0; }
        f.mx = mx; f.my = my;
        f.aimDx = clamp(adx); f.aimDy = clamp(ady);
        f.attackHeld = (flags & Proto.IN_ATTACK) != 0;
        if ((flags & Proto.IN_SUPER) != 0) f.superPressed = true;
        if ((flags & Proto.IN_DASH) != 0) f.dashPressed = true;
    }

    private static float clamp(float v) {
        if (Float.isNaN(v)) return 0;
        return Math.max(-40f, Math.min(40f, v));
    }

    // ---------------------------------------------------------------- такт

    public void tick() {
        for (Iterator<Room> it = rooms.values().iterator(); it.hasNext(); ) {
            Room r = it.next();
            if (r.match == null) continue;
            r.match.tick();
            byte[][] cache = new byte[2][];
            for (UUID u : r.fid.keySet()) {
                int team = r.match.fighters.get(r.fid.get(u)).team;
                if (cache[team] == null) cache[team] = Snap.write(r.match, team);
                host.sendSnap(u, cache[team]);
            }
            if (r.match.finished()) finishMatch(r);
        }
    }

    private void finishMatch(Room r) {
        for (UUID u : r.fid.keySet()) {
            host.setProtected(u, false);
            host.sendEnd(u);
        }
        r.match = null;
        r.fid.clear();
        broadcastLobby(r);
    }

    // --------------------------------------------------------------- лобби

    private Proto.Lobby lobbyFor(Room r) {
        Proto.Lobby l = new Proto.Lobby();
        l.inRoom = true; l.roomId = r.id; l.map = r.map; l.bots = r.bots; l.inMatch = r.match != null;
        l.host = host.nameOf(r.host);
        for (Map.Entry<UUID, Integer> e : r.brawler.entrySet()) { l.names.add(host.nameOf(e.getKey())); l.brawlers.add(e.getValue()); l.teams.add(r.teams.getOrDefault(e.getKey(), 0)); }
        return l;
    }

    private void broadcastLobby(Room r) {
        byte[] data = lobbyFor(r).write();
        for (UUID u : r.brawler.keySet()) host.sendLobby(u, data);
    }

    private void sendLobby(UUID p) {
        Room r = byPlayer.get(p);
        host.sendLobby(p, r == null ? new Proto.Lobby().write() : lobbyFor(r).write());
    }
}
