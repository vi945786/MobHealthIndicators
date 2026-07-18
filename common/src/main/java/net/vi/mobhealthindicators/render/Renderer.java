package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.vi.mobhealthindicators.ModInit.client;
import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;

/**
 * Collects immutable health-bar commands during entity render-state extraction
 * and draws them from a dedicated late level frame-graph pass.
 *
 * <p>The pass runs after translucent blocks, particles, weather and the level
 * post chain while the level color and depth targets are still active. The
 * actual draw uses vanilla text render types, so lightmap selection, alpha
 * blending, resource-pack textures and shader-mod render-target overrides stay
 * inside Minecraft's rendering pipeline. No raw OpenGL state is mutated.</p>
 */
public final class Renderer {
    public static final float defaultPixelSize = 0.025f;
    public static float pixelSize = defaultPixelSize;
    public static final int heightDivisor = 50;

    private static final List<RenderCommand> COMMANDS = new ArrayList<>();
    private static final Set<Integer> QUEUED_ENTITY_IDS = new HashSet<>();

    private static Vec3 cameraPosition = Vec3.ZERO;
    private static float cameraYaw;
    private static boolean collecting;

    private Renderer() {
    }

    public static void beginFrame(CameraRenderState camera) {
        COMMANDS.clear();
        QUEUED_ENTITY_IDS.clear();
        cameraPosition = camera.pos;
        cameraYaw = camera.yRot;
        collecting = true;
    }

    public static boolean isCollecting() {
        return collecting;
    }

    public static void queue(
            LivingEntity livingEntity,
            EntityRenderState renderState,
            Vec3 renderOffset,
            Identifier texture,
            boolean targeted
    ) {
        if (!collecting || config == null || config.opacity <= 0) return;
        if (!QUEUED_ENTITY_IDS.add(livingEntity.getId())) return;

        if (!(client.getTextureManager().getTexture(texture) instanceof DynamicTexture dynamicTexture)) return;
        NativeImage image = dynamicTexture.getPixels();
        if (image == null) return;

        PoseStack poseStack = new PoseStack();
        poseStack.translate(
                renderState.x + renderOffset.x - cameraPosition.x,
                renderState.y + renderOffset.y - cameraPosition.y
                        + renderState.boundingBoxHeight
                        + 0.5f
                        + config.height / (float) heightDivisor,
                renderState.z + renderOffset.z - cameraPosition.z
        );

        if (renderState.nameTag != null && renderState.distanceToCameraSq <= 4096.0) {
            poseStack.translate(0.0, 9.0F * 1.15F * pixelSize, 0.0);
            if (renderState.distanceToCameraSq < 100.0
                    && livingEntity.level().getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME) != null) {
                poseStack.translate(0.0, 9.0F * 1.15F * pixelSize, 0.0);
            }
        }

        poseStack.scale(pixelSize, pixelSize, pixelSize);
        poseStack.last().pose().rotateY(getYaw(cameraYaw));

        int light = config.dynamicBrightness ? renderState.lightCoords : LightCoordsUtil.FULL_BRIGHT;
        float opacity = Mth.clamp(config.opacity / 100.0F, 0.0F, 1.0F);
        boolean renderOnTop = config.renderThroughWalls || (targeted && config.renderOnTopOnHover);

        COMMANDS.add(new RenderCommand(
                new Matrix4f(poseStack.last().pose()),
                texture,
                image.getWidth() / 2.0F,
                image.getHeight(),
                light,
                opacity,
                renderState.distanceToCameraSq,
                renderOnTop
        ));
    }

    /**
     * Draws all queued bars while the level frame-graph pass has its color and
     * depth outputs installed in {@code RenderSystem}. The world model-view
     * matrix is already active at this point.
     */
    public static void flush() {
        if (!collecting || COMMANDS.isEmpty()) return;

        COMMANDS.sort(Comparator.comparingDouble(RenderCommand::distanceToCameraSq).reversed());

        // Preserve depth occlusion for normal bars. Explicitly on-top bars are
        // drawn last with the vanilla see-through text pipeline.
        for (RenderCommand command : COMMANDS) {
            if (!command.renderOnTop()) draw(command);
        }
        for (RenderCommand command : COMMANDS) {
            if (command.renderOnTop()) draw(command);
        }
    }

    public static void endFrame() {
        collecting = false;
        COMMANDS.clear();
        QUEUED_ENTITY_IDS.clear();
    }

    private static void draw(RenderCommand command) {
        RenderType renderType = command.renderOnTop()
                ? RenderTypes.textSeeThrough(command.texture())
                : RenderTypes.text(command.texture());

        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(renderType.mode(), renderType.format());
        drawQuad(command, bufferBuilder);
        MeshData meshData = bufferBuilder.build();
        renderType.draw(meshData);
    }

    private static void drawQuad(RenderCommand command, VertexConsumer consumer) {
        float minY = -heartSize;
        float maxY = command.height() - heartSize;
        float width = command.halfWidth();

        drawVertex(command, consumer, -width, minY, 0.0F, 1.0F);
        drawVertex(command, consumer, width, minY, 1.0F, 1.0F);
        drawVertex(command, consumer, width, maxY, 1.0F, 0.0F);
        drawVertex(command, consumer, -width, maxY, 0.0F, 0.0F);
    }

    private static void drawVertex(
            RenderCommand command,
            VertexConsumer consumer,
            float x,
            float y,
            float u,
            float v
    ) {
        consumer.addVertex(command.modelMatrix(), x, y, 0.0F)
                .setColor(1.0F, 1.0F, 1.0F, command.opacity())
                .setUv(u, v)
                .setLight(command.light());
    }

    private static float getYaw(double yaw) {
        yaw = -Math.toRadians(yaw);
        yaw += Math.PI;

        if (yaw > Math.PI) yaw -= 2 * Math.PI;
        if (yaw < -Math.PI) yaw += 2 * Math.PI;

        return (float) yaw;
    }

    private record RenderCommand(
            Matrix4f modelMatrix,
            Identifier texture,
            float halfWidth,
            float height,
            int light,
            float opacity,
            double distanceToCameraSq,
            boolean renderOnTop
    ) {
    }
}

