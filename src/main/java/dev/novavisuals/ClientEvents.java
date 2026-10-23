package dev.novavisuals;

import dev.novavisuals.gui.ClickGui;
import dev.novavisuals.module.Module;
import dev.novavisuals.module.ModuleManager;
import dev.novavisuals.module.impl.Crosshair;
import dev.novavisuals.module.impl.Hud;
import dev.novavisuals.module.impl.NoRender;
import dev.novavisuals.module.impl.TargetHud;
import dev.novavisuals.module.impl.ViewModel;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ClientEvents {
    private final Minecraft mc = Minecraft.getInstance();

    private static int fps;
    private static int frames;
    private static long lastSecond = System.currentTimeMillis();

    public static int getFps() {
        return fps;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        while (ClientBootstrap.MENU_KEY.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new ClickGui());
        }

        for (Module m : ModuleManager.all()) {
            if (m.isEnabled()) {
                try {
                    m.onTick();
                } catch (Throwable t) {
                    NovaVisuals.LOGGER.error("Module {} tick failed", m.getId(), t);
                }
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ModuleManager.get(ViewModel.class).beginFrame();
            NoRender nr = ModuleManager.get(NoRender.class);
            if (nr.isEnabled()) nr.beforeFrame();
        } else {
            frames++;
            long now = System.currentTimeMillis();
            if (now - lastSecond >= 1000) {
                fps = frames;
                frames = 0;
                lastSecond = now;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderHand(RenderHandEvent event) {
        ViewModel vm = ModuleManager.get(ViewModel.class);
        if (vm.isEnabled() && !event.isCanceled()) vm.onRenderHand(event);
    }

    @SubscribeEvent
    public void onBlockOverlay(RenderBlockOverlayEvent event) {
        if (event.getPlayer() != mc.player) return;
        NoRender nr = ModuleManager.get(NoRender.class);
        if (nr.isEnabled()) nr.onBlockOverlay(event);
    }

    @SubscribeEvent
    public void onOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (mc.player == null) return;
        NoRender nr = ModuleManager.get(NoRender.class);
        if (nr.isEnabled()) nr.onOverlayPre(event);

        if (event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            Crosshair ch = ModuleManager.get(Crosshair.class);
            if (ch.isEnabled() && ch.render(event.getMatrixStack())) event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (mc.options.hideGui || mc.player == null) return;

        Hud hud = ModuleManager.get(Hud.class);
        if (hud.isEnabled()) hud.render(event.getMatrixStack());

        TargetHud th = ModuleManager.get(TargetHud.class);
        if (th.isEnabled()) th.render(event.getMatrixStack());
    }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.getPlayer() != mc.player) return;
        if (event.getTarget() instanceof LivingEntity) {
            ModuleManager.get(TargetHud.class).setTarget((LivingEntity) event.getTarget());
        }
    }
}
