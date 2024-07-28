package net.vi.mobhealthindicators.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.vi.mobhealthindicators.render.Renderer;
import net.vi.mobhealthindicators.render.TextureBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.render.Renderer.entityToOldYaw;
import static net.vi.mobhealthindicators.render.TextureBuilder.textures;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements FeatureRendererContext<T, M> {

    @Shadow protected abstract boolean hasLabel(T livingEntity);

    protected LivingEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    public void renderHealth(T livingEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (!config.shouldRender(livingEntity) || player == null || player.getVehicle() == livingEntity || (livingEntity == player && !Objects.equals(System.getProperty("mobhealthindicators.debug"), "true")) || livingEntity.isInvisibleTo(player)) {
            entityToOldYaw.remove(livingEntity);
            return;
        }

        int redHealth = MathHelper.ceil(livingEntity.getHealth());
        int maxHealth = MathHelper.ceil(livingEntity.getMaxHealth());
        int emptyHealth = maxHealth - redHealth;
        int yellowHealth = MathHelper.ceil(livingEntity.getAbsorptionAmount());

        String healthId = redHealth + " " + emptyHealth + " " + yellowHealth;

        NativeImageBackedTexture texture;

        if (textures.containsKey(healthId)) {
            texture = textures.get(healthId);
        } else {
            texture = TextureBuilder.getTexture(redHealth, maxHealth, yellowHealth);
            textures.put(healthId, texture);
        }

        Renderer.render(client, matrixStack, livingEntity, texture, light, dispatcher.getSquaredDistanceToCamera(livingEntity), this.hasLabel(livingEntity));
    }
}