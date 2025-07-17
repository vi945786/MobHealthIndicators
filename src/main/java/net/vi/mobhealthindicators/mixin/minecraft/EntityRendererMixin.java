package net.vi.mobhealthindicators.mixin.minecraft;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.vi.mobhealthindicators.render.HeartType;
import net.vi.mobhealthindicators.render.Renderer;
import net.vi.mobhealthindicators.render.TextureBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.WeakHashMap;

import static net.vi.mobhealthindicators.MobHealthIndicators.areShadersEnabled;
import static net.vi.mobhealthindicators.MobHealthIndicators.client;
import static net.vi.mobhealthindicators.config.Config.config;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Shadow protected abstract boolean hasLabel(Entity livingEntity, double d);

    @Unique WeakHashMap<EntityRenderState, Entity> entities = new WeakHashMap<>();
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    public void updateRenderState(Entity entity, EntityRenderState entityRenderState, float f, CallbackInfo ci){
        entities.put(entityRenderState, entity);
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void renderHealth(EntityRenderState entityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {

        ClientPlayerEntity player = client.player;

        Entity entity = entities.get(entityRenderState);

        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
        if (!(entity instanceof LivingEntity livingEntity) || !config.shouldRender(livingEntity, dispatcher.targetedEntity) || player == null || player.getVehicle() == livingEntity || livingEntity.isInvisibleTo(player) || ((client.currentScreen instanceof InventoryScreen || client.currentScreen instanceof CreativeInventoryScreen) && livingEntity == player)) {
            return;
        }

        int normalHealth = MathHelper.ceil(livingEntity.getHealth());
        int maxHealth = MathHelper.ceil(livingEntity.getMaxHealth());
        int absorptionHealth = MathHelper.ceil(livingEntity.getAbsorptionAmount());
        HeartType.Effect effect = HeartType.Effect.getEffect(livingEntity);

        double d = dispatcher.getSquaredDistanceToCamera(livingEntity);
        Renderer.HealthBarRenderState state = Renderer.getRenderState(matrixStack, livingEntity, TextureBuilder.getTexture(normalHealth, maxHealth, absorptionHealth, effect), light, d, this.hasLabel(livingEntity, d), dispatcher);
        if (state.isTargeted() && config.renderOnTopOnHover && !areShadersEnabled) {
            Renderer.healthBarRenderStates.add(state);
        } else {
            Renderer.draw(state);
        }
    }
}