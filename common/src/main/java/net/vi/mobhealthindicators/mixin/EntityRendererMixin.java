package net.vi.mobhealthindicators.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.vi.mobhealthindicators.render.HeartType;
import net.vi.mobhealthindicators.render.Renderer;
import net.vi.mobhealthindicators.render.TextureBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.vi.mobhealthindicators.ModInit.client;
import static net.vi.mobhealthindicators.ModInit.targetedEntity;
import static net.vi.mobhealthindicators.config.Config.config;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Shadow
    public abstract Vec3 getRenderOffset(EntityRenderState renderState);

    @Inject(
            method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At("RETURN")
    )
    private void mobhealthindicators$captureHealthBar(
            Entity entity,
            float partialTick,
            CallbackInfoReturnable<EntityRenderState> cir
    ) {
        if (!Renderer.isCollecting() || config == null) return;
        if (!(entity instanceof LivingEntity livingEntity)) return;

        LocalPlayer player = client.player;
        if (player == null
                || player.getVehicle() == livingEntity
                || livingEntity.isInvisibleTo(player)
                || !config.shouldRender(livingEntity, targetedEntity)) {
            return;
        }

        int health = Mth.ceil(livingEntity.getHealth());
        int maxHealth = Mth.ceil(livingEntity.getMaxHealth());
        int absorption = Mth.ceil(livingEntity.getAbsorptionAmount());
        HeartType.Effect effect = HeartType.Effect.getEffect(livingEntity);
        ResourceLocation texture = TextureBuilder.getTexture(health, maxHealth, absorption, effect);

        EntityRenderState renderState = cir.getReturnValue();
        Renderer.queue(
                livingEntity,
                renderState,
                this.getRenderOffset(renderState),
                texture,
                targetedEntity == livingEntity
        );
    }
}
