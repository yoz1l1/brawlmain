package ru.arena.ui;

import ru.arena.game.BrawlerDef;
import ru.arena.game.Brawlers;

/**
 * Рисует бойцов кодом (без текстур): мультяшные фигурки с большими головами, обводкой,
 * оружием, которое поворачивается к прицелу, и анимацией ходьбы/покоя.
 * Локальные координаты: (0,0) — центр под ногами, вверх — отрицательный y, рост ≈ 38 единиц.
 */
public final class BrawlerArt {
    private BrawlerArt() {}

    private static Gfx G;
    private static float X0, Y0, K, DIR, A;
    private static float ET; // время анимации

    private static int c(int argb) { return A >= 0.99f ? argb : D.alpha(argb, A); }

    private static void R(float x1, float y1, float x2, float y2, int col) {
        float a = X0 + x1 * DIR * K, b = X0 + x2 * DIR * K;
        D.rect(G, Math.min(a, b), Y0 + y1 * K, Math.max(a, b), Y0 + y2 * K, c(col));
    }

    private static void O(float cx, float cy, float rx, float ry, int col) {
        D.oval(G, X0 + cx * DIR * K, Y0 + cy * K, rx * K, ry * K, c(col));
    }

    private static void OO(float cx, float cy, float rx, float ry, int fill, int outline) {
        O(cx, cy, rx + 1.1f, ry + 1.1f, outline);
        O(cx, cy, rx, ry, fill);
    }

    /** Прямоугольник с обводкой и срезанными углами. */
    private static void RR(float x1, float y1, float x2, float y2, int fill, int outline) {
        R(x1 - 1.1f, y1 - 0.5f, x2 + 1.1f, y2 + 0.5f, outline);
        R(x1 - 0.5f, y1 - 1.1f, x2 + 0.5f, y2 + 1.1f, outline);
        R(x1, y1, x2, y2, fill);
    }

    private static void L(float x1, float y1, float x2, float y2, float w, int col) {
        D.line(G, X0 + x1 * DIR * K, Y0 + y1 * K, X0 + x2 * DIR * K, Y0 + y2 * K, w * K, c(col));
    }

    /** Предмет в руке, повёрнутый по направлению прицела. */
    private static void inHand(float px, float py, float ang, Runnable body) {
        G.push();
        G.translate(X0 + px * DIR * K, Y0 + py * K);
        G.rotate(ang);
        body.run();
        G.pop();
    }

    private static void W(float x1, float y1, float x2, float y2, int col) {
        D.rect(G, x1 * K, y1 * K, x2 * K, y2 * K, c(col));
    }

    private static final int OUT = 0xFF14102A;

    /** Главная точка входа. */
    public static void draw(Gfx g, int id, float x, float y, float k, int dir, float time,
                            boolean moving, float aim, float alpha) {
        G = g; X0 = x; Y0 = y; K = k; DIR = dir >= 0 ? 1 : -1; A = alpha; ET = time;
        BrawlerDef d = Brawlers.get(id);
        float bob = moving ? (float) Math.abs(Math.sin(time * 12)) * 1.6f : (float) Math.sin(time * 3) * 0.6f;
        float step = moving ? (float) Math.sin(time * 12) * 2.2f : 0f;

        // тень
        O(0, 0.5f, 11, 3.4f, 0x55000000);

        switch (id) {
            case 0: boomer(d, bob, step, aim); break;
            case 1: spark(d, bob, step, aim); break;
            case 2: frost(d, bob, step, aim); break;
            case 3: tesla(d, bob, step, aim); break;
            case 4: tank(d, bob, step, aim); break;
            case 5: shade(d, bob, step, aim); break;
            case 6: mint(d, bob, step, aim); break;
            default: blaster(d, bob, step, aim); break;
        }
    }

    /** Только голова — для иконок в списках. */
    public static void icon(Gfx g, int id, float cx, float cy, float k) {
        draw(g, id, cx, cy + 36 * k * 0.62f, k * 0.62f, 1, 0, false, 0f, 1f);
    }

    // --------------------------------------------------------- общие части

