package com.lampui.client;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** Клиентские звуки: не регистрируются в реестре, играются по id из sounds.json. */
public final class LampSounds {
    public static final SoundEvent HOVER = of("button_hover");
    public static final SoundEvent CLICK = of("button_click");
    public static final SoundEvent MUSIC_DAY = of("menu_day");
    public static final SoundEvent MUSIC_NIGHT = of("menu_night");

    private static SoundEvent of(String name) {
        return SoundEvent.of(Identifier.of(LampUiClient.MOD_ID, name));
    }

    private LampSounds() {}
}
