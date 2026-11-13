package ru.arena.ui;

import java.util.Random;
import ru.arena.game.BrawlerDef;
import ru.arena.game.Brawlers;
import ru.arena.game.GameMap;
import ru.arena.game.Match;
import ru.arena.game.Snap;
import ru.arena.net.Proto;
import ru.arena.ui.ClientState.MatchClient;

/** Экран боя: камера, карта, бойцы, эффекты и интерфейс. */
public final class MatchView extends BaseView {
    private static final float TILE = 28f, ART = 0.85f;
    private static final int BLUE = 0xFF3C8CFF, RED = 0xFFFF4C4C, GREEN = 0xFF3CDC6A, GOLD = 0xFFFFD84D;

    private float camX, camY;
    private boolean camInit, mouseHeld, superLatch, dashLatch;
    private float lastAimDx = 1, lastAimDy = 0, psx, psy;
    private final Random rnd = new Random();
    private float shakeX, shakeY;

    private static Snap.F find(Snap s, int id) {
        for (Snap.F f : s.fighters) if (f.id == id) return f;
        return null;
    }

    private float[] pos(MatchClient mc, Snap.F f) {
        Snap.F p = mc.prevF(f.id);
        float t = mc.lerpT();
        if (p == null || (p.flags & Snap.F_HIDDEN) != 0 || (p.flags & Snap.F_ALIVE) == 0) return new float[]{f.x, f.y};
        return new float[]{p.x + (f.x - p.x) * t, p.y + (f.y - p.y) * t};
    }

    private int teamColor(MatchClient mc, int id) {
        if (id == mc.myId) return GREEN;
        Proto.Meta m = mc.metaOf(id);
        return m != null && m.team == mc.myTeam ? BLUE : RED;
    }

    @Override
    public void render(Gfx g, int w, int h, int mx, int my, float dt) {
        offX = 0; offY = 0;
        layout(w, h, mx, my);
        btns.clear();
        time += dt;
        MatchClient mc = st.match;
        g.fill(0, 0, w, h, 0xFF0E1420);
        if (mc == null || mc.cur == null) {
            g.push(); g.scale(s, s);
            D.big(g, "Загрузка боя...", vw / 2, vh / 2 - 6, 1.5f, 0xFFFFFFFF, 0xFF14102A);
            g.pop();
            return;
        }
        mc.fx.update(dt);
        Snap cs = mc.cur;
        GameMap map = mc.map;
        g.push();
        g.scale(s, s);

        Snap.F me = find(cs, mc.myId);
        float[] mp = me != null ? pos(mc, me) : new float[]{GameMap.W / 2f, GameMap.H / 2f};
        float tx = mp[0] * TILE - vw / 2f, ty = mp[1] * TILE - vh / 2f;
        float mw = GameMap.W * TILE, mh = GameMap.H * TILE;
        tx = mw + 48 <= vw ? (mw - vw) / 2 : Math.max(-24, Math.min(mw - vw + 24, tx));
        ty = mh + 48 <= vh ? (mh - vh) / 2 : Math.max(-24, Math.min(mh - vh + 24, ty));
        if (!camInit) { camX = tx; camY = ty; camInit = true; }
        float k = Math.min(1f, dt * 10f);
        camX += (tx - camX) * k; camY += (ty - camY) * k;
        float shake = mc.fx.shake;
        shakeX = (rnd.nextFloat() - 0.5f) * shake * 2f; shakeY = (rnd.nextFloat() - 0.5f) * shake * 2f;
        float ox = -camX + shakeX, oy = -camY + shakeY;
        psx = mp[0] * TILE - camX; psy = mp[1] * TILE - camY - 12;

        int x0 = (int) Math.floor(camX / TILE) - 1, x1 = (int) Math.ceil((camX + vw) / TILE) + 1;
        int y0 = (int) Math.floor(camY / TILE) - 1, y1 = (int) Math.ceil((camY + vh) / TILE) + 2;

        MapRenderer.ground(g, map, ox, oy, TILE, x0, x1, y0, y1, time);
        for (Snap.Z z : cs.zones) { mc.fx.drawZone(g, z, ox, oy, TILE, time); mc.fx.ambient(z, dt); }
        drawAimGuide(g, mc, me, mp, ox, oy);

        float[] bushA = new float[GameMap.W];
        for (int r = Math.max(0, y0); r <= Math.min(GameMap.H - 1, y1); r++) {
            MapRenderer.raised(g, map, r, ox, oy, TILE, x0, x1);
            for (int xx = 0; xx < GameMap.W; xx++) bushA[xx] = 1f;
            for (Snap.F f : cs.fighters) {
                if ((f.flags & Snap.F_ALIVE) == 0 || (f.flags & Snap.F_HIDDEN) != 0) continue;
                float[] p = pos(mc, f);
                if ((int) Math.floor(p[1]) != r) continue;
                drawFighter(g, mc, f, p, ox, oy, dt);
                Proto.Meta m = mc.metaOf(f.id);
                if (m != null && m.team == mc.myTeam && map.bushAt(p[0], p[1])) bushA[(int) p[0]] = 0.6f;
            }
            MapRenderer.bushes(g, map, r, ox, oy, TILE, x0, x1, time, bushA);
        }

        // снаряды
        float lt = mc.lerpT();
        for (Snap.P p : cs.projs) {
            Snap.P pp = mc.prevP(p.id);
            float px = pp == null ? p.x : pp.x + (p.x - pp.x) * lt, py = pp == null ? p.y : pp.y + (p.y - pp.y) * lt;
            FxRenderer.drawProjectile(g, p.style, ox + px * TILE, oy + py * TILE, p.ang, p.prog, time + p.id, 1f);
            float arc = p.prog >= 0 ? (float) Math.sin(Math.PI * p.prog) * 20f / TILE : 0f;
            mc.fx.trail(p.style, px, py - arc, p.ang, dt);
        }
        mc.fx.draw(g, ox, oy, TILE);

        // полосы здоровья поверх всего
        for (Snap.F f : cs.fighters) {
            if ((f.flags & Snap.F_ALIVE) == 0 || (f.flags & Snap.F_HIDDEN) != 0) continue;
            float[] p = pos(mc, f);
            drawBars(g, mc, f, ox + p[0] * TILE, oy + p[1] * TILE);
        }

        hud(g, mc, cs, me);
        overlays(g, vw / 2f, 30, vh);
        g.pop();
    }

