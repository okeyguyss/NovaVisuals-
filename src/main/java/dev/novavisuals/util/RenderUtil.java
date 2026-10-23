package dev.novavisuals.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class RenderUtil {
    private RenderUtil() {}

    private static void begin() {
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
    }

    private static void end() {
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private static void vtx(BufferBuilder b, Matrix4f m, float x, float y, int c) {
        b.vertex(m, x, y, 0f)
                .color(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, ((c >>> 24) & 255) / 255f)
                .endVertex();
    }

    /** Прямоугольник по углам (float, без округления до пикселя). */
    public static void rect(MatrixStack ms, float x1, float y1, float x2, float y2, int color) {
        gradient(ms, x1, y1, x2, y2, color, color, color, color);
    }

    public static void gradientH(MatrixStack ms, float x1, float y1, float x2, float y2, int left, int right) {
        gradient(ms, x1, y1, x2, y2, left, right, right, left);
    }

    public static void gradientV(MatrixStack ms, float x1, float y1, float x2, float y2, int top, int bottom) {
        gradient(ms, x1, y1, x2, y2, top, top, bottom, bottom);
    }

    /** Цвета: верх-лево, верх-право, низ-право, низ-лево. */
    public static void gradient(MatrixStack ms, float x1, float y1, float x2, float y2, int tl, int tr, int br, int bl) {
        if (x2 <= x1 || y2 <= y1) return;
        Matrix4f m = ms.last().pose();
        begin();
        BufferBuilder b = Tessellator.getInstance().getBuilder();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vtx(b, m, x1, y2, bl);
        vtx(b, m, x2, y2, br);
        vtx(b, m, x2, y1, tr);
        vtx(b, m, x1, y1, tl);
        b.end();
        WorldVertexBufferUploader.end(b);
        end();
    }

    /** Скруглённый прямоугольник: x, y, ширина, высота, радиус. */
    public static void round(MatrixStack ms, float x, float y, float w, float h, float r, int color) {
        if (w <= 0 || h <= 0 || ((color >>> 24) & 255) == 0) return;
        r = Math.min(r, Math.min(w, h) / 2f);
        if (r < 0.5f) {
            rect(ms, x, y, x + w, y + h, color);
            return;
        }
        Matrix4f m = ms.last().pose();
        begin();
        BufferBuilder b = Tessellator.getInstance().getBuilder();
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        quad(b, m, x + r, y, x + w - r, y + h, color);
        quad(b, m, x, y + r, x + r, y + h - r, color);
        quad(b, m, x + w - r, y + r, x + w, y + h - r, color);
        b.end();
        WorldVertexBufferUploader.end(b);

        corner(m, x + r, y + r, r, 180, color);
        corner(m, x + w - r, y + r, r, 270, color);
        corner(m, x + w - r, y + h - r, r, 0, color);
        corner(m, x + r, y + h - r, r, 90, color);
        end();
    }

    private static void quad(BufferBuilder b, Matrix4f m, float x1, float y1, float x2, float y2, int c) {
        if (x2 <= x1 || y2 <= y1) return;
        vtx(b, m, x1, y2, c);
        vtx(b, m, x2, y2, c);
        vtx(b, m, x2, y1, c);
        vtx(b, m, x1, y1, c);
    }

    private static void corner(Matrix4f m, float cx, float cy, float r, int startDeg, int c) {
        BufferBuilder b = Tessellator.getInstance().getBuilder();
        b.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        vtx(b, m, cx, cy, c);
        int seg = 10;
        for (int i = 0; i <= seg; i++) {
            double a = Math.toRadians(startDeg + i * 90.0 / seg);
            vtx(b, m, (float) (cx + Math.cos(a) * r), (float) (cy + Math.sin(a) * r), c);
        }
        b.end();
        WorldVertexBufferUploader.end(b);
    }

    /** Кольцо/дуга: radius — средний радиус, thickness — толщина, углы в градусах (0 = вправо, по часовой). */
    public static void ring(MatrixStack ms, float cx, float cy, float radius, float thickness,
                            float startDeg, float endDeg, int c) {
        if (endDeg <= startDeg || ((c >>> 24) & 255) == 0) return;
        Matrix4f m = ms.last().pose();
        float in = Math.max(0f, radius - thickness / 2f), out = radius + thickness / 2f;
        int seg = Math.max(8, (int) ((endDeg - startDeg) / 6f));
        begin();
        BufferBuilder b = Tessellator.getInstance().getBuilder();
        b.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= seg; i++) {
            double a = Math.toRadians(startDeg + (endDeg - startDeg) * i / (double) seg);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            vtx(b, m, cx + cos * out, cy + sin * out, c);
            vtx(b, m, cx + cos * in, cy + sin * in, c);
        }
        b.end();
        WorldVertexBufferUploader.end(b);
        end();
    }

    /** Текст, который не рисуется при почти нулевой прозрачности (иначе MC рисует его непрозрачным). */
    public static void text(MatrixStack ms, String s, float x, float y, int color, boolean shadow) {
        if (((color >>> 24) & 255) < 8) return;
        FontRenderer f = Minecraft.getInstance().font;
        if (shadow) f.drawShadow(ms, s, x, y, color);
        else f.draw(ms, s, x, y, color);
    }

    /** Текст, переливающийся цветами темы по буквам. */
    public static float gradientText(MatrixStack ms, String s, float x, float y, float alpha, int step) {
        FontRenderer f = Minecraft.getInstance().font;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            text(ms, ch, x, y, ColorUtil.accent(i * step, alpha), true);
            x += f.width(ch);
        }
        return x;
    }

    /** Обрезка рисования областью в GUI-координатах. */
    public static void scissor(float x, float y, float w, float h) {
        MainWindow win = Minecraft.getInstance().getWindow();
        double s = win.getGuiScale();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x * s), (int) (win.getHeight() - (y + h) * s),
                Math.max(0, (int) (w * s)), Math.max(0, (int) (h * s)));
    }

    public static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
