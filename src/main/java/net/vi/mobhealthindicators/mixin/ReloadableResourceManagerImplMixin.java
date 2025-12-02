package net.vi.mobhealthindicators.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
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

@Mixin(ReloadableResourceManager.class)
public class ReloadableResourceManagerImplMixin {

    @Inject(method = "createReload", at = @At("TAIL"))
    public void onCreateReload(Executor backgroundExecutor, Executor gameExecutor, CompletableFuture<Unit> waitingFor, List<PackResources> resourcePacks, CallbackInfoReturnable<ReloadInstance> cir) {
        textures.values().forEach(Minecraft.getInstance().getTextureManager()::release);
        textures.clear();

        emptyTexture = new HeartType.HeartColor(HeartType.EMPTY.getTexture(HeartType.Effect.none), HeartType.EMPTY.getTexture(HeartType.Effect.none));
        normalHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.none), HeartType.HALF.getTexture(HeartType.Effect.none));
        poisonHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.poison), HeartType.HALF.getTexture(HeartType.Effect.poison));
        witherHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.wither), HeartType.HALF.getTexture(HeartType.Effect.wither));
        absorptionHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.absorption), HeartType.HALF.getTexture(HeartType.Effect.absorption));
        frozenHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.frozen), HeartType.HALF.getTexture(HeartType.Effect.frozen));

        heartSize = emptyTexture.fullHeartTexture().getWidth();

        pixelSize = defaultPixelSize / ((float) heartSize / defaultHeartSize);
    }
}