    private void drawAimGuide(Gfx g, MatchClient mc, Snap.F me, float[] mp, float ox, float oy) {
        if (me == null || (me.flags & Snap.F_ALIVE) == 0) return;
        Proto.Meta m = mc.metaOf(mc.myId);
        BrawlerDef d = Brawlers.get(m.brawler);
        float sx = ox + mp[0] * TILE, sy = oy + mp[1] * TILE - 10;
        float ang = (float) Math.atan2(lastAimDy, lastAimDx);
        int col = D.alpha(0xFFFFFFFF, mouseHeld ? 0.5f : 0.22f);
        switch (d.atk) {
            case MELEE: {
                float half = (float) Math.toRadians(d.arc) / 2f, r = d.atkRange * TILE;
                for (int i = 0; i < 10; i++) {
                    float a0 = ang - half + 2 * half * i / 10f, a1 = ang - half + 2 * half * (i + 1) / 10f;
                    D.line(g, sx + (float) Math.cos(a0) * r, sy + (float) Math.sin(a0) * r, sx + (float) Math.cos(a1) * r, sy + (float) Math.sin(a1) * r, 2f, col);
                }
                D.line(g, sx, sy, sx + (float) Math.cos(ang - half) * r, sy + (float) Math.sin(ang - half) * r, 1.2f, col);
                D.line(g, sx, sy, sx + (float) Math.cos(ang + half) * r, sy + (float) Math.sin(ang + half) * r, 1.2f, col);
                break;
            }
            case SPREAD: {
                float half = (float) Math.toRadians(d.spread) / 2f, r = d.atkRange * TILE;
                D.line(g, sx, sy, sx + (float) Math.cos(ang - half) * r, sy + (float) Math.sin(ang - half) * r, 1.4f, col);
                D.line(g, sx, sy, sx + (float) Math.cos(ang + half) * r, sy + (float) Math.sin(ang + half) * r, 1.4f, col);
                break;
            }
            case LOB: {
                float dist = Math.max(2f, Math.min(d.atkRange, (float) Math.hypot(lastAimDx, lastAimDy)));
                float ex = sx + (float) Math.cos(ang) * dist * TILE, ey = sy + (float) Math.sin(ang) * dist * TILE;
                D.line(g, sx, sy, ex, ey, 1.4f, col);
                D.ring(g, ex, ey, d.splash * TILE, 1.6f, col);
                break;
            }
            default: {
                float r = d.atkRange * TILE;
                D.line(g, sx, sy, sx + (float) Math.cos(ang) * r, sy + (float) Math.sin(ang) * r, 2f, col);
                break;
            }
        }
    }