    private static void legs(int col, int boot, float step, float spread) {
        RR(-5 - spread + step * 0.3f, -9, -1.5f - spread + step * 0.3f, -0.5f + Math.max(0, step) * 0.2f, col, OUT);
        RR(1.5f + spread - step * 0.3f, -9, 5 + spread - step * 0.3f, -0.5f + Math.max(0, -step) * 0.2f, col, OUT);
        R(-5.6f - spread + step * 0.3f, -2.2f, -1.2f - spread + step * 0.3f, -0.4f, boot);
        R(1.2f + spread - step * 0.3f, -2.2f, 5.6f + spread - step * 0.3f, -0.4f, boot);
    }

    private static void eyes(float cy, float aim, float spacing, float size, boolean angry) {
        float px = (float) Math.cos(aim) * DIR * 0.9f * (DIR);
        px = (float) Math.cos(aim) * 0.9f;
        float py = (float) Math.sin(aim) * 0.8f;
        for (int s = -1; s <= 1; s += 2) {
            float ex = s * spacing;
            OO(ex, cy, size, size * 1.25f, 0xFFFFFFFF, OUT);
            O(ex + px * DIR * DIR, cy + py, size * 0.55f, size * 0.7f, 0xFF1B1B2B);
            O(ex + px * 0.6f - size * 0.15f, cy + py - size * 0.35f, size * 0.2f, size * 0.2f, 0xFFFFFFFF);
        }
        if (angry) {
            L(-spacing - size, cy - size * 1.5f, -spacing + size * 0.6f, cy - size * 0.9f, 1.3f, OUT);
            L(spacing + size, cy - size * 1.5f, spacing - size * 0.6f, cy - size * 0.9f, 1.3f, OUT);
        }
    }

    private static void mouth(float cy, int kind) {
        if (kind == 0) { R(-2, cy, 2, cy + 1.2f, 0xFF3A1A1A); }
        else if (kind == 1) { R(-3, cy, 3, cy + 2.6f, 0xFF3A1A1A); R(-2, cy + 1.8f, 2, cy + 2.6f, 0xFFE0506A); R(-2.2f, cy, 2.2f, cy + 0.9f, 0xFFFFFFFF); }
        else { R(-2.5f, cy, 2.5f, cy + 1.1f, 0xFF3A1A1A); R(2.5f, cy - 0.8f, 3.5f, cy + 0.3f, 0xFF3A1A1A); }
    }

    private static void arm(float sx, float y, int sleeve, int hand, float r) {
        OO(sx, y, r, r, sleeve, OUT);
        O(sx, y + r * 0.8f, r * 0.75f, r * 0.75f, hand);
    }

    // --------------------------------------------------------- бойцы

    /** Бумер — старатель с дробовиком. */
    private static void boomer(BrawlerDef d, float bob, float step, float aim) {
        int skin = 0xFFF2B98E, vest = d.cMain, shirt = 0xFF4A4F5C;
        legs(0xFF3B4A6B, 0xFF5A3A22, step, 0.5f);
        float b = -bob * 0.5f;
        RR(-8, -23 + b, 8, -8 + b, shirt, OUT);
        RR(-8, -23 + b, -2.5f, -8 + b, vest, OUT);
        RR(2.5f, -23 + b, 8, -8 + b, vest, OUT);
        R(-8, -11 + b, 8, -9.5f + b, 0xFF3A2A1A);
        R(-1, -11.4f + b, 1, -9.2f + b, 0xFFFFC94D);
        arm(-9.5f, -17 + b, shirt, skin, 3);
        arm(9.5f, -17 + b, shirt, skin, 3);
        float hy = -29.5f + b;
        OO(0, hy, 10.5f, 9.5f, skin, OUT);
        // щетина
        for (int i = -3; i <= 3; i++) R(i * 1.9f, hy + 6.2f, i * 1.9f + 0.9f, hy + 7.2f, 0xFF8A5A3A);
        eyes(hy + 0.5f, aim, 3.8f, 2.4f, true);
        mouth(hy + 5f, 2);
        // бандана и очки
        RR(-10.5f, hy - 6.5f, 10.5f, hy - 2.5f, d.cAccent, OUT);
        R(-10.5f, hy - 5f, 10.5f, hy - 4.3f, D.light(d.cAccent, 0.3f));
        RR(8, hy - 5f, 13.5f, hy - 1f, d.cAccent, OUT);
        O(-4, hy - 9.5f, 3.2f, 2.8f, 0xFF2B2B33); O(-4, hy - 9.5f, 2.2f, 1.8f, 0xFF9FE8FF);
        O(4, hy - 9.5f, 3.2f, 2.8f, 0xFF2B2B33); O(4, hy - 9.5f, 2.2f, 1.8f, 0xFF9FE8FF);
        R(-8, hy - 11f, 8, hy - 8.8f, 0xFF3A2A1A);
        inHand(7, -16 + b, aim, () -> {
            W(-3, -2, 9, 2.4f, 0xFF7A4A24); W(-3, -2, 9, -0.6f, 0xFF9A6234);
            W(8, -1.4f, 21, 1.4f, 0xFF2B2F38); W(8, -1.4f, 21, -0.2f, 0xFF4A505C);
            W(8, 1.4f, 15, 2.8f, 0xFF3A2A1A); W(20.5f, -1.6f, 22, 1.6f, 0xFFFF9A3C);
        });
        O(7 * 1f, -15.5f + b, 2.4f, 2.4f, skin);
    }

