package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tessellator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static net.vi.mobhealthindicators.ModInit.client;
import static net.vi.mobhealthindicators.ModInit.isIrisLoaded;
import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;

/**
 * Collects immutable health-bar commands while Minecraft extracts level render
 * state. Depth-tested bars are drawn between opaque terrain and vanilla's
 * translucent feature phase. Explicitly on-top bars are drawn after
 * GameRenderer has completed the level, shader-pack finalization, hands and
 * screen effects.
 */
public final class Renderer {
    public static final float defaultPixelSize = 0.025F;
    public static float pixelSize = defaultPixelSize;
    public static final int heightDivisor = 50;

    /**
     * Both paths use vanilla's world-text render type. Iris already has a stable
     * mapping for this exact pipeline and vertex format, and it samples the
     * lightmap used by dynamic-brightness mode.
     */
    private static final Function<Identifier, RenderType> WORLD_HEALTH_BAR_TYPES = RenderTypes::text;
    private static final Function<Identifier, RenderType> ON_TOP_HEALTH_BAR_TYPES = RenderTypes::text;

    private static final List<RenderCommand> COMMANDS = new ArrayList<>();
    private static final Set<Integer> QUEUED_ENTITY_IDS = new HashSet<>();

    private static Vec3 cameraPosition = Vec3.ZERO;
    private static final Quaternionf CAMERA_ORIENTATION = new Quaternionf();
    private static boolean collecting;
    private static boolean commandsSorted;
    private static boolean worldCommandsFlushed;
    private static boolean onTopCommandsFlushed;

    /** Render state captured before LevelRenderer and restored for the late overlay. */
    private static final Matrix4f WORLD_MODEL_VIEW = new Matrix4f();
    private static GpuBufferSlice worldProjection;
    private static ProjectionType worldProjectionType = ProjectionType.PERSPECTIVE;
    private static GpuBufferSlice worldFog;

    /**
     * A private depth attachment lets the normal lightmapped text pipeline pass
     * depth testing without clearing or modifying Minecraft's real depth buffer.
     */
    private static TextureTarget onTopDepthTarget;

    private Renderer() {
    }

    /** Starts collection before vanilla extracts entity render states. */
    public static void beginFrame(Camera camera) {
        COMMANDS.clear();
        QUEUED_ENTITY_IDS.clear();
        cameraPosition = camera.position();
        CAMERA_ORIENTATION.set(camera.rotation());
        collecting = true;
        commandsSorted = false;
        worldCommandsFlushed = false;
        onTopCommandsFlushed = false;
    }

    /**
     * Captures the matrices and fog that were active for the level. GameRenderer
     * switches to the hand/HUD projection before the final on-top draw runs.
     */
    public static void captureWorldRenderState(Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog) {
        WORLD_MODEL_VIEW.set(modelViewMatrix);
        worldProjection = RenderSystem.getProjectionMatrixBuffer();
        worldProjectionType = RenderSystem.getProjectionType();
        worldFog = terrainFog;
    }

    /**
     * Iris re-extracts and renders entities while building the shadow map. That
     * pass uses the same FeatureRenderDispatcher methods as the main world pass,
     * so it must not add to or consume this frame's health-bar queue.
     */
    private static boolean isRenderingIrisShadowPass() {
        return isIrisLoaded && IrisCompat.isRenderingShadowPass();
    }

    public static boolean isCollecting() {
        return collecting && !isRenderingIrisShadowPass();
    }

    public static void queue(
            LivingEntity livingEntity,
            EntityRenderState renderState,
            Vec3 renderOffset,
            Identifier texture,
            boolean targeted
    ) {
        if (!isCollecting() || client == null || config == null || config.opacity <= 0) return;
        if (!QUEUED_ENTITY_IDS.add(livingEntity.getId())) return;

        if (!(client.getTextureManager().getTexture(texture) instanceof DynamicTexture dynamicTexture)) return;
        NativeImage image = dynamicTexture.getPixels();
        if (image == null) return;

        PoseStack poseStack = new PoseStack();
        poseStack.translate(
                renderState.x + renderOffset.x - cameraPosition.x,
                renderState.y + renderOffset.y - cameraPosition.y
                        + renderState.boundingBoxHeight
                        + 0.5F
                        + config.height / (float) heightDivisor,
                renderState.z + renderOffset.z - cameraPosition.z
        );

        // Match vanilla name-tag billboarding. Using only camera yaw leaves pitch
        // in the view matrix and makes the bar slide or shear while looking up/down.
        poseStack.mulPose(CAMERA_ORIENTATION);
        poseStack.scale(pixelSize, pixelSize, pixelSize);

        // Name and score offsets are screen-local pixels, not world-space Y.
        if (renderState.nameTag != null) {
            poseStack.translate(0.0F, 9.0F * 1.15F, 0.0F);
        }
        if (renderState.scoreText != null) {
            poseStack.translate(0.0F, 9.0F * 1.15F, 0.0F);
        }

        int light = config.fullBright ? LightCoordsUtil.FULL_BRIGHT : renderState.lightCoords;
        float opacity = Mth.clamp(config.opacity / 100.0F, 0.0F, 1.0F);
        boolean renderOnTop = targeted && config.renderOnTopOnHover;

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
        commandsSorted = false;
    }

