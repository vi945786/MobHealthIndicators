package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.IrisPipelines;
import net.irisshaders.iris.pipeline.programs.ShaderKey;

/** Loaded only when the Iris mod is present. */
final class IrisCompat {
    private static boolean onTopPipelineRegistered;

    private IrisCompat() {
    }

    /**
     * Assign the custom depthless pipeline directly to Iris' world-text shader
     * key. Copying another pipeline's mapping is timing-sensitive during parallel
     * NeoForge client setup; a missed copy leaves the custom pipeline compiled
     * with Mojang's shader and therefore outside the active shader pack.
     *
     * <p>The method is synchronized and idempotent because it is called both from
     * client initialization and immediately before the first on-top draw.</p>
     */
    static synchronized void registerOnTopPipeline(RenderPipeline pipeline) {
        if (onTopPipelineRegistered) return;

        IrisPipelines.assignPipeline(pipeline, ShaderKey.TEXT);
        onTopPipelineRegistered = true;
    }

    static boolean isRenderingShadowPass() {
        return IrisApi.getInstance().isRenderingShadowPass();
    }
}
