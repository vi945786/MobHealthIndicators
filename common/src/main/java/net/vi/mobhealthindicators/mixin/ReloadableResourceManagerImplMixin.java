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

import static net.vi.mobhealthindicators.render.Renderer.defaultPixelSize;
import static net.vi.mobhealthindicators.render.Renderer.pixelSize;
import static net.vi.mobhealthindicators.render.TextureBuilder.*;

@Mixin(ReloadableResourceManager.class)
public class ReloadableResourceManagerImplMixin {

    @Inject(method = "createReload", at = @At("TAIL"))
    public void onCreateReload(Executor backgroundExecutor, Executor gameExecutor, CompletableFuture<Unit> waitingFor, List<PackResources> resourcePacks, CallbackInfoReturnable<ReloadInstance> cir) {
        Minecraft client = Minecraft.getInstance();
        textures.values().forEach(client.getTextureManager()::release);
        textures.clear();

        emptyTexture = new HeartType.HeartColor(HeartType.EMPTY.getTexture(HeartType.Effect.none, client), HeartType.EMPTY.getTexture(HeartType.Effect.none, client));
        normalHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.none, client), HeartType.HALF.getTexture(HeartType.Effect.none, client));
        poisonHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.poison, client), HeartType.HALF.getTexture(HeartType.Effect.poison, client));
        witherHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.wither, client), HeartType.HALF.getTexture(HeartType.Effect.wither, client));
        absorptionHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.absorption, client), HeartType.HALF.getTexture(HeartType.Effect.absorption, client));
        frozenHeart = new HeartType.HeartColor(HeartType.FULL.getTexture(HeartType.Effect.frozen, client), HeartType.HALF.getTexture(HeartType.Effect.frozen, client));

        heartSize = emptyTexture.fullHeartTexture().getWidth();

        pixelSize = defaultPixelSize / ((float) heartSize / defaultHeartSize);
    }
}
