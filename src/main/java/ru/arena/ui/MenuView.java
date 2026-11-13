package ru.arena.ui;

import java.util.ArrayList;
import java.util.List;
import ru.arena.game.BrawlerDef;
import ru.arena.game.Brawlers;
import ru.arena.game.GameMap;
import ru.arena.game.Snap;
import ru.arena.game.Zone;
import ru.arena.net.Proto;

/** Главное меню: выбор бойца с просмотром способностей, комната, приглашения и друзья. */
public final class MenuView extends BaseView {
    private static final int GOLD = 0xFFFFC21F, BLUE = 0xFF2D6BD0, GREEN = 0xFF2FBF5B, RED = 0xFFC94444;

    private final FxRenderer fx = new FxRenderer();
    private final Field idField = new Field(4, true);
    private final Field nickField = new Field(16, false);
    private int tab;               // 0 — боец, 1 — комната
    private int facing = 1;
    private float previewT;

    private static final class Fake { float x, y, vx, vy, ang, life, total; int style; boolean lob; float tx; }
    private final List<Fake> fakes = new ArrayList<>();
    private final List<Snap.Z> fakeZones = new ArrayList<>();
    private final List<Float> zoneLife = new ArrayList<>();

    public MenuView() {
        fx.sfx = k -> { if (st.net != null) st.net.sound(k); };
    }

    @Override
    protected void onEnter(Field f) {
        if (f == idField) joinById();
        else if (f == nickField) invite(nickField.text);
    }

    private void joinById() {
        if (idField.text.length() < 4) { st.toast("ID комнаты — 4 цифры"); return; }
        st.net.action(Proto.A_JOIN, Integer.parseInt(idField.text), "");
    }

    private void invite(String nick) {
        if (nick.trim().isEmpty()) { st.toast("Введите ник игрока"); return; }
        st.net.action(Proto.A_INVITE, 0, nick.trim());
    }

    @Override
    public void render(Gfx g, int w, int h, int mx, int my, float dt) {
        st.ensureFriends();
        time += dt; previewT += dt;
        offX = (w / Math.min(w / 480f, h / 270f) - 480) / 2f;
        offY = (h / Math.min(w / 480f, h / 270f) - 270) / 2f;
        layout(w, h, mx, my);
        btns.clear();
        updatePreview(dt);

        // Фон
        for (int i = 0; i < 27; i++) {
            int c = D.mix(0xFF1A1F5C, 0xFF0B0F2A, i / 26f);
            g.fill(0, (int) (i * h / 27f), w, (int) ((i + 1) * h / 27f) + 1, c);
        }
        g.push();
        g.scale(s, s);
        for (int i = -6; i < 40; i++) D.line(g, i * 30 + (time * 6) % 30, -10, i * 30 + 300 + (time * 6) % 30, vh + 10, 9, 0x0AFFFFFF);
        g.translate(offX, offY);

        BrawlerDef def = Brawlers.get(st.selected);
        topBar(g);
        brawlerList(g);
        stage(g, def);
        rightPanel(g, def);
        playButton(g);
        overlays(g, 240, 32, 270);
        g.pop();
    }

    // ---------------------------------------------------------------- верх

    private void topBar(Gfx g) {
        D.rect(g, -offX - 2, -offY - 2, vw, 28, 0xDD0B0F2A);
        D.rect(g, -offX - 2, 28, vw, 29, 0xFF3B4690);
        D.big(g, "БОЕВАЯ АРЕНА", 74, 8, 1.55f, GOLD, 0xFF14102A);
        Proto.Lobby l = st.lobby;
        g.text("ID:", 250, 11, 0xFFD6E4FF, false);
        field(g, idField, 266, 6, 52, 16, "0000");
        button(g, "Войти", 322, 6, 46, 16, BLUE, true, this::joinById);
        if (l.inRoom) button(g, "Покинуть", 372, 6, 100, 16, RED, true, () -> st.net.action(Proto.A_LEAVE, 0, ""));
        else button(g, "Создать комнату", 372, 6, 100, 16, GREEN, true, () -> { st.net.action(Proto.A_CREATE, 0, ""); tab = 1; });
    }

    // ------------------------------------------------------- список бойцов

