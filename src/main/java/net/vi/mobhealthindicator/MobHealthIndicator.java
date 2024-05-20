package net.vi.mobhealthindicator;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.vi.mobhealthindicator.config.ModConfig;

public class MobHealthIndicator implements ModInitializer {

    public static final String modId = "mobhealthindicator";
    public static ConfigHolder<ModConfig> configHolder;

    @Override
    public void onInitialize() {
        AutoConfig.register(ModConfig.class, GsonConfigSerializer::new);
        configHolder = AutoConfig.getConfigHolder(ModConfig.class);
    }
}