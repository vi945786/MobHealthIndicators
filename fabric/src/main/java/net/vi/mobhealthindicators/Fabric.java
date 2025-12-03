package net.vi.mobhealthindicators;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.function.Function;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.minecraft.client.renderer.RenderPipelines.ENTITY_SNIPPET;
import static net.minecraft.client.renderer.RenderStateShard.LIGHTMAP;
import static net.minecraft.client.renderer.RenderStateShard.OVERLAY;
import static net.vi.mobhealthindicators.ModInit.modId;
import static net.vi.mobhealthindicators.render.Renderer.FULL_BRIGHT_INDICATORS_PIPELINE;

public class Fabric extends Platform {
    public static void init() {
        Platform.instance = new Fabric();
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public KeyMapping registerKeyMapping(KeyMapping keyMapping) {
        return KeyBindingHelper.registerKeyBinding(keyMapping);
    }

    @Override
    public LiteralArgumentBuilder<?> getCommandBuilder(String mainCommand) {
        return literal(mainCommand);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerCommand(LiteralArgumentBuilder<?> builder) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register((LiteralArgumentBuilder<FabricClientCommandSource>) builder));
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public RenderPipeline getFullBrightIndicatorsPipeline() {
        return RenderPipelines.register(RenderPipeline.builder(ENTITY_SNIPPET).withLocation(ResourceLocation.fromNamespaceAndPath(modId, "pipeline/full_bright_indicators")).withShaderDefine("ALPHA_CUTOUT", 0.1F).withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING").withSampler("Sampler1").withCull(false).build());
    }

    @Override
    public Function<ResourceLocation, RenderType> getFullBrightIndicatorsRenderTypeFunction() {
        return Util.memoize(texture -> {
            RenderType.CompositeState state = RenderType.CompositeState.builder().setTextureState(new RenderStateShard.TextureStateShard(texture, false)).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false);
            return RenderType.create("full_bright_indicators", 1536, true, false, FULL_BRIGHT_INDICATORS_PIPELINE, state);
        });
    }
}
