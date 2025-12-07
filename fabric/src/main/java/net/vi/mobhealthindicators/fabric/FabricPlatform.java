package net.vi.mobhealthindicators.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.vi.mobhealthindicators.Platform;

import java.nio.file.Path;

public class FabricPlatform extends Platform {

    public static void init() {
        Platform.instance = new FabricPlatform();
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
}