    private void drawFighter(Gfx g, MatchClient mc, Snap.F f, float[] p, float ox, float oy, float dt) {
        Proto.Meta m = mc.metaOf(f.id);
        if (m == null) return;
        float sx = ox + p[0] * TILE, sy = oy + p[1] * TILE;
        Snap.F pf = mc.prevF(f.id);
        boolean moving = pf != null && Math.hypot(f.x - pf.x, f.y - pf.y) > 0.01;
        float alpha = 1f;
        boolean ally = m.team == mc.myTeam;
        if ((f.flags & Snap.F_STEALTH) != 0 && ally) alpha = 0.45f;
        if ((f.flags & Snap.F_INVULN) != 0 && (f.flags & Snap.F_DASH) == 0 && ((int) (time * 10) & 1) == 0) alpha = 0.6f;
        if ((f.flags & Snap.F_DASH) != 0) alpha = Math.min(alpha, 0.8f);
        int tc = teamColor(mc, f.id);
        // кольцо команды под ногами
        D.oval(g, sx, sy + 1, 14, 5.5f, D.alpha(tc, 0.75f * alpha));
        D.oval(g, sx, sy + 1, 12, 4.2f, D.alpha(0xFF14102A, 0.55f * alpha));
        if ((f.flags & Snap.F_SLOW) != 0) D.ring(g, sx, sy - 12, 11, 1.5f, 0xAA8FE3FF);
        int dir = Math.cos(f.aim) >= 0 ? 1 : -1;
        BrawlerArt.draw(g, m.brawler, sx, sy, ART, dir, time + f.id * 1.7f, moving, f.aim, alpha);
        if ((f.flags & Snap.F_STUN) != 0) {
            for (int i = 0; i < 3; i++) {
                float a = time * 6 + i * 2.1f;
                D.circle(g, sx + (float) Math.cos(a) * 9, sy - 42 + (float) Math.sin(a) * 3, 1.8f, 0xFFFFE066);
            }
        }
    }

    private void drawBars(Gfx g, MatchClient mc, Snap.F f, float sx, float sy) {
        Proto.Meta m = mc.metaOf(f.id);
        if (m == null) return;
        BrawlerDef d = Brawlers.get(m.brawler);
        int tc = teamColor(mc, f.id);
        float bw = 32, by = sy - 50;
        D.rect(g, sx - bw / 2 - 1, by - 1, sx + bw / 2 + 1, by + 6, 0xFF14102A);
        D.rect(g, sx - bw / 2, by, sx + bw / 2, by + 5, 0xFF3A3F55);
        float frac = Math.max(0, Math.min(1, f.hp / d.hp));
        D.rect(g, sx - bw / 2, by, sx - bw / 2 + bw * frac, by + 5, tc);
        D.rect(g, sx - bw / 2, by, sx - bw / 2 + bw * frac, by + 1.5f, D.light(tc, 0.4f));
        String hp = Integer.toString((int) f.hp);
        D.scaled(g, hp, sx - g.textWidth(hp) * 0.4f, by - 9, 0.8f, 0xFFFFFFFF, true);
        String nm = m.name;
        D.scaled(g, nm, sx - g.textWidth(nm) * 0.35f, by - 17, 0.7f, D.alpha(tc, 1f), true);
        if (f.id == mc.myId) {
            for (int i = 0; i < 3; i++) {
                float fill = Math.max(0, Math.min(1, f.ammo - i));
                float ax = sx - 14 + i * 10;
                D.rect(g, ax - 0.5f, sy + 5.5f, ax + 8.5f, sy + 9.5f, 0xFF14102A);
                D.rect(g, ax, sy + 6, ax + 8, sy + 9, 0xFF3A3F55);
                D.rect(g, ax, sy + 6, ax + 8 * fill, sy + 9, fill >= 1 ? 0xFFFFA43A : 0xFF9A6A3A);
            }
        }
    }