    private void brawlerList(Gfx g) {
        for (int i = 0; i < Brawlers.ALL.length; i++) {
            BrawlerDef d = Brawlers.get(i);
            float x = 8, y = 34 + i * 29;
            boolean sel = st.selected == i;
            boolean hv = hover(x, y, x + 122, y + 27);
            if (sel) D.rect(g, x - 2, y - 2, x + 124, y + 29, GOLD);
            D.panel(g, x, y, x + 122, y + 27, sel ? D.mix(d.cMain, 0xFFFFFFFF, 0.15f) : (hv ? 0xFF3A4590 : 0xFF2B3566));
            D.rect(g, x + 2, y + 2, x + 28, y + 24, D.alpha(0xFF0B0F2A, 0.45f));
            BrawlerArt.icon(g, i, x + 15, y + 12, 0.95f);
            D.outlined(g, d.name, x + 34, y + 5, 0xFFFFFFFF, 0xFF14102A);
            g.text(d.role, x + 34, y + 16, sel ? 0xFFFFFFFF : 0xFFB8C2F0, false);
            final int id = i;
            hit(x, y, x + 122, y + 27, () -> select(id));
        }
    }

    private void select(int id) {
        st.selected = id;
        fakes.clear(); fakeZones.clear(); zoneLife.clear();
        if (st.lobby.inRoom && !st.lobby.inMatch) st.net.action(Proto.A_SELECT, id, "");
    }

    // ----------------------------------------------------------- сцена

    private void stage(Gfx g, BrawlerDef d) {
        float cx = 240;
        D.oval(g, cx, 140, 92, 76, D.alpha(d.cMain, 0.16f));
        D.oval(g, cx, 140, 62, 54, D.alpha(d.cGlow, 0.12f));
        D.big(g, d.name.toUpperCase(), cx, 34, 2.1f, 0xFFFFFFFF, 0xFF14102A);
        D.ctext(g, d.role, cx, 56, d.cGlow);
        // платформа
        D.oval(g, cx, 196, 58, 13, 0xFF0B0F2A);
        D.oval(g, cx, 194, 56, 12, D.mix(d.cMain, 0xFF1B2244, 0.55f));
        D.oval(g, cx, 192, 44, 8.5f, D.mix(d.cMain, 0xFF1B2244, 0.25f));
        D.ring(g, cx, 192, 44, 1.2f, D.alpha(d.cGlow, 0.9f));
        // эффекты на земле и снаряды под бойцом
        for (Snap.Z z : fakeZones) fx.drawZone(g, z, cx, 192, 36f, time);
        BrawlerArt.draw(g, d.id, cx, 192, 3.1f, facing, time, false, facing > 0 ? 0.15f : 3.0f, 1f);
        for (Fake f : fakes) FxRenderer.drawProjectile(g, f.style, cx + f.x * 36, 150 + f.y * 36, f.ang, f.lob ? 1f - f.life / f.total : -1f, time, 1.5f);
        fx.draw(g, cx, 150, 36f);

        // кнопки показа способностей
        float by = 204;
        button(g, "Атака", cx - 96, by, 60, 15, 0xFFE8A82A, true, () -> playAttack(d));
        button(g, "Рывок", cx - 30, by, 60, 15, 0xFF3CA0FF, true, () -> playDash(d));
        button(g, "Супер", cx + 36, by, 60, 15, 0xFFC94FD6, true, () -> playSuper(d));
        hit(cx - 60, 80, cx + 60, 192, () -> facing = -facing);
        D.ctext(g, "Нажмите на бойца, чтобы повернуть", cx, 222, 0xFF8A93C8);
    }

    // ------------------------------------------------- предпросмотр эффектов

    private void playAttack(BrawlerDef d) {
        float dx = facing;
        switch (d.atk) {
            case SPREAD:
                for (int i = 0; i < d.pellets; i++) spawnFake(d.projStyle, (i - (d.pellets - 1) / 2f) * 0.12f, dx, 11, 2.6f);
                break;
            case BOLT: case PIERCE: case HEAL: spawnFake(d.projStyle, 0, dx, d.atk == BrawlerDef.Atk.PIERCE ? 16 : 10, 3.2f); break;
            case LOB: {
                Fake f = new Fake(); f.style = 6; f.x = 0; f.y = 0.4f; f.lob = true; f.life = f.total = 0.8f; f.vx = dx * 3f / 0.8f; f.ang = 0;
                fakes.add(f);
                break;
            }
            case CHAIN: {
                float px = dx * 0.4f, py = -0.4f;
                for (int i = 1; i <= 3; i++) {
                    float nx = dx * (0.9f * i + 0.4f), ny = -0.4f + (i % 2 == 0 ? 0.35f : -0.35f);
                    fx.onEvent(ev(Snap.E_BEAM, d.id, px, py, nx, ny));
                    fx.onEvent(ev(0, 9, nx, ny, 0, 0));
                    px = nx; py = ny;
                }
                break;
            }
            default:
                fx.onEvent(ev(4, d.id, 0, -0.4f, facing > 0 ? 0f : (float) Math.PI, d.atkRange));
                fx.onEvent(ev(0, d.projStyle, dx * 1.6f, -0.4f, 0, 0));
                break;
        }
    }

