package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.DisplaySlot;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.function.Function;

import static net.minecraft.client.renderer.RenderPipelines.ENTITY_SNIPPET;
import static net.minecraft.client.renderer.RenderStateShard.LIGHTMAP;
import static net.minecraft.client.renderer.RenderStateShard.OVERLAY;
import static net.vi.mobhealthindicators.MobHealthIndicators.*;
import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11C.GL_POLYGON_OFFSET_FACTOR;

public abstract class Renderer {
    private static final RenderPipeline FULL_BRIGHT_INDICATORS_PIPELINE = RenderPipelines.register(RenderPipeline.builder(ENTITY_SNIPPET).withLocation(ResourceLocation.fromNamespaceAndPath(modId, "pipeline/full_bright_indicators")).withShaderDefine("ALPHA_CUTOUT", 0.1F).withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING").withSampler("Sampler1").withCull(false).build());
    public static final Function<ResourceLocation, RenderType> FULL_BRIGHT_INDICATORS = Util.memoize(texture -> {
        RenderType.CompositeState state = RenderType.CompositeState.builder().setTextureState(new RenderStateShard.TextureStateShard(texture, false)).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false);
        return RenderType.create("full_bright_indicators", 1536, true, false, FULL_BRIGHT_INDICATORS_PIPELINE, state);
    });

    public static final float defaultPixelSize = 0.025f;
    public static float pixelSize = defaultPixelSize;
    public static final int heightDivisor = 50;

    public static void render(PoseStack poseStack, LivingEntity livingEntity, ResourceLocation texture, int light, double distance, boolean shouldShowName, EntityRenderDispatcher dispatcher) {
        poseStack.pushPose();
        poseStack.translate(0, livingEntity.getBbHeight() + 0.5f + config.height/(float)heightDivisor, 0);
        if (shouldShowName && distance <= 4096.0) {
            poseStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            if (distance < 100.0 && livingEntity.level().getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME) != null) {
                poseStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            }
        }

        poseStack.scale(pixelSize, pixelSize, pixelSize);
        poseStack.last().pose().rotateY(getYaw(dispatcher.camera.yaw()));

        draw(poseStack.last().pose(), texture, light, targetedEntity == livingEntity);
        poseStack.popPose();
    }

    private static float getYaw(double yaw) {
        yaw = -Math.toRadians(yaw);
        yaw = yaw + Math.PI;

        if (yaw > Math.PI) yaw -= (2 * Math.PI);
        if (yaw < -Math.PI) yaw += (2 * Math.PI);

        return (float) yaw;
    }

    public static void draw(Matrix4f positionMatrix, ResourceLocation texture, int light, boolean isTargeted) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);

        NativeImage image = ((DynamicTexture) Minecraft.getInstance().getTextureManager().getTexture(texture)).getPixels();
        if(image == null) return;
        drawHeart(positionMatrix, bufferBuilder, image.getWidth() / 2f, image.getHeight(), config.dynamicBrightness ? light : LightTexture.FULL_BRIGHT);

        RenderType renderType;
        if(areShadersEnabled || config.dynamicBrightness) renderType = RenderType.entityCutoutNoCull(texture);
        else renderType = Renderer.FULL_BRIGHT_INDICATORS.apply(texture);

        boolean offset = GL11.glGetBoolean(GL_POLYGON_OFFSET_FILL);
        float offset_factor = GL11.glGetFloat(GL_POLYGON_OFFSET_FACTOR);
        float offset_units = GL11.glGetFloat(GL_POLYGON_OFFSET_UNITS);
        boolean blend = GL11.glGetBoolean(GL_BLEND);
        if(isTargeted && config.renderOnTopOnHover) {
            GL11.glEnable(GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);
            GL11.glDisable(GL11.GL_BLEND);
        }

        renderType.draw(bufferBuilder.build());

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
        bufferBuilder.addVertex(model, x, y, 0).setColor(1F, 1F, 1F, 1F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 0);
    }
}
