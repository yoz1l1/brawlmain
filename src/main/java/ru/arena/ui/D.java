package ru.arena.ui;

import java.util.ArrayList;
import java.util.List;

/** Рисовальные помощники: овалы, линии, текст с обводкой, панели, переносы строк. */
public final class D {
    private D() {}

    public static int alpha(int argb, float a) {
        int al = Math.max(0, Math.min(255, Math.round(((argb >>> 24) & 0xFF) * a)));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    public static int rgb(int r, int g, int b) {
        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    public static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255, aa = (a >>> 24) & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255, ba = (b >>> 24) & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static int dark(int c, float k) { return mix(c, 0xFF000000, k); }
    public static int light(int c, float k) { return mix(c, 0xFFFFFFFF, k); }

    public static void rect(Gfx g, float x1, float y1, float x2, float y2, int c) {
        g.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y2), c);
    }

    public static void oval(Gfx g, float cx, float cy, float rx, float ry, int c) {
        if (rx <= 0 || ry <= 0) return;
        int y0 = (int) Math.floor(cy - ry), y1 = (int) Math.ceil(cy + ry);
        if (y1 - y0 <= 1) { g.fill(Math.round(cx - rx), y0, Math.round(cx + rx), y0 + 1, c); return; }
        for (int y = y0; y < y1; y++) {
            float dy = (y + 0.5f - cy) / ry;
            if (Math.abs(dy) > 1) continue;
            float hw = rx * (float) Math.sqrt(1 - dy * dy);
            g.fill(Math.round(cx - hw), y, Math.round(cx + hw), y + 1, c);
        }
    }

    public static void circle(Gfx g, float cx, float cy, float r, int c) { oval(g, cx, cy, r, r, c); }

    public static void ring(Gfx g, float cx, float cy, float r, float w, int c) {
        int segs = Math.max(16, (int) (r * 1.6f));
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * 2 * Math.PI / segs), a1 = (float) ((i + 1) * 2 * Math.PI / segs);
            line(g, cx + (float) Math.cos(a0) * r, cy + (float) Math.sin(a0) * r,
                    cx + (float) Math.cos(a1) * r, cy + (float) Math.sin(a1) * r, w, c);
        }
    }

    public static void line(Gfx g, float x1, float y1, float x2, float y2, float w, int c) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) { circle(g, x1, y1, w / 2, c); return; }
        if (Math.abs(dx) < 0.01f || Math.abs(dy) < 0.01f) {
            rect(g, Math.min(x1, x2) - (Math.abs(dy) < 0.01f ? 0 : w / 2), Math.min(y1, y2) - (Math.abs(dx) < 0.01f ? 0 : w / 2),
                    Math.max(x1, x2) + (Math.abs(dy) < 0.01f ? 0 : w / 2), Math.max(y1, y2) + (Math.abs(dx) < 0.01f ? 0 : w / 2), c);
            return;
        }
        g.push();
        g.translate(x1, y1);
        g.rotate((float) Math.atan2(dy, dx));
        g.fill(0, Math.round(-w / 2), Math.round(len), Math.round(w / 2) == Math.round(-w / 2) ? Math.round(-w / 2) + 1 : Math.round(w / 2), c);
        g.pop();
    }

    public static void text(Gfx g, String s, float x, float y, int c) { g.text(s, x, y, c, true); }

    public static void ctext(Gfx g, String s, float cx, float y, int c) {
        g.text(s, cx - g.textWidth(s) / 2f, y, c, true);
    }

    public static void outlined(Gfx g, String s, float x, float y, int c, int outline) {
        g.text(s, x - 1, y, outline, false);
        g.text(s, x + 1, y, outline, false);
        g.text(s, x, y - 1, outline, false);
        g.text(s, x, y + 1, outline, false);
        g.text(s, x, y, c, false);
    }

    /** Крупный текст с обводкой по центру. */
    public static void big(Gfx g, String s, float cx, float y, float scale, int c, int outline) {
        g.push();
        g.translate(cx, y);
        g.scale(scale, scale);
        float x = -g.textWidth(s) / 2f;
        outlined(g, s, x, 0, c, outline);
        g.pop();
    }

    public static void scaled(Gfx g, String s, float x, float y, float scale, int c, boolean shadow) {
        g.push();
        g.translate(x, y);
        g.scale(scale, scale);
        g.text(s, 0, 0, c, shadow);
        g.pop();
    }

    public static List<String> wrap(Gfx g, String s, int maxW) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : s.split(" ")) {
            String t = cur.length() == 0 ? w : cur + " " + w;
            if (g.textWidth(t) > maxW && cur.length() > 0) { out.add(cur.toString()); cur = new StringBuilder(w); }
            else cur = new StringBuilder(t);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    /** Скруглённый прямоугольник (углы срезаны). */
    public static void rrect(Gfx g, float x1, float y1, float x2, float y2, float r, int c) {
        rect(g, x1 + r, y1, x2 - r, y2, c);
        rect(g, x1, y1 + r, x2, y2 - r, c);
    }

    /** Кнопка/панель в «мультяшном» стиле: тёмная обводка, светлый верх, тёмный низ. */
    public static void panel(Gfx g, float x1, float y1, float x2, float y2, int base) {
        rrect(g, x1 - 1, y1 - 1, x2 + 1, y2 + 1, 2, 0xFF0B0F2A);
        rrect(g, x1, y1, x2, y2, 2, dark(base, 0.35f));
        rrect(g, x1, y1, x2, y2 - 2, 2, base);
        rect(g, x1 + 1, y1 + 1, x2 - 1, y1 + 2, light(base, 0.35f));
    }

    public static boolean in(float x, float y, float x1, float y1, float x2, float y2) {
        return x >= x1 && x < x2 && y >= y1 && y < y2;
    }
}
