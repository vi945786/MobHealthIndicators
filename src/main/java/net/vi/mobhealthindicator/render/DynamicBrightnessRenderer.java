package net.vi.mobhealthindicator.render;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.util.math.MathHelper;
import net.vi.mobhealthindicator.HeartType;
import org.joml.Matrix4f;

public class DynamicBrightnessRenderer {

    public static void renderHealth(LivingEntity livingEntity, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, EntityRenderDispatcher dispatcher, boolean hasLabel, EntityModel<?> entityModel) {
        RenderLayer renderLayerEmpty = entityModel.getLayer(HeartType.EMPTY.icon);
        RenderLayer renderLayerRedFull = entityModel.getLayer(HeartType.RED_FULL.icon);
        RenderLayer renderLayerRedHalf = entityModel.getLayer(HeartType.RED_HALF.icon);
        RenderLayer renderLayerYellowFull = entityModel.getLayer(HeartType.YELLOW_FULL.icon);
        RenderLayer renderLayerYellowHalf = entityModel.getLayer(HeartType.YELLOW_HALF.icon);

        RenderLayer renderLayer = renderLayerEmpty;
        BufferBuilder bufferBuilder = (BufferBuilder) vertexConsumerProvider.getBuffer(renderLayer);
        renderLayer.startDrawing();

        double d = dispatcher.getSquaredDistanceToCamera(livingEntity);

        int healthRed = MathHelper.ceil(livingEntity.getHealth());
        int maxHealth = MathHelper.ceil(livingEntity.getMaxHealth());
        int healthYellow = MathHelper.ceil(livingEntity.getAbsorptionAmount());

        int heartsRed = MathHelper.ceil(healthRed / 2.0F);
        boolean lastRedHalf = (healthRed & 1) == 1;
        int heartsNormal = MathHelper.ceil(maxHealth / 2.0F);
        int heartsYellow = MathHelper.ceil(healthYellow / 2.0F);
        boolean lastYellowHalf = (healthYellow & 1) == 1;
        int heartsTotal = heartsNormal + heartsYellow;


        int heartsPerRow = 10;

        int pixelsTotal = Math.min(heartsTotal, heartsPerRow) * 8 + 1;
        float maxX = pixelsTotal / 2.0f;

        double heartDensity = 50F - (Math.max(4F - Math.ceil(heartsTotal / 10F), -3F) * 5F);

        Matrix4f model = null;
        double h = 0;
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

                if (heart % heartsPerRow == 0 || (lastType != type && lastType != null)) {
                    if (heart != 0) {
                        renderLayer.endDrawing();
                        matrixStack.pop();
                    }

                    if (type.equals(HeartType.EMPTY)) {
                        renderLayer = renderLayerEmpty;
                    } else if (type.equals(HeartType.RED_FULL)) {
                        renderLayer = renderLayerRedFull;
                    } else if (type.equals(HeartType.RED_HALF)) {
                        renderLayer = renderLayerRedHalf;
                    } else if (type.equals(HeartType.YELLOW_FULL)) {
                        renderLayer = renderLayerYellowFull;
                    } else if (type.equals(HeartType.YELLOW_HALF)) {
                        renderLayer = renderLayerYellowHalf;
                    }
                    bufferBuilder = (BufferBuilder) vertexConsumerProvider.getBuffer(renderLayer);

                    matrixStack.push();
                    renderLayer.startDrawing();

                    if (heart % heartsPerRow == 0) h = (heart / heartDensity);

                    matrixStack.translate(0, livingEntity.getHeight() + 0.5f + h, 0);
                    if (hasLabel && d <= 4096.0) {
                        matrixStack.translate(0.0D, 9.0F * 1.15F * 0.025F, 0.0D);
                        if (d < 100.0 && livingEntity instanceof PlayerEntity && livingEntity.getEntityWorld().getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null) {
                            matrixStack.translate(0.0D, 9.0F * 1.15F * 0.025F, 0.0D);
                        }
                    }

                    matrixStack.multiply(dispatcher.getRotation());

                    float pixelSize = 0.025F;
                    matrixStack.scale(pixelSize, pixelSize, pixelSize);

                    model = matrixStack.peek().getPositionMatrix();
                }

                float x = maxX - (heart % 10) * 8;
                lastType = type;

                if (isDrawingEmpty == 0) {
                    drawHeart(model, bufferBuilder, x, light);
                } else {
                    if (type != HeartType.EMPTY) {
                        drawHeart(model, bufferBuilder, x, light);
                    }
                }
            }
            renderLayer.endDrawing();
            matrixStack.pop();
        }
    }

    private static void drawHeart(Matrix4f model, VertexConsumer vertexConsumer, float x, int light) {
        float minU = 0F;
        float maxU = 1F;
        float minV = 0F;
        float maxV = 1F;

        float heartSize = 9F;

        drawVertex(model, vertexConsumer, x, 0F - heartSize, minU, maxV, light);
        drawVertex(model, vertexConsumer, x - heartSize, 0F - heartSize, maxU, maxV, light);
        drawVertex(model, vertexConsumer, x - heartSize, 0F, maxU, minV, light);
        drawVertex(model, vertexConsumer, x, 0F, minU, minV, light);
    }

    private static void drawVertex(Matrix4f model, VertexConsumer vertices, float x, float y, float u, float v, int light) {
        vertices.vertex(model, x, y, 0.0F).color(1F, 1F, 1F, 1F).texture(u, v).overlay(0, 10).light(light).normal(x, y, 0.0F).next();
    }
}