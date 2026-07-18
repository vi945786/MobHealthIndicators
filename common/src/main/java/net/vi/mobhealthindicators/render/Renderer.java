package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static net.vi.mobhealthindicators.ModInit.client;
import static net.vi.mobhealthindicators.ModInit.isIrisLoaded;
import static net.vi.mobhealthindicators.ModInit.modId;
import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;

/**
 * Collects immutable health-bar commands while Minecraft extracts level render
 * state. Depth-tested bars are drawn between opaque terrain and vanilla's
 * translucent feature phase. Explicitly on-top bars are drawn as the final
 * world pass, before Iris composites and finalizes the shader-pack output.
 */
public final class Renderer {
    public static final float defaultPixelSize = 0.025F;
    public static float pixelSize = defaultPixelSize;
    public static final int heightDivisor = 50;

    /**
     * Ordinary bars use the exact same RenderType as vanilla world text. Iris
     * already has a stable mapping for this pipeline and its glyph vertex format.
     */
    private static final Function<Identifier, RenderType> WORLD_HEALTH_BAR_TYPES = RenderTypes::text;

    /**
     * The on-top pipeline keeps vanilla's lightmapped text shader contract while
     * disabling depth testing. Iris maps it to the same shader-pack program as
     * vanilla world text, so it is affected by custom shaders without inheriting
     * entity diffuse lighting or entity motion-vector semantics.
     */
    static final RenderPipeline ON_TOP_HEALTH_BAR_PIPELINE = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath(modId, "pipeline/on_top_health_bar"))
            .withVertexShader("core/rendertype_text")
            .withFragmentShader("core/rendertype_text")
            .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
            .withUniform("Projection", UniformType.UNIFORM_BUFFER)
            .withUniform("Fog", UniformType.UNIFORM_BUFFER)
            .withSampler("Sampler0")
            .withSampler("Sampler2")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withDepthStencilState(Optional.empty())
            .withCull(false)
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
            .build();

    private static final Function<Identifier, RenderType> ON_TOP_HEALTH_BAR_TYPES = Util.memoize(
            Renderer::createOnTopRenderType
    );

    private static final List<RenderCommand> COMMANDS = new ArrayList<>();
    private static final Set<Integer> QUEUED_ENTITY_IDS = new HashSet<>();

    private static Vec3 cameraPosition = Vec3.ZERO;
    private static float cameraYaw;
    private static boolean collecting;
    private static boolean commandsSorted;
    private static boolean worldCommandsFlushed;
    private static boolean onTopCommandsFlushed;

    private Renderer() {
    }

    /** Registers the depthless text variant with Iris' native world-text program. */
    public static void registerIrisPipelines() {
        IrisCompat.registerOnTopPipeline(ON_TOP_HEALTH_BAR_PIPELINE);
    }

    /** Starts collection before vanilla extracts entity render states. */
    public static void beginFrame(Camera camera) {
        COMMANDS.clear();
        QUEUED_ENTITY_IDS.clear();
        cameraPosition = camera.position();
        cameraYaw = camera.yRot();
        collecting = true;
        commandsSorted = false;
        worldCommandsFlushed = false;
        onTopCommandsFlushed = false;
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

    public static boolean hasOnTopCommands() {
        if (!collecting || onTopCommandsFlushed || isRenderingIrisShadowPass()) return false;

        for (RenderCommand command : COMMANDS) {
            if (command.renderOnTop()) return true;
        }
        return false;
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

        // Match vanilla's name-display stack. The health bar belongs above each
        // line that vanilla submitted for this entity.
        if (renderState.nameTag != null) {
            poseStack.translate(0.0F, 9.0F * 1.15F * pixelSize, 0.0F);
        }
        if (renderState.scoreText != null) {
            poseStack.translate(0.0F, 9.0F * 1.15F * pixelSize, 0.0F);
        }

        poseStack.scale(pixelSize, pixelSize, pixelSize);
        poseStack.last().pose().rotateY(getYaw(cameraYaw));

        int light = config.fullBright ? LightCoordsUtil.FULL_BRIGHT : renderState.lightCoords;
        float opacity = Mth.clamp(config.opacity / 100.0F, 0.0F, 1.0F);
        boolean renderOnTop = (targeted && config.renderOnTopOnHover);

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
     * Draws always-on-top bars from the last level frame-graph pass. At that point
     * particles, clouds and weather are complete, but Iris has not yet run its
     * composite and final stages, so the bar still passes through the shader pack.
     */
    public static void flushOnTop() {
        if (!collecting || onTopCommandsFlushed || isRenderingIrisShadowPass()) return;
        onTopCommandsFlushed = true;
        sortCommands();

        for (RenderCommand command : COMMANDS) {
            if (command.renderOnTop()) {
                draw(command, ON_TOP_HEALTH_BAR_TYPES.apply(command.texture()));
            }
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

    private static RenderType createOnTopRenderType(Identifier texture) {
        RenderSetup setup = RenderSetup.builder(ON_TOP_HEALTH_BAR_PIPELINE)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .createRenderSetup();
        return RenderType.create("mobhealthindicators_on_top_health_bar", setup);
    }

    private static void sortCommands() {
        if (commandsSorted) return;
        COMMANDS.sort(Comparator.comparingDouble(RenderCommand::distanceToCameraSq).reversed());
        commandsSorted = true;
    }

    private static void draw(RenderCommand command, RenderType renderType) {
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

        if (yaw > Math.PI) yaw -= 2.0 * Math.PI;
        if (yaw < -Math.PI) yaw += 2.0 * Math.PI;

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
