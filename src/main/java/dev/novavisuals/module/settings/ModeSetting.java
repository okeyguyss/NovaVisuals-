package dev.novavisuals.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class ModeSetting extends Setting<Integer> {
    private final String[] modes;

    public ModeSetting(String id, String name, int def, String... modes) {
        super(id, name, def);
        this.modes = modes;
    }

    public String getMode() { return modes[value]; }
    public int getIndex() { return value; }
    public boolean is(int index) { return value == index; }

    public void next() { value = (value + 1) % modes.length; }
    public void prev() { value = (value - 1 + modes.length) % modes.length; }

    @Override public JsonElement toJson() { return new JsonPrimitive(getMode()); }

    @Override
    public void fromJson(JsonElement e) {
        String s = e.getAsString();
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(s)) { value = i; return; }
        }
    }
}
