package ru.arena.mc;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import ru.arena.ui.Gfx;

/**
 * Мост между интерфейсом мода и Minecraft. Все прямоугольники копятся в один буфер
 * (иначе тысячи отдельных отрисовок за кадр), текст рисуется стандартным шрифтом.
 */
public final class McGfx implements Gfx {
    private DrawContext ctx;
    private final TextRenderer font = MinecraftClient.getInstance().textRenderer;

    // аффинное преобразование: x' = a*x + c*y + e ; y' = b*x + d*y + f
    private float a = 1, b = 0, c = 0, d = 1, e = 0, f = 0;
    private final ArrayDeque<float[]> stack = new ArrayDeque<>();

    private BufferBuilder buf;
    private int quads;
    private Matrix4f base;

    public void begin(DrawContext context) {
        ctx = context;
        a = 1; b = 0; c = 0; d = 1; e = 0; f = 0;
        stack.clear();
        base = ctx.getMatrices().peek().getPositionMatrix();
        buf = null;
        quads = 0;
    }

    public void end() { flush(); }

    private void startBuffer() {
        buf = Tessellator.getInstance().getBuffer();
        buf.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        quads = 0;
    }

    private void flush() {
        if (buf == null || quads == 0) { buf = null; return; }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.enableCull();
        buf = null;
        quads = 0;
    }

    private void vertex(float x, float y, int argb) {
        float px = a * x + c * y + e, py = b * x + d * y + f;
        buf.vertex(base, px, py, 0f)
                .color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255)
                .next();
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        if (x2 <= x1 || y2 <= y1 || (argb >>> 24) == 0) return;
        if (buf == null) startBuffer();
        vertex(x1, y1, argb);
        vertex(x1, y2, argb);
        vertex(x2, y2, argb);
        vertex(x2, y1, argb);
        if (++quads >= 6000) { flush(); }
    }

    @Override
    public void text(String s, float x, float y, int argb, boolean shadow) {
        if (s == null || s.isEmpty()) return;
        flush();
        var m = ctx.getMatrices();
        m.push();
        m.translate(e, f, 0f);
        m.scale(a, d, 1f);
        ctx.drawText(font, s, Math.round(x), Math.round(y), argb, shadow);
        m.pop();
    }

    @Override public int textWidth(String s) { return font.getWidth(s); }

    @Override
    public void push() { stack.push(new float[]{a, b, c, d, e, f}); }

    @Override
    public void pop() {
        float[] t = stack.pop();
        a = t[0]; b = t[1]; c = t[2]; d = t[3]; e = t[4]; f = t[5];
    }

    @Override
    public void translate(float tx, float ty) {
        e += a * tx + c * ty;
        f += b * tx + d * ty;
    }

    @Override
    public void scale(float sx, float sy) {
        a *= sx; b *= sx; c *= sy; d *= sy;
    }

    @Override
    public void rotate(float rad) {
        float cs = (float) Math.cos(rad), sn = (float) Math.sin(rad);
        float na = a * cs + c * sn, nb = b * cs + d * sn;
        float nc = -a * sn + c * cs, nd = -b * sn + d * cs;
        a = na; b = nb; c = nc; d = nd;
    }
}
