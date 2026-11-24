package com.lampui.mixin;

import com.lampui.client.render.LampScene;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Shadow public int width;
    @Shadow public int height;

    /** Заменяем ванильную панораму нашим процедурным пейзажем на всех экранах вне мира. */
    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
    private void lampui$background(DrawContext context, float deltaTicks, CallbackInfo ci) {
        LampScene.INSTANCE.render(context, this.width, this.height, 1f);
        ci.cancel();
    }
}
