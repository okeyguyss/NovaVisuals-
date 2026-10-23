package dev.novavisuals.module.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.novavisuals.module.Module;
import dev.novavisuals.module.settings.ActionSetting;
import dev.novavisuals.module.settings.BoolSetting;
import dev.novavisuals.module.settings.LabelSetting;
import dev.novavisuals.module.settings.NumberSetting;
import dev.novavisuals.module.settings.Setting;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.client.event.RenderHandEvent;

/**
 * Настройка положения предметов в правой и левой руке:
 * лево/право, низ/верх, ближе/дальше, повороты и размер.
 */
public class ViewModel extends Module {
    private static final float PIVOT_X = 0.56f, PIVOT_Y = -0.52f, PIVOT_Z = -0.72f;

    private final HandCfg right;
    private final HandCfg left;
    private final BoolSetting hideOffhand;

    private MatrixStack frameStack;
    private float[] applied;

    public ViewModel() {
        super("viewmodel", "Руки (ViewModel)", "Положение, поворот и размер предметов в руках", true);
        add(new LabelSetting("label_r", "Правая рука"));
        right = new HandCfg("r");
        add(new LabelSetting("label_l", "Левая рука"));
        left = new HandCfg("l");
        add(new LabelSetting("label_o", "Другое"));
        hideOffhand = add(new BoolSetting("hide_off", "Скрыть левую руку", false));
        add(new ActionSetting("reset", "Сбросить руки", this::resetAll));
    }

    private void resetAll() {
        for (Setting<?> s : getSettings()) {
            if (s instanceof NumberSetting) s.reset();
        }
    }

    public void beginFrame() {
        frameStack = null;
        applied = null;
    }

    public void onRenderHand(RenderHandEvent event) {
        MatrixStack ms = event.getMatrixStack();

        // Правая и левая рука рисуются на одном стеке подряд — отменяем трансформацию предыдущей руки
        if (frameStack == ms && applied != null) undo(ms, applied);
        applied = null;
        frameStack = ms;

        boolean main = event.getHand() == Hand.MAIN_HAND;
        HandSide side = main ? mc.options.mainHand : mc.options.mainHand.getOpposite();

        if (side == HandSide.LEFT && hideOffhand.get()) {
            event.setCanceled(true);
            return;
        }

        HandCfg c = side == HandSide.RIGHT ? right : left;
        float sign = side == HandSide.RIGHT ? 1f : -1f;
        float[] v = {
                c.x.getF(), c.y.getF(), -c.z.getF(),
                c.rx.getF(), c.ry.getF(), c.rz.getF(),
                Math.max(0.05f, c.scale.getF()),
                PIVOT_X * sign
        };
        apply(ms, v);
        applied = v;
    }

    private static void apply(MatrixStack ms, float[] v) {
        ms.translate(v[0], v[1], v[2]);
        ms.translate(v[7], PIVOT_Y, PIVOT_Z);
        ms.mulPose(Vector3f.XP.rotationDegrees(v[3]));
        ms.mulPose(Vector3f.YP.rotationDegrees(v[4]));
        ms.mulPose(Vector3f.ZP.rotationDegrees(v[5]));
        ms.scale(v[6], v[6], v[6]);
        ms.translate(-v[7], -PIVOT_Y, -PIVOT_Z);
    }

    private static void undo(MatrixStack ms, float[] v) {
        ms.translate(v[7], PIVOT_Y, PIVOT_Z);
        float inv = 1f / v[6];
        ms.scale(inv, inv, inv);
        ms.mulPose(Vector3f.ZP.rotationDegrees(-v[5]));
        ms.mulPose(Vector3f.YP.rotationDegrees(-v[4]));
        ms.mulPose(Vector3f.XP.rotationDegrees(-v[3]));
        ms.translate(-v[7], -PIVOT_Y, -PIVOT_Z);
        ms.translate(-v[0], -v[1], -v[2]);
    }

    private final class HandCfg {
        final NumberSetting x, y, z, rx, ry, rz, scale;

        HandCfg(String p) {
            x = add(new NumberSetting(p + "_x", "Влево / вправо", 0, -1.5, 1.5, 0.01));
            y = add(new NumberSetting(p + "_y", "Вниз / вверх", 0, -1.5, 1.5, 0.01));
            z = add(new NumberSetting(p + "_z", "Ближе / дальше", 0, -1.5, 1.5, 0.01));
            rx = add(new NumberSetting(p + "_rx", "Наклон вперёд", 0, -180, 180, 1));
            ry = add(new NumberSetting(p + "_ry", "Поворот влево/вправо", 0, -180, 180, 1));
            rz = add(new NumberSetting(p + "_rz", "Крен", 0, -180, 180, 1));
            scale = add(new NumberSetting(p + "_scale", "Размер", 1.0, 0.1, 2.0, 0.01));
        }
    }
}
