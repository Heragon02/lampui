package com.lampui.mixin;

import com.lampui.client.LampClickState;
import com.lampui.client.LampSounds;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClickableWidget.class)
public abstract class ClickableWidgetMixin implements LampClickState {
    @Unique private long lampui$clickMs = -100000L;

    @Override
    public long lampui$getClickMs() {
        return this.lampui$clickMs;
    }

    /** Запоминаем время клика (для вспышки) и подменяем ванильный звук нашим. */
    @Inject(method = "playDownSound", at = @At("HEAD"), cancellable = true)
    private void lampui$click(SoundManager soundManager, CallbackInfo ci) {
        this.lampui$clickMs = Util.getMeasuringTimeMs();
        if ((Object) this instanceof ButtonWidget) {
            float pitch = 0.95f + (float) Math.random() * 0.15f;
            if (com.lampui.client.LampConfig.get().clickSound) {
                soundManager.play(PositionedSoundInstance.master(LampSounds.CLICK, pitch, 0.55f));
            }
            ci.cancel();
        }
    }
}
