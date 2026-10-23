package dev.novavisuals.module.settings;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
    private final String id;
    private final String name;
    protected final T defaultValue;
    protected T value;
    private BooleanSupplier visible = () -> true;

    protected Setting(String id, String name, T defaultValue) {
        this.id = id;
        this.name = name;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    @SuppressWarnings("unchecked")
    public <S extends Setting<T>> S visibleWhen(BooleanSupplier supplier) {
        this.visible = supplier;
        return (S) this;
    }

    public T get() { return value; }
    public void set(T v) { this.value = v; }
    public void reset() { set(defaultValue); }
    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isVisible() { return visible.getAsBoolean(); }

    /** null = настройка не сохраняется в конфиг */
    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement element);
}
