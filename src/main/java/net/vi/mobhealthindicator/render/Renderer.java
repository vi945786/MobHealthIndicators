package net.vi.mobhealthindicator.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.util.math.Vec3d;
import net.vi.mobhealthindicator.config.Config;
import net.vi.mobhealthindicator.render.draw.DefaultRenderer;
import net.vi.mobhealthindicator.render.draw.DynamicBrightnessRenderer;
import org.joml.Vector3d;

import java.util.WeakHashMap;

public class Renderer {

    public static final float defaultPixelSize = 0.025f;
    public static float pixelSize = 0.025f;

    public static void render(MinecraftClient client, MatrixStack matrixStack, LivingEntity livingEntity, NativeImageBackedTexture texture, Config config, int light, double distance, boolean hasLabel) {
        matrixStack.push();
        matrixStack.translate(0, livingEntity.getHeight() + 0.5f, 0);
        if (hasLabel && distance <= 4096.0) {
            matrixStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            if (distance < 100.0 && livingEntity.getEntityWorld().getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null) {
                matrixStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            }
        }

        matrixStack.peek().getPositionMatrix().rotateY((float) getYaw(livingEntity, client));
        matrixStack.scale(pixelSize, pixelSize, pixelSize);

        if(config.dynamicBrightness) {
            DynamicBrightnessRenderer.draw(matrixStack, texture, light);
        } else {
            DefaultRenderer.draw(matrixStack, texture);
        }

        matrixStack.pop();
    }

    public static final WeakHashMap<LivingEntity, Double> entityToOldYaw = new WeakHashMap<>();

    private static double getYaw(LivingEntity livingEntity, MinecraftClient client) {
        Vec3d entityPos = livingEntity.getPos();
        Vec3d playerPos = client.gameRenderer.getCamera().getPos();

        Vector3d direction = new Vector3d(playerPos.x - entityPos.x, 0, playerPos.z - entityPos.z);

        double yaw = Math.atan2(direction.x, direction.z);

        double oldYaw = yaw;
        if(entityToOldYaw.containsKey(livingEntity)) oldYaw = entityToOldYaw.get(livingEntity);

        double tickDelta = 0.1F;
        double CS = (1-tickDelta) * Math.cos(oldYaw) + tickDelta * Math.cos(yaw);
        double SN = (1-tickDelta) * Math.sin(oldYaw) + tickDelta * Math.sin(yaw);

        yaw = Math.atan2(SN,CS);
        entityToOldYaw.put(livingEntity, yaw);

        return yaw;
    }
}
