package com.lampui.client.render;

import static com.lampui.client.render.LampDraw.*;

import com.lampui.client.LampConfig;
import java.time.LocalTime;
import java.util.Random;
import net.minecraft.client.gui.DrawContext;

/**
 * Процедурный «вращающийся» пейзаж. Мир — круг (период 2π), горизонт крутится бесшовно.
 * Время суток — по системным часам (или вручную в настройках, или -Dlampui.hour=21.5).
 */
public final class LampScene {
    public static final LampScene INSTANCE = new LampScene();

    private static final int LAYERS = 4;
    private static final int[] FREQ = {1, 2, 3, 5, 7, 11};
    private static final float[] SPEED = {0.010f, 0.017f, 0.028f, 0.045f};
    private static final float FOV = 1.7f;
    private static final float TAU = (float) (Math.PI * 2);

    private static final float[] KH = {0f, 5f, 6.5f, 8.5f, 12f, 16.5f, 18.5f, 20f, 21.5f, 24f};
    private static final int[] KTOP = {0xFF050914, 0xFF0B1030, 0xFF3A4A7A, 0xFF5A8FD0, 0xFF3F86D6, 0xFF4A86CC, 0xFF3B3A78, 0xFF1A1A40, 0xFF0A0F26, 0xFF050914};
    private static final int[] KBOT = {0xFF111A33, 0xFF2A2447, 0xFFF2A070, 0xFFCFE3EE, 0xFFA9D4F0, 0xFFE8D9B0, 0xFFFF8A4C, 0xFF7A3A5A, 0xFF1D2547, 0xFF111A33};

    private static final float CABIN_THETA = 0.9f;
    private static final String HOUR_OVERRIDE = System.getProperty("lampui.hour");

    private final long t0 = System.nanoTime();
    private final float offset;
    private final float[][] weight = new float[LAYERS][FREQ.length];
    private final float[][] phase = new float[LAYERS][FREQ.length];
    private final float[][] drift = new float[LAYERS][FREQ.length];

    private final float[][] treeTheta = new float[2][];
    private final float[][] treeScale = new float[2][];
    private final float[] starTh = new float[110], starY = new float[110], starPh = new float[110];
    private final float[] cloudTh = new float[7], cloudY = new float[7], cloudS = new float[7];
    private final float[] ffTh = new float[36], ffY = new float[36], ffPh = new float[36];
    private final float[] flX = new float[90], flY = new float[90], flPh = new float[90];

    // отражения для озера
    private final float[][] refY = new float[3][700];
    private final int[][] refC = new int[3][700];
    private int refCols;

    // состояние кадра
    private DrawContext g;
    private Biome b = Biome.FOREST;
    private float alpha, rotT, lastT;
    private int W, H;
    private float daylight, night, warm;
    private int skyTop, skyBot, lightCol;

    private LampScene() {
        Random r = new Random();
        offset = r.nextFloat() * TAU;
        for (int l = 0; l < LAYERS; l++) {
            for (int k = 0; k < FREQ.length; k++) {
                weight[l][k] = (1f / (1f + k * 0.8f)) * (0.7f + r.nextFloat() * 0.6f) * (l >= 2 && k >= 4 ? 0.35f : 1f);
                phase[l][k] = r.nextFloat() * TAU;
                drift[l][k] = (r.nextFloat() - 0.5f) * 0.12f;
            }
        }
        int[] counts = {34, 20};
        for (int i = 0; i < 2; i++) {
            treeTheta[i] = new float[counts[i]];
            treeScale[i] = new float[counts[i]];
            for (int j = 0; j < counts[i]; j++) {
                float th = (j + r.nextFloat() * 0.9f) / counts[i] * TAU;
                treeTheta[i][j] = th;
                treeScale[i][j] = 0.6f + r.nextFloat() * 0.8f;
            }
        }
        for (int i = 0; i < starTh.length; i++) { starTh[i] = r.nextFloat() * TAU; starY[i] = r.nextFloat(); starPh[i] = r.nextFloat() * TAU; }
        for (int i = 0; i < cloudTh.length; i++) {
            cloudTh[i] = (i + r.nextFloat() * 0.8f) / cloudTh.length * TAU;
            cloudY[i] = 0.08f + r.nextFloat() * 0.30f;
            cloudS[i] = 0.7f + r.nextFloat() * 0.8f;
        }
        for (int i = 0; i < ffTh.length; i++) { ffTh[i] = r.nextFloat() * TAU; ffY[i] = 0.60f + r.nextFloat() * 0.32f; ffPh[i] = r.nextFloat() * TAU; }
        for (int i = 0; i < flX.length; i++) { flX[i] = r.nextFloat(); flY[i] = r.nextFloat(); flPh[i] = r.nextFloat() * 6f; }
    }

