package com.lampui.mixin;

import com.lampui.client.LampClickState;
import com.lampui.client.LampSounds;
import com.lampui.client.render.LampButtons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin extends ClickableWidget {
    @Unique private float lampui$hover;
    @Unique private long lampui$last;
    @Unique private boolean lampui$wasHovered;
    @Unique private boolean lampui$init;

    protected PressableWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void lampui$render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object) this instanceof ButtonWidget)) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = Util.getMeasuringTimeMs();

        float dt = this.lampui$last == 0L ? 0.016f : Math.min(0.1f, (now - this.lampui$last) / 1000f);
        this.lampui$last = now;

        boolean hovered = this.active && (this.isHovered() || this.isFocused());
        this.lampui$hover += ((hovered ? 1f : 0f) - this.lampui$hover) * (1f - (float) Math.exp(-dt * 14f));

        if (this.lampui$init && hovered && !this.lampui$wasHovered && com.lampui.client.LampConfig.get().hoverSound) {
            float pitch = 0.92f + (float) Math.random() * 0.18f;
            mc.getSoundManager().play(PositionedSoundInstance.master(LampSounds.HOVER, pitch, 0.4f));
        }
        this.lampui$wasHovered = hovered;
        this.lampui$init = true;

        long click = ((LampClickState) (Object) this).lampui$getClickMs();
        float clickT = (now - click) / 260f;

        LampButtons.draw(context, mc.textRenderer, this.getMessage(), this.getX(), this.getY(),
                this.getWidth(), this.getHeight(), this.lampui$hover, clickT, this.active, this.alpha, now);
        ci.cancel();
    }
}
