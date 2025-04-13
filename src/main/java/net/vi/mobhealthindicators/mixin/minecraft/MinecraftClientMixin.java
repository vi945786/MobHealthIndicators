package net.vi.mobhealthindicators.mixin.minecraft;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.vi.mobhealthindicators.render.Renderer.entityToOldYaw;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "handleInputEvents()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getPerspective()Lnet/minecraft/client/option/Perspective;"))
    private void handleInputEvents(CallbackInfo ci) {
        entityToOldYaw.clear();
    }
}