//package net.vi.mobhealthindicators.render;
//
//import com.mojang.blaze3d.pipeline.*;
//import com.mojang.blaze3d.platform.NativeImage;
//import com.mojang.blaze3d.systems.RenderSystem;
//import com.mojang.blaze3d.vertex.*;
//import net.minecraft.client.renderer.RenderPipelines;
//import net.minecraft.client.renderer.SubmitNodeCollector;
//import net.minecraft.client.renderer.rendertype.RenderSetup;
//import net.minecraft.client.renderer.rendertype.RenderType;
//import net.minecraft.util.LightCoordsUtil;
//import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
//import net.minecraft.client.renderer.texture.DynamicTexture;
//import net.minecraft.client.renderer.texture.OverlayTexture;
//import net.minecraft.resources.Identifier;
//import net.minecraft.util.Util;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.scores.DisplaySlot;
//import org.joml.Matrix4f;
//
//import java.util.Optional;
//import java.util.function.Function;
//
//import static net.minecraft.client.renderer.RenderPipelines.ENTITY_SNIPPET;
//import static net.vi.mobhealthindicators.ModInit.*;
//import static net.vi.mobhealthindicators.config.Config.config;
//import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;
//
//public abstract class Renderer {
//    public static final RenderPipeline FULL_BRIGHT_PIPELINE = RenderPipelines.register(RenderPipeline.builder(ENTITY_SNIPPET).withLocation(Identifier.fromNamespaceAndPath(modId, "pipeline/full_bright_indicators")).withShaderDefine("ALPHA_CUTOUT", 0.1F).withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING").withSampler("Sampler1").withColorTargetState(new ColorTargetState(Optional.of(BlendFunction.TRANSLUCENT), ColorTargetState.WRITE_ALL)).withCull(false).build());
//    public static final Function<Identifier, RenderType> FULL_BRIGHT_RENDER_TYPE = Util.memoize(texture -> {
//        RenderSetup state = RenderSetup.builder(FULL_BRIGHT_PIPELINE).withTexture("Sampler0", texture).useLightmap().useOverlay().createRenderSetup();
//        return RenderType.create("full_bright_indicators", state);
//    });
//
//    public record RenderData(PoseStack.Pose pose, RenderType renderType, LivingEntity livingEntity, Identifier texture, int light, double distance, boolean shouldShowName, EntityRenderDispatcher dispatcher) {}
//
//    public static final float defaultPixelSize = 0.025f;
//    public static float pixelSize = defaultPixelSize;
//    public static final int heightDivisor = 50;
//
//    public static void render(RenderData renderData) {
//        render(renderData.pose, renderData.renderType, renderData.livingEntity, renderData.texture, renderData.light, renderData.distance, renderData.shouldShowName, renderData.dispatcher);
//    }
//
//    public static void render(PoseStack.Pose pose, RenderType renderType, LivingEntity livingEntity, Identifier texture, int light, double distance, boolean shouldShowName, EntityRenderDispatcher dispatcher) {
//        pose.translate(0, livingEntity.getBbHeight() + 0.5f + config.height / (float) heightDivisor, 0);
//        if (shouldShowName && distance <= 4096.0) {
//            pose.translate(0.0F, 9.0F * 1.15F * pixelSize, 0.0F);
//            if (distance < 100.0 && livingEntity.level().getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME) != null) {
//                pose.translate(0.0F, 9.0F * 1.15F * pixelSize, 0.0F);
//            }
//        }
//
//        pose.scale(pixelSize, pixelSize, pixelSize);
//        pose.pose().rotateY(getYaw(dispatcher.camera.yaw()));
//
//        NativeImage image = ((DynamicTexture) client.getTextureManager().getTexture(texture)).getPixels();
//
//        VertexConsumer buffer = client.renderBuffers().bufferSource().getBuffer(renderType);
//
//        drawHeart(pose.pose(), buffer, image.getWidth() / 2f, image.getHeight(), config.dynamicBrightness ? light : LightCoordsUtil.FULL_BRIGHT);
//    }
//
//    private static float getYaw(double yaw) {
//        yaw = -Math.toRadians(yaw);
//        yaw = yaw + Math.PI;
//
//        if (yaw > Math.PI) yaw -= (2 * Math.PI);
//        if (yaw < -Math.PI) yaw += (2 * Math.PI);
//
//        return (float) yaw;
//    }
//
//    public static void drawHeart(Matrix4f matrix4f, VertexConsumer bufferBuilder, float width, float height, int light) {
//        drawVertex(matrix4f, bufferBuilder, -width, -heartSize, 0, 1, light);
//        drawVertex(matrix4f, bufferBuilder, +width, -heartSize, 1, 1, light);
//        drawVertex(matrix4f, bufferBuilder, +width, height-heartSize, 1, 0, light);
//        drawVertex(matrix4f, bufferBuilder, -width, height-heartSize, 0, 0, light);
//    }
//
//    private static void drawVertex(Matrix4f model, VertexConsumer bufferBuilder, float x, float y, float u, float v, int light) {
//        bufferBuilder.addVertex(model, x, y, 0).setColor(1F, 1F, 1F, 1F).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(0, 0, 0);
//    }
//}
