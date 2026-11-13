package ru.arena.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import ru.arena.game.BrawlerDef;
import ru.arena.game.Brawlers;
import ru.arena.game.Fx;
import ru.arena.game.Snap;
import ru.arena.game.Zone;

/** Частицы, молнии, взмахи, метеоры и рисование снарядов/зон. Координаты мира — в клетках. */
public final class FxRenderer {
    /** Стили снарядов: 0 дробь, 1 огненный шар, 2 осколок льда, 3 целебный шар, 4 удар, 5 ракета, 6 шашка, 7 тяжёлый заряд, 8 ледяная волна, 9 электричество, 10 тень. */
    public static final int[] STYLE_COL = {
        0xFFFFD84D, 0xFFFF8A2A, 0xFF8FE3FF, 0xFF7DFFA6, 0xFFE3E8EE, 0xFFFF7A3A, 0xFFFFB13B, 0xFFFFA24D, 0xFFBFEFFF, 0xFF6FD3FF, 0xFFB65CFF
    };

    private static final class Pt {
        float x, y, vx, vy, life, max, size, grav;
        int color, kind; // 0 квадрат, 1 круг, 2 кольцо, 3 плюс, 4 дым
    }
    private static final class Beam { float[] xs, ys; float life, max; int color; }
    private static final class Slash { float x, y, ang, range, life, max; int color; }
    private static final class Meteor { float x, y, t; }

    private final List<Pt> pts = new ArrayList<>();
    private final List<Beam> beams = new ArrayList<>();
    private final List<Slash> slashes = new ArrayList<>();
    private final List<Meteor> meteors = new ArrayList<>();
    private final Random rnd = new Random();
    public float shake;
    public Consumer<String> sfx = s -> {};
    public float time;

    public static int styleColor(int style) { return STYLE_COL[Math.max(0, Math.min(STYLE_COL.length - 1, style))]; }

    // ----------------------------------------------------------- события

