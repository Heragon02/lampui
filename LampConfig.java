package com.lampui.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Настройки хранятся в config/lampui.json */
public final class LampConfig {
    public static final String[] BIOMES = {"auto", "forest", "snow", "desert", "lake"};

    public String biome = "auto";
    public float rotationSpeed = 1f;
    public boolean realTime = true;
    public float manualHour = 20f;
    public boolean musicEnabled = true;
    public boolean hoverSound = false;   // по умолчанию звуков поменьше
    public boolean clickSound = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static LampConfig instance;

    public static LampConfig get() {
        if (instance == null) load();
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("lampui.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                instance = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), LampConfig.class);
            }
        } catch (Exception e) {
            LampUiClient.LOGGER.warn("Не удалось прочитать конфиг, используем стандартный", e);
        }
        if (instance == null) instance = new LampConfig();
    }

    public void save() {
        try {
            Files.writeString(path(), GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LampUiClient.LOGGER.warn("Не удалось сохранить конфиг", e);
        }
    }

    public void nextBiome() {
        int i = 0;
        for (int k = 0; k < BIOMES.length; k++) if (BIOMES[k].equals(biome)) i = k;
        biome = BIOMES[(i + 1) % BIOMES.length];
    }
}