    /** Искра — огненная волшебница. */
    private static void spark(BrawlerDef d, float bob, float step, float aim) {
        int skin = 0xFFF7C9A0;
        legs(0xFF3A1F2B, 0xFF2B1620, step, 0.3f);
        float b = -bob * 0.5f;
        // мантия-трапеция
        RR(-8, -23 + b, 8, -12 + b, d.cMain, OUT);
        RR(-10, -14 + b, 10, -5.5f + b, d.cMain, OUT);
        R(-10, -7.5f + b, 10, -5.5f + b, d.cAccent);
        R(-1.2f, -23 + b, 1.2f, -5.5f + b, d.cAccent);
        for (int i = -3; i <= 3; i++) R(i * 2.8f - 0.8f, -5.5f + b, i * 2.8f + 0.8f, -3.5f - Math.abs(i) * 0.3f + b + (float) Math.sin(ET * 8 + i) * 0.8f, d.cGlow);
        arm(-9.5f, -18 + b, d.cMain, skin, 2.8f);
        float hy = -29.5f + b;
        // огненные волосы
        for (int i = -3; i <= 3; i++) {
            float fl = (float) Math.sin(ET * 7 + i * 1.7f) * 1.2f;
            O(i * 3.2f, hy - 2f - Math.abs(i) * -0.3f, 3f, 4.5f + fl, i % 2 == 0 ? d.cGlow : d.cAccent);
        }
        OO(0, hy, 9.8f, 9f, skin, OUT);
        eyes(hy + 0.8f, aim, 3.6f, 2.4f, false);
        mouth(hy + 5.2f, 1);
        O(-6.5f, hy + 3.5f, 1.8f, 1.2f, 0x66FF6F6F); O(6.5f, hy + 3.5f, 1.8f, 1.2f, 0x66FF6F6F);
        // шляпа
        OO(0, hy - 7f, 13.5f, 3.2f, 0xFF8A1E22, OUT);
        RR(-7.5f, hy - 17f, 7.5f, hy - 7f, 0xFFB5262C, OUT);
        RR(-5, hy - 22f, 5, hy - 16f, 0xFFB5262C, OUT);
        RR(-2.5f, hy - 26f, 2.5f, hy - 21f, 0xFFB5262C, OUT);
        R(-7.5f, hy - 10.5f, 7.5f, hy - 8.5f, d.cAccent);
        O(0, hy - 27f, 2.2f, 2.2f, d.cAccent);
        // посох
        inHand(9, -16 + b, aim, () -> {
            W(-6, -1.2f, 17, 1.2f, 0xFF6B3F20); W(-6, -1.2f, 17, 0f, 0xFF8A5530);
            float pulse = 1f + (float) Math.sin(ET * 6) * 0.12f;
            D.circle(G, 20 * K, 0, 5.2f * K * pulse, c(D.alpha(d.cGlow, 0.35f)));
            D.circle(G, 20 * K, 0, 3.6f * K * pulse, c(d.cGlow));
            D.circle(G, 20 * K, 0, 2f * K * pulse, c(0xFFFFF2B0));
        });
        O(9, -15.5f + b, 2.4f, 2.4f, skin);
    }

