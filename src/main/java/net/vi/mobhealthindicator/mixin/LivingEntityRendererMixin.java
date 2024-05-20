package net.vi.mobhealthindicator.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.vi.mobhealthindicator.config.ModConfig;
import net.vi.mobhealthindicator.render.DefaultRenderer;
import net.vi.mobhealthindicator.render.DynamicBrightnessRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.vi.mobhealthindicator.MobHealthIndicator.configHolder;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements FeatureRendererContext<T, M> {

    @Shadow protected M model;

    @Shadow protected abstract boolean hasLabel(T livingEntity);

    protected LivingEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    public void renderHealth(T livingEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ModConfig config = configHolder.getConfig();

        if (config.shouldRender(livingEntity) && player != null && player.getVehicle() != livingEntity && livingEntity != player && !livingEntity.isInvisibleTo(player)) {
            if(config.dynamicBrightness) {
                DynamicBrightnessRenderer.renderHealth(livingEntity, matrixStack, vertexConsumerProvider, light, dispatcher, this.hasLabel(livingEntity), model);
            } else {
                DefaultRenderer.renderHealth(livingEntity, matrixStack, dispatcher, this.hasLabel(livingEntity));
            }
        }
    }
}