    private void playDash(BrawlerDef d) {
        fx.onEvent(ev(2, d.dashStyle, 0, -0.2f, facing * Math.min(3.2f, d.dashDist), -0.2f));
    }

    private void playSuper(BrawlerDef d) {
        float dx = facing;
        fx.onEvent(ev(5, d.id, 0, -0.4f, 0, 0));
        switch (d.sup) {
            case BLAST:
                for (int i = 0; i < d.supCount; i++) spawnFake(7, (i - (d.supCount - 1) / 2f) * 0.08f, dx, 11, 2.6f);
                break;
            case ROCKETS:
                for (int i = 0; i < d.supCount; i++) spawnFake(5, (i - (d.supCount - 1) / 2f) * 0.07f, dx, 10, 3f);
                break;
            case METEOR: addZone(Zone.FIRE, dx * 2.6f, -0.2f, d.supRadius * 0.7f, 3f); break;
            case WAVE: spawnFake(8, 0, dx, 10, 3.4f); break;
            case STORM: addZone(Zone.SHOCK, 0, -0.2f, d.supRadius * 0.8f, 3f); break;
            case DOME: addZone(Zone.HEAL, dx * 2f, -0.2f, d.supRadius * 0.8f, 3f); break;
            case SLAM:
                fx.onEvent(ev(2, 7, 0, -0.2f, dx * 2.4f, -0.2f));
                fx.onEvent(ev(1, 4, dx * 2.4f, -0.2f, d.supRadius * 0.8f, 0));
                break;
            default:
                fx.onEvent(ev(2, 5, 0, -0.2f, dx * 2.2f, -0.2f));
                fx.onEvent(ev(4, 5, dx * 2.2f, -0.4f, facing > 0 ? 0f : (float) Math.PI, 2.2f));
                break;
        }
    }

    private static Snap.E ev(int type, int style, float x, float y, float x2, float y2) {
        Snap.E e = new Snap.E();
        e.type = type; e.style = style; e.x = x; e.y = y; e.x2 = x2; e.y2 = y2;
        return e;
    }

    private void spawnFake(int style, float angOff, float dx, float speed, float range) {
        Fake f = new Fake();
        f.style = style; f.x = dx * 0.4f; f.y = -0.4f;
        float a = (dx > 0 ? 0f : (float) Math.PI) + angOff * dx;
        f.ang = a; f.vx = (float) Math.cos(a) * speed; f.vy = (float) Math.sin(a) * speed;
        f.life = f.total = range / speed * 1.0f;
        fakes.add(f);
    }

    private void addZone(int kind, float x, float y, float r, float life) {
        Snap.Z z = new Snap.Z();
        z.id = fakeZones.size() + 1; z.kind = kind; z.x = x; z.y = y; z.r = r; z.frac = 1f;
        fakeZones.add(z); zoneLife.add(life);
    }

    private void updatePreview(float dt) {
        fx.update(dt);
        for (int i = fakes.size() - 1; i >= 0; i--) {
            Fake f = fakes.get(i);
            f.life -= dt;
            f.x += f.vx * dt; f.y += f.vy * dt;
            fx.trail(f.style, f.x, f.y - (f.lob ? (float) Math.sin(Math.PI * (1 - f.life / f.total)) * 0.6f : 0), f.ang, dt);
            if (f.life <= 0) {
                if (f.lob) fx.onEvent(ev(1, 6, f.x, f.y, 1.5f, 0)); else fx.onEvent(ev(0, f.style, f.x, f.y, 0, 0));
                fakes.remove(i);
            }
        }
        for (int i = fakeZones.size() - 1; i >= 0; i--) {
            float life = zoneLife.get(i) - dt;
            Snap.Z z = fakeZones.get(i);
            fx.ambient(z, dt);
            z.frac = Math.min(1f, life / 3f * 3f);
            if (life <= 0) { fakeZones.remove(i); zoneLife.remove(i); }
            else zoneLife.set(i, life);
        }
    }

