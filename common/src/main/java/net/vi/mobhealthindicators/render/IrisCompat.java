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
     * Copy Iris' exact world-text routing onto the depthless health-bar variant.
     * This keeps the custom depth state while using the shader pack's text/glyph
     * program, vertex extension, lightmap handling and render targets.
     */
    static void registerOnTopPipeline(RenderPipeline pipeline) {
        IrisPipelines.copyPipeline(RenderPipelines.TEXT, pipeline);
    }

    static boolean isRenderingShadowPass() {
        return IrisApi.getInstance().isRenderingShadowPass();
    }
}
