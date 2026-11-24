package com.lampui.mixin;

import com.lampui.client.render.LampLoading;
import com.lampui.client.render.LampScene;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Shadow private long reloadCompleteTime;
    @Shadow private float progress;

    /**
     * Ванильный оверлей отрисовывается как обычно, а мы поверх него рисуем свой экран с той же
     * прозрачностью (она затухает тем же способом) — красный фон и логотип Mojang закрываются.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void lampui$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int w = context.getScaledWindowWidth();
        int h = context.getScaledWindowHeight();
        float f = this.reloadCompleteTime > -1L ? (Util.getMeasuringTimeMs() - this.reloadCompleteTime) / 1000f : -1f;
        float alpha = f >= 1f ? 1f - MathHelper.clamp(f - 1f, 0f, 1f) : 1f;
        LampScene.INSTANCE.render(context, w, h, alpha);
        LampLoading.draw(context, w, h, alpha, this.progress);
    }
}
