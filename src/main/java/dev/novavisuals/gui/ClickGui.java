package dev.novavisuals.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.novavisuals.NovaVisuals;
import dev.novavisuals.config.ConfigManager;
import dev.novavisuals.module.Module;
import dev.novavisuals.module.ModuleManager;
import dev.novavisuals.module.impl.Interface;
import dev.novavisuals.module.settings.ActionSetting;
import dev.novavisuals.module.settings.BoolSetting;
import dev.novavisuals.module.settings.LabelSetting;
import dev.novavisuals.module.settings.ModeSetting;
import dev.novavisuals.module.settings.NumberSetting;
import dev.novavisuals.module.settings.Setting;
import dev.novavisuals.util.ColorUtil;
import dev.novavisuals.util.RenderUtil;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Меню визуалов (Right Shift). Слева — модули, справа — их настройки.
 * Фон не затемняется полностью, чтобы руки и Target HUD были видны вживую при настройке.
 */
public class ClickGui extends Screen {
    private static final int W = 400, H = 270, LEFT = 130, HEADER = 28, ROW = 22;
    private static final int C_TEXT = 0xFFFFFFFF, C_SUB = 0xFF8A8A9A, C_DIM = 0xFFB4B4C0;
    private static final int C_TRACK = 0xFF2A2A36, C_BTN = 0xFF1E1E28;

    private static Module lastSelected;

    private Module selected;
    private int px, py;
    private long openTime, lastFrame;
    private float scroll, scrollTarget;
    private float scale = 1f;
    private float alpha = 1f;
    private NumberSetting dragging;
    private final Map<String, Float> anims = new HashMap<>();

    public ClickGui() {
        super(new StringTextComponent(NovaVisuals.NAME));
    }

    @Override
    protected void init() {
        px = Math.max(8, (width - W) / 4);
        py = Math.max(8, (height - H) / 2);
        if (selected == null) selected = lastSelected != null ? lastSelected : ModuleManager.all().get(0);
        openTime = System.currentTimeMillis();
        lastFrame = System.nanoTime();
    }

    // ---------- геометрия ----------
    private float cx() { return px + W / 2f; }
    private float cy() { return py + H / 2f; }
    private double localX(double x) { return (x - cx()) / scale + cx(); }
    private double localY(double y) { return (y - cy()) / scale + cy(); }
    private float screenX(float x) { return cx() + (x - cx()) * scale; }
    private float screenY(float y) { return cy() + (y - cy()) * scale; }

    private int areaX() { return px + LEFT + 10; }
    private int areaW() { return W - LEFT - 24; }
    private int areaTop() { return py + HEADER + 34; }
    private int areaBottom() { return py + H - 8; }