    // --------------------------------------------------------- правая панель

    private void rightPanel(Gfx g, BrawlerDef d) {
        float x = 352, w = 120;
        D.panel(g, x, 34, x + 56, 48, tab == 0 ? 0xFF2D6BD0 : 0xFF2B3566);
        D.panel(g, x + 60, 34, x + w, 48, tab == 1 ? 0xFF2D6BD0 : 0xFF2B3566);
        D.ctext(g, "БОЕЦ", x + 28, 38, 0xFFFFFFFF);
        D.ctext(g, "КОМНАТА", x + 90, 38, 0xFFFFFFFF);
        hit(x, 34, x + 56, 48, () -> tab = 0);
        hit(x + 60, 34, x + w, 48, () -> tab = 1);
        D.rect(g, x, 50, x + w, 266, 0xCC141A44);
        if (tab == 0) fighterTab(g, d, x + 4, 54); else roomTab(g, x + 4, 54);
    }

    private void bar(Gfx g, String label, float frac, int col, float x, float y) {
        g.text(label, x, y, 0xFFB8C2F0, false);
        D.rect(g, x + 46, y, x + 112, y + 6, 0xFF0B0F2A);
        D.rect(g, x + 47, y + 1, x + 47 + 64 * Math.max(0.05f, Math.min(1f, frac)), y + 5, col);
    }

    private void fighterTab(Gfx g, BrawlerDef d, float x, float y) {
        bar(g, "Здоровье", d.hp / 9500f, 0xFF3CDC6A, x, y);
        bar(g, "Скорость", d.speed / 4.6f, 0xFF3CA0FF, x, y + 10);
        bar(g, "Урон", d.burst() / 2300f, 0xFFFF6A4C, x, y + 20);
        bar(g, "Дальность", d.atkRange / 11f, 0xFFFFC21F, x, y + 30);
        float yy = y + 43;
        yy = ability(g, "АТАКА", d.atkName, d.atkDesc, 0xFFE8A82A, x, yy);
        yy = ability(g, "РЫВОК", d.dashName, d.dashDesc, 0xFF3CA0FF, x, yy);
        ability(g, "СУПЕР", d.supName, d.supDesc, 0xFFC94FD6, x, yy);
    }

    private float ability(Gfx g, String tag, String name, String desc, int col, float x, float y) {
        D.panel(g, x, y, x + 30, y + 10, col);
        D.scaled(g, tag, x + 3, y + 1.5f, 0.8f, 0xFFFFFFFF, false);
        D.outlined(g, name, x + 34, y + 1, 0xFFFFFFFF, 0xFF14102A);
        float yy = y + 12;
        for (String line : D.wrap(g, desc, 110)) { g.text(line, x, yy, 0xFFC8D0F5, false); yy += 9; }
        return yy + 3;
    }

