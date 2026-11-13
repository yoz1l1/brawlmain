package ru.arena.ui;

import java.util.ArrayList;
import java.util.List;
import ru.arena.net.Proto;

/** Общая часть экранов: масштаб под любое окно, кнопки, поля ввода, уведомления и приглашения. */
public abstract class BaseView implements View {
    protected static final class Btn { float x1, y1, x2, y2; Runnable act; }
    protected static final class Field {
        public String text = ""; public int max = 16; public boolean digits;
        public Field(int max, boolean digits) { this.max = max; this.digits = digits; }
    }

    protected final ClientState st = ClientState.I;
    protected final List<Btn> btns = new ArrayList<>();
    protected float s = 1, vw = 480, vh = 270;
    protected float offX, offY;           // смещение области дизайна
    protected float mvx, mvy;             // мышь в координатах дизайна
    protected Field focus;
    protected float time;

    protected void layout(int w, int h, int mx, int my) {
        s = Math.min(w / 480f, h / 270f);
        vw = w / s; vh = h / s;
        mvx = mx / s - offX; mvy = my / s - offY;
    }

    protected boolean hover(float x1, float y1, float x2, float y2) { return D.in(mvx, mvy, x1, y1, x2, y2); }

    protected void hit(float x1, float y1, float x2, float y2, Runnable act) {
        Btn b = new Btn(); b.x1 = x1; b.y1 = y1; b.x2 = x2; b.y2 = y2; b.act = act; btns.add(b);
    }

    /** Мультяшная кнопка с подписью. */
    protected void button(Gfx g, String label, float x, float y, float w, float h, int color, boolean enabled, Runnable act) {
        boolean hv = enabled && hover(x, y, x + w, y + h);
        int base = enabled ? (hv ? D.light(color, 0.18f) : color) : 0xFF59607A;
        D.panel(g, x, y, x + w, y + h, base);
        int tc = enabled ? 0xFFFFFFFF : 0xFFAAB0C8;
        float ty = y + (h - 9) / 2f - 1;
        D.outlined(g, label, x + (w - g.textWidth(label)) / 2f, ty, tc, 0xFF14102A);
        if (enabled && act != null) hit(x, y, x + w, y + h, act);
    }

    protected void field(Gfx g, Field f, float x, float y, float w, float h, String placeholder) {
        boolean foc = focus == f;
        D.rect(g, x - 1, y - 1, x + w + 1, y + h + 1, foc ? 0xFFFFD84D : 0xFF0B0F2A);
        D.rect(g, x, y, x + w, y + h, 0xFF1B2244);
        String t = f.text;
        if (t.isEmpty() && !foc) g.text(placeholder, x + 4, y + (h - 8) / 2f, 0xFF7A83A8, false);
        else {
            String shown = t;
            while (g.textWidth(shown) > w - 8 && shown.length() > 1) shown = shown.substring(1);
            g.text(shown + (foc && (System.currentTimeMillis() / 450) % 2 == 0 ? "_" : ""), x + 4, y + (h - 8) / 2f, 0xFFFFFFFF, false);
        }
        hit(x, y, x + w, y + h, () -> focus = f);
    }

    /** Уведомление снизу и приглашения сверху. Рисуется в координатах дизайна. */
    protected void overlays(Gfx g, float cx, float top, float bottom) {
        long now = System.currentTimeMillis();
        if (now < st.toastUntil && !st.toast.isEmpty()) {
            int w = g.textWidth(st.toast) + 16;
            D.panel(g, cx - w / 2f, top, cx + w / 2f, top + 16, 0xFF2B3566);
            D.outlined(g, st.toast, cx - g.textWidth(st.toast) / 2f, top + 4, 0xFFFFFFFF, 0xFF14102A);
        }
        st.invites.removeIf(i -> i.expires < now);
        float y = top + (now < st.toastUntil && !st.toast.isEmpty() ? 20 : 0);
        for (ClientState.Invite iv : new ArrayList<>(st.invites)) {
            D.panel(g, cx - 130, y, cx + 130, y + 30, 0xFF2D6BD0);
            D.outlined(g, iv.from + " зовёт вас в бой!", cx - 124, y + 4, 0xFFFFFFFF, 0xFF14102A);
            g.text("Комната #" + iv.room, cx - 124, y + 17, 0xFFD6E4FF, false);
            final ClientState.Invite fiv = iv;
            button(g, "Принять", cx + 22, y + 15, 52, 13, 0xFF2FBF5B, true, () -> {
                st.invites.remove(fiv);
                st.net.action(Proto.A_JOIN, fiv.room, "");
            });
            button(g, "Отклонить", cx + 78, y + 15, 50, 13, 0xFFC94444, true, () -> st.invites.remove(fiv));
            y += 33;
        }
    }

    // ------------------------------------------------------------- ввод

    @Override
    public boolean mouseDown(int mx, int my, int button) {
        float x = mx / s - offX, y = my / s - offY;
        for (int i = btns.size() - 1; i >= 0; i--) {
            Btn b = btns.get(i);
            if (D.in(x, y, b.x1, b.y1, b.x2, b.y2)) {
                if (button == 0) {
                    focus = null; // клик по кнопке снимает фокус; поле выставит его заново
                    b.act.run();
                    if (st.net != null) st.net.sound("click");
                }
                return true;
            }
        }
        focus = null;
        return false;
    }

    @Override public void mouseUp(int mx, int my, int button) {}
    @Override public void tick(int w, int h, int mx, int my) {}

    @Override
    public void key(int code, boolean down) {
        if (!down || focus == null) return;
        if (code == 259 && !focus.text.isEmpty()) focus.text = focus.text.substring(0, focus.text.length() - 1); // Backspace
        else if (code == 257 || code == 335) onEnter(focus);
    }

    protected void onEnter(Field f) {}

    @Override
    public void typed(char c) {
        if (focus == null || c < 32 || c == 127) return;
        if (focus.digits && (c < '0' || c > '9')) return;
        if (!focus.digits && !(Character.isLetterOrDigit(c) || c == '_')) return;
        if (focus.text.length() < focus.max) focus.text += c;
    }
}
