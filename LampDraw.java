package com.lampui.client.render;

import net.minecraft.client.gui.DrawContext;

/** Небольшие помощники для рисования: цвета, прозрачность, эллипсы. Всё через fill/fillGradient. */
public final class LampDraw {
    private LampDraw() {}

    public static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }

    public static float smooth(float e0, float e1, float x) {
        float t = clamp01((x - e0) / (e1 - e0));
        return t * t * (3f - 2f * t);
    }

    public static int argb(float a, int r, int g, int b) {
        int ia = Math.max(0, Math.min(255, Math.round(a * 255f)));
        return (ia << 24) | ((r & 255) << 16) | ((g & 255) << 8) | (b & 255);
    }

    private static int mix(int c1, int c2, int shift, float t) {
        int a = (c1 >>> shift) & 255;
        int b = (c2 >>> shift) & 255;
        return Math.max(0, Math.min(255, Math.round(a + (b - a) * t)));
    }

    public static int lerp(int c1, int c2, float t) {
        t = clamp01(t);
        return (mix(c1, c2, 24, t) << 24) | (mix(c1, c2, 16, t) << 16) | (mix(c1, c2, 8, t) << 8) | mix(c1, c2, 0, t);
    }

    public static int mulAlpha(int c, float m) {
        int a = Math.round(((c >>> 24) & 255) * clamp01(m));
        return (a << 24) | (c & 0xFFFFFF);
    }

    public static void fill(DrawContext g, int x1, int y1, int x2, int y2, int c, float alpha) {
        if (x2 <= x1 || y2 <= y1) return;
        int cc = mulAlpha(c, alpha);
        if ((cc >>> 24) == 0) return;
        g.fill(x1, y1, x2, y2, cc);
    }

    public static void grad(DrawContext g, int x1, int y1, int x2, int y2, int c1, int c2, float alpha) {
        if (x2 <= x1 || y2 <= y1) return;
        g.fillGradient(x1, y1, x2, y2, mulAlpha(c1, alpha), mulAlpha(c2, alpha));
    }

    public static void ellipse(DrawContext g, float cx, float cy, float rx, float ry, int c, float alpha, int rowStep) {
        if (rx <= 0f || ry <= 0f) return;
        for (float dy = -ry; dy <= ry; dy += rowStep) {
            float k = 1f - (dy * dy) / (ry * ry);
            if (k <= 0f) continue;
            float hw = rx * (float) Math.sqrt(k);
            int y = Math.round(cy + dy);
            fill(g, Math.round(cx - hw), y, Math.round(cx + hw) + 1, y + rowStep, c, alpha);
        }
    }

    /** Рамка толщиной 1 пиксель. */
    public static void frame(DrawContext g, int x1, int y1, int x2, int y2, int c, float alpha) {
        fill(g, x1, y1, x2, y1 + 1, c, alpha);
        fill(g, x1, y2 - 1, x2, y2, c, alpha);
        fill(g, x1, y1 + 1, x1 + 1, y2 - 1, c, alpha);
        fill(g, x2 - 1, y1 + 1, x2, y2 - 1, c, alpha);
    }
}
