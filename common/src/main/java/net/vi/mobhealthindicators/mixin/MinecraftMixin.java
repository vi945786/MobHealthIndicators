package net.vi.mobhealthindicators.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.vi.mobhealthindicators.EntityTypeToEntity;
import net.vi.mobhealthindicators.ModInit;
import net.vi.mobhealthindicators.config.Config;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.vi.mobhealthindicators.ModInit.*;
import static net.vi.mobhealthindicators.config.Config.config;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    @Nullable
    public abstract Entity getCameraEntity();

    @Shadow
    @Final
    public GameRenderer gameRenderer;

    @Shadow
    @Nullable
    public Entity crosshairPickEntity;

    @Shadow
    public abstract DeltaTracker getDeltaTracker();

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        if(config == null) return;

        EntityTypeToEntity.update();
        ModInit.areShadersEnabled = isIrisLoaded && net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse();
        while (toggleKey.consumeClick()) {
            config.showHearts = !config.showHearts;
            sendMessage((config.showHearts ? "enabled" : "disabled") + "rendering");
            Config.save();
        }

        if(config.infiniteHoverRange) {
            if (this.getCameraEntity() != null) {
                HitResult hitResult = ((GameRendererAccessor) this.gameRenderer).invokePick(this.getCameraEntity(), 10000, 10000, this.getDeltaTracker().getGameTimeDeltaPartialTick(true));

                if (hitResult instanceof EntityHitResult entityHitResult) {
                    targetedEntity = entityHitResult.getEntity();
                } else {
                    targetedEntity = null;
                }
            }
        } else {
            targetedEntity = this.crosshairPickEntity;
        }
    }
}
