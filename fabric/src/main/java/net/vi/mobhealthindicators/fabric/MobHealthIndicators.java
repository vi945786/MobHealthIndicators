package net.vi.mobhealthindicators.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.vi.mobhealthindicators.ModInit;

public class MobHealthIndicators implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricPlatform.init();
        ModInit.init();
    }
}