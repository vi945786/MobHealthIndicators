package net.vi.mobhealthindicator.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.vi.mobhealthindicator.HeartType;
import net.vi.mobhealthindicator.config.ModConfig;
import net.vi.mobhealthindicator.render.DefaultRenderer;
import net.vi.mobhealthindicator.render.DynamicBrightnessRenderer;
import net.vi.mobhealthindicator.render.Renderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.vi.mobhealthindicator.MobHealthIndicator.configHolder;
import static net.vi.mobhealthindicator.MobHealthIndicator.divideBy;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements FeatureRendererContext<T, M> {

    @Shadow protected M model;

    @Shadow protected abstract boolean hasLabel(T livingEntity);

    protected LivingEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Unique private static final int heartsPerRow = 10;
    @Unique private static final float pixelSize = 0.025F;

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    public void renderHealth(T livingEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ModConfig config = configHolder.getConfig();

        if (!(config.shouldRender(livingEntity) && player != null && player.getVehicle() != livingEntity && livingEntity != player && !livingEntity.isInvisibleTo(player))) return;

        Renderer renderer;

        if(config.dynamicBrightness) {
            renderer = new DynamicBrightnessRenderer(matrixStack, vertexConsumerProvider, model, light);
        } else {
            renderer = new DefaultRenderer(matrixStack);
        }

        renderer.init();

        double distance = dispatcher.getSquaredDistanceToCamera(livingEntity);

        int healthRed = MathHelper.ceil(livingEntity.getHealth());
        int maxHealth = MathHelper.ceil(livingEntity.getMaxHealth());
        int healthYellow = MathHelper.ceil(livingEntity.getAbsorptionAmount());

        int heartsRed = MathHelper.ceil(healthRed / 2.0F);
        boolean lastRedHalf = (healthRed & 1) == 1;
        int heartsNormal = MathHelper.ceil(maxHealth / 2.0F);
        int heartsYellow = MathHelper.ceil(healthYellow / 2.0F);
        boolean lastYellowHalf = (healthYellow & 1) == 1;
        int heartsTotal = heartsNormal + heartsYellow;
        int heartRows = (int) Math.ceil(heartsTotal / 10F);

        int pixelsTotal = Math.min(heartsTotal, heartsPerRow) * 8 + 1;
        float maxX = pixelsTotal / 2.0f;

        double heartDensity = Math.max(10 - (heartRows - 2), 3) * 0.0024F;
        double heartHeight = 0;

        HeartType lastType = null;
        for (int isDrawingEmpty = 0; isDrawingEmpty < 2; isDrawingEmpty++) {
            for (int heart = 0; heart < heartsTotal; heart++) {

                HeartType type = HeartType.EMPTY;
                if (isDrawingEmpty != 0) {
                    if (heart < heartsRed) {
                        type = HeartType.RED_FULL;
                        if (heart == heartsRed - 1 && lastRedHalf) {
                            type = HeartType.RED_HALF;
                        }
                    } else if (heart < heartsNormal) {
                        type = HeartType.EMPTY;
                    } else {
                        type = HeartType.YELLOW_FULL;
                        if (heart == heartsTotal - 1 && lastYellowHalf) {
                            type = HeartType.YELLOW_HALF;
                        }
                    }
                }

                if (heart % heartsPerRow == 0 || lastType != type) {
                    if (heart != 0) {
                        renderer.endRendering();
                    }

                    renderer.startRendering(type);

                    if (heart % heartsPerRow == 0) heartHeight = (heart * heartDensity);

                    matrixStack.translate(0, livingEntity.getHeight() + 0.5f + heartHeight, 0);
                    if (this.hasLabel(livingEntity) && distance <= 4096.0) {
                        matrixStack.translate(0.0D, 9.0F * 1.15F * 0.025F, 0.0D);
                        if (distance < 100.0 && livingEntity instanceof PlayerEntity && livingEntity.getEntityWorld().getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null) {
                            matrixStack.translate(0.0D, 9.0F * 1.15F * 0.025F, 0.0D);
                        }
                    }

                    matrixStack.multiply(dispatcher.getRotation());

                    matrixStack.scale(pixelSize, pixelSize, pixelSize);
                }

                float x = maxX - (heart % heartsPerRow) * 8;
                lastType = type;

                if (isDrawingEmpty == 0) {
                    renderer.render(x);
                } else {
                    if (type != HeartType.EMPTY) {
                        renderer.render(x);
                    }
                }
            }
            renderer.endRendering();
        }
    }
}