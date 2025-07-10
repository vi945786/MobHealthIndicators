package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.util.Identifier;
import net.vi.mobhealthindicators.config.Config;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.client.gl.RenderPipelines.*;
import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.config.Config.heightDivisor;
import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;

public abstract class Renderer {

     private static final RenderPipeline FULL_BRIGHT_INDICATORS = register(RenderPipeline.builder(MATRICES_SNIPPET)
            .withLocation(Identifier.of("mobhealthindicators", "pipeline/full_bright_indicators"))
            .withVertexShader(Identifier.of("mobhealthindicators", "full_bright"))
            .withFragmentShader(Identifier.of("mobhealthindicators", "full_bright"))
            .withSampler("Sampler0")
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withPolygonMode(PolygonMode.FILL)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS)
            .withCull(false)
            .build());

    public static class AbstractRenderLayerTexture extends RenderPhase.TextureBase {
        public AbstractRenderLayerTexture(AbstractTexture texture) {
            super(() -> RenderSystem.setShaderTexture(0, texture.getGlTexture()), () -> {});
        }
    }

    private static final Map<Triple<AbstractTexture, Boolean, Boolean>, RenderLayer.MultiPhase> renderLayerCache = new ConcurrentHashMap<>();
    private static RenderLayer.MultiPhase getHealthIndicatorsRenderLayer(Config config, AbstractTexture texture, boolean renderOverBlocks) {
        renderOverBlocks = renderOverBlocks && config.renderOnTopOnHover;
        boolean areShadersEnabled = FabricLoader.getInstance().isModLoaded("iris") && net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse() || config.dynamicBrightness;
        Triple<AbstractTexture, Boolean, Boolean> key = Triple.of(texture, renderOverBlocks, areShadersEnabled);

        return renderLayerCache.computeIfAbsent(key, (Triple<AbstractTexture, Boolean, Boolean> triple) -> {
            RenderLayer.MultiPhaseParameters multiPhase = RenderLayer.MultiPhaseParameters.builder().texture(new AbstractRenderLayerTexture(triple.getLeft())).target(triple.getMiddle() ? RenderPhase.OUTLINE_TARGET : RenderPhase.MAIN_TARGET).build(true);
            return RenderLayer.of("health_indicators", 1536, false, true, triple.getRight() ? ENTITY_CUTOUT_NO_CULL : FULL_BRIGHT_INDICATORS, multiPhase);
        });
    }

    public static final float defaultPixelSize = 0.025f;
    public static float pixelSize = defaultPixelSize;

    public static void render(MatrixStack matrixStack, LivingEntity livingEntity, NativeImageBackedTexture texture, int light, double distance, boolean hasLabel, EntityRenderDispatcher dispatcher, VertexConsumerProvider vertexConsumerProvider) {
        matrixStack.push();
        matrixStack.translate(0, livingEntity.getHeight() + 0.5f + config.height/(float)heightDivisor, 0);
        if (hasLabel && distance <= 4096.0) {
            matrixStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            if (distance < 100.0 && livingEntity.getEntityWorld().getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null) {
                matrixStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            }
        }

        matrixStack.scale(pixelSize, pixelSize, pixelSize);
        matrixStack.peek().getPositionMatrix().rotateY(getYaw(dispatcher.camera.getYaw()));

        draw(matrixStack, texture, vertexConsumerProvider, light, dispatcher.targetedEntity == livingEntity, config);

        matrixStack.pop();
    }

    private static void draw(MatrixStack matrixStack, NativeImageBackedTexture texture, VertexConsumerProvider vertexConsumerProvider, int light, boolean renderOverBlocks, Config config) {
        VertexConsumer bufferBuilder = vertexConsumerProvider.getBuffer(getHealthIndicatorsRenderLayer(config, texture, renderOverBlocks));

        NativeImage image = texture.getImage();
        drawHeart(matrixStack.peek().getPositionMatrix(), bufferBuilder, image.getWidth() / 2f, image.getHeight() , config.dynamicBrightness ? light : LightmapTextureManager.MAX_LIGHT_COORDINATE);
    }

    private static void drawHeart(Matrix4f matrix4f, VertexConsumer bufferBuilder, float width, float height, int light) {
        drawVertex(matrix4f, bufferBuilder, -width, -heartSize, 0, 1, light);
        drawVertex(matrix4f, bufferBuilder, +width, -heartSize, 1, 1, light);
        drawVertex(matrix4f, bufferBuilder, +width, height-heartSize, 1, 0, light);
        drawVertex(matrix4f, bufferBuilder, -width, height-heartSize, 0, 0, light);
    }

    private static void drawVertex(Matrix4f model, VertexConsumer bufferBuilder, float x, float y, float u, float v, int light) {
        bufferBuilder.vertex(model, x, y, 0).color(1F, 1F, 1F, 1F).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 0);
    }

    public static float getYaw(double yaw) {
        yaw = -Math.toRadians(yaw);
        yaw = yaw + Math.PI;

        if (yaw > Math.PI) yaw -= (2 * Math.PI);
        if (yaw < -Math.PI) yaw += (2 * Math.PI);

        return (float) yaw;
    }
}