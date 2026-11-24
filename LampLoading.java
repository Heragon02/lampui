package com.lampui.client.render;

import static com.lampui.client.render.LampDraw.*;

import net.minecraft.client.gui.DrawContext;

/** Фонарик с живым огоньком и тёплый индикатор прогресса для экрана загрузки. Без текста и текстур. */
public final class LampLoading {
    private LampLoading() {}

    public static void draw(DrawContext g, int W, int H, float alpha, float progress) {
        if (alpha <= 0.01f) return;
        float t = LampScene.INSTANCE.elapsed();
        float flick = 0.85f + 0.15f * (float) Math.sin(t * 9f) + 0.05f * (float) Math.sin(t * 23f);
        int cx = W / 2;
        int cy = (int) (H * 0.42f);

        // свечение
        for (int i = 6; i >= 1; i--) {
            float r = 10f + i * 10f;
            ellipse(g, cx, cy, r, r, 0xFFFFB45A, alpha * 0.045f * flick, 2);
        }
        int frame = 0xFF1A1210;
        // кольцо и крышка
        frame(g, cx - 3, cy - 20, cx + 4, cy - 14, frame, alpha);
        fill(g, cx - 8, cy - 14, cx + 9, cy - 11, frame, alpha);
        // корпус
        fill(g, cx - 7, cy - 11, cx + 8, cy + 9, frame, alpha);
        grad(g, cx - 6, cy - 10, cx + 7, cy + 8, lerp(0xFFFFE9A8, 0xFFFFFFFF, flick - 0.85f), 0xFFFF8E3C, alpha);
        // огонёк
        ellipse(g, cx + 0.5f, cy - 1f, 2.5f, 4f * flick, 0xFFFFFFE8, alpha, 1);
        // основание
        fill(g, cx - 8, cy + 9, cx + 9, cy + 12, frame, alpha);

        // прогресс
        int bw = 160, bh = 4;
        int bx = cx - bw / 2;
        int by = (int) (H * 0.62f);
        fill(g, bx - 1, by - 1, bx + bw + 1, by + bh + 1, argb(0.45f, 0, 0, 0), alpha);
        int fw = (int) (bw * clamp01(progress));
        fill(g, bx, by, bx + fw, by + bh, 0xFFE89A4A, alpha);
        fill(g, bx, by, bx + fw, by + 1, 0xFFFFD89A, alpha);
        if (fw > 0) {
            ellipse(g, bx + fw, by + bh / 2f, 10, 10, 0xFFFFB45A, alpha * 0.18f * flick, 2);
            ellipse(g, bx + fw, by + bh / 2f, 4, 4, 0xFFFFE8B0, alpha * 0.5f, 1);
        }
    }
}
