package ru.arena.game;

/** Событие для визуальных эффектов (клиент превращает их в частицы). */
public final class Fx {
    public static final int HIT = 0, EXPLOSION = 1, DASH = 2, BEAM = 3, SLASH = 4, SUPER = 5, DEATH = 6, HEAL = 7, SPAWN = 8;
    public final int type, style;
    public final float x, y, x2, y2;

    public Fx(int type, int style, float x, float y, float x2, float y2) {
        this.type = type; this.style = style; this.x = x; this.y = y; this.x2 = x2; this.y2 = y2;
    }
}
