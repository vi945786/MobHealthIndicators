package net.vi.mobhealthindicators.render;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.math.MathHelper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TextureBuilder {

    public static BufferedImage emptyTexture;
    public static BufferedImage normalFullTexture;
    public static BufferedImage normalHalfTexture;
    public static BufferedImage poisonFullTexture;
    public static BufferedImage poisonHalfTexture;
    public static BufferedImage witherFullTexture;
    public static BufferedImage witherHalfTexture;
    public static BufferedImage absorbingFullTexture;
    public static BufferedImage absorbingHalfTexture;
    public static BufferedImage frozenFullTexture;
    public static BufferedImage frozenHalfTexture;

    public static int heartSize;

    public static final Map<String, NativeImageBackedTexture> textures = new HashMap<>();
    private static final int heartsPerRow = 10;

    public static NativeImageBackedTexture getTexture(int normalHealth, int maxHealth, int absorbingHealth, HeartType.Effect effect) {

        int normalHearts = MathHelper.ceil(normalHealth / 2.0F);
        int maxHearts = MathHelper.ceil(maxHealth / 2.0F);
        int absorptionHearts = MathHelper.ceil(absorbingHealth / 2.0F);
        boolean lastAbsorptionHalf = (absorptionHearts & 1) == 1;
        int totalHearts = maxHearts + absorptionHearts;
        int heartRows = (int) Math.ceil(totalHearts / (float) heartsPerRow);

        int heartDensity = Math.max(heartsPerRow - (heartRows - 2), 3);
        int yPixelsTotal = (heartRows - 1) * heartDensity + heartSize;

        int xPixelsTotal = Math.min(totalHearts, heartsPerRow) * (heartSize -1) + 1;

        BufferedImage healthBar = new BufferedImage(xPixelsTotal, yPixelsTotal, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = healthBar.getGraphics();

        for (int heart = totalHearts - 1; heart >= 0; heart--) {
            addHeart(graphics, emptyTexture, heartRows, heartDensity, heart, heartSize);
            HeartColor heartColor = getHeartTexture(heart, normalHearts,  absorptionHearts, maxHearts, effect, lastAbsorptionHalf);

            if(heartColor == null) continue;

            if (heart == heartColor.heartAmount - 1 && heartColor.lastHeart) {
                addHeart(graphics, heartColor.halfHeartTexture, heartRows, heartDensity, heart, heartSize);
            } else {
                addHeart(graphics, heartColor.fullHeartTexture, heartRows, heartDensity, heart, heartSize);
            }
        }

        graphics.dispose();

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            ImageIO.write(healthBar, "png", byteArrayOutputStream);
            return new NativeImageBackedTexture(NativeImage.read(byteArrayOutputStream.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public record HeartColor(BufferedImage fullHeartTexture, BufferedImage halfHeartTexture, boolean lastHeart, int heartAmount) {}

    public static HeartColor getHeartTexture(int currentHeart, int totalNormalHearts, int totalAbsorptionHearts, int maxHearts, HeartType.Effect effect, boolean lastAbsorptionHalf) {
        if (currentHeart < totalNormalHearts) {
            boolean lastNormalHalf = totalNormalHearts % 2 == 1;
            return switch (effect) {
                case NONE -> new HeartColor(normalFullTexture, normalHalfTexture, lastNormalHalf, totalNormalHearts);
                case POISON -> new HeartColor(poisonFullTexture, poisonHalfTexture, lastNormalHalf, totalNormalHearts);
                case WITHER -> new HeartColor(witherFullTexture, witherHalfTexture, lastNormalHalf, totalNormalHearts);
                case FROZEN -> new HeartColor(frozenFullTexture, frozenHalfTexture, lastNormalHalf, totalNormalHearts);
                default -> null;
            };
        } else if (currentHeart >= maxHearts) {
            return new HeartColor(absorbingFullTexture, absorbingHalfTexture, lastAbsorptionHalf, totalAbsorptionHearts);
        }
        return null;
    }

    private static void addHeart(Graphics graphics, Image image, int heartRows, int heartDensity, int heart, int heartSize) {
        graphics.drawImage(image, (heart % heartsPerRow) * (heartSize -1), (heartRows - (heart / heartsPerRow) - 1) * heartDensity, heartSize, heartSize, null);
    }
}
