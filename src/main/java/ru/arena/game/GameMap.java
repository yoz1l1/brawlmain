package ru.arena.game;

import java.util.ArrayList;
import java.util.List;

/** Карта боя: 36x24 клеток, строится зеркально из четверти (поэтому честная для обеих команд). */
public final class GameMap {
    public static final int W = 36, H = 24;
    public static final int GROUND = 0, WALL = 1, BUSH = 2, WATER = 3, CRATE = 4;
    public static final int COUNT = 3;
    public static final String[] NAMES = {"Зелёная долина", "Огненный каньон", "Ледяной пик"};

    // Верхняя левая четверть (18 x 12). S = точка появления, # стена, b куст, ~ вода/лава/пропасть, x ящик
    private static final String[][] Q = {
        { // Зелёная долина
            "......b....##.....",
            ".##..bbb....#.....",
            ".#....b.....#.bb..",
            "S.......xx.....bb.",
            "..S..........x....",
            "S......##.........",
            ".......#..bb......",
            ".##........bb..~~.",
            ".#.....x.......~~.",
            "....bb..x...bb....",
            "....bb......bbb.bb",
            ".........xx..bbbbb",
        },
        { // Огненный каньон
            "..........#.....~~",
            "..........#.....~~",
            ".S...#....#..bb.~~",
            "...S.#.....bbb....",
            ".S...#............",
            "...xx.......##....",
            ".......~~...#.....",
            "..bb...~~....x....",
            "..bb..............",
            ".....#.....bb..xx.",
            "..#..#.....bb.....",
            "..#.........~.....",
        },
        { // Ледяной пик
            "..#...............",
            "..#...bb....xx....",
            ".S....bb..........",
            "...S.......##.....",
            ".S.........#..b...",
            "......x....#..bb..",
            "..##..x...........",
            "..##.....~~.......",
            "......b..~~...xx..",
            ".....bbb..........",
            "..x...b....##.bb..",
            "..x........##.bb..",
        }
    };

    public final int index;
    public final byte[][] t = new byte[H][W];
    public final List<float[]> spawns0 = new ArrayList<>();
    public final List<float[]> spawns1 = new ArrayList<>();

    public GameMap(int index) {
        this.index = ((index % COUNT) + COUNT) % COUNT;
        String[] q = Q[this.index];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int qx = x < W / 2 ? x : W - 1 - x;
                int qy = y < H / 2 ? y : H - 1 - y;
                String row = q[qy];
                char c = qx < row.length() ? row.charAt(qx) : '.';
                int v = GROUND;
                switch (c) {
                    case '#': v = WALL; break;
                    case 'b': v = BUSH; break;
                    case '~': v = WATER; break;
                    case 'x': v = CRATE; break;
                    case 'S':
                        (x < W / 2 ? spawns0 : spawns1).add(new float[]{x + 0.5f, y + 0.5f});
                        break;
                    default: break;
                }
                t[y][x] = (byte) v;
            }
        }
    }

    public int get(int x, int y) {
        if (x < 0 || y < 0 || x >= W || y >= H) return WALL;
        return t[y][x];
    }

    public boolean solid(int x, int y) {
        int v = get(x, y);
        return v == WALL || v == WATER || v == CRATE;
    }

    public boolean blocksShot(int x, int y) {
        int v = get(x, y);
        return v == WALL || v == CRATE;
    }

    public boolean bushAt(float x, float y) {
        return get((int) Math.floor(x), (int) Math.floor(y)) == BUSH;
    }

    public boolean circleHits(float cx, float cy, float r) {
        int x0 = (int) Math.floor(cx - r), x1 = (int) Math.floor(cx + r);
        int y0 = (int) Math.floor(cy - r), y1 = (int) Math.floor(cy + r);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                if (!solid(x, y)) continue;
                float nx = Math.max(x, Math.min(cx, x + 1));
                float ny = Math.max(y, Math.min(cy, y + 1));
                float dx = cx - nx, dy = cy - ny;
                if (dx * dx + dy * dy < r * r) return true;
            }
        }
        return false;
    }

    public boolean lineBlocked(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        int n = Math.max(1, (int) Math.ceil(len / 0.25f));
        for (int i = 1; i <= n; i++) {
            float k = (float) i / n;
            if (blocksShot((int) Math.floor(x1 + dx * k), (int) Math.floor(y1 + dy * k))) return true;
        }
        return false;
    }

    /** Длина луча до ближайшей преграды для выстрела (не больше max). */
    public float rayLimit(float x, float y, float dx, float dy, float max) {
        float s = 0;
        while (s < max) {
            s += 0.2f;
            if (blocksShot((int) Math.floor(x + dx * s), (int) Math.floor(y + dy * s))) return Math.max(0, s - 0.2f);
        }
        return max;
    }
}
