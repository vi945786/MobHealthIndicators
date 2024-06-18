package net.vi.mobhealthindicator.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.util.math.Vec3d;
import net.vi.mobhealthindicator.config.ModConfig;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Unique;

import java.util.function.Supplier;

public class Renderer {

    private static final float pixelSize = 0.025f;

    public static void render(MinecraftClient client, MatrixStack matrixStack, LivingEntity livingEntity, NativeImageBackedTexture texture, ModConfig config, int light, double distance,boolean hasLabel) {
        matrixStack.push();
        matrixStack.translate(0, livingEntity.getHeight() + 0.5f, 0);
        if (hasLabel && distance <= 4096.0) {
            matrixStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            if (distance < 100.0 && livingEntity instanceof PlayerEntity && livingEntity.getEntityWorld().getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null) {
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

    @Unique
    private static double getYaw(LivingEntity livingEntity, MinecraftClient client) {
        Vec3d entityPos = livingEntity.getPos();
        Vec3d playerPos = client.gameRenderer.getCamera().getPos();

        Vector3d direction = new Vector3d(playerPos.x - entityPos.x, playerPos.y - entityPos.y, playerPos.z - entityPos.z);
        direction.normalize();

        return (float) Math.atan2(direction.x, direction.z);
    }
}
