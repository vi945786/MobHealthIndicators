package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.IrisPipelines;
import net.minecraft.client.renderer.RenderPipelines;

/** Loaded only when the Iris mod is present. */
final class IrisCompat {
    private IrisCompat() {
    }

    /**
     * Copy the exact mapping used by vanilla world text instead of asking Iris to
     * guess an entity program from the custom pipeline. The guessed translucent
     * entity program applies entity diffuse lighting and motion-vector semantics,
     * which causes brightness flicker and temporal ghosting on several packs.
     */
    static void registerPipelines(RenderPipeline worldPipeline, RenderPipeline onTopPipeline) {
        IrisPipelines.copyPipeline(RenderPipelines.TEXT, worldPipeline);
        IrisPipelines.copyPipeline(RenderPipelines.TEXT, onTopPipeline);
    }

    static boolean isRenderingShadowPass() {
        return IrisApi.getInstance().isRenderingShadowPass();
    }
}
