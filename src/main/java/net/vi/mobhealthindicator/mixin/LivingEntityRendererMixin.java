package net.vi.mobhealthindicator.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.vi.mobhealthindicator.render.HeartType;
import net.vi.mobhealthindicator.config.ModConfig;
import net.vi.mobhealthindicator.render.Renderer;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static net.vi.mobhealthindicator.MobHealthIndicator.getConfig;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements FeatureRendererContext<T, M> {

    @Shadow protected abstract boolean hasLabel(T livingEntity);

    protected LivingEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Unique private static final Map<String, NativeImageBackedTexture> textures = new HashMap<>();
    @Unique private static final int heartsPerRow = 10;
    @Unique private static final float pixelSize = 0.025f;

    @Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    public void renderHealth(T livingEntity, float yaw, float tickDelta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        ModConfig config = getConfig();

        if (!(config.shouldRender(livingEntity) && player != null && player.getVehicle() != livingEntity && /*livingEntity != player &&*/ !livingEntity.isInvisibleTo(player))) return;

        double distance = dispatcher.getSquaredDistanceToCamera(livingEntity);

        int redHealth = MathHelper.ceil(livingEntity.getHealth());
        int maxHealth = MathHelper.ceil(livingEntity.getMaxHealth());
        int emptyHealth = maxHealth - redHealth;
        int yellowHealth = MathHelper.ceil(livingEntity.getAbsorptionAmount());

        String healthId = redHealth + " " + emptyHealth + " " + yellowHealth;

        NativeImageBackedTexture texture;

        if(textures.containsKey(healthId)) {
            texture = textures.get(healthId);
        } else {

            int redHearts = MathHelper.ceil(redHealth / 2.0F);
            boolean lastRedHalf = (redHealth & 1) == 1;
            int normalHearts = MathHelper.ceil(maxHealth / 2.0F);
            int yellowHearts = MathHelper.ceil(yellowHealth / 2.0F);
            boolean lastYellowHalf = (yellowHealth & 1) == 1;
            int totalHearts = normalHearts + yellowHearts;
            int heartRows = (int) Math.ceil(totalHearts / 10F);

            int heartDensity = Math.max(10 - (heartRows - 2), 3);
            int yPixelsTotal = (heartRows - 1) * heartDensity + 9;

            int xPixelsTotal = Math.min(totalHearts, heartsPerRow) * 8 + 1;

            BufferedImage EmptyTexture = HeartType.EMPTY.getTexture();
            BufferedImage RedFullTexture = HeartType.RED_FULL.getTexture();
            BufferedImage RedHalfTexture = HeartType.RED_HALF.getTexture();
            BufferedImage YellowFullTexture = HeartType.YELLOW_FULL.getTexture();
            BufferedImage YellowHalfTexture = HeartType.YELLOW_HALF.getTexture();

            BufferedImage healthBar = new BufferedImage(xPixelsTotal, yPixelsTotal, BufferedImage.TYPE_INT_ARGB);
            Graphics graphics = healthBar.getGraphics();


            for (int heart = totalHearts - 1; heart >= 0; heart--) {

                addHeart(graphics, EmptyTexture, heartRows, heartDensity, heart);

                if (heart < redHearts) {
                    if (heart == redHearts - 1 && lastRedHalf) {
                        addHeart(graphics, RedHalfTexture, heartRows, heartDensity, heart);
                    } else {
                        addHeart(graphics, RedFullTexture, heartRows, heartDensity, heart);
                    }
                } else if (heart >= normalHearts) {
                    if (heart == totalHearts - 1 && lastYellowHalf) {
                        addHeart(graphics, YellowHalfTexture, heartRows, heartDensity, heart);
                    } else {
                        addHeart(graphics, YellowFullTexture, heartRows, heartDensity, heart);
                    }
                }
            }

            graphics.dispose();

            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                ImageIO.write(healthBar, "png", byteArrayOutputStream);
                texture = new NativeImageBackedTexture(NativeImage.read(byteArrayOutputStream.toByteArray()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            textures.put(healthId, texture);
        }


        matrixStack.push();
        matrixStack.translate(0, livingEntity.getHeight() + 0.5f, 0);
        if (this.hasLabel(livingEntity) && distance <= 4096.0) {
            matrixStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            if (distance < 100.0 && livingEntity instanceof PlayerEntity && livingEntity.getEntityWorld().getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME) != null) {
                matrixStack.translate(0.0D, 9.0F * 1.15F * pixelSize, 0.0D);
            }
        }

        matrixStack.peek().getPositionMatrix().rotateY((float) getYaw(livingEntity, client));
        matrixStack.scale(pixelSize, pixelSize, pixelSize);

        Renderer.render(matrixStack, texture, config, light);

        matrixStack.pop();
    }

    @Unique
    private void addHeart(Graphics graphics, Image image, int heartRows, int heartDensity, int heart) {
        graphics.drawImage(image, (heart % heartsPerRow) * 8, (heartRows - (heart / heartsPerRow) -1) * heartDensity, 9, 9, null);
    }

    @Unique
    private double getYaw(T livingEntity, MinecraftClient client) {
        Vec3d entityPos = livingEntity.getPos();
        Vec3d playerPos = client.gameRenderer.getCamera().getPos();

        Vector3d direction = new Vector3d(playerPos.x - entityPos.x, playerPos.y - entityPos.y, playerPos.z - entityPos.z);
        direction.normalize();

        return (float) Math.atan2(direction.x, direction.z);
    }
}