    /** Лёдик — снайпер в парке. */
    private static void frost(BrawlerDef d, float bob, float step, float aim) {
        int skin = 0xFFF4D2BB;
        legs(0xFF2B3550, 0xFF1E2437, step, 0.5f);
        float b = -bob * 0.5f;
        RR(-8.5f, -23 + b, 8.5f, -8 + b, d.cMain, OUT);
        R(-8.5f, -12 + b, 8.5f, -10.5f + b, 0xFF1E3E78);
        R(-1, -23 + b, 1, -8 + b, d.cAccent);
        O(0, -23 + b, 9.5f, 3.2f, d.cAccent);
        // снежинка
        L(4, -18 + b, 7, -15 + b, 1, d.cAccent); L(7, -18 + b, 4, -15 + b, 1, d.cAccent);
        arm(-9.8f, -17 + b, d.cMain, 0xFF2B3550, 3.2f);
        float hy = -29.5f + b;
        OO(0, hy - 0.5f, 11.5f, 10.5f, d.cMain, OUT);        // капюшон
        OO(0, hy + 1f, 8.3f, 7.8f, skin, OUT);                // лицо
        O(0, hy - 6.5f, 11.8f, 3f, d.cAccent);                // мех
        O(-9, hy + 1f, 2.8f, 7f, d.cAccent); O(9, hy + 1f, 2.8f, 7f, d.cAccent);
        // очки
        R(-8.4f, hy - 1.6f, 8.4f, hy + 3.2f, 0xFF1C2A44);
        for (int s = -1; s <= 1; s += 2) { O(s * 4, hy + 0.8f, 3.2f, 2.3f, d.cGlow); O(s * 4 + (float) Math.cos(aim) * 0.8f, hy + 0.8f + (float) Math.sin(aim) * 0.6f, 1.1f, 1.1f, 0xFFFFFFFF); }
        mouth(hy + 5.8f, 0);
        inHand(7, -16 + b, aim, () -> {
            W(-6, -1.6f, 6, 2f, 0xFF4A3A2E); W(-6, -1.6f, 6, -0.4f, 0xFF6A5444);
            W(5, -1.1f, 25, 1.1f, 0xFF2B3140); W(5, -1.1f, 25, 0f, 0xFF4A5468);
            W(10, -3.4f, 17, -1.1f, 0xFF2B3140); D.circle(G, 13.5f * K, -2.6f * K, 1.7f * K, c(d.cGlow));
            W(24, -1.6f, 26, 1.6f, d.cGlow);
        });
        O(7, -15f + b, 2.4f, 2.4f, 0xFF2B3550);
    }

    /** Тесла — изобретатель. */
    private static void tesla(BrawlerDef d, float bob, float step, float aim) {
        int skin = 0xFFF2C29A;
        legs(0xFF2B2B33, 0xFFF2C21B, step, 0.5f);
        float b = -bob * 0.5f;
        RR(-8, -23 + b, 8, -8 + b, d.cMain, OUT);
        L(-8, -10 + b, 8, -20 + b, 2.2f, d.cAccent);
        R(-8, -11 + b, 8, -9.6f + b, d.cAccent);
        // катушка за спиной
        R(-8.5f, -31 + b, -6.5f, -20 + b, 0xFF2B2B33);
        float p = 1f + (float) Math.sin(ET * 9) * 0.2f;
        O(-7.5f, -33 + b, 3f * p, 3f * p, D.alpha(d.cGlow, 0.45f)); O(-7.5f, -33 + b, 2f, 2f, d.cGlow);
        arm(-9.8f, -17 + b, d.cMain, 0xFF444455, 3.4f);
        arm(9.8f, -17 + b, d.cMain, 0xFF444455, 3.4f);
        // искры на перчатках
        for (int s = -1; s <= 1; s += 2) {
            float f = (float) Math.sin(ET * 15 + s * 2);
            if (f > -0.3f) L(s * 9.8f, -13 + b, s * (12 + f * 2), -10 + b + f * 2, 1.0f, d.cGlow);
        }
        float hy = -29.5f + b;
        // шипастые волосы
        for (int i = -3; i <= 3; i++) { O(i * 2.9f, hy - 7.5f - (3 - Math.abs(i)) * 1.4f, 1.7f, 3.4f, 0xFFFFD83A); }
        OO(0, hy, 10.2f, 9.3f, skin, OUT);
        // очки на лбу и на глазах
        RR(-9, hy - 4.5f, 9, hy - 2f, 0xFF3A3A44, OUT);
        eyes(hy + 1f, aim, 3.8f, 2.5f, false);
        for (int s = -1; s <= 1; s += 2) { OO(s * 4.8f, hy - 7.4f, 3.1f, 2.4f, 0xFF3A3A44, OUT); O(s * 4.8f, hy - 7.4f, 2f, 1.4f, d.cGlow); }
        mouth(hy + 5.5f, 1);
    }

