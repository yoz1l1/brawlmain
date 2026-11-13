package ru.arena.mc;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.arena.ui.ClientState;
import ru.arena.ui.MatchView;
import ru.arena.ui.MenuView;
import ru.arena.ui.View;

/** Единственный экран мода: показывает меню или бой в зависимости от состояния. */
public final class ArenaScreen extends Screen {
    private final MenuView menu = new MenuView();
    private final MatchView match = new MatchView();
    private McGfx gfx;
    private long last = System.nanoTime();
    private int mouseX, mouseY;

    public ArenaScreen() { super(Text.literal("Боевая Арена")); }

    private View current() { return ClientState.I.match != null ? match : menu; }

    @Override
    public void render(DrawContext context, int mx, int my, float delta) {
        if (gfx == null) gfx = new McGfx();
        mouseX = mx; mouseY = my;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
        last = now;
        gfx.begin(context);
        current().render(gfx, width, height, mx, my, dt);
        gfx.end();
    }

    @Override
    public void tick() { current().tick(width, height, mouseX, mouseY); }

    @Override public boolean shouldPause() { return false; }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        current().mouseDown((int) x, (int) y, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        current().mouseUp((int) x, (int) y, button);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { close(); return true; } // Esc
        current().key(keyCode, true);
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        current().key(keyCode, false);
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        current().typed(chr);
        return true;
    }
}
