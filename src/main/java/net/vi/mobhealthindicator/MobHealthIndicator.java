package net.vi.mobhealthindicator;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class MobHealthIndicator implements ModInitializer {

    public static final String modId = "mobhealthindicator";
    public static final String configFile = "mobhealthindicator.json";

    public static final KeyBinding renderKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + modId + ".renderingenabled",
            InputUtil.UNKNOWN_KEY.getCode(),
            "key.categories." + modId
    ));;

    public static final KeyBinding heartStackKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + modId + ".heartstackingenabled",
            InputUtil.UNKNOWN_KEY.getCode(),
            "key.categories." + modId
    ));
    public static final KeyBinding increaseHeartHeightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + modId + ".increaseheartoffset",
            InputUtil.UNKNOWN_KEY.getCode(),
            "key.categories." + modId
    ));
    public static final KeyBinding decreaseHeartHeightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + modId + ".decreaseheartoffset",
            InputUtil.UNKNOWN_KEY.getCode(),
            "key.categories." + modId
    ));

    @Override
    public void onInitialize() {
        Config.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (renderKey.wasPressed()) {
                Config.setRendering(!Config.getRendering());
                sendMessage((Config.getRendering() ? "enabled" : "disabled") + "rendering", client);
            }

            while (increaseHeartHeightKey.wasPressed()) {
                Config.setHeartOffset(Config.getHeartOffset() + 1);
                sendMessage("setheartheight", client, Config.getHeartOffset());
            }

            while (decreaseHeartHeightKey.wasPressed()) {
                Config.setHeartOffset(Config.getHeartOffset() - 1);
                sendMessage("setheartheight", client, Config.getHeartOffset());
            }
        });
    }

    private void sendMessage(String message, MinecraftClient client, Object... args) {
        if(client.player != null) {
            client.player.sendMessage(Text.translatable(modId + "." + message, args), true);
        }
    }
}