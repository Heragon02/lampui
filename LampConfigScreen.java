package com.lampui.client;

import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class LampConfigScreen extends Screen {
    private final Screen parent;
    private SliderWidget hourSlider;

    public LampConfigScreen(Screen parent) {
        super(Text.literal("Lamp UI — настройки"));
        this.parent = parent;
    }

    private static String biomeName(String id) {
        return switch (id) {
            case "forest" -> "Лес";
            case "snow" -> "Снег";
            case "desert" -> "Пустыня";
            case "lake" -> "Озеро";
            default -> "Случайный";
        };
    }

    private static String onOff(boolean v) { return v ? "вкл" : "выкл"; }

    @Override
    protected void init() {
        LampConfig c = LampConfig.get();
        int w = 220, x = this.width / 2 - w / 2, y = this.height / 2 - 92, gap = 24;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Пейзаж: " + biomeName(c.biome)), btn -> {
            c.nextBiome();
            btn.setMessage(Text.literal("Пейзаж: " + biomeName(c.biome)));
        }).dimensions(x, y, w, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Время суток: " + (c.realTime ? "реальное" : "ручное")), btn -> {
            c.realTime = !c.realTime;
            btn.setMessage(Text.literal("Время суток: " + (c.realTime ? "реальное" : "ручное")));
            if (hourSlider != null) hourSlider.active = !c.realTime;
        }).dimensions(x, y + gap, w, 20).build());

        hourSlider = new FloatSlider(x, y + gap * 2, w, 20, "Час", 0, 24, c.manualHour, "%.1f", v -> c.manualHour = v);
        hourSlider.active = !c.realTime;
        this.addDrawableChild(hourSlider);

        this.addDrawableChild(new FloatSlider(x, y + gap * 3, w, 20, "Скорость вращения", 0.2, 3, c.rotationSpeed, "%.1fx",
                v -> c.rotationSpeed = v));

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Музыка: " + onOff(c.musicEnabled)), btn -> {
            c.musicEnabled = !c.musicEnabled;
            btn.setMessage(Text.literal("Музыка: " + onOff(c.musicEnabled)));
        }).dimensions(x, y + gap * 4, w, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Звук наведения: " + onOff(c.hoverSound)), btn -> {
            c.hoverSound = !c.hoverSound;
            btn.setMessage(Text.literal("Звук наведения: " + onOff(c.hoverSound)));
        }).dimensions(x, y + gap * 5, w, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Звук клика: " + onOff(c.clickSound)), btn -> {
            c.clickSound = !c.clickSound;
            btn.setMessage(Text.literal("Звук клика: " + onOff(c.clickSound)));
        }).dimensions(x, y + gap * 6, w, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), btn -> this.close())
                .dimensions(x, y + gap * 7 + 6, w, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - 118, 0xFFFFE4A0);
    }

    @Override
    public void close() {
        LampConfig.get().save();
        if (this.client != null) this.client.setScreen(parent);
    }

    private static final class FloatSlider extends SliderWidget {
        private final String label;
        private final double min, max;
        private final String fmt;
        private final Consumer<Float> callback;

        FloatSlider(int x, int y, int w, int h, String label, double min, double max, double cur, String fmt,
                    Consumer<Float> callback) {
            super(x, y, w, h, Text.empty(), (cur - min) / (max - min));
            this.label = label; this.min = min; this.max = max; this.fmt = fmt; this.callback = callback;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            double v = min + (max - min) * this.value;
            this.setMessage(Text.literal(label + ": " + String.format(Locale.ROOT, fmt, v)));
        }

        @Override
        protected void applyValue() {
            callback.accept((float) (min + (max - min) * this.value));
        }
    }
}
