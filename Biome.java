package com.lampui.client.render;

import java.util.Random;

/** Набор палитр и особенностей для каждого вида пейзажа. */
public final class Biome {
    public enum Tree { PINE, SNOW_PINE, CACTUS }

    public final String id;
    public final int[] dayL, nightL;
    public final float[] base, amp, fog;
    public final boolean[] ridge;
    public final float detail;          // насколько «шершавый» рельеф (0..1)
    public final Tree tree;
    public final int treeDay, treeNight;
    public final int tint; public final float tintAmt;   // оттенок неба днём
    public final boolean cabin, snowCaps, aurora, snowfall, dust, water, fireflies;

    private Biome(String id, int[] dayL, int[] nightL, float[] base, float[] amp, float[] fog, boolean[] ridge,
                  float detail, Tree tree, int treeDay, int treeNight, int tint, float tintAmt,
                  boolean cabin, boolean snowCaps, boolean aurora, boolean snowfall, boolean dust,
                  boolean water, boolean fireflies) {
        this.id = id; this.dayL = dayL; this.nightL = nightL; this.base = base; this.amp = amp; this.fog = fog;
        this.ridge = ridge; this.detail = detail; this.tree = tree; this.treeDay = treeDay; this.treeNight = treeNight;
        this.tint = tint; this.tintAmt = tintAmt; this.cabin = cabin; this.snowCaps = snowCaps; this.aurora = aurora;
        this.snowfall = snowfall; this.dust = dust; this.water = water; this.fireflies = fireflies;
    }

    public static final Biome FOREST = new Biome("forest",
            new int[]{0xFF6B7BA0, 0xFF53688A, 0xFF3C5E4C, 0xFF264636},
            new int[]{0xFF101830, 0xFF0D1426, 0xFF0A121A, 0xFF070D10},
            new float[]{0.50f, 0.58f, 0.68f, 0.80f}, new float[]{0.20f, 0.14f, 0.10f, 0.06f},
            new float[]{0.70f, 0.48f, 0.26f, 0.05f}, new boolean[]{true, true, false, false},
            1f, Tree.PINE, 0xFF1F4030, 0xFF070E0E, 0xFFFFFFFF, 0f,
            true, true, false, false, false, false, true);

    public static final Biome SNOW = new Biome("snow",
            new int[]{0xFFC4D2E8, 0xFFA9BCD8, 0xFFD2DCEA, 0xFFE9EFF8},
            new int[]{0xFF1B2640, 0xFF161F36, 0xFF1E283E, 0xFF232E46},
            new float[]{0.46f, 0.56f, 0.68f, 0.82f}, new float[]{0.24f, 0.16f, 0.10f, 0.06f},
            new float[]{0.65f, 0.45f, 0.25f, 0.05f}, new boolean[]{true, true, false, false},
            1f, Tree.SNOW_PINE, 0xFF2B4A47, 0xFF0A1214, 0xFFB8D0F0, 0.25f,
            true, false, true, true, false, false, false);

    public static final Biome DESERT = new Biome("desert",
            new int[]{0xFFD9A06E, 0xFFC48A5A, 0xFFB27A4C, 0xFF96623C},
            new int[]{0xFF241A26, 0xFF1D1522, 0xFF18111C, 0xFF130D16},
            new float[]{0.56f, 0.62f, 0.70f, 0.82f}, new float[]{0.12f, 0.10f, 0.08f, 0.05f},
            new float[]{0.55f, 0.38f, 0.20f, 0.04f}, new boolean[]{false, false, false, false},
            0.25f, Tree.CACTUS, 0xFF3B5A34, 0xFF0A1008, 0xFFFFB878, 0.22f,
            false, false, false, false, true, false, false);

    public static final Biome LAKE = new Biome("lake",
            new int[]{0xFF6E7FA6, 0xFF566C8E, 0xFF3F6350, 0xFF2A4A38},
            new int[]{0xFF101830, 0xFF0D1426, 0xFF0A121A, 0xFF070D10},
            new float[]{0.42f, 0.50f, 0.58f, 0.88f}, new float[]{0.20f, 0.13f, 0.07f, 0.04f},
            new float[]{0.65f, 0.45f, 0.25f, 0.05f}, new boolean[]{true, true, false, false},
            1f, Tree.PINE, 0xFF1F4030, 0xFF070E0E, 0xFFFFFFFF, 0f,
            false, true, false, false, false, true, true);

    public static final Biome[] ALL = {FOREST, SNOW, DESERT, LAKE};
    private static final Biome AUTO = ALL[new Random().nextInt(ALL.length)];

    public static Biome resolve(String id) {
        if (id != null) {
            for (Biome b : ALL) if (b.id.equals(id)) return b;
        }
        return AUTO; // "auto": случайный на каждый запуск игры
    }
}
