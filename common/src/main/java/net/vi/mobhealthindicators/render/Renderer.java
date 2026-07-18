package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.api.v0.IrisProgram;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
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
import static net.vi.mobhealthindicators.ModInit.modId;
import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;

/**
 * Collects immutable health-bar commands while Minecraft extracts level render
 * state. Depth-tested bars are drawn between opaque terrain and vanilla's
 * translucent feature phase; explicitly on-top bars are drawn in a final
 * frame-graph pass after level transparency composition has completed.
 */
public final class Renderer {
    public static final float defaultPixelSize = 0.025F;
    public static float pixelSize = defaultPixelSize;
    public static final int heightDivisor = 50;

    /**
     * Uses the entity shader instead of the text shader so both normal and
     * see-through bars sample the world lightmap. Normal bars write their depth
     * before vanilla copies it to the translucent targets, which gives glass,
     * water and particles the correct front/behind ordering.
     */
    private static final RenderPipeline WORLD_HEALTH_BAR_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(modId, "pipeline/world_health_bar"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("NO_OVERLAY")
                    .withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, true))
                    .withCull(false)
                    .build()
    );

    /**
     * Same lightmapped entity shader, but with depth testing disabled. This is
     * used only for render-through-walls and render-on-hover commands.
     */
    private static final RenderPipeline ON_TOP_HEALTH_BAR_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(modId, "pipeline/on_top_health_bar"))
                    .withShaderDefine("ALPHA_CUTOUT", 0.1F)
                    .withShaderDefine("NO_OVERLAY")
                    .withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(Optional.empty())
                    .withCull(false)
                    .build()
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
     * Maps the custom pipelines to Iris' translucent entity program. The
     * pipeline still controls depth testing, blending and lightmap use.
     */
    public static void registerIrisPipelines() {
        IrisApi iris = IrisApi.getInstance();
        iris.assignPipeline(WORLD_HEALTH_BAR_PIPELINE, IrisProgram.ENTITIES_TRANSLUCENT);
        iris.assignPipeline(ON_TOP_HEALTH_BAR_PIPELINE, IrisProgram.ENTITIES_TRANSLUCENT);
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

    public static boolean isCollecting() {
        return collecting;
    }

    public static boolean hasOnTopCommands() {
        if (!collecting || onTopCommandsFlushed) return false;

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
        if (!collecting || client == null || config == null || config.opacity <= 0) return;
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
        if (!collecting || worldCommandsFlushed) return;
        worldCommandsFlushed = true;
        sortCommands();

        for (RenderCommand command : COMMANDS) {
            if (!command.renderOnTop()) {
                draw(command, WORLD_HEALTH_BAR_TYPES.apply(command.texture()));
            }
        }
    }

    /** Draws only the explicitly always-on-top commands in the final pass. */
    public static void flushOnTop() {
        if (!collecting || onTopCommandsFlushed) return;
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
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(command.light())
                .setNormal(0.0F, 0.0F, 1.0F);
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
