package net.vi.mobhealthindicators.mixin;

import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.vi.mobhealthindicators.render.Renderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Places ordinary health bars between vanilla's opaque and translucent feature
 * phases. This lets glass, water, particles and later weather passes blend over
 * the bar while opaque terrain still occludes it through the depth buffer.
 */
@Mixin(FeatureRenderDispatcher.class)
public abstract class FeatureRenderDispatcherMixin {

    @Inject(method = "renderTranslucentFeatures", at = @At("HEAD"))
    private void mobhealthindicators$renderWorldHealthBars(CallbackInfo ci) {
        Renderer.flushWorld();
    }
}
