package net.vi.mobhealthindicators;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.vi.mobhealthindicators.commands.Commands;
import net.vi.mobhealthindicators.config.Config;

import static net.vi.mobhealthindicators.config.Config.config;

public class MobHealthIndicators implements ModInitializer {

    public static final String modId = "mobhealthindicators";
    public static final MinecraftClient client = MinecraftClient.getInstance();

    public static final KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.mobhealthindicators.toggle",
            InputUtil.UNKNOWN_KEY.getCode(),
            "mobhealthindicators.name"
    ));
    public static final KeyBinding overrideFiltersKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.mobhealthindicators.overridefilters",
            InputUtil.UNKNOWN_KEY.getCode(),
            "mobhealthindicators.name"
    ));

    @Override
    public void onInitialize() {
        Config.load();
        Commands.registerCommands();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.wasPressed()) {
                config.showHearts = !config.showHearts;
                sendMessage((config.showHearts ? "enabled" : "disabled") + "rendering");
            }
        });
    }

    public static void sendMessage(String message, Object... args) {
        if(client.player != null) {
            client.player.sendMessage(Text.translatable("message.mobhealthindicators." + message, args), true);
        }
    }
}