    public float elapsed() { return (System.nanoTime() - t0) / 1e9f; }

    /** Текущий «час» сцены с учётом настроек. */
    public static float hourNow() {
        LampConfig c = LampConfig.get();
        if (!c.realTime) return c.manualHour % 24f;
        if (HOUR_OVERRIDE != null) {
            try { return Float.parseFloat(HOUR_OVERRIDE) % 24f; } catch (NumberFormatException ignored) { }
        }
        LocalTime n = LocalTime.now();
        return n.getHour() + n.getMinute() / 60f + n.getSecond() / 3600f;
    }

    private static float wrap(double d) {
        double tau = Math.PI * 2;
        d %= tau;
        if (d > Math.PI) d -= tau; else if (d < -Math.PI) d += tau;
        return (float) d;
    }

    private void px(int x1, int y1, int x2, int y2, int c, float a) {
        LampDraw.fill(g, x1, y1, x2, y2, c, a * alpha);
    }

    private void el(float cx, float cy, float rx, float ry, int c, float a, int step) {
        ellipse(g, cx, cy, rx, ry, c, a * alpha, step);
    }

    // ---------------------------------------------------------------- рендер

    public void render(DrawContext ctx, int w, int h, float alpha) {
        if (alpha <= 0.003f) return;
        this.g = ctx; this.alpha = alpha; this.W = w; this.H = h;
        LampConfig cfg = LampConfig.get();
        b = Biome.resolve(cfg.biome);

        float t = elapsed();
        float dt = Math.min(0.25f, Math.max(0f, t - lastT));
        lastT = t;
        rotT += dt * cfg.rotationSpeed;

        float hour = hourNow();
        double a = (hour - 6f) / 12.0 * Math.PI;
        float sunElev = (float) Math.sin(a);
        daylight = smooth(-0.12f, 0.35f, sunElev);
        night = 1f - daylight;
        warm = clamp01(1f - Math.abs(sunElev + 0.02f) / 0.30f);
        sampleSky(hour);
        int sunWarm = lerp(0xFFFFF2D0, 0xFFFF9A50, warm);
        lightCol = lerp(sunWarm, 0xFFB8C8FF, night);

        drawSky();
        drawStars(t);
        if (b.aurora) drawAurora(t);
        drawCelestials(a);
        drawClouds(t);
        drawLandscape(t, a);
        drawParticles(t);
        drawLighting();
    }

    private void sampleSky(float h) {
        int i = 0;
        while (i < KH.length - 2 && h >= KH[i + 1]) i++;
        float t = (h - KH[i]) / (KH[i + 1] - KH[i]);
        t = t * t * (3f - 2f * t);
        skyTop = lerp(KTOP[i], KTOP[i + 1], t);
        skyBot = lerp(KBOT[i], KBOT[i + 1], t);
        if (b.tintAmt > 0f) {
            skyTop = lerp(skyTop, b.tint, b.tintAmt * daylight * 0.6f);
            skyBot = lerp(skyBot, b.tint, b.tintAmt * daylight);
        }
    }

    private void drawSky() {
        int hy = (int) (H * 0.66f);
        grad(g, 0, 0, W, hy, skyTop, skyBot, alpha);
        grad(g, 0, hy, W, H, skyBot, lerp(skyBot, 0xFF000000, 0.35f), alpha);
        if (warm > 0.01f) {
            grad(g, 0, hy - (int) (H * 0.28f), W, hy, argb(0f, 255, 140, 70), argb(0.30f * warm, 255, 140, 70), alpha);
        }
    }

    private void drawStars(float t) {
        if (night < 0.05f) return;
        float th0 = t * 0.004f;
        for (int i = 0; i < starTh.length; i++) {
            float d = wrap(starTh[i] - th0);
            int x = (int) (W * 0.5f + d / (FOV * 1.3f) * W);
            if (x < 0 || x >= W) continue;
            int y = (int) (starY[i] * H * 0.62f);
            float fade = 1f - starY[i] * 0.8f;
            float tw = 0.55f + 0.45f * (float) Math.sin(t * (1.2f + starPh[i] * 0.3f) + starPh[i] * 10f);
            float a = night * night * tw * fade;
            px(x, y, x + 1, y + 1, 0xFFFFF5E1, a);
            if (starPh[i] > 5.8f) {
                px(x - 1, y, x + 2, y + 1, 0xFFFFF5E1, a * 0.4f);
                px(x, y - 1, x + 1, y + 2, 0xFFFFF5E1, a * 0.4f);
            }
        }
    }

