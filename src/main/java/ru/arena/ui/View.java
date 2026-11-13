package ru.arena.ui;

/** Экран мода (меню или матч). Минecraft-обёртка передаёт сюда события. */
public interface View {
    void render(Gfx g, int w, int h, int mx, int my, float dt);
    void tick(int w, int h, int mx, int my);
    boolean mouseDown(int mx, int my, int button);
    void mouseUp(int mx, int my, int button);
    void key(int code, boolean down);
    void typed(char c);
}
