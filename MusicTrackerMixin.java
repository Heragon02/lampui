package com.lampui.mixin;

import com.lampui.client.LampConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicTracker.class)
public abstract class MusicTrackerMixin {
    /** Пока мы в меню и включена наша музыка — ванильная не запускается. */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void lampui$tick(CallbackInfo ci) {
        if (LampConfig.get().musicEnabled && MinecraftClient.getInstance().world == null) {
            ci.cancel();
        }
    }
}
