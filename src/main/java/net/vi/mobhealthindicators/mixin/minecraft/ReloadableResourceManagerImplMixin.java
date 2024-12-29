package net.vi.mobhealthindicators.mixin.minecraft;

import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceReload;
import net.minecraft.util.Unit;
import net.vi.mobhealthindicators.render.HeartType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static net.vi.mobhealthindicators.render.Renderer.*;
import static net.vi.mobhealthindicators.render.TextureBuilder.*;

@Mixin(ReloadableResourceManagerImpl.class)
public class ReloadableResourceManagerImplMixin {

    @Inject(method = "reload", at = @At("TAIL"))
    public void reload(Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage, List<ResourcePack> packs, CallbackInfoReturnable<ResourceReload> cir) {
        textures.values().forEach(NativeImageBackedTexture::close);
        textures.clear();

        emptyTexture = HeartType.EMPTY.getTexture(HeartType.Effect.NONE);
        normalFullTexture = HeartType.FULL.getTexture(HeartType.Effect.NONE);
        normalHalfTexture = HeartType.HALF.getTexture(HeartType.Effect.NONE);
        poisonFullTexture = HeartType.FULL.getTexture(HeartType.Effect.POISON);
        poisonHalfTexture = HeartType.HALF.getTexture(HeartType.Effect.POISON);
        witherFullTexture = HeartType.FULL.getTexture(HeartType.Effect.WITHER);
        witherHalfTexture = HeartType.HALF.getTexture(HeartType.Effect.WITHER);
        absorbingFullTexture = HeartType.FULL.getTexture(HeartType.Effect.ABSORPTION);
        absorbingHalfTexture = HeartType.HALF.getTexture(HeartType.Effect.ABSORPTION);
        frozenFullTexture = HeartType.FULL.getTexture(HeartType.Effect.FROZEN);
        frozenHalfTexture = HeartType.HALF.getTexture(HeartType.Effect.FROZEN);

        heartSize = emptyTexture.getWidth();

        pixelSize = defaultPixelSize / (heartSize / 9F);
    }
}
