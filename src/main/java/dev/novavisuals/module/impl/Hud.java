package dev.novavisuals.module.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.novavisuals.ClientEvents;
import dev.novavisuals.NovaVisuals;
import dev.novavisuals.module.Module;
import dev.novavisuals.module.ModuleManager;
import dev.novavisuals.module.settings.BoolSetting;
import dev.novavisuals.util.ColorUtil;
import dev.novavisuals.util.RenderUtil;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.math.MathHelper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Hud extends Module {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int BG = 0xC8101016;

    private final BoolSetting watermark = add(new BoolSetting("watermark", "Ватермарка", true));
    private final BoolSetting fps = add(new BoolSetting("fps", "FPS", true));
    private final BoolSetting ping = add(new BoolSetting("ping", "Пинг", true));
    private final BoolSetting time = add(new BoolSetting("time", "Время", true));
    private final BoolSetting coords = add(new BoolSetting("coords", "Координаты", true));
    private final BoolSetting nether = add(new BoolSetting("nether", "Коорд. ада/мира", false));
    private final BoolSetting list = add(new BoolSetting("list", "Список модулей", true));

    public Hud() {
        super("hud", "HUD", "Ватермарка, FPS, пинг, координаты, список модулей", true);
    }

    public void render(MatrixStack ms) {
        FontRenderer f = mc.font;
        float y = 4;

        if (mc.options.renderDebug) return; // не мешаем F3

        if (watermark.get()) {
            List<String> parts = new ArrayList<>();
            if (fps.get()) parts.add(ClientEvents.getFps() + " fps");
            if (ping.get()) parts.add(getPing() + " ms");
            if (time.get()) parts.add(LocalTime.now().format(TIME));
            String info = String.join("  ·  ", parts);

            String title = NovaVisuals.NAME;
            float w = 12 + f.width(title) + (info.isEmpty() ? 0 : 12 + f.width(info));
            RenderUtil.round(ms, 4, y, w, 16, 4, BG);
            RenderUtil.gradientH(ms, 8, y + 14.5f, 4 + w - 4, y + 15.5f, ColorUtil.accent(0), ColorUtil.accent(180));
            float x = RenderUtil.gradientText(ms, title, 10, y + 4, 1f, 18);
            if (!info.isEmpty()) {
                RenderUtil.rect(ms, x + 5, y + 4, x + 6, y + 12, 0x40FFFFFF);
                RenderUtil.text(ms, info, x + 11, y + 4, 0xFFD6D6DE, true);
            }
            y += 20;
        }

        if (coords.get() && mc.player != null) {
            int px = MathHelper.floor(mc.player.getX());
            int py = MathHelper.floor(mc.player.getY());
            int pz = MathHelper.floor(mc.player.getZ());
            String c = "XYZ  " + px + "  " + py + "  " + pz;
            if (nether.get()) {
                boolean inNether = mc.level != null && mc.level.dimension().location().getPath().equals("the_nether");
                String other = inNether
                        ? (px * 8) + "  " + (pz * 8)
                        : (MathHelper.floor(px / 8.0)) + "  " + (MathHelper.floor(pz / 8.0));
                c += "   " + (inNether ? "Мир" : "Ад") + "  " + other;
            }
            float w = f.width(c) + 12;
            RenderUtil.round(ms, 4, y, w, 14, 4, BG);
            RenderUtil.round(ms, 4, y + 3, 2, 8, 1, ColorUtil.accent(0));
            RenderUtil.text(ms, c, 10, y + 3, 0xFFD6D6DE, true);
        }

        if (list.get()) renderList(ms, f);
    }

    private void renderList(MatrixStack ms, FontRenderer f) {
        List<Module> enabled = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            if (m.canToggle() && m.isEnabled() && m != this) enabled.add(m);
        }
        enabled.sort((a, b) -> f.width(b.getName()) - f.width(a.getName()));

        int sw = mc.getWindow().getGuiScaledWidth();
        float y = 4;
        int i = 0;
        for (Module m : enabled) {
            String n = m.getName();
            float w = f.width(n) + 8;
            float x = sw - 4 - w;
            RenderUtil.rect(ms, x, y, sw - 4, y + 11, 0xA0101016);
            RenderUtil.rect(ms, sw - 5, y, sw - 3, y + 11, ColorUtil.accent(i * 20));
            RenderUtil.text(ms, n, x + 3, y + 2, ColorUtil.accent(i * 20), true);
            y += 11;
            i++;
        }
    }

    private int getPing() {
        if (mc.getConnection() == null || mc.player == null) return 0;
        NetworkPlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return info == null ? 0 : info.getLatency();
    }
}
