package dev.novavisuals.module.impl;

import dev.novavisuals.module.Module;
import dev.novavisuals.module.settings.BoolSetting;
import dev.novavisuals.module.settings.ModeSetting;
import dev.novavisuals.module.settings.NumberSetting;

public class Interface extends Module {
    public final ModeSetting theme = add(new ModeSetting("theme", "Тема", 0,
            "Фиолетовая", "Голубая", "Розовая", "Мятная", "Красная", "Радуга"));
    public final NumberSetting speed = add(new NumberSetting("speed", "Скорость переливания", 1.0, 0.0, 3.0, 0.1));
    public final BoolSetting dim = add(new BoolSetting("dim", "Затемнять фон меню", true));

    public Interface() {
        super("interface", "Интерфейс", "Цвета и стиль всех визуалов", true, false);
    }
}
