package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.pipeline.IrisPipelines;
import net.irisshaders.iris.vertices.ImmediateState;
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
    static void registerWorldPipeline(RenderPipeline worldPipeline) {
        IrisPipelines.copyPipeline(RenderPipelines.TEXT, worldPipeline);
    }

    /**
     * Runs a draw with vanilla pipeline compilation and the vanilla vertex
     * layout. This is used only after Iris has finalized the level, where drawing
     * through an Iris gbuffer program would target an already-composited buffer.
     */
    static void runVanilla(Runnable draw) {
        boolean previousBypass = ImmediateState.bypass;
        boolean previousExtendedFormat = ImmediateState.renderWithExtendedVertexFormat;
        boolean previousSkipExtension = ImmediateState.skipExtension.get();

        ImmediateState.bypass = true;
        ImmediateState.renderWithExtendedVertexFormat = false;
        ImmediateState.skipExtension.set(true);
        try {
            draw.run();
        } finally {
            ImmediateState.skipExtension.set(previousSkipExtension);
            ImmediateState.renderWithExtendedVertexFormat = previousExtendedFormat;
            ImmediateState.bypass = previousBypass;
        }
    }

    static boolean isRenderingShadowPass() {
        return IrisApi.getInstance().isRenderingShadowPass();
    }
}