    /** Танкуша — стальной здоровяк. */
    private static void tank(BrawlerDef d, float bob, float step, float aim) {
        int skin = 0xFFD9A47E, steel = d.cMain;
        legs(0xFF4E5865, 0xFF2B2F38, step * 0.8f, 1.6f);
        float b = -bob * 0.4f;
        RR(-11.5f, -26 + b, 11.5f, -8 + b, steel, OUT);
        R(-11.5f, -23 + b, 11.5f, -22 + b, D.light(steel, 0.35f));
        R(-11.5f, -12 + b, 11.5f, -10 + b, 0xFF3A3F48);
        R(-2, -12.4f + b, 2, -9.6f + b, d.cAccent);
        for (int s = -1; s <= 1; s += 2) { O(s * 8, -20 + b, 0.9f, 0.9f, 0xFFCFD6DE); O(s * 8, -14 + b, 0.9f, 0.9f, 0xFFCFD6DE); }
        // наплечники
        OO(-13, -23 + b, 5.6f, 5f, D.light(steel, 0.15f), OUT);
        OO(13, -23 + b, 5.6f, 5f, D.light(steel, 0.15f), OUT);
        R(-15, -26 + b, -11, -25 + b, d.cAccent); R(11, -26 + b, 15, -25 + b, d.cAccent);
        // кулаки
        float fl = (float) Math.sin(ET * 4) * 0.4f;
        OO(-15, -12 + b + fl, 5.2f, 5.2f, skin, OUT);
        OO(15, -12 + b - fl, 5.2f, 5.2f, skin, OUT);
        R(-17, -14 + b, -13, -13.3f + b, d.cAccent); R(13, -14 + b, 17, -13.3f + b, d.cAccent);
        float hy = -32.5f + b;
        OO(0, hy, 9.2f, 8.4f, skin, OUT);
        eyes(hy + 1.2f, aim, 3.5f, 2.3f, true);
        mouth(hy + 5.8f, 0);
        // шлем с рогами
        RR(-9.8f, hy - 8.8f, 9.8f, hy - 3f, steel, OUT);
        R(-9.8f, hy - 7f, 9.8f, hy - 6.2f, D.light(steel, 0.4f));
        R(-1.6f, hy - 3f, 1.6f, hy + 3.5f, steel);
        for (int s = -1; s <= 1; s += 2) {
            L(s * 9.8f, hy - 6f, s * 13.5f, hy - 10f, 3.2f, OUT); L(s * 9.8f, hy - 6f, s * 13.5f, hy - 10f, 1.8f, 0xFFF0E6D0);
            L(s * 13.5f, hy - 10f, s * 12.5f, hy - 14f, 3.2f, OUT); L(s * 13.5f, hy - 10f, s * 12.5f, hy - 14f, 1.8f, 0xFFF0E6D0);
        }
    }