    public void onEvent(Snap.E e) {
        switch (e.type) {
            case Fx.HIT: {
                int col = styleColor(e.style);
                burst(e.x, e.y, col, 7, 3.5f, 2.2f, 0.35f, 0);
                ring(e.x, e.y, col, 10, 0.25f);
                sfx.accept("hit");
                break;
            }
            case Fx.EXPLOSION: {
                float r = e.x2;
                int col = styleColor(e.style);
                boolean fire = e.style == 1 || e.style == 5 || e.style == 6 || e.style == 7;
                ring(e.x, e.y, fire ? 0xFFFFC25A : col, r * 30, 0.4f);
                ring(e.x, e.y, fire ? 0xFFFF6A1F : D.light(col, 0.4f), r * 20, 0.3f);
                burst(e.x, e.y, fire ? 0xFFFF8A2A : col, (int) (r * 14), 5f, 3.2f, 0.5f, 0);
                burst(e.x, e.y, fire ? 0xFFFFE08A : 0xFFFFFFFF, (int) (r * 6), 3f, 2f, 0.35f, 0);
                for (int i = 0; i < r * 5; i++) smoke(e.x + (rnd.nextFloat() - 0.5f) * r, e.y + (rnd.nextFloat() - 0.5f) * r, fire ? 0xFF3A2A22 : 0xFFBFC8D0);
                shake = Math.max(shake, r * 1.6f);
                sfx.accept("explosion");
                break;
            }
            case Fx.DASH: dashFx(e); sfx.accept("dash"); break;
            case Fx.BEAM: {
                Beam b = new Beam();
                float dx = e.x2 - e.x, dy = e.y2 - e.y;
                float len = (float) Math.hypot(dx, dy);
                int n = Math.max(2, (int) (len * 3));
                b.xs = new float[n + 1]; b.ys = new float[n + 1];
                float nx = -dy / Math.max(0.01f, len), ny = dx / Math.max(0.01f, len);
                for (int i = 0; i <= n; i++) {
                    float k = (float) i / n;
                    float off = (i == 0 || i == n) ? 0 : (rnd.nextFloat() - 0.5f) * 0.45f;
                    b.xs[i] = e.x + dx * k + nx * off; b.ys[i] = e.y + dy * k + ny * off;
                }
                b.life = b.max = 0.2f; b.color = Brawlers.get(e.style).cGlow;
                addBeam(b);
                burst(e.x2, e.y2, b.color, 3, 2.5f, 2f, 0.25f, 0);
                sfx.accept("zap");
                break;
            }
            case Fx.SLASH: {
                Slash s = new Slash();
                s.x = e.x; s.y = e.y; s.ang = e.x2; s.range = e.y2; s.life = s.max = 0.25f;
                s.color = e.style == 5 ? 0xFFD9A8FF : e.style == 4 ? 0xFFFFC27A : Brawlers.get(e.style).cGlow;
                if (slashes.size() < 24) slashes.add(s);
                sfx.accept("slash");
                break;
            }
            case Fx.SUPER: {
                BrawlerDef d = Brawlers.get(e.style);
                ring(e.x, e.y, d.cGlow, 70, 0.55f);
                ring(e.x, e.y, 0xFFFFFFFF, 40, 0.4f);
                burst(e.x, e.y, d.cGlow, 26, 7f, 3.4f, 0.6f, 0);
                burst(e.x, e.y, 0xFFFFFFFF, 10, 5f, 2.2f, 0.45f, 0);
                shake = Math.max(shake, 3f);
                sfx.accept("super");
                break;
            }
            case Fx.DEATH: {
                int col = e.style == 0 ? 0xFF4C8DFF : 0xFFFF4C4C;
                ring(e.x, e.y, col, 34, 0.5f);
                burst(e.x, e.y, col, 18, 5f, 3.4f, 0.7f, 6f);
                burst(e.x, e.y, 0xFFFFFFFF, 8, 4f, 2.4f, 0.5f, 6f);
                for (int i = 0; i < 4; i++) smoke(e.x, e.y, 0xFF55556A);
                sfx.accept("death");
                break;
            }
            case Fx.HEAL:
                for (int i = 0; i < 3; i++) plus(e.x + (rnd.nextFloat() - 0.5f) * 0.6f, e.y - rnd.nextFloat() * 0.3f, 0xFF7DFFA6);
                sfx.accept("heal");
                break;
            case Fx.SPAWN: {
                int col = e.style == 0 ? 0xFF4C8DFF : 0xFFFF4C4C;
                ring(e.x, e.y, col, 26, 0.5f);
                burst(e.x, e.y, 0xFFFFFFFF, 10, 3f, 2f, 0.6f, -2f);
                break;
            }
            default: break;
        }
    }

