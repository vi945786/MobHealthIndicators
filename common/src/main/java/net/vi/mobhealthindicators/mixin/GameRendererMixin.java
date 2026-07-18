package net.vi.mobhealthindicators.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.vi.mobhealthindicators.render.Renderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Finishes health-bar rendering only after GameRenderer.renderLevel has returned.
 * Iris performs its game-level color-space conversion at that method's tail, so
 * this boundary is later and more reliable than LevelRenderer.renderLevel tail.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void mobhealthindicators$renderOnTopAfterLevel(
            DeltaTracker deltaTracker,
            boolean advanceGameTime,
            CallbackInfo ci
    ) {
        try {
            Renderer.flushOnTop();
        } finally {
            Renderer.endFrame();
        }
    }
}