    /**
     * Draws ordinary bars after opaque terrain exists but before vanilla copies
     * depth to and renders its translucent targets. Using RenderTypes.text keeps
     * the shader-pack path identical to vanilla name tags while writing the bar's
     * depth for correct glass, water, particle and weather composition.
     */
    public static void flushWorld() {
        // Iris calls FeatureRenderDispatcher.renderAllFeatures() for its shadow
        // map before the normal main pass. Do not mark the queue as flushed there.
        if (!collecting || worldCommandsFlushed || isRenderingIrisShadowPass()) return;
        worldCommandsFlushed = true;
        sortCommands();

        for (RenderCommand command : COMMANDS) {
            if (!command.renderOnTop()) {
                draw(command, WORLD_HEALTH_BAR_TYPES.apply(command.texture()));
            }
        }
    }

    /**
     * Draws always-on-top bars after GameRenderer.renderLevel has returned. The
     * exact vanilla text pipeline is used with Iris bypassed, while color is sent
     * to the real main target and depth is sent to a private cleared attachment.
     * This preserves lightmap brightness without feeding the bar into shader-pack
     * temporal history or requiring a custom Iris pipeline mapping.
     */
    public static void flushOnTop() {
        if (!collecting || onTopCommandsFlushed || isRenderingIrisShadowPass()) return;
        onTopCommandsFlushed = true;
        sortCommands();

        boolean hasOnTopCommands = false;
        for (RenderCommand command : COMMANDS) {
            if (command.renderOnTop()) {
                hasOnTopCommands = true;
                break;
            }
        }
        if (!hasOnTopCommands || client == null) return;

        Runnable drawCommands = Renderer::drawOnTopCommands;
        if (isIrisLoaded) {
            IrisCompat.runVanilla(drawCommands);
        } else {
            drawCommands.run();
        }
    }

    private static void drawOnTopCommands() {
        RenderTarget mainTarget = client.getMainRenderTarget();
        ensureOnTopDepthTarget(mainTarget.width, mainTarget.height);

        // Minecraft 26.2 uses reversed depth, so the far-plane clear value is 0.
        RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(onTopDepthTarget.getDepthTexture(), 0.0D);

        GpuBufferSlice previousProjection = RenderSystem.getProjectionMatrixBuffer();
        ProjectionType previousProjectionType = RenderSystem.getProjectionType();
        GpuBufferSlice previousFog = RenderSystem.getShaderFog();
        GpuTextureView previousColorTarget = RenderSystem.outputColorTextureOverride;
        GpuTextureView previousDepthTarget = RenderSystem.outputDepthTextureOverride;
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();

        modelViewStack.pushMatrix();
        modelViewStack.set(WORLD_MODEL_VIEW);
        RenderSystem.setProjectionMatrix(worldProjection, worldProjectionType);
        RenderSystem.setShaderFog(worldFog);
        RenderSystem.outputColorTextureOverride = mainTarget.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = onTopDepthTarget.getDepthTextureView();

        try {
            for (RenderCommand command : COMMANDS) {
                if (command.renderOnTop()) {
                    draw(command, ON_TOP_HEALTH_BAR_TYPES.apply(command.texture()));
                }
            }
        } finally {
            RenderSystem.outputDepthTextureOverride = previousDepthTarget;
            RenderSystem.outputColorTextureOverride = previousColorTarget;
            RenderSystem.setShaderFog(previousFog);
            RenderSystem.setProjectionMatrix(previousProjection, previousProjectionType);
            modelViewStack.popMatrix();
        }
    }

    private static void ensureOnTopDepthTarget(int width, int height) {
        if (onTopDepthTarget == null) {
            onTopDepthTarget = new TextureTarget("Mob Health Indicators on-top depth", width, height, true);
        } else if (onTopDepthTarget.width != width || onTopDepthTarget.height != height) {
            onTopDepthTarget.resize(width, height);
        }
    }

    public static void endFrame() {
        collecting = false;
        commandsSorted = false;
        worldCommandsFlushed = false;
        onTopCommandsFlushed = false;
        COMMANDS.clear();
        QUEUED_ENTITY_IDS.clear();
    }

    private static void sortCommands() {
        if (commandsSorted) return;
        COMMANDS.sort(Comparator.comparingDouble(RenderCommand::distanceToCameraSq).reversed());
        commandsSorted = true;
    }

    private static void draw(RenderCommand command, RenderType renderType) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(renderType.mode(), renderType.format());
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
