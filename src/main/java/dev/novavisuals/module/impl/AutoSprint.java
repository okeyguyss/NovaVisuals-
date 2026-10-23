package dev.novavisuals.module.impl;

import dev.novavisuals.module.Module;
import dev.novavisuals.module.settings.BoolSetting;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;

/**
 * Авто-спринт: держит клавишу спринта "зажатой" за тебя.
 * Все ванильные условия (голод, присед, еда, стена) проверяет сама игра —
 * поэтому это обычный легитный спринт, как если бы ты держал Ctrl.
 */
public class AutoSprint extends Module {
    private final BoolSetting inGui = add(new BoolSetting("gui", "Работать в меню/инвентаре", false));

    public AutoSprint() {
        super("autosprint", "Авто-спринт", "Бег без зажатия Ctrl", true);
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (mc.screen != null && !inGui.get()) return;
        mc.options.keySprint.setDown(true);
    }

    @Override
    protected void onDisable() {
        KeyBinding k = mc.options.keySprint;
        boolean physical = false;
        try {
            physical = InputMappings.isKeyDown(mc.getWindow().getWindow(), k.getKey().getValue());
        } catch (Throwable ignored) {
        }
        k.setDown(physical);
    }
}
