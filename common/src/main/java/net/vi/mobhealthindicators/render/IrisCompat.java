package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;

/** Loaded only when the Iris mod is present. */
final class IrisCompat {
    private IrisCompat() {
    }

    static void registerPipelines(RenderPipeline worldPipeline, RenderPipeline onTopPipeline) {
        IrisApi iris = IrisApi.getInstance();
        iris.assignPipeline(worldPipeline, IrisProgram.ENTITIES_TRANSLUCENT);
        iris.assignPipeline(onTopPipeline, IrisProgram.ENTITIES_TRANSLUCENT);
    }

    static boolean isRenderingShadowPass() {
        return IrisApi.getInstance().isRenderingShadowPass();
    }
}
