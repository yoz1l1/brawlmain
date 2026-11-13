package ru.arena.ui;

import ru.arena.game.GameMap;

/** Рисует карту в «косой» проекции: земля, затем (по рядам) стены, ящики и кусты с высотой. */
public final class MapRenderer {
    private MapRenderer() {}

    private static int hash(int x, int y, int s) {
        int h = x * 374761393 + y * 668265263 + s * 1274126177;
        h = (h ^ (h >> 13)) * 1274126177;
        return (h ^ (h >> 16)) & 0x7FFFFFFF;
    }

    private static final int[][] GROUND = {{0xFF62AE45, 0xFF5AA43F}, {0xFFD6995C, 0xFFCC8F52}, {0xFFE2EFF8, 0xFFD6E7F3}};
    private static final int[] WALL = {0xFF8C929E, 0xFF9A5238, 0xFF8FC8E8};
    private static final int[] WALL_TOP = {0xFFB4BAC6, 0xFFC8714F, 0xFFD8F1FF};
    private static final int[] BUSH_A = {0xFF2C8C3C, 0xFF7C8E3A, 0xFF3E9482};
    private static final int[] BUSH_B = {0xFF47B04F, 0xFFA3B04A, 0xFF6CC0AC};
    private static final int[] DEEP = {0xFF3B8FD6, 0xFFE8541C, 0xFF2B5FA8};

    /** Земля, вода/лава и декор. */
    public static void ground(Gfx g, GameMap m, float ox, float oy, float tile, int x0, int x1, int y0, int y1, float time) {
        int th = m.index;
        for (int y = Math.max(0, y0); y <= Math.min(GameMap.H - 1, y1); y++) {
            for (int x = Math.max(0, x0); x <= Math.min(GameMap.W - 1, x1); x++) {
                float px = ox + x * tile, py = oy + y * tile;
                int t = m.get(x, y);
                int base = GROUND[th][(x + y) & 1];
                if (t == GameMap.WATER) {
                    int c = DEEP[th];
                    D.rect(g, px, py, px + tile, py + tile, c);
                    for (int i = 0; i < 3; i++) {
                        float wy = py + tile * (0.2f + i * 0.3f);
                        float wo = (float) Math.sin(time * 1.6f + x * 0.9f + i * 2) * tile * 0.12f;
                        D.rect(g, px + tile * 0.15f + wo, wy, px + tile * 0.6f + wo, wy + 1.4f, th == 1 ? 0xFFFFC25A : 0x66FFFFFF);
                    }
                    if (th == 1) { // тёмная корка у лавы
                        if (!m.solid(x, y - 1)) D.rect(g, px, py, px + tile, py + 3, 0xFF6A2A18);
                        D.rect(g, px, py + tile - 2, px + tile, py + tile, 0x33000000);
                    }
                    continue;
                }
                D.rect(g, px, py, px + tile, py + tile, base);
                int h = hash(x, y, 1);
                switch (h % 11) {
                    case 0: case 1: { // пучки травы / трещины / снежинки
                        int c = th == 0 ? 0xFF4D9A38 : th == 1 ? 0xFFB57C46 : 0xFFFFFFFF;
                        float tx = px + tile * (0.2f + (h >> 4) % 6 / 10f), ty = py + tile * (0.3f + (h >> 8) % 5 / 10f);
                        D.rect(g, tx, ty, tx + 1.5f, ty + 4, c); D.rect(g, tx + 3, ty + 1, tx + 4.5f, ty + 4, c); D.rect(g, tx - 3, ty + 1.5f, tx - 1.5f, ty + 4, c);
                        break;
                    }
                    case 2: { // цветы / камушки
                        float tx = px + tile * 0.55f, ty = py + tile * 0.3f;
                        if (th == 0) { int c = (h >> 6) % 2 == 0 ? 0xFFFFE066 : 0xFFFF8AB0; D.rect(g, tx, ty, tx + 3, ty + 3, c); D.rect(g, tx + 1, ty + 1, tx + 2, ty + 2, 0xFFFFFFFF); }
                        else { D.rect(g, tx, ty + 2, tx + 5, ty + 5, D.dark(base, 0.18f)); D.rect(g, tx, ty + 2, tx + 5, ty + 3, D.light(base, 0.2f)); }
                        break;
                    }
                    default: break;
                }
            }
        }
    }