    private void dashFx(Snap.E e) {
        BrawlerDef d = Brawlers.get(e.style);
        float dx = e.x2 - e.x, dy = e.y2 - e.y;
        float len = (float) Math.max(0.01, Math.hypot(dx, dy));
        int n = (int) (len * 5);
        for (int i = 0; i <= n; i++) {
            float k = (float) i / Math.max(1, n);
            float px = e.x + dx * k, py = e.y + dy * k;
            switch (e.style) {
                case 0: burst(px, py, i % 2 == 0 ? 0xFFFFB13B : 0xFFFF5A1F, 1, 1.5f, 3f, 0.45f, 0); smoke(px, py, 0xFF6A5A52); break;
                case 1: burst(px, py, 0xFFFFC25A, 1, 2f, 2.6f, 0.5f, 0); break;
                case 2: burst(px, py, i % 2 == 0 ? 0xFFEAF6FF : 0xFF8FE3FF, 2, 1.5f, 2.4f, 0.7f, 1f); break;
                case 3: burst(px, py, i % 2 == 0 ? 0xFF6FD3FF : 0xFFFFF27A, 1, 2f, 2f, 0.3f, 0); break;
                case 4: smoke(px, py, 0xFFB7A58A); burst(px, py, 0xFFFFC27A, 1, 2f, 2.6f, 0.35f, 2f); break;
                case 5: smoke(px, py, 0xFF6A3AA0); burst(px, py, 0xFFB65CFF, 1, 1f, 3f, 0.5f, 0); break;
                case 6: if (i % 2 == 0) plus(px, py, 0xFF7DFFA6); burst(px, py, 0xFF7DFFA6, 1, 1f, 2.4f, 0.5f, 0); break;
                default: burst(px, py, i % 2 == 0 ? 0xFFFFB13B : 0xFFFFFFFF, 1, 2f, 3f, 0.5f, 0); smoke(px, py, 0xFF554A44); break;
            }
        }
        if (e.style == 1 || e.style == 5) { ring(e.x, e.y, d.cGlow, 26, 0.35f); ring(e.x2, e.y2, d.cGlow, 26, 0.35f); }
        if (e.style == 3) {
            Beam b = new Beam();
            int m = Math.max(3, (int) (len * 4));
            b.xs = new float[m + 1]; b.ys = new float[m + 1];
            for (int i = 0; i <= m; i++) {
                float k = (float) i / m;
                float off = (i == 0 || i == m) ? 0 : (rnd.nextFloat() - 0.5f) * 0.5f;
                b.xs[i] = e.x + dx * k - dy / len * off; b.ys[i] = e.y + dy * k + dx / len * off;
            }
            b.life = b.max = 0.3f; b.color = d.cGlow;
            addBeam(b);
        }
        if (e.style == 7) { ring(e.x, e.y, 0xFFFF9A3C, 40, 0.4f); burst(e.x, e.y, 0xFFFF9A3C, 14, 5f, 3f, 0.5f, 0); shake = Math.max(shake, 1.5f); }
        if (e.style == 4) shake = Math.max(shake, 1.2f);
    }

    // ----------------------------------------------------------- частицы

    public void burst(float x, float y, int color, int n, float speed, float size, float life, float grav) {
        if (pts.size() > 700) return;
        for (int i = 0; i < n; i++) {
            Pt p = new Pt();
            float a = rnd.nextFloat() * 6.2832f, sp = speed * (0.25f + rnd.nextFloat() * 0.75f);
            p.x = x; p.y = y; p.vx = (float) Math.cos(a) * sp; p.vy = (float) Math.sin(a) * sp;
            p.life = p.max = life * (0.6f + rnd.nextFloat() * 0.6f);
            p.size = size * (0.6f + rnd.nextFloat() * 0.8f); p.grav = grav; p.color = color; p.kind = rnd.nextBoolean() ? 0 : 1;
            pts.add(p);
        }
    }

    private void addBeam(Beam b) { if (beams.size() < 60) beams.add(b); }

    public void ring(float x, float y, int color, float maxR, float life) {
        if (pts.size() > 700) return;
        Pt p = new Pt();
        p.x = x; p.y = y; p.life = p.max = life; p.size = maxR; p.color = color; p.kind = 2;
        pts.add(p);
    }

    public void smoke(float x, float y, int color) {
        if (pts.size() > 700) return;
        Pt p = new Pt();
        p.x = x; p.y = y; p.vx = (rnd.nextFloat() - 0.5f) * 0.8f; p.vy = -0.3f - rnd.nextFloat() * 0.6f;
        p.life = p.max = 0.5f + rnd.nextFloat() * 0.4f; p.size = 4 + rnd.nextFloat() * 3; p.color = color; p.kind = 4;
        pts.add(p);
    }

    public void plus(float x, float y, int color) {
        if (pts.size() > 700) return;
        Pt p = new Pt();
        p.x = x; p.y = y; p.vx = (rnd.nextFloat() - 0.5f) * 0.4f; p.vy = -1.2f - rnd.nextFloat() * 0.6f;
        p.life = p.max = 0.8f; p.size = 3; p.color = color; p.kind = 3;
        pts.add(p);
    }