    // --------------------------------------------------------------- HUD

    private void circleBtn(Gfx g, float cx, float cy, float r, float fill, int colFull, int colEmpty, boolean ready, String label, String hint) {
        D.circle(g, cx, cy, r + 2.5f, 0xFF14102A);
        int rows = (int) Math.ceil(r * 2);
        for (int i = 0; i < rows; i++) {
            float y = cy - r + i + 0.5f;
            float dy = (y - cy) / r;
            float hw = r * (float) Math.sqrt(Math.max(0, 1 - dy * dy));
            boolean filled = (1 - (y - (cy - r)) / (2 * r)) <= fill;
            g.fill(Math.round(cx - hw), (int) Math.floor(y), Math.round(cx + hw), (int) Math.floor(y) + 1, filled ? colFull : colEmpty);
        }
        if (ready) D.ring(g, cx, cy, r + 3 + (float) Math.sin(time * 8) * 1.2f, 2f, D.alpha(GOLD, 0.9f));
        D.ctext(g, label, cx, cy - 4, 0xFFFFFFFF);
        if (hint != null) D.scaled(g, hint, cx - g.textWidth(hint) * 0.3f, cy + r + 4, 0.6f, 0xFFD6E4FF, true);
    }

    private void hud(Gfx g, MatchClient mc, Snap cs, Snap.F me) {
        // Табло
        int myScore = mc.myTeam == 0 ? cs.score0 : cs.score1, enScore = mc.myTeam == 0 ? cs.score1 : cs.score0;
        float cx = vw / 2f;
        D.panel(g, cx - 28, 4, cx + 28, 20, 0xFF1B2244);
        int secs = Math.max(0, (int) Math.ceil(cs.time));
        String tm = cs.state == 0 ? "2:30" : (secs / 60) + ":" + String.format("%02d", secs % 60);
        D.ctext(g, tm, cx, 9, 0xFFFFFFFF);
        D.panel(g, cx - 66, 4, cx - 32, 20, BLUE);
        D.big(g, Integer.toString(myScore), cx - 49, 7, 1.3f, 0xFFFFFFFF, 0xFF14102A);
        D.panel(g, cx + 32, 4, cx + 66, 20, RED);
        D.big(g, Integer.toString(enScore), cx + 49, 7, 1.3f, 0xFFFFFFFF, 0xFF14102A);
        D.ctext(g, "до " + Match.WIN_SCORE + " побед", cx, 22, 0xFFD6E4FF);

        // Выход
        button(g, "Выйти", 6, 6, 40, 14, 0xFFC94444, true, () -> {
            st.net.action(Proto.A_LEAVE, 0, "");
            st.match = null;
        });

        // Миникарта
        drawMinimap(g, mc, cs, 6, vh - 78);

        // Кнопки способностей
        if (me != null) {
            Proto.Meta m = mc.metaOf(mc.myId);
            BrawlerDef d = Brawlers.get(m.brawler);
            float ar = 26, sr = 19, dr = 17;
            float ax = vw - 50, ay = vh - 50, sx = vw - 104, sy = vh - 40, dx = vw - 38, dy = vh - 104;
            boolean canAttack = me.ammo >= 1f;
            circleBtn(g, ax, ay, ar, 1f, mouseHeld ? 0xFFFFC94D : (canAttack ? 0xFFE8A82A : 0xFF8A6A3A), 0xFF8A6A3A, false, "АТАКА", "ЛКМ");
            boolean sup = me.sup >= 0.995f;
            circleBtn(g, sx, sy, sr, me.sup, sup ? 0xFFFFD84D : 0xFFC89B2A, 0xFF3A3F55, sup, "СУПЕР", "E");
            float cd = d.dashCd <= 0 ? 0 : Math.min(1f, me.dashCd / d.dashCd);
            circleBtn(g, dx, dy, dr, 1f - cd, 0xFF3CA0FF, 0xFF3A3F55, cd <= 0, "РЫВОК", "ПРОБЕЛ");
            // собственная полоса здоровья
            float bw = 150, bx = vw / 2f - bw / 2, by = vh - 16;
            D.rect(g, bx - 1.5f, by - 1.5f, bx + bw + 1.5f, by + 8.5f, 0xFF14102A);
            D.rect(g, bx, by, bx + bw, by + 7, 0xFF3A3F55);
            float frac = Math.max(0, Math.min(1, me.hp / d.hp));
            D.rect(g, bx, by, bx + bw * frac, by + 7, GREEN);
            D.rect(g, bx, by, bx + bw * frac, by + 2, D.light(GREEN, 0.4f));
            D.ctext(g, (int) me.hp + " / " + d.hp + "  " + d.name, vw / 2f, by - 9, 0xFFFFFFFF);
            if ((me.flags & Snap.F_ALIVE) == 0 && cs.state == 1) {
                g.fill(0, 0, (int) vw, (int) vh, 0x55000000);
                D.big(g, "Вас нокаутировали", vw / 2, vh / 2 - 22, 1.8f, 0xFFFF6A6A, 0xFF14102A);
                D.big(g, "Возрождение через " + (int) Math.ceil(me.resp), vw / 2, vh / 2 + 2, 1.2f, 0xFFFFFFFF, 0xFF14102A);
            }
        }

        // Отсчёт и «БОЙ!»
        if (cs.state == 0) {
            int n = Math.max(1, Math.min(3, (int) Math.ceil(cs.time)));
            D.big(g, "ПРИГОТОВЬТЕСЬ", vw / 2, vh / 2 - 60, 1.6f, GOLD, 0xFF14102A);
            D.big(g, Integer.toString(n), vw / 2, vh / 2 - 36, 4f, 0xFFFFFFFF, 0xFF14102A);
            D.big(g, GameMap.NAMES[mc.map.index], vw / 2, vh / 2 + 10, 1.1f, 0xFFD6E4FF, 0xFF14102A);
        } else if (cs.state == 1 && mc.playStart > 0 && (System.nanoTime() - mc.playStart) < 1_000_000_000L) {
            D.big(g, "В БОЙ!", vw / 2, vh / 2 - 24, 3.2f, GOLD, 0xFF14102A);
        }
        if (cs.state == 2) resultOverlay(g, mc, cs);
    }

