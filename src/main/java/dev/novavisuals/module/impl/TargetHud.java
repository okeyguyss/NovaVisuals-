package dev.novavisuals.module.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.novavisuals.gui.ClickGui;
import dev.novavisuals.module.Module;
import dev.novavisuals.module.settings.BoolSetting;
import dev.novavisuals.module.settings.NumberSetting;
import dev.novavisuals.util.ColorUtil;
import dev.novavisuals.util.RenderUtil;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Панель с HP противника: голова, ник, здоровье, броня, индикатор победы. */
public class TargetHud extends Module {
    private static final int W = 150, H = 50;

    private final NumberSetting posX = add(new NumberSetting("x", "Позиция X (%)", 50, 0, 100, 0.5));
    private final NumberSetting posY = add(new NumberSetting("y", "Позиция Y (%)", 58, 0, 100, 0.5));
    private final NumberSetting scale = add(new NumberSetting("scale", "Размер", 1.0, 0.5, 2.0, 0.05));
    private final BoolSetting head = add(new BoolSetting("head", "Голова / модель", true));
    private final BoolSetting armor = add(new BoolSetting("armor", "Броня и предмет", true));
    private final BoolSetting win = add(new BoolSetting("win", "Победа / проигрыш", true));
    private final BoolSetting scoreHp = add(new BoolSetting("score", "HP из скорборда (RW)", true));
    private final BoolSetting mobs = add(new BoolSetting("mobs", "Показывать мобов", true));
    private final NumberSetting keep = add(new NumberSetting("keep", "Держать цель (сек)", 3, 1, 10, 0.5));

    private LivingEntity target;
    private LivingEntity shown;
    private long lastSeen;
    private float anim;
    private float hpAnim = -1;
    private long lastFrame = System.nanoTime();

    public TargetHud() {
        super("targethud", "Таргет HUD", "HP противника, броня и кто выигрывает", true);
    }

