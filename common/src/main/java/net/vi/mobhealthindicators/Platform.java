package net.vi.mobhealthindicators;

import java.nio.file.Path;

public abstract class Platform {
    protected static Platform instance;

    public static Platform getInstance() {
        return instance;
    }

    public abstract Path getConfigDir();
    public abstract boolean isModLoaded(String modId);
}
