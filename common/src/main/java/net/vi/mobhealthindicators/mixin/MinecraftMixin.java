package net.vi.mobhealthindicators.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.vi.mobhealthindicators.config.Config;
import org.jetbrains.annotations.Nullable;
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
    @Nullable
    public Entity crosshairPickEntity;

    @Shadow
    public abstract DeltaTracker getDeltaTracker();

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo ci) {
        if(config == null) return;

        while (toggleKey.consumeClick()) {
            config.showHearts = !config.showHearts;
            sendMessage("rendering." + (config.showHearts ? "enabled" : "disabled"), (config.showHearts ? ChatFormatting.GREEN : ChatFormatting.RED));
            Config.save();
        }

        if(config.infiniteHoverRange) {
            if (this.getCameraEntity() != null && this.player != null) {
                HitResult hitResult = ((LocalPlayerAccessor) this.player).invokePick(this.getCameraEntity(), 10000, 10000, this.getDeltaTracker().getGameTimeDeltaPartialTick(true));

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