    private static boolean in(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private float anim(String key, float target, float dt) {
        Float v = anims.get(key);
        float c = v == null ? target : v;
        c += (target - c) * Math.min(1f, dt * 14f);
        anims.put(key, c);
        return c;
    }

    private List<Setting<?>> visibleSettings() {
        List<Setting<?>> out = new ArrayList<>();
        for (Setting<?> s : selected.getSettings()) if (s.isVisible()) out.add(s);
        return out;
    }

    private static int heightOf(Setting<?> s) {
        if (s instanceof NumberSetting) return 28;
        if (s instanceof LabelSetting) return 18;
        return 20;
    }

    private int contentHeight() {
        int h = 0;
        for (Setting<?> s : visibleSettings()) h += heightOf(s);
        return h;
    }

    // ---------- рендер ----------
    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        long nowN = System.nanoTime();
        float dt = Math.min(0.1f, (nowN - lastFrame) / 1e9f);
        lastFrame = nowN;

        float t = MathHelper.clamp((System.currentTimeMillis() - openTime) / 220f, 0f, 1f);
        float ease = 1f - (float) Math.pow(1f - t, 3);
        scale = 0.92f + 0.08f * ease;
        alpha = ease;

        Interface ui = ModuleManager.get(Interface.class);
        if (ui.dim.get()) {
            RenderUtil.gradientH(ms, 0, 0, width * 0.75f, height, ColorUtil.withAlpha(0x90000000, alpha), 0x00000000);
        }

        scroll += (scrollTarget - scroll) * Math.min(1f, dt * 14f);
        if (dragging != null) {
            dragging.setPercent((localX(mouseX) - areaX()) / areaW());
        }

        double mx = localX(mouseX), my = localY(mouseY);

        ms.pushPose();
        ms.translate(cx(), cy(), 0);
        ms.scale(scale, scale, 1f);
        ms.translate(-cx(), -cy(), 0);

        // панель
        RenderUtil.round(ms, px - 3, py - 3, W + 6, H + 6, 9, ColorUtil.withAlpha(0x55000000, alpha));
        RenderUtil.round(ms, px, py, W, H, 7, ColorUtil.withAlpha(0xF2111117, alpha));

        // шапка
        RenderUtil.gradientText(ms, NovaVisuals.NAME, px + 12, py + 10, alpha, 18);
        String ver = "v" + NovaVisuals.VERSION + "  ·  1.16.5";
        RenderUtil.text(ms, ver, px + W - 12 - font.width(ver), py + 10, ColorUtil.withAlpha(0xFF6B6B7B, alpha), false);
        RenderUtil.gradientH(ms, px + 10, py + HEADER - 2, px + W - 10, py + HEADER - 1,
                ColorUtil.accent(0, alpha), ColorUtil.accent(180, alpha));

        drawModules(ms, mx, my, dt);

        RenderUtil.rect(ms, px + LEFT, py + HEADER + 6, px + LEFT + 1, py + H - 10, ColorUtil.withAlpha(0x20FFFFFF, alpha));

        drawSettings(ms, mx, my, dt);

        ms.popPose();
        super.render(ms, mouseX, mouseY, partialTicks);
    }

    private void drawModules(MatrixStack ms, double mx, double my, float dt) {
        float ly = py + HEADER + 6;
        for (Module m : ModuleManager.all()) {
            boolean sel = m == selected;
            boolean hov = in(mx, my, px + 6, ly, LEFT - 12, ROW - 2);
            float sa = anim("sel_" + m.getId(), sel ? 1f : 0f, dt);
            float ha = anim("hov_" + m.getId(), hov ? 1f : 0f, dt);

            RenderUtil.round(ms, px + 6, ly, LEFT - 12, ROW - 2, 5, ColorUtil.withAlpha(0x14FFFFFF, ha * alpha * (1f - sa)));
            if (sa > 0.01f) {
                RenderUtil.round(ms, px + 6, ly, LEFT - 12, ROW - 2, 5, ColorUtil.accent(0, 0.16f * sa * alpha));
                RenderUtil.round(ms, px + 6, ly + 5, 2, ROW - 12, 1, ColorUtil.accent(0, sa * alpha));
            }
            int col = ColorUtil.lerp(C_DIM, C_TEXT, Math.max(sa, ha));
            String name = font.plainSubstrByWidth(m.getName(), LEFT - (m.canToggle() ? 50 : 24));
            RenderUtil.text(ms, name, px + 14 + 3 * sa, ly + 6, ColorUtil.withAlpha(col, alpha), false);
            if (m.canToggle()) {
                drawSwitch(ms, px + LEFT - 32, ly + 5, m.isEnabled(), "mod_" + m.getId(), dt);
            }
            ly += ROW;
        }
        RenderUtil.text(ms, "ПКМ — вкл/выкл", px + 12, py + H - 26, ColorUtil.withAlpha(0xFF55556A, alpha), false);
        RenderUtil.text(ms, "RShift / Esc — закрыть", px + 12, py + H - 15, ColorUtil.withAlpha(0xFF55556A, alpha), false);
    }