    /** Шлейф летящего снаряда (вызывается каждый кадр для каждого снаряда). */
    public void trail(int style, float x, float y, float ang, float dt) {
        if (rnd.nextFloat() > dt * 40) return;
        switch (style) {
            case 1: burst(x, y, rnd.nextBoolean() ? 0xFFFF8A2A : 0xFFFFD25A, 1, 0.8f, 3f, 0.35f, -1f); break;
            case 2: burst(x, y, rnd.nextBoolean() ? 0xFFEAF6FF : 0xFF8FE3FF, 1, 0.6f, 1.8f, 0.4f, 0); break;
            case 3: burst(x, y, 0xFF7DFFA6, 1, 0.5f, 2f, 0.35f, 0); break;
            case 5: smoke(x - (float) Math.cos(ang) * 0.3f, y - (float) Math.sin(ang) * 0.3f, 0xFF6A5A52); burst(x, y, 0xFFFFB13B, 1, 0.8f, 2.4f, 0.25f, 0); break;
            case 6: burst(x, y, 0xFFFFB13B, 1, 0.8f, 1.8f, 0.2f, 0); break;
            case 7: burst(x, y, 0xFFFFA24D, 1, 0.6f, 2.6f, 0.3f, 0); break;
            case 8: burst(x, y + (rnd.nextFloat() - 0.5f) * 2f, 0xFFFFFFFF, 1, 0.8f, 2f, 0.5f, 0); break;
            default: break;
        }
    }

    /** Постоянные эффекты внутри зоны. */
    public void ambient(Snap.Z z, float dt) {
        float r = z.r;
        switch (z.kind) {
            case Zone.FIRE:
                if (rnd.nextFloat() < dt * 22) {
                    float a = rnd.nextFloat() * 6.28f, d = (float) Math.sqrt(rnd.nextFloat()) * r;
                    Pt p = new Pt();
                    p.x = z.x + (float) Math.cos(a) * d; p.y = z.y + (float) Math.sin(a) * d * 0.9f;
                    p.vy = -1.8f; p.life = p.max = 0.5f; p.size = 3.4f; p.color = rnd.nextBoolean() ? 0xFFFF7A1F : 0xFFFFD25A; p.kind = 1;
                    pts.add(p);
                }
                if (rnd.nextFloat() < dt * 5) {
                    Meteor m = new Meteor();
                    float a = rnd.nextFloat() * 6.28f, d = (float) Math.sqrt(rnd.nextFloat()) * r * 0.9f;
                    m.x = z.x + (float) Math.cos(a) * d; m.y = z.y + (float) Math.sin(a) * d * 0.9f;
                    if (meteors.size() < 30) meteors.add(m);
                }
                break;
            case Zone.ICE:
                if (rnd.nextFloat() < dt * 12) {
                    float a = rnd.nextFloat() * 6.28f, d = (float) Math.sqrt(rnd.nextFloat()) * r;
                    burst(z.x + (float) Math.cos(a) * d, z.y + (float) Math.sin(a) * d, 0xFFFFFFFF, 1, 0.3f, 1.8f, 0.7f, -0.5f);
                }
                break;
            case Zone.SHOCK:
                if (rnd.nextFloat() < dt * 18) {
                    float a = rnd.nextFloat() * 6.28f;
                    burst(z.x + (float) Math.cos(a) * r, z.y + (float) Math.sin(a) * r, rnd.nextBoolean() ? 0xFF6FD3FF : 0xFFFFF27A, 1, 1.5f, 2.2f, 0.3f, 0);
                }
                break;
            case Zone.HEAL:
                if (rnd.nextFloat() < dt * 8) {
                    float a = rnd.nextFloat() * 6.28f, d = (float) Math.sqrt(rnd.nextFloat()) * r;
                    plus(z.x + (float) Math.cos(a) * d, z.y + (float) Math.sin(a) * d * 0.9f, 0xFF7DFFA6);
                }
                break;
            default: break;
        }
    }

    // ------------------------------------------------------------- обновление

