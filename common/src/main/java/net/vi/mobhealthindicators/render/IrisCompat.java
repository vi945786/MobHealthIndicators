package net.vi.mobhealthindicators.render;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional Iris integration.
 *
 * <p>This class deliberately contains no symbolic references to Iris classes.
 * That keeps the base mod loadable and buildable without Iris while still using
 * Iris' public shadow-pass API and its immediate-render state when Iris is
 * installed.</p>
 */
final class IrisCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Bindings BINDINGS = Bindings.load();

    private IrisCompat() {
    }

    /**
     * Runs the final always-on-top draw with vanilla pipeline compilation and the
     * vanilla vertex layout. Iris has already finalized the level at this point,
     * so the draw must not be redirected into an Iris gbuffer or inherit a stale
     * composite-pass framebuffer suppression flag.
     */
    static void runVanilla(Runnable draw) {
        if (!BINDINGS.hasImmediateState()) {
            draw.run();
            return;
        }

        final boolean previousBypass;
        final boolean previousExtendedFormat;
        final boolean previousSafeToMultiply;
        final boolean previousTemporarilyIgnorePass;
        final ThreadLocal<Boolean> skipExtension;
        final Boolean previousSkipExtension;

        try {
            previousBypass = BINDINGS.bypass.getBoolean(null);
            previousExtendedFormat = BINDINGS.renderWithExtendedVertexFormat.getBoolean(null);
            previousSafeToMultiply = BINDINGS.safeToMultiply.getBoolean(null);
            previousTemporarilyIgnorePass = BINDINGS.temporarilyIgnorePass.getBoolean(null);

            @SuppressWarnings("unchecked")
            ThreadLocal<Boolean> threadLocal = (ThreadLocal<Boolean>) BINDINGS.skipExtension.get(null);
            skipExtension = threadLocal;
            previousSkipExtension = skipExtension.get();

            BINDINGS.bypass.setBoolean(null, true);
            BINDINGS.renderWithExtendedVertexFormat.setBoolean(null, false);
            skipExtension.set(true);
            BINDINGS.safeToMultiply.setBoolean(null, false);
            BINDINGS.temporarilyIgnorePass.setBoolean(null, false);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("Could not prepare Iris state for the health-bar overlay; using the current render state", exception);
            draw.run();
            return;
        }

        try {
            draw.run();
        } finally {
            try {
                BINDINGS.temporarilyIgnorePass.setBoolean(null, previousTemporarilyIgnorePass);
                BINDINGS.safeToMultiply.setBoolean(null, previousSafeToMultiply);
                skipExtension.set(previousSkipExtension);
                BINDINGS.renderWithExtendedVertexFormat.setBoolean(null, previousExtendedFormat);
                BINDINGS.bypass.setBoolean(null, previousBypass);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                LOGGER.error("Could not restore Iris state after rendering the health-bar overlay", exception);
            }
        }
    }

    static boolean isRenderingShadowPass() {
        if (!BINDINGS.hasShadowApi()) return false;

        try {
            return (boolean) BINDINGS.isRenderingShadowPass.invoke(BINDINGS.apiInstance);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOGGER.warn("Could not query the Iris shadow pass; health bars will use the main-pass fallback", exception);
            return false;
        }
    }

    private record Bindings(
            Object apiInstance,
            Method isRenderingShadowPass,
            Field bypass,
            Field renderWithExtendedVertexFormat,
            Field skipExtension,
            Field safeToMultiply,
            Field temporarilyIgnorePass
    ) {
        private static Bindings load() {
            Object apiInstance = null;
            Method shadowPass = null;
            Field bypass = null;
            Field extendedFormat = null;
            Field skipExtension = null;
            Field safeToMultiply = null;
            Field temporarilyIgnorePass = null;

            try {
                Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                apiInstance = apiClass.getMethod("getInstance").invoke(null);
                shadowPass = apiClass.getMethod("isRenderingShadowPass");
            } catch (ReflectiveOperationException | LinkageError exception) {
                LOGGER.debug("Iris shadow-pass API is unavailable", exception);
            }

            try {
                Class<?> immediateState = Class.forName("net.irisshaders.iris.vertices.ImmediateState");
                bypass = immediateState.getField("bypass");
                extendedFormat = immediateState.getField("renderWithExtendedVertexFormat");
                skipExtension = immediateState.getField("skipExtension");
                safeToMultiply = immediateState.getField("safeToMultiply");
                temporarilyIgnorePass = immediateState.getField("temporarilyIgnorePass");
            } catch (ReflectiveOperationException | LinkageError exception) {
                LOGGER.debug("Iris immediate-render state is unavailable", exception);
            }

            return new Bindings(
                    apiInstance,
                    shadowPass,
                    bypass,
                    extendedFormat,
                    skipExtension,
                    safeToMultiply,
                    temporarilyIgnorePass
            );
        }

        private boolean hasShadowApi() {
            return apiInstance != null && isRenderingShadowPass != null;
        }

        private boolean hasImmediateState() {
            return bypass != null
                    && renderWithExtendedVertexFormat != null
                    && skipExtension != null
                    && safeToMultiply != null
                    && temporarilyIgnorePass != null;
        }
    }
}