    private void drawSettings(MatrixStack ms, double mx, double my, float dt) {
        int ax = areaX(), aw = areaW(), top = areaTop(), bottom = areaBottom();

        RenderUtil.text(ms, selected.getName(), ax, py + HEADER + 8, ColorUtil.withAlpha(C_TEXT, alpha), true);
        RenderUtil.text(ms, font.plainSubstrByWidth(selected.getDescription(), aw + 10), ax, py + HEADER + 20,
                ColorUtil.withAlpha(C_SUB, alpha), false);

        List<Setting<?>> list = visibleSettings();
        float maxScroll = Math.max(0, contentHeight() - (bottom - top));
        scrollTarget = MathHelper.clamp(scrollTarget, 0, maxScroll);
        scroll = MathHelper.clamp(scroll, 0, maxScroll);

        if (list.isEmpty()) {
            RenderUtil.text(ms, "Настроек нет — просто включи модуль", ax, top + 4, ColorUtil.withAlpha(C_SUB, alpha), false);
            return;
        }

        RenderUtil.scissor(screenX(ax - 2), screenY(top), (aw + 4) * scale, (bottom - top) * scale);
        boolean mouseInArea = my >= top && my < bottom;
        float y = top - scroll;
        for (Setting<?> s : list) {
            int h = heightOf(s);
            if (y + h > top - 2 && y < bottom + 2) {
                boolean hov = mouseInArea && in(mx, my, ax, y, aw, h);
                drawSetting(ms, s, ax, y, aw, h, hov, dt);
            }
            y += h;
        }
        RenderUtil.endScissor();

        if (maxScroll > 0) {
            float trackH = bottom - top;
            float barH = Math.max(16, trackH * trackH / (trackH + maxScroll));
            float barY = top + (trackH - barH) * (scroll / maxScroll);
            RenderUtil.round(ms, px + W - 8, top, 2, trackH, 1, ColorUtil.withAlpha(0x18FFFFFF, alpha));
            RenderUtil.round(ms, px + W - 8, barY, 2, barH, 1, ColorUtil.accent(0, alpha));
        }
    }

    private void drawSetting(MatrixStack ms, Setting<?> s, float x, float y, float w, int h, boolean hov, float dt) {
        String key = selected.getId() + "_" + s.getId();
        float ha = anim("h_" + key, hov ? 1f : 0f, dt);

        if (s instanceof LabelSetting) {
            RenderUtil.text(ms, s.getName(), x, y + 7, ColorUtil.accent(0, alpha), true);
            float tw = font.width(s.getName()) + 6;
            RenderUtil.rect(ms, x + tw, y + 11, x + w, y + 12, ColorUtil.withAlpha(0x20FFFFFF, alpha));
            return;
        }

        RenderUtil.round(ms, x, y + 1, w, h - 2, 4, ColorUtil.withAlpha(0x0EFFFFFF, ha * alpha));

        if (s instanceof BoolSetting) {
            RenderUtil.text(ms, s.getName(), x + 5, y + 6, ColorUtil.withAlpha(C_DIM, alpha), false);
            drawSwitch(ms, x + w - 26, y + 5, ((BoolSetting) s).get(), "b_" + key, dt);

        } else if (s instanceof NumberSetting) {
            NumberSetting n = (NumberSetting) s;
            RenderUtil.text(ms, n.getName(), x + 5, y + 4, ColorUtil.withAlpha(C_DIM, alpha), false);
            String v = n.display();
            RenderUtil.text(ms, v, x + w - 5 - font.width(v), y + 4, ColorUtil.withAlpha(C_TEXT, alpha), false);

            float tx = x + 5, tw = w - 10, ty = y + 18;
            float p = anim("n_" + key, (float) n.getPercent(), dt * 1.5f);
            RenderUtil.round(ms, tx, ty, tw, 3, 1.5f, ColorUtil.withAlpha(C_TRACK, alpha));
            // от нуля, если диапазон симметричный (удобно для позиций рук)
            float zero = n.getMin() < 0 && n.getMax() > 0 ? (float) (-n.getMin() / (n.getMax() - n.getMin())) : 0f;
            float a1 = Math.min(zero, p), a2 = Math.max(zero, p);
            if (a2 - a1 > 0.001f) {
                RenderUtil.gradientH(ms, tx + tw * a1, ty, tx + tw * a2, ty + 3, ColorUtil.accent(0, alpha), ColorUtil.accent(120, alpha));
            }
            float knob = (dragging == n) ? 8f : 6f + 1f * ha;
            RenderUtil.round(ms, tx + tw * p - knob / 2f, ty + 1.5f - knob / 2f, knob, knob, knob / 2f,
                    ColorUtil.withAlpha(C_TEXT, alpha));

        } else if (s instanceof ModeSetting) {
            ModeSetting m = (ModeSetting) s;
            RenderUtil.text(ms, m.getName(), x + 5, y + 6, ColorUtil.withAlpha(C_DIM, alpha), false);
            String v = "< " + m.getMode() + " >";
            float bw = font.width(v) + 12;
            RenderUtil.round(ms, x + w - 4 - bw, y + 3, bw, 14, 4, ColorUtil.withAlpha(C_BTN, alpha));
            RenderUtil.text(ms, v, x + w - 4 - bw + 6, y + 6, ColorUtil.accent(0, alpha), false);

        } else if (s instanceof ActionSetting) {
            RenderUtil.round(ms, x + 2, y + 2, w - 4, h - 4, 4,
                    ColorUtil.withAlpha(ColorUtil.lerp(C_BTN, 0xFF2C2C3A, ha), alpha));
            String n = s.getName();
            RenderUtil.text(ms, n, x + w / 2f - font.width(n) / 2f, y + 6, ColorUtil.withAlpha(C_TEXT, alpha), false);
        }
    }

