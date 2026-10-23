package dev.novavisuals.module.settings;

import com.google.gson.JsonElement;

/** Заголовок-разделитель внутри списка настроек. */
public class LabelSetting extends Setting<String> {
    public LabelSetting(String id, String name) {
        super(id, name, name);
    }

    @Override public JsonElement toJson() { return null; }
    @Override public void fromJson(JsonElement e) {}
}
