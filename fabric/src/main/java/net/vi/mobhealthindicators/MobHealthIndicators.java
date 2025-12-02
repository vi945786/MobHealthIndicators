package net.vi.mobhealthindicators;

import net.fabricmc.api.ClientModInitializer;

public class MobHealthIndicators implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Fabric.init();
        ModInit.init();
    }
}