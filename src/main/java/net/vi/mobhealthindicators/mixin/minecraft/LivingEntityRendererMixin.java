package net.vi.mobhealthindicators.mixin.minecraft;

import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
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

import static net.vi.mobhealthindicators.MobHealthIndicators.client;
import static net.vi.mobhealthindicators.config.Config.config;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends EntityRenderer<T, S> implements FeatureRendererContext<S, M> {


    @Shadow protected abstract boolean hasLabel(T livingEntity, double d);

    protected LivingEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Unique WeakHashMap<S, T> livingEntitys = new WeakHashMap<>();
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    public void updateRenderState(T livingEntity, S livingEntityRenderState, float f, CallbackInfo ci){
        livingEntitys.put(livingEntityRenderState, livingEntity);
    }

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    public void renderHealth(S livingEntityRenderState, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {

        ClientPlayerEntity player = client.player;

        T livingEntity = livingEntitys.get(livingEntityRenderState);

        if (livingEntity != null && !config.shouldRender(livingEntity) || player == null || player.getVehicle() == livingEntity || livingEntity.isInvisibleTo(player) || client.currentScreen instanceof InventoryScreen || client.currentScreen instanceof CreativeInventoryScreen) {
            return;
        }

        int normalHealth = MathHelper.ceil(livingEntity.getHealth());
        int maxHealth = MathHelper.ceil(livingEntity.getMaxHealth());
        int absorptionHealth = MathHelper.ceil(livingEntity.getAbsorptionAmount());
        HeartType.Effect effect = HeartType.Effect.getEffect(livingEntity);

        double d = dispatcher.getSquaredDistanceToCamera(livingEntity);
        Renderer.render(matrixStack, livingEntity, TextureBuilder.getTexture(normalHealth, maxHealth, absorptionHealth, effect), light, d, this.hasLabel(livingEntity, d), dispatcher);
    }
}