    public void update(float dt) {
        time += dt;
        shake = Math.max(0, shake - dt * 8f);
        for (Iterator<Pt> it = pts.iterator(); it.hasNext(); ) {
            Pt p = it.next();
            p.life -= dt;
            if (p.life <= 0) { it.remove(); continue; }
            p.vy += p.grav * dt;
            p.x += p.vx * dt; p.y += p.vy * dt;
            if (p.kind != 3) { p.vx *= 0.97f; }
        }
        for (Iterator<Beam> it = beams.iterator(); it.hasNext(); ) { Beam b = it.next(); b.life -= dt; if (b.life <= 0) it.remove(); }
        for (Iterator<Slash> it = slashes.iterator(); it.hasNext(); ) { Slash s = it.next(); s.life -= dt; if (s.life <= 0) it.remove(); }
        for (Iterator<Meteor> it = meteors.iterator(); it.hasNext(); ) {
            Meteor m = it.next();
            m.t += dt;
            if (m.t >= 0.3f) {
                burst(m.x, m.y, 0xFFFF8A2A, 8, 3.5f, 2.6f, 0.4f, 0);
                ring(m.x, m.y, 0xFFFFC25A, 16, 0.25f);
                shake = Math.max(shake, 0.6f);
                it.remove();
            }
        }
    }

    // ---------------------------------------------------------------- рисование

    /** Рисует всё «в воздухе»: частицы, молнии, взмахи, метеоры. screen = world*tile + offset. */
    public void draw(Gfx g, float ox, float oy, float tile) {
        for (Meteor m : meteors) {
            float k = m.t / 0.3f;
            float sx = ox + m.x * tile, sy = oy + m.y * tile;
            float hx = sx - 30 * (1 - k), hy = sy - 150 * (1 - k);
            D.line(g, hx + 10, hy - 30, hx, hy, 4, 0x55FF7A1F);
            D.line(g, hx + 5, hy - 15, hx, hy, 3, 0xAAFFB13B);
            D.circle(g, hx, hy, 4, 0xFFFF7A1F);
            D.circle(g, hx, hy, 2, 0xFFFFF2B0);
            D.oval(g, sx, sy, 6 * k, 2.5f * k, 0x33000000);
        }
        for (Pt p : pts) {
            float a = p.life / p.max;
            float sx = ox + p.x * tile, sy = oy + p.y * tile;
            int col = D.alpha(p.color, Math.min(1f, a * 1.4f));
            switch (p.kind) {
                case 0: { float s = p.size * (0.5f + a * 0.5f); g.fill(Math.round(sx - s / 2), Math.round(sy - s / 2), Math.round(sx + s / 2), Math.round(sy + s / 2), col); break; }
                case 1: D.circle(g, sx, sy, p.size * (0.4f + a * 0.6f) / 2 + 0.5f, col); break;
                case 2: D.ring(g, sx, sy, p.size * (1 - a) + 2, 1.6f, col); break;
                case 3: { int c = D.alpha(p.color, a); g.fill(Math.round(sx - 1), Math.round(sy - 3), Math.round(sx + 1), Math.round(sy + 3), c); g.fill(Math.round(sx - 3), Math.round(sy - 1), Math.round(sx + 3), Math.round(sy + 1), c); break; }
                default: D.circle(g, sx, sy, p.size * (2 - a) / 2, D.alpha(p.color, a * 0.45f));
            }
        }
        for (Beam b : beams) {
            float a = b.life / b.max;
            for (int i = 0; i + 1 < b.xs.length; i++) {
                D.line(g, ox + b.xs[i] * tile, oy + b.ys[i] * tile, ox + b.xs[i + 1] * tile, oy + b.ys[i + 1] * tile, 4.2f, D.alpha(b.color, a * 0.45f));
                D.line(g, ox + b.xs[i] * tile, oy + b.ys[i] * tile, ox + b.xs[i + 1] * tile, oy + b.ys[i + 1] * tile, 1.6f, D.alpha(0xFFFFFFFF, a));
            }
        }
        for (Slash s : slashes) {
            float prog = Math.min(1f, (1 - s.life / s.max) * 2.4f);
            float a = s.life / s.max;
            float half = 1.05f;
            int steps = 12;
            for (int i = 0; i < steps; i++) {
                float k0 = (float) i / steps, k1 = (float) (i + 1) / steps;
                if (k0 > prog) break;
                float a0 = s.ang - half + 2 * half * k0, a1 = s.ang - half + 2 * half * k1;
                float r = s.range * tile * 0.9f;
                float w = 2f + 5f * k1 * (1 - (1 - a) * 0.5f);
                D.line(g, ox + s.x * tile + (float) Math.cos(a0) * r, oy + s.y * tile + (float) Math.sin(a0) * r,
                        ox + s.x * tile + (float) Math.cos(a1) * r, oy + s.y * tile + (float) Math.sin(a1) * r, w, D.alpha(s.color, a));
                D.line(g, ox + s.x * tile + (float) Math.cos(a0) * r * 0.9f, oy + s.y * tile + (float) Math.sin(a0) * r * 0.9f,
                        ox + s.x * tile + (float) Math.cos(a1) * r * 0.9f, oy + s.y * tile + (float) Math.sin(a1) * r * 0.9f, w * 0.4f, D.alpha(0xFFFFFFFF, a));
            }
        }
    }

