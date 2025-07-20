package net.vi.mobhealthindicators.mixin;

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

import static net.vi.mobhealthindicators.MobHealthIndicators.client;
import static net.vi.mobhealthindicators.render.Renderer.*;
import static net.vi.mobhealthindicators.render.TextureBuilder.*;

@Mixin(ReloadableResourceManagerImpl.class)
public class ReloadableResourceManagerImplMixin {

    @Inject(method = "reload", at = @At("TAIL"))
    public void reload(Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage, List<ResourcePack> packs, CallbackInfoReturnable<ResourceReload> cir) {
        textures.values().forEach(client.getTextureManager()::destroyTexture);
        textures.clear();

        emptyTexture = new HeartType.HeartColor(HeartType.EMPTY.getTexture(HeartType.Effect.none), HeartType.EMPTY.getTexture(HeartType.Effect.none));
        normalHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.none), HeartType.HALF.getTexture(HeartType.Effect.none));
        poisonHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.poison), HeartType.HALF.getTexture(HeartType.Effect.poison));
        witherHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.wither), HeartType.HALF.getTexture(HeartType.Effect.wither));
        absorptionHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.absorption), HeartType.HALF.getTexture(HeartType.Effect.absorption));
        frozenHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.frozen), HeartType.HALF.getTexture(HeartType.Effect.frozen));

        heartSize = emptyTexture.fullHeartTexture().getWidth();

        pixelSize = defaultPixelSize / (heartSize / defaultHeartSize);
    }
}