    private void drawAurora(float t) {
        if (night < 0.3f) return;
        int step = 6;
        for (int band = 0; band < 3; band++) {
            int col = band == 1 ? 0xFFAA6EFF : 0xFF50FFAA;
            for (int x = 0; x < W; x += step) {
                float yTop = H * (0.10f + 0.07f * band) + (float) Math.sin(x * 0.02f + t * 0.4f + band * 2f) * H * 0.04f;
                float hh = H * 0.18f;
                float a = night * night * 0.24f * (0.6f + 0.4f * (float) Math.sin(x * 0.05f + t * 0.8f + band));
                int mid = (int) (yTop + hh * 0.4f);
                grad(g, x, (int) yTop, x + step, mid, mulAlpha(col, 0f), mulAlpha(col, a), alpha);
                grad(g, x, mid, x + step, (int) (yTop + hh), mulAlpha(col, a), mulAlpha(col, 0f), alpha);
            }
        }
    }

    private float sunX(double a) { return W * 0.5f - (float) Math.cos(a) * W * 0.52f; }
    private float sunY(double a) { return H * 0.62f - (float) Math.sin(a) * H * 0.50f; }

    private void drawCelestials(double a) {
        float R = H * 0.042f;
        float sx = sunX(a), sy = sunY(a);
        float sunVis = clamp01((float) Math.sin(a) + 0.25f);
        if (sunVis > 0.01f && sy < H * 0.85f) {
            int sunCol = lerp(0xFFFFF4D2, 0xFFFF9248, warm);
            for (int i = 6; i >= 1; i--) {
                float r = R * (1f + i * 1.1f);
                el(sx, sy, r, r, sunCol, (0.018f + 0.012f * (6 - i)) * (0.5f + warm * 0.9f) * sunVis, 2);
            }
            el(sx, sy, R, R, sunCol, sunVis, 1);
        }
        float mx = W * 0.5f + (float) Math.cos(a) * W * 0.52f;
        float my = H * 0.62f + (float) Math.sin(a) * H * 0.50f;
        float moonVis = clamp01(-(float) Math.sin(a) + 0.25f);
        if (moonVis > 0.01f && my < H * 0.85f) {
            for (int i = 5; i >= 1; i--) {
                float r = R * (1f + i * 0.9f);
                el(mx, my, r, r, 0xFFB8C8FF, (0.012f + 0.008f * (5 - i)) * moonVis, 2);
            }
            el(mx, my, R * 0.9f, R * 0.9f, 0xFFE8EEFF, moonVis, 1);
            int cr = 0xFFC4CCE4;
            el(mx - R * 0.3f, my - R * 0.2f, R * 0.22f, R * 0.22f, cr, moonVis * 0.8f, 1);
            el(mx + R * 0.25f, my + R * 0.2f, R * 0.30f, R * 0.30f, cr, moonVis * 0.7f, 1);
            el(mx + R * 0.1f, my - R * 0.45f, R * 0.14f, R * 0.14f, cr, moonVis * 0.7f, 1);
        }
    }

    private void drawClouds(float t) {
        float ct = t * 0.012f;
        int base = lerp(0xFF4A5478, 0xFFFFFFFF, daylight);
        base = lerp(base, 0xFFFF9C70, warm * 0.55f);
        base = lerp(base, skyBot, 0.2f);
        int under = lerp(base, 0xFF000000, 0.18f);
        float s0 = H / 270f;
        for (int i = 0; i < cloudTh.length; i++) {
            float d = wrap(cloudTh[i] - ct);
            float x = W * 0.5f + d / FOV * W;
            if (x < -120f * s0 || x > W + 120f * s0) continue;
            float y = cloudY[i] * H, s = cloudS[i] * s0;
            el(x, y + 3 * s, 38 * s, 8 * s, under, 0.5f, 1);
            el(x - 18 * s, y, 22 * s, 9 * s, base, 0.5f, 1);
            el(x + 8 * s, y - 5 * s, 26 * s, 11 * s, base, 0.5f, 1);
            el(x + 26 * s, y + 1 * s, 18 * s, 7 * s, base, 0.5f, 1);
        }
    }