    /** Тень — ассасин в капюшоне. */
    private static void shade(BrawlerDef d, float bob, float step, float aim) {
        legs(0xFF22152F, 0xFF120C1B, step, 0.3f);
        float b = -bob * 0.5f;
        // плащ
        RR(-8.5f, -24 + b, 8.5f, -9 + b, d.cMain, OUT);
        R(-8.5f, -9.6f + b, 8.5f, -8f + b, d.cAccent);
        R(-0.8f, -24 + b, 0.8f, -9 + b, D.dark(d.cMain, 0.4f));
        // шарф с анимацией
        float w1 = (float) Math.sin(ET * 7) * 1.5f, w2 = (float) Math.sin(ET * 7 + 1.2f) * 2f, w3 = (float) Math.sin(ET * 7 + 2.4f) * 2.4f;
        RR(-9, -26 + b, 9, -22 + b, d.cAccent, OUT);
        RR(-11 - 0f, -24.5f + b + w1, -8, -20.5f + b + w1, d.cAccent, OUT);
        RR(-15, -23.5f + b + w2, -11, -19.5f + b + w2, D.dark(d.cAccent, 0.15f), OUT);
        RR(-19, -22.5f + b + w3, -15, -18.5f + b + w3, D.dark(d.cAccent, 0.3f), OUT);
        arm(-9.2f, -17 + b, d.cMain, 0xFF333344, 2.8f);
        float hy = -30f + b;
        OO(0, hy, 10.8f, 10f, d.cMain, OUT);                   // капюшон
        OO(0, hy + 1.2f, 7.5f, 7f, 0xFF0C0814, OUT);            // тёмное лицо
        float px = (float) Math.cos(aim) * 0.8f, py = (float) Math.sin(aim) * 0.6f;
        for (int s = -1; s <= 1; s += 2) {
            O(s * 3.3f + px, hy + 1f + py, 2.2f, 2.8f, D.alpha(d.cGlow, 0.5f));
            O(s * 3.3f + px, hy + 1f + py, 1.5f, 2.0f, 0xFFFF6BFF);
            O(s * 3.3f + px, hy + 1f + py, 0.6f, 1.0f, 0xFFFFFFFF);
        }
        R(-1.2f, hy - 10.5f, 1.2f, hy - 6f, D.dark(d.cMain, 0.3f));
        // клинки
        inHand(8, -15 + b, aim, () -> {
            W(-2, -1.1f, 3, 1.1f, 0xFF3A2A44);
            W(3, -2.4f, 4.4f, 2.4f, 0xFFB65CFF);
            W(4.4f, -1.4f, 14, 1.4f, 0xFFDDE3EE); W(4.4f, -1.4f, 14, -0.2f, 0xFFFFFFFF); W(14, -0.8f, 16, 0.8f, 0xFFDDE3EE);
        });
        O(8, -14.5f + b, 2.3f, 2.3f, 0xFF333344);
    }

    /** Мята — полевой лекарь. */
    private static void mint(BrawlerDef d, float bob, float step, float aim) {
        int skin = 0xFFF5CFB0;
        legs(0xFF2F6E4A, 0xFFF4FFF7, step, 0.5f);
        float b = -bob * 0.5f;
        RR(-8.5f, -24 + b, 8.5f, -8 + b, 0xFFF4FFF7, OUT);
        R(-8.5f, -9.6f + b, 8.5f, -8f + b, d.cMain);
        R(-1, -24 + b, 1, -8 + b, 0xFFD8EBDD);
        // красный крест → зелёный
        R(3.2f, -20 + b, 6.6f, -18.6f + b, d.cMain); R(4.2f, -21.4f + b, 5.6f, -17.2f + b, d.cMain);
        // сумка
        RR(-11.5f, -17 + b, -7, -10 + b, 0xFF8A5A3A, OUT);
        R(-10.6f, -14 + b, -8, -13 + b, 0xFFF4FFF7);
        arm(-9.8f, -18 + b, 0xFFF4FFF7, skin, 2.8f);
        float hy = -29.5f + b;
        O(0, hy - 8.5f, 5.5f, 5f, 0xFF2F9A58);                  // пучок
        OO(0, hy - 10.8f, 3.3f, 3.3f, 0xFF3FB86B, OUT);
        OO(0, hy, 10f, 9.3f, skin, OUT);
        // чёлка
        for (int i = -3; i <= 3; i++) O(i * 3f, hy - 6f, 2.4f, 3.6f, i % 2 == 0 ? 0xFF3FB86B : 0xFF2F9A58);
        eyes(hy + 1f, aim, 3.7f, 2.4f, false);
        mouth(hy + 5.5f, 1);
        // очки
        for (int s = -1; s <= 1; s += 2) { D.ring(G, X0 + s * 3.7f * DIR * K, Y0 + (hy + 1f) * K, 3.2f * K, 0.8f * K, c(0xFF2B2F38)); }
        R(-1, hy + 0.6f, 1, hy + 1.4f, 0xFF2B2F38);
        // стетоскоп
        L(-3, -23 + b, -4, -15 + b, 0.9f, 0xFF2B2F38); L(3, -23 + b, 4, -15 + b, 0.9f, 0xFF2B2F38); O(0, -13.5f + b, 1.6f, 1.6f, 0xFFB0B8C4);
        inHand(8, -16 + b, aim, () -> {
            W(-5, -1.1f, 14, 1.1f, 0xFFF4FFF7);
            D.circle(G, 17 * K, 0, 5.4f * K, c(D.alpha(d.cGlow, 0.35f)));
            W(14, -4, 20, 4, c(D.dark(d.cMain, 0.2f))); W(15.4f, -3.4f, 18.6f, 3.4f, d.cGlow); W(13.4f, -1.2f, 21.2f, 1.2f, d.cGlow);
        });
        O(8, -15.5f + b, 2.4f, 2.4f, skin);
    }