    /** Диск зоны на земле (рисуется под бойцами). */
    public void drawZone(Gfx g, Snap.Z z, float ox, float oy, float tile, float t) {
        float cx = ox + z.x * tile, cy = oy + z.y * tile, r = z.r * tile;
        float fade = Math.min(1f, z.frac * 5f);
        float pulse = 1f + (float) Math.sin(t * 5) * 0.03f;
        int col;
        switch (z.kind) {
            case Zone.FIRE: col = 0xFFFF6A1F; break;
            case Zone.ICE: col = 0xFFBFEFFF; break;
            case Zone.SHOCK: col = 0xFFFFF27A; break;
            default: col = 0xFF5CFF8F; break;
        }
        D.oval(g, cx, cy, r * pulse, r * pulse, D.alpha(col, 0.20f * fade));
        D.oval(g, cx, cy, r * 0.72f, r * 0.72f, D.alpha(col, 0.14f * fade));
        D.ring(g, cx, cy, r * pulse, 2f, D.alpha(col, 0.8f * fade));
        D.ring(g, cx, cy, r * 0.72f * pulse, 1f, D.alpha(col, 0.35f * fade));
        switch (z.kind) {
            case Zone.SHOCK:
                for (int i = 0; i < 6; i++) {
                    float a0 = t * 2.5f + i * 1.0472f;
                    float px = cx, py = cy;
                    for (int s = 1; s <= 5; s++) {
                        float rr = r * s / 5f;
                        float aa = a0 + (float) Math.sin(t * 20 + i * 3 + s * 2) * 0.22f;
                        float nx = cx + (float) Math.cos(aa) * rr, ny = cy + (float) Math.sin(aa) * rr;
                        D.line(g, px, py, nx, ny, 1.6f, D.alpha(0xFF6FD3FF, 0.8f * fade));
                        px = nx; py = ny;
                    }
                }
                break;
            case Zone.ICE:
                for (int i = 0; i < 7; i++) {
                    float a = i * 0.9f + z.id, d = (0.25f + (i * 37 % 10) / 14f) * r;
                    float px = cx + (float) Math.cos(a) * d, py = cy + (float) Math.sin(a) * d;
                    D.line(g, px, py - 4, px, py + 4, 2f, D.alpha(0xFFFFFFFF, 0.7f * fade));
                    D.line(g, px - 3, py, px + 3, py, 1.4f, D.alpha(0xFFFFFFFF, 0.7f * fade));
                }
                break;
            case Zone.HEAL:
                D.oval(g, cx, cy, r * 0.5f, r * 0.5f, D.alpha(0xFFFFFFFF, 0.07f * fade));
                break;
            default: break;
        }
    }

    // --------------------------------------------------------------- снаряды

