package com.lampui.client;

import com.lampui.client.render.LampScene;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.util.Util;

/** Спокойная музыка в меню: днём и ночью разные треки, в мире выключается. */
public final class LampMusic {
    private static PositionedSoundInstance current;
    private static long startedAt;

    private LampMusic() {}

    public static void tick(MinecraftClient mc) {
        SoundManager sm = mc.getSoundManager();
        boolean want = LampConfig.get().musicEnabled && mc.world == null && mc.getOverlay() == null;
        if (want) {
            long now = Util.getMeasuringTimeMs();
            if (current == null || (now - startedAt > 3000L && !sm.isPlaying(current))) {
                float h = LampScene.hourNow();
                boolean day = h >= 6f && h < 19f;
                current = PositionedSoundInstance.music(day ? LampSounds.MUSIC_DAY : LampSounds.MUSIC_NIGHT);
                sm.play(current);
                startedAt = now;
            }
        } else if (current != null) {
            sm.stop(current);
            current = null;
        }
    }

    /** Перезапуск (например, после смены режима времени). */
    public static void restart(MinecraftClient mc) {
        if (current != null) mc.getSoundManager().stop(current);
        current = null;
    }
}