    // ---------------------------------------------------------------- ландшафт

    private float terrain(int l, double th, float t) {
        float sum = 0f, wsum = 0f;
        for (int k = 0; k < FREQ.length; k++) {
            float w = weight[l][k] * (k >= 3 ? b.detail : 1f);
            float s = (float) Math.sin(FREQ[k] * th + phase[l][k] + drift[l][k] * t);
            if (b.ridge[l]) s = 1f - 2f * Math.abs(s);
            sum += w * s;
            wsum += w;
        }
        return sum / wsum;
    }

    private float groundY(int l, double th, float t) {
        return H * b.base[l] - terrain(l, th, t) * H * b.amp[l];
    }

    private void drawLandscape(float t, double a) {
        int step = Math.max(1, Math.round(W / 480f));
        float sunElev = (float) Math.sin(a);
        float lightDir = Math.signum(sunElev >= 0 ? (float) -Math.cos(a) : (float) Math.cos(a));
        if (lightDir == 0f) lightDir = 1f;
        float lightAmt = daylight * (0.4f + 0.6f * warm) + night * 0.25f;
        int cols = Math.min(W / step + 2, 700);
        refCols = cols;

        for (int l = 0; l < LAYERS; l++) {
            if (b.water && l == 3) drawWater(t, a, step);
            float th0 = rotT * SPEED[l] + offset;
            int body = lerp(lerp(b.nightL[l], b.dayL[l], daylight), skyBot, b.fog[l]);
            float prevY = 0f;
            for (int i = 0; i < cols; i++) {
                int x = i * step;
                double th = th0 + (x / (float) W - 0.5f) * FOV;
                float y = groundY(l, th, t);
                float facing = i == 0 ? 0f : clamp01(0.5f + (y - prevY) / (4f * step)) * 2f - 1f;
                float lit = facing * lightDir;
                int c = lit > 0f
                        ? lerp(body, lightCol, lit * 0.22f * lightAmt * (l < 2 ? 1f : 0.6f))
                        : lerp(body, 0xFF000000, -lit * 0.18f);
                int iy = (int) y;
                px(x, iy, x + step, H, c, 1f);
                if (b.snowCaps && l == 0) {
                    float snowLine = H * 0.37f;
                    if (y < snowLine) {
                        int snow = lerp(lerp(0xFF3A4A66, 0xFFF4F0F8, daylight), lightCol, warm * 0.4f);
                        snow = lerp(snow, skyBot, b.fog[0] * 0.5f);
                        px(x, iy, x + step, (int) Math.min(snowLine, y + (snowLine - y) * 0.5f + 2), snow, 1f);
                    }
                }
                px(x, iy, x + step, iy + 2, lerp(c, lightCol, 0.35f * lightAmt), 1f);
                if (b.water && l < 3) { refY[l][i] = y; refC[l][i] = c; }
                prevY = y;
            }
            if (l >= 2) drawTrees(l, th0, t);
            if (l == 3 && b.cabin) drawCabin(th0, t);
        }
    }

    private void drawWater(float t, double a, int step) {
        int wy = (int) (H * 0.64f);
        int top = lerp(skyBot, 0xFF0A2030, 0.35f);
        int bot = lerp(skyTop, 0xFF05121C, 0.55f);
        grad(g, 0, wy, W, H, top, bot, alpha);
        // отражения гор
        for (int l = 0; l < 3; l++) {
            for (int i = 0; i < refCols; i++) {
                float y = refY[l][i];
                if (y >= wy) continue;
                int depth = (int) ((wy - y) * 0.7f);
                int x = i * step + Math.round((float) Math.sin(t * 1.2f + i * 0.3f + l) * 1.2f);
                px(x, wy, x + step, Math.min(H, wy + depth), lerp(refC[l][i], top, 0.4f), 0.55f);
            }
        }
        // отражение светила
        float sunVis = clamp01((float) Math.sin(a) + 0.25f), moonVis = clamp01(-(float) Math.sin(a) + 0.25f);
        float rx = sunVis > moonVis ? sunX(a) : W - sunX(a);
        float vis = Math.max(sunVis, moonVis);
        int rc = sunVis > moonVis ? lerp(0xFFFFF4D2, 0xFFFF9248, warm) : 0xFFDDE6FF;
        for (int row = wy + 2; row < H; row += 3) {
            float k = (row - wy) / (float) (H - wy);
            float wid = 3f + k * 14f * (0.6f + 0.4f * (float) Math.sin(row * 0.7f + t * 2f));
            float cx = rx + (float) Math.sin(row * 0.5f + t * 2.4f) * (2f + k * 10f);
            px((int) (cx - wid / 2), row, (int) (cx + wid / 2), row + 2, rc, 0.45f * vis * (1f - k * 0.8f));
        }
        // блики на воде
        for (int j = 0; j < 40; j++) {
            int x = (int) (((j * 977L) + (long) (t * 6f * (1 + j % 3))) % W);
            int y = wy + 3 + (int) ((j * 37L) % Math.max(1, H - wy - 6));
            float tw = 0.5f + 0.5f * (float) Math.sin(t + j);
            px(x, y, x + 8 + (j % 5) * 5, y + 1, lightCol, 0.10f * tw);
        }
        // дымка над водой
        grad(g, 0, wy - (int) (H * 0.05f), W, wy, argb(0f, 255, 255, 255), mulAlpha(skyBot, 0.35f), alpha);
    }

