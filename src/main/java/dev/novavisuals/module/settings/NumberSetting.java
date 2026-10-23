package dev.novavisuals.module.settings;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public class NumberSetting extends Setting<Double> {
    private final double min, max, step;

    public NumberSetting(String id, String name, double def, double min, double max, double step) {
        super(id, name, def);
        this.min = min;
        this.max = max;
        this.step = step;
    }

    @Override
    public void set(Double v) {
        double c = Math.max(min, Math.min(max, v));
        c = Math.round((c - min) / step) * step + min;
        c = Math.max(min, Math.min(max, c));
        this.value = Math.round(c * 10000.0) / 10000.0;
    }

    public float getF() { return value.floatValue(); }
    public double getMin() { return min; }
    public double getMax() { return max; }

    public double getPercent() { return (value - min) / (max - min); }

    public void setPercent(double p) {
        set(min + Math.max(0, Math.min(1, p)) * (max - min));
    }

    public String display() {
        if (step >= 1) return String.valueOf(Math.round(value));
        if (step >= 0.1) return String.format(Locale.US, "%.1f", value);
        return String.format(Locale.US, "%.2f", value);
    }

    @Override public JsonElement toJson() { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement e) { set(e.getAsDouble()); }
}
