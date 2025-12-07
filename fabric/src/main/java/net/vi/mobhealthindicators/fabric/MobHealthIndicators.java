package net.vi.mobhealthindicators.fabric;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.vi.mobhealthindicators.ModInit;
import net.vi.mobhealthindicators.commands.RegisterCommands;

public class MobHealthIndicators implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricPlatform.init();
        DummyWorldFabric.init();

        KeyBindingHelper.registerKeyBinding(ModInit.toggleKey);
        KeyBindingHelper.registerKeyBinding(ModInit.overrideFiltersKey);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for(LiteralArgumentBuilder<FabricClientCommandSource> builder : RegisterCommands.registerCommands(ClientCommandManager::literal)) {
                dispatcher.register(builder);
            }
        });

        ModInit.init();
    }
}