    private void drawSwitch(MatrixStack ms, float x, float y, boolean on, String key, float dt) {
        float a = anim(key, on ? 1f : 0f, dt);
        int track = ColorUtil.lerp(C_TRACK, ColorUtil.accent(0), a);
        RenderUtil.round(ms, x, y, 20, 10, 5, ColorUtil.withAlpha(track, alpha));
        RenderUtil.round(ms, x + 2 + a * 10, y + 2, 6, 6, 3, ColorUtil.withAlpha(0xFFFFFFFF, alpha));
    }

    // ---------- ввод ----------
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = localX(mouseX), my = localY(mouseY);

        float ly = py + HEADER + 6;
        for (Module m : ModuleManager.all()) {
            if (in(mx, my, px + 6, ly, LEFT - 12, ROW - 2)) {
                boolean onSwitch = m.canToggle() && in(mx, my, px + LEFT - 36, ly, 30, ROW - 2);
                if (m.canToggle() && (button == 1 || onSwitch)) {
                    m.toggle();
                    ConfigManager.save();
                } else if (button == 0 && selected != m) {
                    selected = m;
                    lastSelected = m;
                    scroll = scrollTarget = 0;
                }
                return true;
            }
            ly += ROW;
        }

        int ax = areaX(), aw = areaW(), top = areaTop(), bottom = areaBottom();
        if (my >= top && my < bottom && mx >= ax && mx < ax + aw) {
            float y = top - scroll;
            for (Setting<?> s : visibleSettings()) {
                int h = heightOf(s);
                if (my >= y && my < y + h) {
                    handleSettingClick(s, button, mx);
                    return true;
                }
                y += h;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleSettingClick(Setting<?> s, int button, double mx) {
        if (s instanceof BoolSetting) {
            ((BoolSetting) s).toggle();
            ConfigManager.save();
        } else if (s instanceof NumberSetting) {
            NumberSetting n = (NumberSetting) s;
            if (button == 1) {
                n.reset(); // ПКМ по слайдеру — сброс
                ConfigManager.save();
            } else {
                dragging = n;
                n.setPercent((mx - areaX()) / areaW());
            }
        } else if (s instanceof ModeSetting) {
            if (button == 1) ((ModeSetting) s).prev();
            else ((ModeSetting) s).next();
            ConfigManager.save();
        } else if (s instanceof ActionSetting) {
            ((ActionSetting) s).run();
            ConfigManager.save();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            ConfigManager.save();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scrollTarget -= (float) delta * 24f;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        ConfigManager.save();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
