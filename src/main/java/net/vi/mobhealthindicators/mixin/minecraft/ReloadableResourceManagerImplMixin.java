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

        emptyTexture = new HeartType.HeartColor(HeartType.EMPTY.getTexture(HeartType.Effect.NONE), HeartType.EMPTY.getTexture(HeartType.Effect.NONE));
        normalHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.NONE), HeartType.HALF.getTexture(HeartType.Effect.NONE));
        poisonHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.POISON), HeartType.HALF.getTexture(HeartType.Effect.POISON));
        witherHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.WITHER), HeartType.HALF.getTexture(HeartType.Effect.WITHER));
        absorptionHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.ABSORPTION), HeartType.HALF.getTexture(HeartType.Effect.ABSORPTION));
        frozenHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.FROZEN), HeartType.HALF.getTexture(HeartType.Effect.FROZEN));

        heartSize = emptyTexture.fullHeartTexture().getWidth();

        pixelSize = defaultPixelSize / (heartSize / 9F);
    }
}
