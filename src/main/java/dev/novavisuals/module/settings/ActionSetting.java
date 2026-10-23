package dev.novavisuals.module.settings;

import com.google.gson.JsonElement;

public class ActionSetting extends Setting<Runnable> {
    public ActionSetting(String id, String name, Runnable action) {
        super(id, name, action);
    }

    public void run() { value.run(); }

    @Override public JsonElement toJson() { return null; }
    @Override public void fromJson(JsonElement e) {}
}
