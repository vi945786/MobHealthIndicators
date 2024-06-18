package net.vi.mobhealthindicator;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.vi.mobhealthindicator.commands.Commands;
import net.vi.mobhealthindicator.config.ModConfig;

public class MobHealthIndicator implements ModInitializer {

    public static final String modId = "mobhealthindicator";
    public static ConfigHolder<ModConfig> configHolder;

    public static final KeyBinding renderKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + modId + ".renderingenabled",
            InputUtil.UNKNOWN_KEY.getCode(),
            "key.categories." + modId
    ));

    @Override
    public void onInitialize() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        configHolder = AutoConfig.getConfigHolder(ModConfig.class);

        Commands.registerCommands();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (renderKey.wasPressed()) {
                boolean showHearts = !configHolder.getConfig().showHearts;
                configHolder.getConfig().showHearts = showHearts;
                sendMessage((showHearts ? "enabled" : "disabled") + "rendering", client);
            }
        });
    }

    public static void sendMessage(String message, MinecraftClient client, Object... args) {
        if(client.player != null) {
            client.player.sendMessage(Text.translatable(modId + "." + message, args), true);
        }
    }

    public static ModConfig getConfig() {
        return configHolder.getConfig();
    }
}