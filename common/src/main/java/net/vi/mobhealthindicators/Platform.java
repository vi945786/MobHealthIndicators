package net.vi.mobhealthindicators;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.function.Function;

public abstract class Platform {
    protected static Platform instance;

    public static Platform getInstance() {
        return instance;
    }

    public abstract Path getConfigDir();
    public abstract KeyMapping registerKeyMapping(KeyMapping keyMapping);
    public abstract LiteralArgumentBuilder<?> getCommandBuilder(String mainCommand);
    public abstract void registerCommand(LiteralArgumentBuilder<?> builder);
    public abstract boolean isModLoaded(String modId);
    public abstract RenderPipeline getFullBrightIndicatorsPipeline();
    public abstract Function<ResourceLocation, RenderType> getFullBrightIndicatorsRenderTypeFunction();
}
