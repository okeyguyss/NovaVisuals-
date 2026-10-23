package dev.novavisuals.module;

import dev.novavisuals.module.settings.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    protected final Minecraft mc = Minecraft.getInstance();

    private final String id;
    private final String name;
    private final String description;
    private final boolean toggleable;
    private boolean enabled;
    private final List<Setting<?>> settings = new ArrayList<>();

    protected Module(String id, String name, String description, boolean enabledByDefault) {
        this(id, name, description, enabledByDefault, true);
    }

    protected Module(String id, String name, String description, boolean enabledByDefault, boolean toggleable) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.enabled = enabledByDefault;
        this.toggleable = toggleable;
    }

    protected <T extends Setting<?>> T add(T setting) {
        settings.add(setting);
        return setting;
    }

    public void setEnabled(boolean value) {
        if (!toggleable) value = true;
        if (enabled == value) return;
        enabled = value;
        if (value) onEnable();
        else onDisable();
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    protected void onEnable() {}

    protected void onDisable() {}

    public void onTick() {}

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }
    public boolean canToggle() { return toggleable; }
    public List<Setting<?>> getSettings() { return Collections.unmodifiableList(settings); }
}