    private void drawMinimap(Gfx g, MatchClient mc, Snap cs, float x, float y) {
        float c = 3f;
        float w = GameMap.W * c, h = GameMap.H * c;
        D.rect(g, x - 1, y - 1, x + w + 1, y + h + 1, 0xFF14102A);
        D.rect(g, x, y, x + w, y + h, 0xAA1B2A3A);
        for (int ty = 0; ty < GameMap.H; ty++) {
            for (int tx = 0; tx < GameMap.W; tx++) {
                int t = mc.map.get(tx, ty);
                if (t == GameMap.GROUND) continue;
                int col = t == GameMap.WALL || t == GameMap.CRATE ? 0xFFB4BAC6 : t == GameMap.BUSH ? 0xCC2C8C3C : 0xCC3B8FD6;
                D.rect(g, x + tx * c, y + ty * c, x + tx * c + c, y + ty * c + c, col);
            }
        }
        for (Snap.F f : cs.fighters) {
            if ((f.flags & Snap.F_ALIVE) == 0 || (f.flags & Snap.F_HIDDEN) != 0) continue;
            int col = teamColor(mc, f.id);
            float r = f.id == mc.myId ? 2.4f : 1.8f;
            D.circle(g, x + f.x * c, y + f.y * c, r + 0.8f, 0xFF14102A);
            D.circle(g, x + f.x * c, y + f.y * c, r, col);
        }
    }