    public void setTarget(LivingEntity e) {
        if (!mobs.get() && !(e instanceof PlayerEntity)) return;
        if (e != target) hpAnim = -1;
        target = e;
        lastSeen = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.level == null) {
            target = null;
            return;
        }
        Entity look = mc.crosshairPickEntity;
        if (look instanceof LivingEntity && look != mc.player && look.isAlive() && !look.isInvisible()) {
            setTarget((LivingEntity) look);
        }
        if (target != null) {
            boolean lost = target.level != mc.level
                    || !target.isAlive()
                    || target.distanceTo(mc.player) > 16f
                    || System.currentTimeMillis() - lastSeen > keep.get() * 1000;
            if (lost) target = null;
        }
    }

    public void render(MatrixStack ms) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;

        LivingEntity t = target;
        if (t == null && mc.screen instanceof ClickGui) t = mc.player; // превью в меню
        if (t != null) {
            if (t != shown) hpAnim = -1;
            shown = t;
        }
        anim += ((t != null ? 1f : 0f) - anim) * Math.min(1f, dt * 10f);
        if (anim < 0.02f || shown == null) {
            if (t == null) shown = null;
            return;
        }
        LivingEntity e = shown;
        FontRenderer f = mc.font;

        // ---- здоровье ----
        float hp = e.getHealth();
        float max = e.getMaxHealth();
        if (scoreHp.get() && e instanceof PlayerEntity && mc.level != null) {
            Scoreboard sb = mc.level.getScoreboard();
            ScoreObjective obj = sb.getDisplayObjective(2); // слот "под ником" — там RW показывает HP
            String n = e.getScoreboardName();
            if (obj != null && sb.hasPlayerScore(n, obj)) {
                int s = sb.getOrCreatePlayerScore(n, obj).getScore();
                if (s > 0) {
                    hp = s;
                    max = Math.max(max, s);
                }
            }
        }
        float absorb = e.getAbsorptionAmount();
        if (max <= 0) max = 20;
        if (hpAnim < 0) hpAnim = hp;
        hpAnim += (hp - hpAnim) * Math.min(1f, dt * 8f);

        // ---- позиция и анимация появления ----
        float ease = 1f - (float) Math.pow(1f - anim, 3);
        float s = scale.getF() * (0.7f + 0.3f * ease);
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        float cx = MathHelper.clamp(sw * posX.getF() / 100f, W * s / 2f, sw - W * s / 2f);
        float cy = MathHelper.clamp(sh * posY.getF() / 100f, H * s / 2f, sh - H * s / 2f);

        RenderSystem.pushMatrix();
        RenderSystem.translatef(cx, cy, 0);
        RenderSystem.scalef(s, s, 1f);
        RenderSystem.translatef(-W / 2f, -H / 2f, 0);
        MatrixStack m = new MatrixStack();
        float a = anim;

        RenderUtil.round(m, -2, -2, W + 4, H + 4, 7, ColorUtil.withAlpha(0x40000000, a));
        RenderUtil.round(m, 0, 0, W, H, 5, ColorUtil.withAlpha(0xD8101016, a));
        RenderUtil.gradientH(m, 6, 0, W - 6, 1.5f, ColorUtil.accent(0, a), ColorUtil.accent(180, a));

        // ---- голова / модель ----
        int textX = 6;
        if (head.get()) {
            float hurt = e.hurtTime > 0 ? e.hurtTime / 10f : 0f;
            if (e instanceof AbstractClientPlayerEntity) {
                ResourceLocation skin = ((AbstractClientPlayerEntity) e).getSkinTextureLocation();
                RenderUtil.round(m, 5, 5, 32, 32, 4, ColorUtil.withAlpha(0xFF1C1C24, a));
                mc.getTextureManager().bind(skin);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.color4f(1f, 1f - hurt * 0.6f, 1f - hurt * 0.6f, a);
                AbstractGui.blit(m, 6, 6, 30, 30, 8, 8, 8, 8, 64, 64);
                AbstractGui.blit(m, 6, 6, 30, 30, 40, 8, 8, 8, 64, 64);
                RenderSystem.color4f(1f, 1f, 1f, 1f);
            } else {
                RenderUtil.round(m, 5, 5, 32, 32, 4, ColorUtil.withAlpha(0xFF1C1C24, a));
                if (a > 0.85f) {
                    try {
                        float size = Math.max(e.getBbHeight(), e.getBbWidth());
                        int sc = (int) MathHelper.clamp(26f / Math.max(0.1f, size), 4f, 30f);
                        InventoryScreen.renderEntityInInventory(21, 34, sc, 25f, -5f, e);
                    } catch (Throwable ignored) {
                    }
                }
            }
            textX = 42;
        }

        // ---- ник ----
        String name = f.plainSubstrByWidth(e.getName().getString(), W - textX - 6);
        RenderUtil.text(m, name, textX, 6, ColorUtil.withAlpha(0xFFFFFFFF, a), true);

        // ---- HP текстом ----
        float pct = MathHelper.clamp(hp / max, 0f, 1f);
        String hpText = String.format(Locale.US, "%.1f", hp + absorb) + " ❤";
        RenderUtil.text(m, hpText, textX, 17, ColorUtil.withAlpha(ColorUtil.health(pct), a), true);

        if (win.get() && e != mc.player && mc.player != null) {
            float mine = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            float theirs = hp + absorb;
            String w;
            int col;
            if (mine > theirs + 0.5f) { w = "Победа"; col = 0xFF4ADE80; }
            else if (mine < theirs - 0.5f) { w = "Проигрыш"; col = 0xFFEF4444; }
            else { w = "Ничья"; col = 0xFFFACC15; }
            RenderUtil.text(m, w, W - 6 - f.width(w), 17, ColorUtil.withAlpha(col, a), true);
        }

        // ---- броня + предмет в руке ----
        if (armor.get() && a > 0.6f) {
            List<ItemStack> items = new ArrayList<>();
            List<ItemStack> armorList = new ArrayList<>();
            for (ItemStack st : e.getArmorSlots()) armorList.add(st);
            for (int i = armorList.size() - 1; i >= 0; i--) items.add(armorList.get(i)); // шлем первым
            items.add(e.getMainHandItem());
            items.add(e.getOffhandItem());

            RenderSystem.pushMatrix();
            RenderSystem.translatef(textX, 27, 0);
            RenderSystem.scalef(0.7f, 0.7f, 1f);
            int ix = 0;
            for (ItemStack st : items) {
                if (st != null && !st.isEmpty()) {
                    mc.getItemRenderer().renderAndDecorateItem(st, ix, 0);
                    mc.getItemRenderer().renderGuiItemDecorations(f, st, ix, 0);
                }
                ix += 17;
            }
            RenderSystem.popMatrix();
        }

        // ---- полоска HP ----
        float bx = 5, by = H - 8, bw = W - 10, bh = 4;
        RenderUtil.round(m, bx, by, bw, bh, 2, ColorUtil.withAlpha(0x80000000, a));
        float animPct = MathHelper.clamp(hpAnim / max, 0f, 1f);
        float realPct = pct;
        if (animPct > realPct) { // "след" урона
            RenderUtil.round(m, bx, by, bw * animPct, bh, 2, ColorUtil.withAlpha(0xA0FFFFFF, a));
        }
        float fill = Math.min(animPct, realPct);
        if (fill > 0) {
            RenderUtil.round(m, bx, by, Math.max(bh, bw * fill), bh, 2, ColorUtil.accent(0, a));
            RenderUtil.gradientH(m, bx + 2, by, bx + Math.max(bh, bw * fill) - 2, by + bh,
                    ColorUtil.accent(0, a), ColorUtil.accent(180, a));
        }
        if (absorb > 0) {
            float ap = MathHelper.clamp(absorb / max, 0f, 1f);
            RenderUtil.round(m, bx, by - 2.5f, bw * ap, 1.5f, 0.75f, ColorUtil.withAlpha(0xFFFACC15, a));
        }

        RenderSystem.popMatrix();
    }
}