    /** Рисует снаряд в точке (sx,sy); k — множитель размера. */
    public static void drawProjectile(Gfx g, int style, float sx, float sy, float ang, float prog, float t, float k) {
        g.push();
        g.translate(sx, sy);
        g.scale(k, k);
        switch (style) {
            case 0:
                D.circle(g, 0, 0, 2.6f, 0xFF7A4A10); D.circle(g, 0, 0, 2f, 0xFFFFD84D); D.circle(g, -0.4f, -0.4f, 0.9f, 0xFFFFFFFF);
                break;
            case 1: {
                float f = 1f + (float) Math.sin(t * 30) * 0.12f;
                D.circle(g, 0, 0, 8 * f, 0x33FF7A1F); D.circle(g, 0, 0, 5.4f * f, 0xFFFF5A1F);
                D.circle(g, 0, 0, 3.6f * f, 0xFFFF9A3C); D.circle(g, 0, 0, 2f * f, 0xFFFFF2B0);
                break;
            }
            case 2:
                g.push(); g.rotate(ang);
                D.line(g, -9, 0, 8, 0, 4.4f, 0xFF3B7FD0); D.line(g, -8, 0, 7, 0, 2.8f, 0xFF8FE3FF); D.line(g, -5, 0, 5, 0, 1.2f, 0xFFFFFFFF);
                D.circle(g, 9, 0, 1.8f, 0xFFFFFFFF);
                g.pop();
                break;
            case 3: {
                float f = 1f + (float) Math.sin(t * 14) * 0.1f;
                D.circle(g, 0, 0, 7.5f * f, 0x337DFFA6); D.circle(g, 0, 0, 5f * f, 0xFF3FB86B); D.circle(g, 0, 0, 3.6f * f, 0xFF7DFFA6);
                g.fill(-1, -3, 1, 3, 0xFFFFFFFF); g.fill(-3, -1, 3, 1, 0xFFFFFFFF);
                break;
            }
            case 5:
                g.push(); g.rotate(ang);
                float fl = 3 + (float) Math.sin(t * 40) * 1.2f;
                D.oval(g, -9 - fl / 2, 0, 4 + fl, 2.6f, 0xFFFF7A1F); D.oval(g, -8, 0, 3, 1.5f, 0xFFFFE08A);
                g.fill(-6, -2, 5, 2, 0xFF14102A); g.fill(-5, -1, 5, 1, 0xFFB7C0CC);
                g.fill(5, -2, 8, 2, 0xFF14102A); g.fill(5, -1, 7, 1, 0xFFD0361F);
                g.fill(-7, -4, -4, -1, 0xFF8A93A0); g.fill(-7, 1, -4, 4, 0xFF8A93A0);
                g.pop();
                break;
            case 6: {
                float arc = prog >= 0 ? (float) Math.sin(Math.PI * prog) * 20f : 0;
                D.oval(g, 0, 0, 4.5f * (1 - arc / 60f), 2f, 0x44000000);
                g.translate(0, -arc);
                g.rotate(t * 8);
                D.circle(g, 0, 0, 4.4f, 0xFF14102A); D.circle(g, 0, 0, 3.4f, 0xFFB5651D); g.fill(-3, -1, 3, 1, 0xFFF5C518);
                g.fill(-1, -6, 1, -4, 0xFF8A5A3A);
                D.circle(g, 0, -7, 1.6f + (float) Math.sin(t * 40) * 0.5f, 0xFFFFB13B); D.circle(g, 0, -7, 0.8f, 0xFFFFFFFF);
                break;
            }
            case 7:
                D.circle(g, 0, 0, 7f, 0x33FFA24D); D.circle(g, 0, 0, 4.8f, 0xFF8A4A10); D.circle(g, 0, 0, 3.8f, 0xFFFFA24D); D.circle(g, -0.8f, -0.8f, 1.6f, 0xFFFFF2B0);
                break;
            case 8:
                g.push(); g.rotate(ang);
                for (int i = -6; i <= 6; i++) {
                    float x = -Math.abs(i) * 1.3f + 4, y = i * 3.4f;
                    D.circle(g, x, y, 4.4f, 0x558FE3FF);
                    D.circle(g, x, y, 3f, i % 2 == 0 ? 0xFFEAF6FF : 0xFF8FE3FF);
                }
                g.pop();
                break;
            default:
                D.circle(g, 0, 0, 3f, 0xFFFFFFFF);
                break;
        }
        g.pop();
    }
}
