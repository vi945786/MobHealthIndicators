package net.vi.mobhealthindicator;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.vi.mobhealthindicator.commands.Commands;

import static net.vi.mobhealthindicator.config.Config.config;

public class MobHealthIndicator implements ModInitializer {

    public static final String modId = "mobhealthindicator";

    public static final KeyBinding renderKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + modId + ".renderingenabled",
            InputUtil.UNKNOWN_KEY.getCode(),
            "key.categories." + modId
    ));

    @Override
    public void onInitialize() {
        Commands.registerCommands();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (renderKey.wasPressed()) {
                boolean showHearts = !config.showHearts;
                config.showHearts = showHearts;
                sendMessage((showHearts ? "enabled" : "disabled") + "rendering", client);
            }
        });
    }

    public static void sendMessage(String message, MinecraftClient client, Object... args) {
        if(client.player != null) {
            client.player.sendMessage(Text.translatable(modId + "." + message, args), true);
        }
    }
}