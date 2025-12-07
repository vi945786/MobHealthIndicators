package net.vi.mobhealthindicators.neoforge;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.vi.mobhealthindicators.ModInit;
import net.vi.mobhealthindicators.commands.RegisterCommands;
import net.vi.mobhealthindicators.config.screen.ConfigScreenHandler;

@Mod(value = ModInit.modId, dist = Dist.CLIENT)
public class MobHealthIndicators {

    public MobHealthIndicators(IEventBus modBus) {
       ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (container, parent) -> ConfigScreenHandler.getConfigScreen(parent));

       DummyWorldNeoForge.init();
       NeoForgePlatform.init();
       NeoForge.EVENT_BUS.addListener(this::onRegisterClientCommands);
       modBus.addListener(this::onRegisterKeys);
       modBus.addListener(this::onClientSetup);
    }

    public void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(ModInit.toggleKey);
        event.register(ModInit.overrideFiltersKey);
    }

    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        for(LiteralArgumentBuilder<CommandSourceStack> builder : RegisterCommands.registerCommands(Commands::literal)) {
            event.getDispatcher().register(builder);
        }
    }

    public void onClientSetup(FMLClientSetupEvent event) {
        ModInit.init();
    }
}