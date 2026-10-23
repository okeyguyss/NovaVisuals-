package dev.novavisuals.module.impl;

import dev.novavisuals.module.Module;

public class Fullbright extends Module {
    private double previous = 1.0;

    public Fullbright() {
        super("fullbright", "Яркость", "Fullbright — светло в шахтах и ночью", false);
    }

    @Override
    protected void onEnable() {
        if (mc.options == null) return;
        if (mc.options.gamma <= 1.0) previous = mc.options.gamma;
        mc.options.gamma = 16.0;
    }

    @Override
    public void onTick() {
        if (mc.options.gamma < 16.0) mc.options.gamma = 16.0;
    }

    @Override
    protected void onDisable() {
        mc.options.gamma = Math.min(previous, 1.0);
    }
}
