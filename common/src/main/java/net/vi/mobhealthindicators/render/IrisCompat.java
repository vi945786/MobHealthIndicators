package net.vi.mobhealthindicators.render;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.vertices.ImmediateState;

/** Loaded only when the Iris mod is present. */
final class IrisCompat {
    private IrisCompat() {
    }

    /**
     * Runs the final always-on-top draw with vanilla pipeline compilation and the
     * vanilla vertex layout. At this point Iris has already finalized the level,
     * so routing the draw through a gbuffer program would feed it into temporal
     * history or an already-composited shader target.
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
