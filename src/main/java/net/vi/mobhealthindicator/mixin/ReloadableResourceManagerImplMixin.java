package net.vi.mobhealthindicator.mixin;

import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Unit;
import net.vi.mobhealthindicator.render.HeartType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static net.vi.mobhealthindicator.render.TextureBuilder.*;

@Mixin(ReloadableResourceManagerImpl.class)
public class ReloadableResourceManagerImplMixin {

    @Inject(method = "reload", at = @At("TAIL"))
    public void reload(Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage, List<ResourcePack> packs, CallbackInfoReturnable<ResourceReload> cir) {
        textures.values().forEach(NativeImageBackedTexture::close);
        textures.clear();

        EmptyTexture = HeartType.EMPTY.getTexture();
        RedFullTexture = HeartType.RED_FULL.getTexture();
        RedHalfTexture = HeartType.RED_HALF.getTexture();
        YellowFullTexture = HeartType.YELLOW_FULL.getTexture();
        YellowHalfTexture = HeartType.YELLOW_HALF.getTexture();
    }
}
