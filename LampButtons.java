package com.lampui.client.render;

import static com.lampui.client.render.LampDraw.*;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Отрисовка полупрозрачной «стеклянной» кнопки с тёплым свечением, бликом и вспышкой при клике. */
public final class LampButtons {
    private LampButtons() {}

    public static void draw(DrawContext g, TextRenderer tr, Text msg, int x, int y, int w, int h,
                            float hover, float clickT, boolean active, float alpha, long nowMs) {
        draw(g, tr, msg, x, y, w, h, hover, clickT, active, alpha, nowMs, -1f);
    }

    /** fillFrac >= 0 — режим слайдера: тёплая заливка и бегунок. */
    public static void draw(DrawContext g, TextRenderer tr, Text msg, int x, int y, int w, int h,
                            float hover, float clickT, boolean active, float alpha, long nowMs, float fillFrac) {
        float e = hover * 1.5f;
        int x1 = Math.round(x - e), y1 = Math.round(y - e * 0.7f);
        int x2 = Math.round(x + w + e), y2 = Math.round(y + h + e * 0.7f);

        // внешнее свечение
        if (hover > 0.02f) {
            for (int i = 3; i >= 1; i--) {
                frame(g, x1 - i, y1 - i, x2 + i, y2 + i, argb(1f, 255, 170, 80), 0.06f * hover * (4 - i) / 3f * alpha);
            }
        }

        int top, bot, border;
        if (active) {
            top = lerp(argb(0.40f, 56, 46, 66), argb(0.62f, 150, 92, 46), hover);
            bot = lerp(argb(0.46f, 30, 24, 38), argb(0.70f, 96, 52, 28), hover);
            border = lerp(argb(0.30f, 255, 214, 160), argb(0.95f, 255, 196, 112), hover);
        } else {
            top = argb(0.28f, 44, 40, 48);
            bot = argb(0.28f, 30, 28, 34);
            border = argb(0.14f, 200, 190, 180);
        }

        // «скруглённая» рамка и тело (углы срезаны на 1 пиксель)
        fill(g, x1 + 1, y1, x2 - 1, y1 + 1, border, alpha);
        fill(g, x1 + 1, y2 - 1, x2 - 1, y2, border, alpha);
        fill(g, x1, y1 + 1, x1 + 1, y2 - 1, border, alpha);
        fill(g, x2 - 1, y1 + 1, x2, y2 - 1, border, alpha);
        grad(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, top, bot, alpha);

        if (fillFrac >= 0f) {
            int inner = x2 - x1 - 2;
            fill(g, x1 + 1, y1 + 1, x1 + 1 + (int) (inner * fillFrac), y2 - 1, argb(active ? 0.35f : 0.12f, 255, 170, 80), alpha);
            int hx = x1 + 1 + (int) ((inner - 4) * fillFrac);
            if (active) ellipse(g, hx + 2, (y1 + y2) / 2f, 7, 7, 0xFFFFB45A, 0.16f * alpha, 2);
            fill(g, hx, y1 - 1, hx + 4, y2 + 1, active ? 0xFFFFE4A0 : 0xFF8A8478, alpha);
        } else if (active) {
            // линия-подчёркивание, растёт от центра
            int wid = (int) ((x2 - x1 - 8) * hover);
            int cx = (x1 + x2) / 2;
            fill(g, cx - wid / 2, y2 - 3, cx + wid / 2, y2 - 2, argb(0.9f, 255, 205, 130), alpha);

            // бегущий блик
            if (hover > 0.02f) {
                float ph = (nowMs % 1600L) / 1600f;
                int sx = (int) (x1 + ph * ((x2 - x1) + 30) - 15);
                int sx1 = Math.max(x1 + 1, sx), sx2 = Math.min(x2 - 1, sx + 7);
                fill(g, sx1, y1 + 1, sx2, y2 - 1, argb(0.12f * hover, 255, 240, 210), alpha);
            }
        }

        // вспышка при клике
        if (clickT < 1f) {
            fill(g, x1 + 1, y1 + 1, x2 - 1, y2 - 1, argb((1f - clickT) * 0.28f, 255, 230, 180), alpha);
        }

        if (alpha > 0.05f) {
            int col = active ? lerp(0xFFE6D8C8, 0xFFFFE4A0, hover) : 0xFF80786E;
            g.drawCenteredTextWithShadow(tr, msg, (x1 + x2) / 2, y + (h - 8) / 2, mulAlpha(col, alpha));
        }
    }
}
