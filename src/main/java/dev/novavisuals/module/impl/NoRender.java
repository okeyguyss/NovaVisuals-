package dev.novavisuals.module.impl;

import dev.novavisuals.NovaVisuals;
import dev.novavisuals.module.Module;
import dev.novavisuals.module.settings.BoolSetting;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.SpellParticle;
import net.minecraft.potion.Effects;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;

/** Убирает с экрана огонь, тряску камеры при ударе и прочие мешающие оверлеи. */
public class NoRender extends Module {
    private final BoolSetting fire = add(new BoolSetting("fire", "Огонь на экране", true));
    private final BoolSetting hurtCam = add(new BoolSetting("hurtcam", "Тряска при ударе", true));
    private final BoolSetting water = add(new BoolSetting("water", "Оверлей воды", false));
    private final BoolSetting inWall = add(new BoolSetting("wall", "Блок в лицо (в стене)", true));
    private final BoolSetting pumpkin = add(new BoolSetting("pumpkin", "Тыква на голове", true));
    private final BoolSetting portal = add(new BoolSetting("portal", "Эффект портала", false));
    private final BoolSetting bossbar = add(new BoolSetting("bossbar", "Боссбар", false));
    private final BoolSetting potions = add(new BoolSetting("potions", "Частицы зелий (брызги)", true));
    private final BoolSetting nausea = add(new BoolSetting("nausea", "Тошнота (искажение)", true));

    private boolean reflectionFailed;
    private Field particlesField;
    private Field toAddField;

    public NoRender() {
        super("norender", "Чистый экран", "Огонь, тряска при ударе, брызги зелий и т.д.", true);
    }

    /**
     * Тряска камеры (hurt cam) считается из hurtTime игрока. Обнуляем его перед
     * каждым кадром — камера перестаёт дёргаться, а урон и HP считаются как обычно.
     */
    public void beforeFrame() {
        if (hurtCam.get() && mc.player != null) {
            mc.player.hurtTime = 0;
        }
        if (potions.get()) removePotionParticles();
    }

    @Override
    public void onTick() {
        beforeFrame();
        // Убираем эффект тошноты только у себя на клиенте — экран перестаёт плыть
        if (nausea.get() && mc.player != null && mc.player.hasEffect(Effects.CONFUSION)) {
            mc.player.removeEffectNoUpdate(Effects.CONFUSION);
        }
    }

    /**
     * Брызги брошенного зелья и "спирали" эффектов — это SpellParticle.
     * Forge 1.16.5 не даёт события на частицы, поэтому чистим очереди ParticleManager.
     * Поля ищем по типу, а не по имени, чтобы работало и в собранном моде.
     */
    @SuppressWarnings("unchecked")
    private void removePotionParticles() {
        if (reflectionFailed || mc.particleEngine == null) return;
        try {
            if (particlesField == null) findFields();
            Object map = particlesField.get(mc.particleEngine);
            if (map instanceof Map) {
                for (Object q : ((Map<Object, Object>) map).values()) {
                    if (q instanceof Collection) ((Collection<Object>) q).removeIf(p -> p instanceof SpellParticle);
                }
            }
            if (toAddField != null) {
                Object add = toAddField.get(mc.particleEngine);
                if (add instanceof Collection) ((Collection<Object>) add).removeIf(p -> p instanceof SpellParticle);
            }
        } catch (Throwable t) {
            reflectionFailed = true;
            NovaVisuals.LOGGER.warn("NoRender: не удалось убрать частицы зелий", t);
        }
    }

    private void findFields() throws NoSuchFieldException {
        for (Field f : ParticleManager.class.getDeclaredFields()) {
            String generic = f.getGenericType().getTypeName();
            if (Map.class.isAssignableFrom(f.getType()) && generic.contains("IParticleRenderType")) {
                f.setAccessible(true);
                particlesField = f;
            } else if (Queue.class.isAssignableFrom(f.getType())
                    && generic.contains("Particle") && !generic.contains("EmitterParticle")) {
                f.setAccessible(true);
                toAddField = f;
            }
        }
        if (particlesField == null) throw new NoSuchFieldException("ParticleManager.particles");
    }

    public void onBlockOverlay(RenderBlockOverlayEvent event) {
        switch (event.getOverlayType()) {
            case FIRE:
                if (fire.get()) event.setCanceled(true);
                break;
            case WATER:
                if (water.get()) event.setCanceled(true);
                break;
            case BLOCK:
                if (inWall.get()) event.setCanceled(true);
                break;
            default:
                break;
        }
    }

    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        RenderGameOverlayEvent.ElementType type = event.getType();
        if ((type == RenderGameOverlayEvent.ElementType.HELMET && pumpkin.get())
                || (type == RenderGameOverlayEvent.ElementType.PORTAL && portal.get())
                || (type == RenderGameOverlayEvent.ElementType.BOSSHEALTH && bossbar.get())) {
            event.setCanceled(true);
        }
    }
}
