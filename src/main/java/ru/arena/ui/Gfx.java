package ru.arena.ui;

/** Минимальная графическая абстракция. В игре её реализует DrawContext, в тестах — Java2D. */
public interface Gfx {
    void fill(int x1, int y1, int x2, int y2, int argb);
    void text(String s, float x, float y, int argb, boolean shadow);
    int textWidth(String s);
    void push();
    void pop();
    void translate(float x, float y);
    void rotate(float rad);
    void scale(float sx, float sy);
}