    private void roomTab(Gfx g, float x, float y) {
        Proto.Lobby l = st.lobby;
        if (!l.inRoom) {
            D.ctext(g, "Вы не в комнате", x + 56, y + 2, 0xFFFFFFFF);
            float yy = y + 14;
            for (String line : D.wrap(g, "Создайте комнату и пригласите друзей по нику или дайте им её ID. Можно и просто сыграть с ботами.", 112)) { g.text(line, x, yy, 0xFFC8D0F5, false); yy += 9; }
            button(g, "Создать комнату", x, yy + 6, 112, 16, GREEN, true, () -> st.net.action(Proto.A_CREATE, 0, ""));
            friends(g, x, yy + 32);
            return;
        }
        D.big(g, "КОМНАТА #" + l.roomId, x + 56, y, 1.1f, GOLD, 0xFF14102A);
        boolean host = l.host.equals(st.net.myName());
        float yy = y + 14;
        for (int i = 0; i < l.names.size(); i++) {
            boolean isHost = l.names.get(i).equals(l.host);
            D.rect(g, x, yy, x + 112, yy + 11, D.alpha(0xFFFFFFFF, i % 2 == 0 ? 0.08f : 0.03f));
            BrawlerArt.icon(g, l.brawlers.get(i), x + 6, yy + 5, 0.32f);
            g.text((isHost ? "* " : "") + l.names.get(i), x + 14, yy + 2, isHost ? GOLD : 0xFFFFFFFF, false);
            String bn = Brawlers.get(l.brawlers.get(i)).name;
            g.text(bn, x + 112 - g.textWidth(bn) - 2, yy + 2, 0xFF9FB0F0, false);
            yy += 12;
        }
        yy = y + 14 + 6 * 12 + 2;
        g.text("Карта:", x, yy + 3, 0xFFB8C2F0, false);
        yy += 0;
        button(g, "<", x + 36, yy, 11, 12, 0xFF2B3566, host && !l.inMatch, () -> st.net.action(Proto.A_MAP, l.map - 1, ""));
        button(g, ">", x + 101, yy, 11, 12, 0xFF2B3566, host && !l.inMatch, () -> st.net.action(Proto.A_MAP, l.map + 1, ""));
        String mn = GameMap.NAMES[Math.max(0, Math.min(GameMap.COUNT - 1, l.map))];
        D.scaled(g, mn, x + 49 + (50 - g.textWidth(mn) * 0.8f) / 2f, yy + 3, 0.8f, 0xFFFFFFFF, true);
        yy += 15;
        button(g, l.bots ? "Боты: вкл" : "Боты: выкл", x, yy, 112, 13, l.bots ? GREEN : 0xFF59607A, host && !l.inMatch, () -> st.net.action(Proto.A_BOTS, 0, ""));
        yy += 17;
        field(g, nickField, x, yy, 112, 14, "ник друга");
        yy += 16;
        button(g, "Пригласить", x, yy, 74, 13, BLUE, true, () -> invite(nickField.text));
        button(g, "+друг", x + 76, yy, 36, 13, 0xFF8A4FD6, true, () -> { st.addFriend(nickField.text); nickField.text = ""; });
        friends(g, x, yy + 17);
    }

    private void friends(Gfx g, float x, float y) {
        if (st.friends.isEmpty()) return;
        g.text("Друзья:", x, y, 0xFFB8C2F0, false);
        List<String> online = st.net.online();
        float yy = y + 10;
        int n = 0;
        for (String f : new ArrayList<>(st.friends)) {
            if (n++ >= 4) break;
            boolean on = false;
            for (String o : online) if (o.equalsIgnoreCase(f)) on = true;
            D.circle(g, x + 3, yy + 4, 2.2f, on ? 0xFF3CDC6A : 0xFF59607A);
            g.text(f, x + 9, yy + 1, on ? 0xFFFFFFFF : 0xFF8A93C8, false);
            button(g, "+", x + 88, yy, 11, 10, BLUE, on, () -> invite(f));
            button(g, "x", x + 101, yy, 11, 10, RED, true, () -> st.removeFriend(f));
            yy += 12;
        }
    }

    // --------------------------------------------------------------- Играть

    private void playButton(Gfx g) {
        Proto.Lobby l = st.lobby;
        float x = 176, y = 232, w = 128, h = 30;
        String label;
        boolean enabled = true;
        Runnable act;
        if (!l.inRoom) { label = "ИГРАТЬ"; act = () -> st.net.action(Proto.A_PRACTICE, 0, ""); }
        else if (l.inMatch) { label = "Идёт бой..."; enabled = false; act = null; }
        else if (l.host.equals(st.net.myName())) { label = "НАЧАТЬ БОЙ"; act = () -> st.net.action(Proto.A_START, 0, ""); }
        else { label = "Ждём хозяина..."; enabled = false; act = null; }
        boolean hv = enabled && hover(x, y, x + w, y + h);
        int base = enabled ? (hv ? 0xFFFFD84D : GOLD) : 0xFF59607A;
        D.panel(g, x, y, x + w, y + h, base);
        float pulse = enabled ? 1f + (float) Math.sin(time * 4) * 0.03f : 1f;
        D.big(g, label, x + w / 2, y + 9, 1.6f * pulse, 0xFFFFFFFF, 0xFF3A2A00);
        if (enabled) hit(x, y, x + w, y + h, act);
        if (!l.inRoom) D.ctext(g, "бой 3 на 3 с ботами", 240, y + h + 3, 0xFF8A93C8);
    }
}
