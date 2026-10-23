package dev.novavisuals.module.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.novavisuals.module.Module;
import dev.novavisuals.module.settings.BoolSetting;
import dev.novavisuals.module.settings.ModeSetting;
import dev.novavisuals.module.settings.NumberSetting;
import dev.novavisuals.util.ColorUtil;
import dev.novavisuals.util.RenderUtil;
import net.minecraft.util.math.MathHelper;

/** Кастомный прицел: круг, квадрат, крест или точка + индикатор перезарядки удара. */
public class Crosshair extends Module {
    private final ModeSetting shape = add(new ModeSetting("shape", "Форма", 0, "Круг", "Квадрат", "Крест", "Точка"));
    private final NumberSetting size = add(new NumberSetting("size", "Размер", 4, 1, 15, 0.5));
    private final NumberSetting thick = add(new NumberSetting("thick", "Толщина", 1, 0.5, 4, 0.5));
    private final NumberSetting gap = add(new NumberSetting("gap", "Отступ (крест)", 2, 0, 10, 0.5));
    private final BoolSetting dot = add(new BoolSetting("dot", "Точка в центре", true));
    private final BoolSetting outline = add(new BoolSetting("outline", "Обводка", true));
    private final BoolSetting dynamic = add(new BoolSetting("dynamic", "Расширяется при беге", false));
    private final BoolSetting cooldown = add(new BoolSetting("cooldown", "Перезарядка удара", true));
    private final ModeSetting color = add(new ModeSetting("color", "Цвет", 0, "Тема", "Белый", "Свой"));
    private final NumberSetting r = add(new NumberSetting("r", "Красный", 255, 0, 255, 1));
    private final NumberSetting g = add(new NumberSetting("g", "Зелёный", 255, 0, 255, 1));
    private final NumberSetting b = add(new NumberSetting("b", "Синий", 255, 0, 255, 1));

    private float spread;

    public Crosshair() {
        super("crosshair", "Прицел", "Кастомный прицел: круг, квадрат, крест, точка", true);
        gap.visibleWhen(() -> shape.is(2));
        r.visibleWhen(() -> color.is(2));
        g.visibleWhen(() -> color.is(2));
        b.visibleWhen(() -> color.is(2));
    }

    private int mainColor() {
        if (color.is(1)) return 0xFFFFFFFF;
        if (color.is(2)) return 0xFF000000 | (r.get().intValue() << 16) | (g.get().intValue() << 8) | b.get().intValue();
        return ColorUtil.accent(0);
    }

    /** @return true если наш прицел нарисован и ванильный надо скрыть */
    public boolean render(MatrixStack ms) {
        if (mc.player == null) return false;
        if (!mc.options.getCameraType().isFirstPerson()) return true; // в 3-м лице прицела нет, как в ванилле
        if (mc.options.renderDebug) return false; // F3 — оставляем ванильные оси

        float cx = mc.getWindow().getGuiScaledWidth() / 2f;
        float cy = mc.getWindow().getGuiScaledHeight() / 2f;

        float target = 0f;
        if (dynamic.get()) {
            double vx = mc.player.getX() - mc.player.xo, vz = mc.player.getZ() - mc.player.zo;
            target = (float) Math.min(4.0, Math.sqrt(vx * vx + vz * vz) * 12.0);
            if (!mc.player.isOnGround()) target += 1.5f;
        }
        spread += (target - spread) * 0.2f;

        float s = size.getF() + spread;
        float t = thick.getF();
        int c = mainColor();

        if (outline.get()) drawShape(ms, cx, cy, s, t, 0.5f, 0xB0000000);
        drawShape(ms, cx, cy, s, t, 0f, c);

        if (dot.get() && !shape.is(3)) {
            float d = Math.max(1f, t);
            if (outline.get()) RenderUtil.rect(ms, cx - d / 2f - 0.5f, cy - d / 2f - 0.5f, cx + d / 2f + 0.5f, cy + d / 2f + 0.5f, 0xB0000000);
            RenderUtil.rect(ms, cx - d / 2f, cy - d / 2f, cx + d / 2f, cy + d / 2f, c);
        }

        if (cooldown.get()) {
            float k = mc.player.getAttackStrengthScale(0f);
            if (k < 1f) {
                float rr = s + t + 3f;
                RenderUtil.ring(ms, cx, cy, rr, 1.5f, -90, 270, 0x60000000);
                RenderUtil.ring(ms, cx, cy, rr, 1.5f, -90, -90 + 360f * MathHelper.clamp(k, 0f, 1f), c);
            }
        }
        return true;
    }

    /** grow — насколько расширить фигуру (для обводки). */
    private void drawShape(MatrixStack ms, float cx, float cy, float s, float t, float grow, int c) {
        switch (shape.getIndex()) {
            case 0: // Круг
                RenderUtil.ring(ms, cx, cy, s + t / 2f, t + grow * 2f, 0, 360, c);
                break;
            case 1: { // Квадрат (контур)
                float o = s + t + grow, i = s - grow;
                RenderUtil.rect(ms, cx - o, cy - o, cx + o, cy - i, c);
                RenderUtil.rect(ms, cx - o, cy + i, cx + o, cy + o, c);
                RenderUtil.rect(ms, cx - o, cy - i, cx - i, cy + i, c);
                RenderUtil.rect(ms, cx + i, cy - i, cx + o, cy + i, c);
                break;
            }
            case 2: { // Крест
                float gp = gap.getF() + spread, h = t / 2f + grow, len = s + 2f;
                RenderUtil.rect(ms, cx - h, cy - gp - len - grow, cx + h, cy - gp + grow, c);
                RenderUtil.rect(ms, cx - h, cy + gp - grow, cx + h, cy + gp + len + grow, c);
                RenderUtil.rect(ms, cx - gp - len - grow, cy - h, cx - gp + grow, cy + h, c);
                RenderUtil.rect(ms, cx + gp - grow, cy - h, cx + gp + len + grow, cy + h, c);
                break;
            }
            default: { // Точка
                float d = s / 2f + grow;
                RenderUtil.round(ms, cx - d, cy - d, d * 2, d * 2, d, c);
            }
        }
    }
}