    /** Бахарь — шахтёр-подрывник. */
    private static void blaster(BrawlerDef d, float bob, float step, float aim) {
        int skin = 0xFFC98E65;
        legs(0xFF3E3A36, 0xFF1E1B18, step, 0.6f);
        float b = -bob * 0.5f;
        // ракеты за спиной
        for (int i = -1; i <= 1; i++) { RR(-9.5f + i * 2.3f, -32 + b, -7.3f + i * 2.3f, -20 + b, 0xFFB7C0CC, OUT); O(-8.4f + i * 2.3f, -33 + b, 1.2f, 1.9f, 0xFFD0361F); }
        RR(-8.5f, -23 + b, 8.5f, -8 + b, d.cMain, OUT);
        L(-8, -22 + b, 7, -9 + b, 2.2f, 0xFF3A2A1A);
        for (int i = 0; i < 3; i++) O(-5 + i * 4.2f, -16.4f + i * 0.0f + b - (i - 1) * 1.8f, 1.1f, 1.1f, d.cAccent);
        R(-8.5f, -11 + b, 8.5f, -9.4f + b, 0xFF2B2B33);
        arm(-9.8f, -17 + b, d.cMain, skin, 3f);
        float hy = -29.5f + b;
        OO(0, hy, 10.3f, 9.4f, skin, OUT);
        eyes(hy + 0.8f, aim, 3.8f, 2.4f, true);
        // усы
        O(-3, hy + 5.2f, 3.2f, 1.7f, 0xFF1E1B18); O(3, hy + 5.2f, 3.2f, 1.7f, 0xFF1E1B18);
        // каска с лампой
        RR(-10.6f, hy - 8.8f, 10.6f, hy - 3.5f, d.cAccent, OUT);
        R(-10.6f, hy - 7.2f, 10.6f, hy - 6.4f, D.light(d.cAccent, 0.4f));
        R(-12.5f, hy - 4.2f, 12.5f, hy - 2.6f, D.dark(d.cAccent, 0.25f));
        O(0, hy - 8f, 3f, 2.6f, 0xFF3A3A44); O(0, hy - 8f, 2f, 1.6f, 0xFFFFF7B0);
        // бомба в руке
        inHand(8, -15 + b, aim, () -> {
            D.circle(G, 7 * K, 0, 4.6f * K, c(OUT)); D.circle(G, 7 * K, 0, 3.6f * K, c(0xFF2B2B33));
            D.circle(G, 5.8f * K, -1.2f * K, 1.1f * K, c(0xFF6A6A78));
            W(7, -6, 8.2f, -4, 0xFF8A5A3A);
            float sp = (float) Math.sin(ET * 20);
            D.circle(G, 7.6f * K, -7 * K, (1.6f + sp * 0.4f) * K, c(0xFFFFB13B));
            D.circle(G, 7.6f * K, -7 * K, 0.8f * K, c(0xFFFFFFFF));
        });
        O(8, -14.5f + b, 2.4f, 2.4f, skin);
    }
}
