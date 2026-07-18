package net.vi.mobhealthindicators.mixin;

import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.vi.mobhealthindicators.render.Renderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws ordinary health bars after opaque terrain has populated the main depth
 * buffer, but before vanilla flushes solid entity features and copies depth to
 * its translucent targets. This gives glass, water, particles and weather the
 * correct front/behind relationship with the bar.
 */
@Mixin(FeatureRenderDispatcher.class)
public abstract class FeatureRenderDispatcherMixin {

    @Inject(method = "renderSolidFeatures", at = @At("HEAD"))
    private void mobhealthindicators$renderWorldHealthBars(CallbackInfo ci) {
        Renderer.flushWorld();
    }
}
