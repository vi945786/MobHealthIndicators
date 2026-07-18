package net.vi.mobhealthindicators.render;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.vertices.ImmediateState;

/** Loaded only when the Iris mod is present. */
final class IrisCompat {
    private IrisCompat() {
    }

    /**
     * Runs the final always-on-top draw with vanilla pipeline compilation and the
     * vanilla vertex layout. This happens after Iris has completed both level and
     * game finalization, so the draw must not be redirected into an Iris gbuffer
     * or inherit a composite-pass framebuffer suppression flag.
     */
    static void runVanilla(Runnable draw) {
        boolean previousBypass = ImmediateState.bypass;
        boolean previousExtendedFormat = ImmediateState.renderWithExtendedVertexFormat;
        boolean previousSkipExtension = ImmediateState.skipExtension.get();
        boolean previousSafeToMultiply = ImmediateState.safeToMultiply;
        boolean previousTemporarilyIgnorePass = ImmediateState.temporarilyIgnorePass;

        ImmediateState.bypass = true;
        ImmediateState.renderWithExtendedVertexFormat = false;
        ImmediateState.skipExtension.set(true);
        ImmediateState.safeToMultiply = false;
        ImmediateState.temporarilyIgnorePass = false;
        try {
            draw.run();
        } finally {
            ImmediateState.temporarilyIgnorePass = previousTemporarilyIgnorePass;
            ImmediateState.safeToMultiply = previousSafeToMultiply;
            ImmediateState.skipExtension.set(previousSkipExtension);
            ImmediateState.renderWithExtendedVertexFormat = previousExtendedFormat;
            ImmediateState.bypass = previousBypass;
        }
    }

    static boolean isRenderingShadowPass() {
        return IrisApi.getInstance().isRenderingShadowPass();
    }
}