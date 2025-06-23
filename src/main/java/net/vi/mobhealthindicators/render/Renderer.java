package net.vi.mobhealthindicators.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;

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

    public static void render(MatrixStack matrixStack, LivingEntity livingEntity, NativeImageBackedTexture texture, int light, double distance, boolean hasLabel, EntityRenderDispatcher dispatcher) {
        matrixStack.push();
        matrixStack.translate(0, livingEntity.getHeight() + 0.5f + config.height/(float)heightDivisor, 0);
        if (hasLabel && distance <= 4096.0) {
            matrixStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            if (distance < 100.0 && livingEntity.getEntityWorld().getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null) {
                matrixStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            }
        }

        matrixStack.peek().getPositionMatrix().rotateY(getYaw(dispatcher.camera.getYaw()));
        matrixStack.scale(pixelSize, pixelSize, pixelSize);

        config.getRenderer().draw(matrixStack, texture, light);

        matrixStack.pop();
    }

    public static float getYaw(double yaw) {
        yaw = -Math.toRadians(yaw);
        yaw = yaw + Math.PI;

        if (yaw > Math.PI) yaw -= (2 * Math.PI);
        if (yaw < -Math.PI) yaw += (2 * Math.PI);

        return (float) yaw;
    }
}