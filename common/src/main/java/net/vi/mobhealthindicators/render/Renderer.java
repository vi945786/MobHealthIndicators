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
import net.minecraft.client.renderer.texture.DynamicTexture;
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
import java.util.Set;

import static net.vi.mobhealthindicators.ModInit.client;
import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;

/**
 * Collects immutable health-bar commands while Minecraft extracts level render
 * state, then draws them from a dedicated late frame-graph pass.
 *
 * <p>Minecraft 26.1 performs entity extraction in {@code extractLevel(...)}
 * before {@code renderLevel(...)} builds and executes the frame graph. The
 * collection lifetime deliberately spans those two stages.</p>
 */
public final class Renderer {
    public static final float defaultPixelSize = 0.025F;
    public static float pixelSize = defaultPixelSize;
    public static final int heightDivisor = 50;

    private static final List<RenderCommand> COMMANDS = new ArrayList<>();
    private static final Set<Integer> QUEUED_ENTITY_IDS = new HashSet<>();

    private static Vec3 cameraPosition = Vec3.ZERO;
    private static float cameraYaw;
    private static boolean collecting;

    private Renderer() {
    }

    /**
     * Starts collection before vanilla extracts entity render states.
     */
    public static void beginFrame(Camera camera) {
        COMMANDS.clear();
        QUEUED_ENTITY_IDS.clear();
        cameraPosition = camera.position();
        cameraYaw = camera.yRot();
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

        // Match vanilla's name-display stack: score text is below the name,
        // and the health bar belongs above every line that was submitted.
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
    }

    /**
     * Draws all queued bars after blocks, entities, particles, weather and the
     * level post chain have completed while the final level targets are active.
     */
    public static void flush() {
        if (!collecting || COMMANDS.isEmpty()) return;

        COMMANDS.sort(Comparator.comparingDouble(RenderCommand::distanceToCameraSq).reversed());

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
