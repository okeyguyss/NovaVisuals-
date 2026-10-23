package dev.novavisuals;

import dev.novavisuals.config.ConfigManager;
import dev.novavisuals.module.ModuleManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public final class ClientBootstrap {
    public static final KeyBinding MENU_KEY =
            new KeyBinding("key.novavisuals.menu", GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.novavisuals");

    private ClientBootstrap() {}

    public static void init() {
        ModuleManager.init();
        ConfigManager.load();
        ClientRegistry.registerKeyBinding(MENU_KEY);
        MinecraftForge.EVENT_BUS.register(new ClientEvents());
        NovaVisuals.LOGGER.info("{} {} loaded", NovaVisuals.NAME, NovaVisuals.VERSION);
    }
}
