package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
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

    public static void beginFrame(Camera camera) {
        COMMANDS.clear();
        QUEUED_ENTITY_IDS.clear();
        cameraPosition = camera.position();
        cameraYaw = camera.yaw();
        collecting = true;
    }

    public static boolean isCollecting() {
        return collecting;
    }

    public static void queue(
            LivingEntity livingEntity,
            EntityRenderState renderState,
            Vec3 renderOffset,
            ResourceLocation texture,
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

        int light = config.dynamicBrightness ? renderState.lightCoords : LightTexture.FULL_BRIGHT;
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
                ? RenderType.textSeeThrough(command.texture())
                : RenderType.text(command.texture());

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
            ResourceLocation texture,
            float halfWidth,
            float height,
            int light,
            float opacity,
            double distanceToCameraSq,
            boolean renderOnTop
    ) {
    }
}
