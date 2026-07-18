package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
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
 * translucent feature phase. Explicitly on-top bars are drawn after the level
 * frame graph and shader-pack final pass have completed.
 */
public final class Renderer {
    public static final float defaultPixelSize = 0.025F;
    public static float pixelSize = defaultPixelSize;
    public static final int heightDivisor = 50;

    /**
     * Health bars deliberately use Minecraft's text shader contract rather than
     * the entity contract. Shader packs already handle billboarding text and its
     * lightmap-only lighting model, so the bar does not inherit entity diffuse
     * lighting, motion-vector artifacts, or temporal ghosting.
     */
    private static final RenderPipeline WORLD_HEALTH_BAR_PIPELINE = createHealthBarPipeline(
            "pipeline/world_health_bar",
            Optional.of(DepthStencilState.DEFAULT)
    );

    /**
     * Uses the same lightmapped text shader as the world pipeline, but disables
     * depth testing for render-through-walls and render-on-hover commands.
     * Unlike vanilla's text-see-through shader, this still samples Sampler2, so
     * dynamic brightness works for always-on-top bars as well.
     */
    private static final RenderPipeline ON_TOP_HEALTH_BAR_PIPELINE = createHealthBarPipeline(
            "pipeline/on_top_health_bar",
            Optional.empty()
    );

    private static final Function<Identifier, RenderType> WORLD_HEALTH_BAR_TYPES = Util.memoize(
            texture -> createRenderType("mobhealthindicators_world_health_bar", WORLD_HEALTH_BAR_PIPELINE, texture)
    );
    private static final Function<Identifier, RenderType> ON_TOP_HEALTH_BAR_TYPES = Util.memoize(
            texture -> createRenderType("mobhealthindicators_on_top_health_bar", ON_TOP_HEALTH_BAR_PIPELINE, texture)
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

    /**
     * Creates an unregistered text-compatible pipeline with configurable depth
     * testing. Keeping these pipelines out of Minecraft's static preload list is
     * important for the post-shader on-top path: Iris must not precompile that
     * pipeline with a gbuffer override before the final overlay draw.
     */
    private static RenderPipeline createHealthBarPipeline(
            String path,
            Optional<DepthStencilState> depthStencilState
    ) {
        return RenderPipeline.builder()
                .withLocation(Identifier.fromNamespaceAndPath(modId, path))
                .withVertexShader("core/rendertype_text")
                .withFragmentShader("core/rendertype_text")
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("Fog", UniformType.UNIFORM_BUFFER)
                .withSampler("Sampler0")
                .withSampler("Sampler2")
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(depthStencilState)
                .withCull(false)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS)
                .build();
    }

    /** Copies Iris' native text-pipeline mapping onto the in-world variant. */
    public static void registerIrisPipelines() {
        IrisCompat.registerWorldPipeline(WORLD_HEALTH_BAR_PIPELINE);
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
     * so it must not consume this frame's health-bar queue.
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
        commandsSorted = false;
    }

    /**
     * Draws ordinary bars after opaque terrain exists but before vanilla copies
     * depth to and renders its translucent targets.
     */
    public static void flushWorld() {
        // Iris calls FeatureRenderDispatcher.renderAllFeatures() for its shadow
        // map before the normal main pass. Do not mark the world queue as flushed
        // there; the main pass still needs to draw it.
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
     * Draws always-on-top bars after shader post-processing. When Iris is loaded,
     * the draw temporarily bypasses its gbuffer program replacement and extended
     * vertex format so the final overlay cannot be fed into temporal AA, motion
     * blur, bloom history, or an already-finalized shader render target.
     */
    public static void flushOnTop() {
        if (!collecting || onTopCommandsFlushed || isRenderingIrisShadowPass()) return;
        onTopCommandsFlushed = true;
        sortCommands();

        Runnable drawCommands = () -> {
            for (RenderCommand command : COMMANDS) {
                if (command.renderOnTop()) {
                    draw(command, ON_TOP_HEALTH_BAR_TYPES.apply(command.texture()));
                }
            }
        };

        if (isIrisLoaded) {
            IrisCompat.runVanilla(drawCommands);
        } else {
            drawCommands.run();
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

    private static RenderType createRenderType(String name, RenderPipeline pipeline, Identifier texture) {
        RenderSetup setup = RenderSetup.builder(pipeline)
                .withTexture("Sampler0", texture)
                .useLightmap()
                .createRenderSetup();
        return RenderType.create(name, setup);
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
