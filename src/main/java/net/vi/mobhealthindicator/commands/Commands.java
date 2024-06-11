package net.vi.mobhealthindicator.commands;

import com.terraformersmc.modmenu.ModMenu;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.vi.mobhealthindicator.MobHealthIndicator;
import net.vi.mobhealthindicator.config.ModConfig;

public class Commands {

    @Environment(EnvType.CLIENT)
    public static void registerCommands(){
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandRegistryAccess) ->
                dispatcher.register(ClientCommandManager.literal(MobHealthIndicator.modId).executes(context -> {
                MinecraftClient client = context.getSource().getClient();
                Screen configScreen = AutoConfig.getConfigScreen(ModConfig.class, client.currentScreen).get();
                client.setScreen(configScreen);
                return 1;
        })));
    }
}
