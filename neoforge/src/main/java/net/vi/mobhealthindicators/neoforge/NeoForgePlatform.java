package net.vi.mobhealthindicators.neoforge;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.vi.mobhealthindicators.Platform;

import java.nio.file.Path;

public class NeoForgePlatform extends Platform {

    public static void init() {
        Platform.instance = new NeoForgePlatform();
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
