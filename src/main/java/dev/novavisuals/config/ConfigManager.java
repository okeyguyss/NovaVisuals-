package dev.novavisuals.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.novavisuals.NovaVisuals;
import dev.novavisuals.module.Module;
import dev.novavisuals.module.ModuleManager;
import dev.novavisuals.module.settings.Setting;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigManager() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("novavisuals.json");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) {
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(r).getAsJsonObject();
            for (Module m : ModuleManager.all()) {
                JsonElement el = root.get(m.getId());
                if (el == null || !el.isJsonObject()) continue;
                JsonObject o = el.getAsJsonObject();

                if (o.has("settings") && o.get("settings").isJsonObject()) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting<?> setting : m.getSettings()) {
                        if (!s.has(setting.getId())) continue;
                        try {
                            setting.fromJson(s.get(setting.getId()));
                        } catch (Exception ignored) {
                        }
                    }
                }
                if (o.has("enabled") && m.canToggle()) {
                    m.setEnabled(o.get("enabled").getAsBoolean());
                }
            }
        } catch (Exception e) {
            NovaVisuals.LOGGER.warn("Не удалось прочитать конфиг NovaVisuals, используются настройки по умолчанию", e);
        }
    }

    public static void save() {
        JsonObject root = new JsonObject();
        for (Module m : ModuleManager.all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            JsonObject s = new JsonObject();
            for (Setting<?> setting : m.getSettings()) {
                JsonElement v = setting.toJson();
                if (v != null) s.add(setting.getId(), v);
            }
            o.add("settings", s);
            root.add(m.getId(), o);
        }
        try {
            Path f = file();
            Files.createDirectories(f.getParent());
            try (Writer w = Files.newBufferedWriter(f, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            NovaVisuals.LOGGER.warn("Не удалось сохранить конфиг NovaVisuals", e);
        }
    }
}