    /** Возвышающиеся объекты одного ряда: стены, ящики. */
    public static void raised(Gfx g, GameMap m, int row, float ox, float oy, float tile, int x0, int x1) {
        int th = m.index;
        for (int x = Math.max(0, x0); x <= Math.min(GameMap.W - 1, x1); x++) {
            int t = m.get(x, row);
            if (t != GameMap.WALL && t != GameMap.CRATE) continue;
            float px = ox + x * tile, py = oy + row * tile;
            float hgt = tile * 0.42f;
            if (t == GameMap.WALL) {
                int front = D.dark(WALL[th], 0.18f), top = WALL_TOP[th];
                D.rect(g, px - 1, py - hgt - 1, px + tile + 1, py + tile + 1, 0xFF14102A);
                D.rect(g, px, py + tile - hgt, px + tile, py + tile, front);
                // кладка
                D.rect(g, px, py + tile - hgt * 0.5f, px + tile, py + tile - hgt * 0.5f + 1, D.dark(front, 0.25f));
                D.rect(g, px + tile * 0.3f, py + tile - hgt, px + tile * 0.3f + 1, py + tile - hgt * 0.5f, D.dark(front, 0.25f));
                D.rect(g, px + tile * 0.7f, py + tile - hgt * 0.5f, px + tile * 0.7f + 1, py + tile, D.dark(front, 0.25f));
                D.rect(g, px, py - hgt, px + tile, py + tile - hgt, top);
                D.rect(g, px + 1, py - hgt + 1, px + tile - 1, py - hgt + 3, D.light(top, 0.35f));
                int hh = hash(x, row, 7) % 5;
                if (hh == 0) { D.rect(g, px + 5, py - hgt + 6, px + 9, py - hgt + 9, th == 2 ? 0xFFFFFFFF : D.dark(top, 0.12f)); }
                if (th == 0 && hh == 1) { D.rect(g, px + 3, py - hgt + 2, px + tile - 6, py - hgt + 5, 0xFF6DB84F); }
            } else {
                int wood = th == 2 ? 0xFFB68A55 : 0xFFC98B45;
                D.rect(g, px + 1, py - hgt * 0.5f - 1, px + tile - 1, py + tile - 1, 0xFF14102A);
                D.rect(g, px + 2, py + tile - hgt - 2, px + tile - 2, py + tile - 2, D.dark(wood, 0.2f));
                D.rect(g, px + 2, py - hgt * 0.5f, px + tile - 2, py + tile - hgt - 2, D.light(wood, 0.15f));
                D.rect(g, px + 2, py + tile - hgt * 0.5f - 1, px + tile - 2, py + tile - hgt * 0.5f, D.dark(wood, 0.45f));
                D.line(g, px + 3, py - hgt * 0.5f + 1, px + tile - 3, py + tile - hgt - 3, 1.6f, D.dark(wood, 0.35f));
                D.line(g, px + tile - 3, py - hgt * 0.5f + 1, px + 3, py + tile - hgt - 3, 1.6f, D.dark(wood, 0.35f));
            }
        }
    }

    /** Кусты одного ряда. alphaFor возвращает прозрачность куста (когда в нём стоит союзник — меньше). */
    public static void bushes(Gfx g, GameMap m, int row, float ox, float oy, float tile, int x0, int x1, float time, float[] alphaRow) {
        int th = m.index;
        for (int x = Math.max(0, x0); x <= Math.min(GameMap.W - 1, x1); x++) {
            if (m.get(x, row) != GameMap.BUSH) continue;
            float a = alphaRow == null ? 1f : alphaRow[x];
            float px = ox + x * tile, py = oy + row * tile;
            float sway = (float) Math.sin(time * 1.8f + x * 0.7f + row) * 0.8f;
            int dark = D.alpha(BUSH_A[th], a), light = D.alpha(BUSH_B[th], a), edge = D.alpha(0xFF14301A, a);
            // тёмная основа, несколько пышных «шапок», блики
            float[][] blobs = {{0.28f, 0.62f, 0.34f}, {0.72f, 0.62f, 0.34f}, {0.5f, 0.4f, 0.4f}, {0.3f, 0.3f, 0.28f}, {0.72f, 0.28f, 0.28f}, {0.5f, 0.78f, 0.3f}};
            for (float[] b : blobs) D.circle(g, px + tile * b[0] + sway * (1 - b[1]), py + tile * b[1], tile * (b[2] + 0.06f), edge);
            for (float[] b : blobs) D.circle(g, px + tile * b[0] + sway * (1 - b[1]), py + tile * b[1], tile * b[2], dark);
            for (float[] b : blobs) D.circle(g, px + tile * b[0] - 1.5f + sway * (1 - b[1]), py + tile * b[1] - 2.5f, tile * b[2] * 0.62f, light);
            int h = hash(x, row, 3);
            if (h % 3 == 0) D.circle(g, px + tile * 0.62f, py + tile * 0.35f, 1.6f, D.alpha(th == 0 ? 0xFFFF6B8A : 0xFFFFF2B0, a));
            if (th == 2) { D.circle(g, px + tile * 0.4f, py + tile * 0.22f, 2.2f, D.alpha(0xFFFFFFFF, a)); D.circle(g, px + tile * 0.66f, py + tile * 0.45f, 1.8f, D.alpha(0xFFFFFFFF, a)); }
        }
    }
}
