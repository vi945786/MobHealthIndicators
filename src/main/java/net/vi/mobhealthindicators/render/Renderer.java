package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

import java.util.WeakHashMap;

import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.config.Config.heightDivisor;

public abstract class Renderer {

    public static class AbstractRenderLayerTexture extends RenderPhase.TextureBase {
        public AbstractRenderLayerTexture(AbstractTexture texture) {
            super(() -> RenderSystem.setShaderTexture(0, texture.getGlTexture()), () -> {});
        }
    }

    public abstract void draw(MatrixStack matrixStack, NativeImageBackedTexture texture, int light);

    public static final float defaultPixelSize = 0.025f;
    public static float pixelSize = 0.025f;

    public static void render(MinecraftClient client, MatrixStack matrixStack, LivingEntity livingEntity, NativeImageBackedTexture texture, int light, double distance, boolean hasLabel) {
        matrixStack.push();
        matrixStack.translate(0, livingEntity.getHeight() + 0.5f + config.height/(float)heightDivisor, 0);
        if (hasLabel && distance <= 4096.0) {
            matrixStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            if (distance < 100.0 && livingEntity.getEntityWorld().getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null) {
                matrixStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            }
        }

        matrixStack.peek().getPositionMatrix().rotateY((float) getYaw(livingEntity, client));
        matrixStack.scale(pixelSize, pixelSize, pixelSize);

        config.getRenderer().draw(matrixStack, texture, light);

        matrixStack.pop();
    }

    public static final WeakHashMap<LivingEntity, Double> entityToOldYaw = new WeakHashMap<>();

    private static double getYaw(LivingEntity livingEntity, MinecraftClient client) {
        Vec3d entityPos = livingEntity.getPos();
        Vec3d playerPos = client.gameRenderer.getCamera().getPos();

        Vector3d direction = new Vector3d(playerPos.x - entityPos.x, 0, playerPos.z - entityPos.z);

        double yaw = Math.atan2(direction.x, direction.z);

        double oldYaw = entityToOldYaw.getOrDefault(livingEntity, yaw);

        double tickDelta = 0.1F;
        double CS = (1-tickDelta) * Math.cos(oldYaw) + tickDelta * Math.cos(yaw);
        double SN = (1-tickDelta) * Math.sin(oldYaw) + tickDelta * Math.sin(yaw);

        yaw = Math.atan2(SN, CS);
        entityToOldYaw.put(livingEntity, yaw);

        return yaw;
    }
}