    private void drawTrees(int l, float th0, float t) {
        int idx = l - 2;
        int treeCol = lerp(lerp(b.treeNight, b.treeDay, daylight), skyBot, b.fog[l] * 0.8f);
        float sc = H / 270f * (l == 2 ? 0.55f : 1f);
        for (int i = 0; i < treeTheta[idx].length; i++) {
            float s = treeScale[idx][i];
            if (l == 3 && b.cabin && Math.abs(wrap(treeTheta[idx][i] - CABIN_THETA)) < 0.13f) continue;
            float d = wrap(treeTheta[idx][i] - th0);
            int x = Math.round(W * 0.5f + d / FOV * W);
            if (x < -40 || x > W + 40) continue;
            int by = Math.round(groundY(l, treeTheta[idx][i], t)) + 2;
            if (b.tree == Biome.Tree.CACTUS) cactus(x, by, Math.max(6, (int) (18 * s * sc)), treeCol);
            else pine(x, by, Math.max(6, (int) (26 * s * sc)), treeCol, b.tree == Biome.Tree.SNOW_PINE);
        }
    }

    private void pine(int cx, int baseY, int hgt, int color, boolean snowy) {
        int tier = Math.max(3, hgt / 4);
        int cap = lerp(color, 0xFFFFFFFF, 0.55f * daylight + 0.2f);
        for (int r = 0; r < hgt; r++) {
            float tt = (float) r / hgt;
            float hw = 0.6f + tt * hgt * 0.18f + ((r % tier) / (float) tier) * hgt * 0.07f;
            int c = snowy && (r % tier) == 0 ? cap : color;
            px((int) (cx - hw), baseY - hgt + r, (int) (cx + hw) + 1, baseY - hgt + r + 1, c, 1f);
        }
        px(cx - 1, baseY, cx + 1, baseY + Math.max(2, hgt / 8), color, 1f);
    }

    private void cactus(int cx, int baseY, int hgt, int c) {
        px(cx - 1, baseY - hgt, cx + 2, baseY + 1, c, 1f);
        int ay = baseY - (int) (hgt * 0.5f);
        px(cx - 5, ay, cx - 1, ay + 2, c, 1f);
        px(cx - 5, ay - (int) (hgt * 0.25f), cx - 3, ay + 2, c, 1f);
        int by2 = baseY - (int) (hgt * 0.35f);
        px(cx + 2, by2, cx + 6, by2 + 2, c, 1f);
        px(cx + 4, by2 - (int) (hgt * 0.22f), cx + 6, by2 + 2, c, 1f);
    }

