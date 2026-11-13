package ru.arena.ui;

import java.util.ArrayList;
import java.util.List;
import ru.arena.game.GameMap;
import ru.arena.game.Snap;
import ru.arena.net.Proto;

/** Всё, что клиент знает о лобби и матче. Пакеты из Minecraft-слоя попадают сюда. */
public final class ClientState {
    public static final ClientState I = new ClientState();

    /** Мост в Minecraft: отправка пакетов, клавиши, звуки. */
    public interface Net {
        void action(int action, int arg, String str);
        void input(float mx, float my, float adx, float ady, int flags);
        boolean keyDown(int glfwCode);
        List<String> online();
        String myName();
        void sound(String key);
        List<String> loadFriends();
        void saveFriends(List<String> friends);
    }

    public static final class Invite { public String from; public int room; public long expires; }

    public Net net;
    public Proto.Lobby lobby = new Proto.Lobby();
    public final List<Invite> invites = new ArrayList<>();
    public String toast = "";
    public long toastUntil;
    public MatchClient match;
    public int selected = 0;
    public final List<String> friends = new ArrayList<>();

    // Облачный профиль: только чтение публичной статистики.
    public String cloudPlayerUuid = "";
    public String cloudStatus = "не загружен";
    public int cloudTrophies;
    public int cloudWins;
    public int cloudLosses;
    public int cloudDraws;

    public void setCloudProfile(String uuid, int trophies, int wins, int losses, int draws) {
        cloudPlayerUuid = uuid;
        cloudTrophies = trophies;
        cloudWins = wins;
        cloudLosses = losses;
        cloudDraws = draws;
        cloudStatus = "загружен";
    }

    public void setCloudStatus(String uuid, String status) {
        if (uuid == null || uuid.equals(cloudPlayerUuid)) cloudStatus = status;
    }
    private int syncedRoom = -1;
    private boolean friendsLoaded;

    public void ensureFriends() {
        if (friendsLoaded || net == null) return;
        friendsLoaded = true;
        friends.clear();
        friends.addAll(net.loadFriends());
    }

    public void addFriend(String nick) {
        nick = nick.trim();
        if (nick.isEmpty() || friends.contains(nick) || friends.size() >= 20) return;
        friends.add(nick);
        net.saveFriends(friends);
    }

    public void removeFriend(String nick) {
        friends.remove(nick);
        net.saveFriends(friends);
    }

    public void toast(String s) { toast = s; toastUntil = System.currentTimeMillis() + 3500; }

    // ---- вызываются из слоя Minecraft (в основном потоке клиента)

    public void onLobby(byte[] data) {
        lobby = Proto.Lobby.read(data);
        if (lobby.inRoom && !lobby.inMatch) {
            int myIndex = -1;
            if (net != null) {
                String me = net.myName();
                for (int i = 0; i < lobby.names.size(); i++) {
                    if (lobby.names.get(i).equalsIgnoreCase(me)) { myIndex = i; break; }
                }
            }
            boolean entered = lobby.roomId != syncedRoom;
            if (entered) syncedRoom = lobby.roomId;
            boolean serverDiffers = myIndex >= 0 && myIndex < lobby.brawlers.size()
                    && lobby.brawlers.get(myIndex) != selected;
            // Повторно подтверждаем выбор при входе и при устаревшем снимке лобби.
            if (net != null && (entered || serverDiffers)) net.action(Proto.A_SELECT, selected, "");
        }
        if (!lobby.inRoom) syncedRoom = -1;
    }

    public void onInvite(byte[] data) {
        Object[] o = Proto.readInvite(data);
        Invite iv = new Invite();
        iv.from = (String) o[0]; iv.room = (Integer) o[1]; iv.expires = System.currentTimeMillis() + 30000;
        invites.removeIf(x -> x.from.equals(iv.from));
        invites.add(iv);
        if (net != null) net.sound("invite");
    }

    public void onStart(byte[] data) {
        match = new MatchClient(Proto.Start.read(data));
        match.fx.sfx = k -> { if (net != null) net.sound(k); };
    }

    public void onSnap(byte[] data) {
        if (match != null) match.apply(Snap.read(data));
    }

    public void onEnd() { match = null; }

    public void reset() { match = null; lobby = new Proto.Lobby(); invites.clear(); syncedRoom = -1; }

    // ------------------------------------------------------------

    public static final class MatchClient {
        public final GameMap map;
        public final Proto.Start meta;
        public final int myId, myTeam;
        public Snap cur, prev;
        public long curAt = System.nanoTime();
        public final FxRenderer fx = new FxRenderer();
        public long playStart = -1;
        public int lastState = -1;

        public MatchClient(Proto.Start s) {
            meta = s;
            map = new GameMap(s.map);
            myId = s.myId;
            int t = 0;
            for (Proto.Meta m : s.fighters) if (m.id == s.myId) t = m.team;
            myTeam = t;
        }

        public void apply(Snap s) {
            prev = cur; cur = s; curAt = System.nanoTime();
            for (Snap.E e : s.events) fx.onEvent(e);
            if (s.state != lastState) {
                if (s.state == 1) playStart = System.nanoTime();
                lastState = s.state;
            }
        }

        public Proto.Meta metaOf(int id) {
            for (Proto.Meta m : meta.fighters) if (m.id == id) return m;
            return null;
        }

        /** Доля интерполяции между прошлым и текущим снимком. */
        public float lerpT() {
            return Math.max(0f, Math.min(1f, (System.nanoTime() - curAt) / 50_000_000f));
        }

        public Snap.F prevF(int id) {
            if (prev == null) return null;
            for (Snap.F f : prev.fighters) if (f.id == id) return f;
            return null;
        }

        public Snap.P prevP(int id) {
            if (prev == null) return null;
            for (Snap.P p : prev.projs) if (p.id == id) return p;
            return null;
        }
    }
}
