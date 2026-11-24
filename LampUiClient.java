package com.lampui.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LampUiClient implements ClientModInitializer {
    public static final String MOD_ID = "lampui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LampConfig.load();
        ClientTickEvents.END_CLIENT_TICK.register(LampMusic::tick);

        // маленькая кнопка настроек в углу главного меню
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof TitleScreen) {
                Screens.getButtons(screen).add(
                        ButtonWidget.builder(Text.literal("Lamp UI"), b -> client.setScreen(new LampConfigScreen(screen)))
                                .dimensions(6, 6, 64, 20).build());
            }
        });
        LOGGER.info("Lamp UI загружен — уютного вечера!");
    }
}