    private void resultOverlay(Gfx g, MatchClient mc, Snap cs) {
        g.fill(0, 0, (int) vw, (int) vh, 0xAA0B0F2A);
        boolean draw = cs.winner == 2;
        boolean win = cs.winner == mc.myTeam;
        String title = draw ? "НИЧЬЯ" : win ? "ПОБЕДА!" : "ПОРАЖЕНИЕ";
        D.big(g, title, vw / 2, 28, 3.2f, draw ? 0xFFD6E4FF : win ? GOLD : 0xFFFF6A6A, 0xFF14102A);
        D.big(g, cs.score0 + " : " + cs.score1 + "   (команда 1 — " + (mc.myTeam == 0 ? "ваша" : "соперники") + ")", vw / 2, 66, 0.9f, 0xFFD6E4FF, 0xFF14102A);
        float x = vw / 2 - 130, y = 86;
        D.panel(g, x, y, x + 260, y + 14, 0xFF2B3566);
        g.text("Игрок", x + 6, y + 3, 0xFFFFFFFF, false);
        g.text("Боец", x + 100, y + 3, 0xFFFFFFFF, false);
        g.text("Убийства", x + 160, y + 3, 0xFFFFFFFF, false);
        g.text("Смерти", x + 215, y + 3, 0xFFFFFFFF, false);
        y += 17;
        for (int team = 0; team < 2; team++) {
            for (Snap.F f : cs.fighters) {
                Proto.Meta m = mc.metaOf(f.id);
                if (m == null || m.team != team) continue;
                int tc = m.team == mc.myTeam ? BLUE : RED;
                D.rect(g, x, y, x + 260, y + 13, D.alpha(tc, f.id == mc.myId ? 0.6f : 0.3f));
                g.text(m.name, x + 6, y + 3, 0xFFFFFFFF, false);
                g.text(Brawlers.get(m.brawler).name, x + 100, y + 3, 0xFFFFFFFF, false);
                g.text(Integer.toString(f.kills), x + 175, y + 3, 0xFFFFFFFF, false);
                g.text(Integer.toString(f.deaths), x + 228, y + 3, 0xFFFFFFFF, false);
                y += 14;
            }
        }
        button(g, "В меню", vw / 2 - 40, vh - 34, 80, 20, 0xFF2D6BD0, true, () -> st.match = null);
    }

    // ------------------------------------------------------------------ ввод

    @Override
    public void tick(int w, int h, int mx, int my) {
        MatchClient mc = st.match;
        if (mc == null || mc.cur == null || st.net == null) return;
        layout(w, h, mx, my);
        float ix = 0, iy = 0;
        if (st.net.keyDown(87) || st.net.keyDown(265)) iy -= 1;
        if (st.net.keyDown(83) || st.net.keyDown(264)) iy += 1;
        if (st.net.keyDown(65) || st.net.keyDown(263)) ix -= 1;
        if (st.net.keyDown(68) || st.net.keyDown(262)) ix += 1;
        boolean overHud = overHudButtons(mvx, mvy);
        if (!overHud) {
            Snap.F me = find(mc.cur, mc.myId);
            if (me != null) {
                float adx = (mvx - psx) / TILE, ady = (mvy - psy) / TILE;
                if (Math.hypot(adx, ady) > 0.05) { lastAimDx = adx; lastAimDy = ady; }
            }
        }
        int flags = 0;
        if (mouseHeld) flags |= Proto.IN_ATTACK;
        if (superLatch) flags |= Proto.IN_SUPER;
        if (dashLatch) flags |= Proto.IN_DASH;
        superLatch = false; dashLatch = false;
        st.net.input(ix, iy, lastAimDx, lastAimDy, flags);
    }

    private boolean overHudButtons(float x, float y) {
        float ax = vw - 50, ay = vh - 50, sx = vw - 104, sy = vh - 40, dx = vw - 38, dy = vh - 104;
        return Math.hypot(x - ax, y - ay) < 28 || Math.hypot(x - sx, y - sy) < 21 || Math.hypot(x - dx, y - dy) < 19;
    }

    @Override
    public boolean mouseDown(int mx, int my, int button) {
        if (super.mouseDown(mx, my, button)) return true;
        if (button != 0) return false;
        float x = mx / s, y = my / s;
        if (Math.hypot(x - (vw - 104), y - (vh - 40)) < 21) { superLatch = true; return true; }
        if (Math.hypot(x - (vw - 38), y - (vh - 104)) < 19) { dashLatch = true; return true; }
        mouseHeld = true;
        return true;
    }

    @Override
    public void mouseUp(int mx, int my, int button) { if (button == 0) mouseHeld = false; }

    @Override
    public void key(int code, boolean down) {
        super.key(code, down);
        if (!down) return;
        if (code == 69 || code == 81) superLatch = true;                    // E, Q
        if (code == 32 || code == 340 || code == 344) dashLatch = true;     // пробел, Shift
    }
}