    private void drawCabin(float th0, float t) {
        float d = wrap(CABIN_THETA - th0);
        float x = W * 0.5f + d / FOV * W;
        float s = H / 270f;
        if (x < -40 * s || x > W + 40 * s) return;
        int cx = Math.round(x);
        int y = Math.round(groundY(3, CABIN_THETA, t)) + 1;
        int bw = (int) (26 * s), bh = (int) (14 * s), rh = (int) (9 * s);
        int wall = lerp(0xFF0C0A0E, 0xFF4A3426, daylight);
        int roof = lerp(0xFF08090C, b == Biome.SNOW ? 0xFFDDE6F2 : 0xFF2C2024, daylight);

        px(cx - bw / 2, y - bh, cx + bw / 2, y, wall, 1f);
        for (int r = 0; r < rh; r++) {
            float hw = (r + 1) * (bw / 2f + 2 * s) / rh;
            px((int) (cx - hw), y - bh - rh + r, (int) (cx + hw) + 1, y - bh - rh + r + 1, roof, 1f);
        }
        int chx = cx + bw / 4;
        px(chx, y - bh - rh, chx + 3, y - bh - rh / 2, wall, 1f);

        float lit = clamp01(night + warm * 0.5f);
        int ww = Math.max(3, (int) (5 * s));
        int wx = cx - ww / 2, wy = y - bh + (int) (3 * s);
        px(wx, wy, wx + ww, wy + ww, lerp(0xFF3A4A58, 0xFFFFC46A, lit * 0.95f), 1f);
        if (lit > 0.05f) {
            for (int i = 3; i >= 1; i--) {
                float r = ww * (1.5f + i * 1.6f);
                el(wx + ww / 2f, wy + ww / 2f, r, r, 0xFFFFB45A, 0.07f * lit, 2);
            }
        }
        int smoke = lerp(0xFFB0B8C8, 0xFF404860, night);
        for (int k = 0; k < 4; k++) {
            float ph = (t * 0.4f + k * 0.25f) % 1f;
            float sx = chx + 1 + ph * 10 * s + (float) Math.sin(ph * 6 + k) * 2;
            float sy = y - bh - rh - ph * 26 * s;
            float r = 2f + ph * 4f;
            el(sx, sy, r, r * 0.8f, smoke, (1f - ph) * 0.25f, 1);
        }
    }

    private void drawParticles(float t) {
        if (b.fireflies) {
            float vis = smooth(0.25f, 0.8f, night);
            if (vis >= 0.02f) {
                float th0 = rotT * SPEED[3] + offset;
                for (int i = 0; i < ffTh.length; i++) {
                    float d = wrap(ffTh[i] - th0);
                    float x = W * 0.5f + d / FOV * W + (float) Math.sin(t * 0.6f + ffPh[i]) * 10f;
                    if (x < -10 || x > W + 10) continue;
                    float y = H * ffY[i] + (float) Math.sin(t * 0.9f + ffPh[i] * 2f) * 6f;
                    float a = vis * Math.max(0f, (float) Math.sin(t * 1.3f + ffPh[i] * 3f));
                    if (a < 0.03f) continue;
                    el(x, y, 9, 9, 0xFFDCFF96, a * 0.07f, 2);
                    el(x, y, 5, 5, 0xFFDCFF96, a * 0.18f, 1);
                    px((int) x, (int) y, (int) x + 2, (int) y + 2, 0xFFEAFFB0, a);
                }
            }
        }
        if (b.snowfall) {
            for (int i = 0; i < flX.length; i++) {
                float x = (((flX[i] * W + (float) Math.sin(t * 0.6f + flPh[i]) * 14f + t * 6f) % W) + W) % W;
                float y = (flY[i] * H + t * (16f + flPh[i] * 3f)) % H;
                int sz = flPh[i] > 4.7f ? 2 : 1;
                px((int) x, (int) y, (int) x + sz, (int) y + sz, 0xFFFFFFFF, 0.75f);
            }
        }
        if (b.dust && daylight > 0.05f) {
            for (int i = 0; i < 40; i++) {
                float x = (flX[i] * W + t * (12f + flPh[i] * 3f)) % W;
                float y = H * (0.45f + flY[i] * 0.4f) + (float) Math.sin(t + flPh[i]) * 4f;
                px((int) x, (int) y, (int) x + 1, (int) y + 1, 0xFFE8C090, 0.35f * daylight);
            }
        }
    }

    private void drawLighting() {
        px(0, 0, W, H, 0xFFFFA050, 0.035f + 0.09f * warm);
        px(0, 0, W, H, 0xFF101A46, 0.22f * night);
        for (int i = 0; i < 12; i++) {
            float k = 1f - i / 12f;
            float al = k * k * 0.38f;
            px(i * 3, 0, i * 3 + 3, H, 0xFF000000, al);
            px(W - i * 3 - 3, 0, W - i * 3, H, 0xFF000000, al);
        }
        grad(g, 0, 0, W, H / 6, 0x70000000, 0x00000000, alpha);
        grad(g, 0, H - H / 5, W, H, 0x00000000, 0x80000000, alpha);
    }
}
