package net.vi.mobhealthindicators.mixin;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.util.Unit;
import net.vi.mobhealthindicators.render.TextureBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ReloadableResourceManager.class)
public class ReloadableResourceManagerImplMixin {

    @Inject(method = "createReload", at = @At("TAIL"))
    private void mobhealthindicators$reloadHeartTexturesAfterResources(
            Executor backgroundExecutor,
            Executor gameExecutor,
            CompletableFuture<Unit> waitingFor,
            List<PackResources> resourcePacks,
            CallbackInfoReturnable<ReloadInstance> cir
    ) {
        cir.getReturnValue().done().thenRunAsync(TextureBuilder::reload, gameExecutor);
    }
}
