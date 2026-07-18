package net.vi.mobhealthindicators.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.vi.mobhealthindicators.ModInit;
import net.vi.mobhealthindicators.config.screen.ConfigScreenHandler;

@Mod(value = ModInit.modId, dist = Dist.CLIENT)
public class MobHealthIndicators {

    public MobHealthIndicators(IEventBus modBus) {
       ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (_, parent) -> ConfigScreenHandler.getConfigScreen(parent));

       NeoForgePlatform.init();
       modBus.addListener(this::onClientSetup);
    }

    public void onClientSetup(FMLClientSetupEvent event) {
        ModInit.init();
    }
}