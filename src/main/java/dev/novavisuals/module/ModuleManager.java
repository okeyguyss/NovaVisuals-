package dev.novavisuals.module;

import dev.novavisuals.module.impl.AutoSprint;
import dev.novavisuals.module.impl.Crosshair;
import dev.novavisuals.module.impl.NoRender;
import dev.novavisuals.module.impl.Fullbright;
import dev.novavisuals.module.impl.Hud;
import dev.novavisuals.module.impl.Interface;
import dev.novavisuals.module.impl.TargetHud;
import dev.novavisuals.module.impl.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static final Map<Class<?>, Module> BY_CLASS = new HashMap<>();

    private ModuleManager() {}

    public static void init() {
        if (!MODULES.isEmpty()) return;
        register(new AutoSprint());
        register(new TargetHud());
        register(new ViewModel());
        register(new Crosshair());
        register(new NoRender());
        register(new Hud());
        register(new Fullbright());
        register(new Interface());
    }

    private static void register(Module m) {
        MODULES.add(m);
        BY_CLASS.put(m.getClass(), m);
    }

    public static <T extends Module> T get(Class<T> type) {
        return type.cast(BY_CLASS.get(type));
    }

    public static List<Module> all() {
        return Collections.unmodifiableList(MODULES);
    }
}
