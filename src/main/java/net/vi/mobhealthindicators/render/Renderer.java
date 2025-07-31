package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.function.Function;

import static net.minecraft.client.gl.RenderPipelines.*;
import static net.minecraft.client.render.RenderPhase.*;
import static net.vi.mobhealthindicators.MobHealthIndicators.*;
import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11C.GL_POLYGON_OFFSET_FACTOR;

public abstract class Renderer {
    private static final RenderPipeline FULL_BRIGHT_INDICATORS_PIPELINE = register(RenderPipeline.builder(ENTITY_SNIPPET).withLocation(Identifier.of(modId, "pipeline/full_bright_indicators")).withShaderDefine("ALPHA_CUTOUT", 0.1F).withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING").withSampler("Sampler1").withCull(false).build());
    public static final Function<Identifier, RenderLayer> FULL_BRIGHT_INDICATORS = Util.memoize(texture -> {
        RenderLayer.MultiPhaseParameters multiPhaseParameters = RenderLayer.MultiPhaseParameters.builder().texture(new Texture(texture, false)).lightmap(ENABLE_LIGHTMAP).overlay(ENABLE_OVERLAY_COLOR).build(false);
        return RenderLayer.of("full_bright_indicators", 1536, true, false, FULL_BRIGHT_INDICATORS_PIPELINE, multiPhaseParameters);
    });

    public static final float defaultPixelSize = 0.025f;
    public static float pixelSize = defaultPixelSize;
    public static final int heightDivisor = 50;

    public static void render(MatrixStack matrixStack, LivingEntity livingEntity, Identifier texture, int light, double distance, boolean hasLabel, EntityRenderDispatcher dispatcher) {
        matrixStack.push();
        matrixStack.translate(0, livingEntity.getHeight() + 0.5f + config.height/(float)heightDivisor, 0);
        if (hasLabel && distance <= 4096.0) {
            matrixStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            if (distance < 100.0 && livingEntity.getWorld().getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null) {
                matrixStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            }
        }

        matrixStack.scale(pixelSize, pixelSize, pixelSize);
        matrixStack.peek().getPositionMatrix().rotateY(getYaw(dispatcher.camera.getYaw()));

        draw(matrixStack.peek().getPositionMatrix(), texture, light, targetedEntity == livingEntity);
        matrixStack.pop();
    }

    private static float getYaw(double yaw) {
        yaw = -Math.toRadians(yaw);
        yaw = yaw + Math.PI;

        if (yaw > Math.PI) yaw -= (2 * Math.PI);
        if (yaw < -Math.PI) yaw += (2 * Math.PI);

        return (float) yaw;
    }

    public static void draw(Matrix4f positionMatrix, Identifier texture, int light, boolean isTargeted) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        NativeImage image = ((NativeImageBackedTexture) client.getTextureManager().getTexture(texture)).getImage();
        if(image == null) return;
        drawHeart(positionMatrix, bufferBuilder, image.getWidth() / 2f, image.getHeight(), config.dynamicBrightness ? light : LightmapTextureManager.MAX_LIGHT_COORDINATE);

        RenderLayer renderLayer;
        if(areShadersEnabled || config.dynamicBrightness) renderLayer = RenderLayer.getEntityCutoutNoCull(texture);
        else renderLayer = Renderer.FULL_BRIGHT_INDICATORS.apply(texture);

        boolean offset = GL11.glGetBoolean(GL_POLYGON_OFFSET_FILL);
        float offset_factor = GL11.glGetFloat(GL_POLYGON_OFFSET_FACTOR);
        float offset_units = GL11.glGetFloat(GL_POLYGON_OFFSET_UNITS);
        boolean blend = GL11.glGetBoolean(GL_BLEND);
        if(isTargeted && config.renderOnTopOnHover) {
            GL11.glEnable(GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
            GL11.glDisable(GL11.GL_BLEND);
        }

        renderLayer.draw(bufferBuilder.end());

        if(isTargeted && config.renderOnTopOnHover) {
            if(!offset) GL11.glDisable(GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(offset_factor, offset_units);
            if(blend) GL11.glEnable(GL_BLEND);
        }
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
}
