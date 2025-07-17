package net.vi.mobhealthindicators.mixin.minecraft;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.vi.mobhealthindicators.render.Renderer.render;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "render", at = @At(value = "RETURN", shift = At.Shift.BY, by =-2))
    private void afterRender(CallbackInfo ci) {
        render(true);
    }
}
