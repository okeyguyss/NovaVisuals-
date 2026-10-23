package dev.novavisuals.util;

import dev.novavisuals.module.ModuleManager;
import dev.novavisuals.module.impl.Interface;

public final class ColorUtil {
    // пары цветов тем: [начало, конец]
    private static final int[][] THEMES = {
            {0xFF8B5CF6, 0xFFE879F9}, // Фиолетовая
            {0xFF38BDF8, 0xFF6366F1}, // Голубая
            {0xFFF472B6, 0xFFFB7185}, // Розовая
            {0xFF34D399, 0xFF22D3EE}, // Мятная
            {0xFFEF4444, 0xFFF97316}, // Красная
    };

    private ColorUtil() {}

    public static int withAlpha(int color, float alpha) {
        int a = (int) (((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, alpha)));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 255) + ((((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t));
        int r = (int) (((c1 >> 16) & 255) + ((((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t));
        int g = (int) (((c1 >> 8) & 255) + ((((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t));
        int b = (int) ((c1 & 255) + (((c2 & 255) - (c1 & 255)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        int i = (int) (h * 6);
        float f = h * 6 - i, p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    /** Акцентный цвет текущей темы, offset — сдвиг фазы в градусах (для переливания). */
    public static int accent(int offset) {
        Interface ui = ModuleManager.get(Interface.class);
        int theme = ui == null ? 0 : ui.theme.getIndex();
        double speed = ui == null ? 1.0 : ui.speed.get();
        double deg = (System.currentTimeMillis() * 0.1 * speed) % 360.0 + offset;

        if (theme >= THEMES.length) { // Радуга
            return hsv((float) (deg / 360.0), 0.55f, 1f);
        }
        float k = (float) (Math.sin(Math.toRadians(deg)) * 0.5 + 0.5);
        return lerp(THEMES[theme][0], THEMES[theme][1], k);
    }

    public static int accent(int offset, float alpha) {
        return withAlpha(accent(offset), alpha);
    }

    /** Красный -> жёлтый -> зелёный по проценту здоровья. */
    public static int health(float pct) {
        pct = Math.max(0f, Math.min(1f, pct));
        if (pct < 0.5f) return lerp(0xFFEF4444, 0xFFFACC15, pct * 2f);
        return lerp(0xFFFACC15, 0xFF4ADE80, (pct - 0.5f) * 2f);
    }
}
