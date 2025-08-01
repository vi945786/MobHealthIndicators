package net.vi.mobhealthindicators.render;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static net.vi.mobhealthindicators.MobHealthIndicators.client;
import static net.vi.mobhealthindicators.MobHealthIndicators.modId;

public class TextureBuilder {

    public static HeartType.HeartColor emptyTexture;
    public static HeartType.HeartColor normalHeart;
    public static HeartType.HeartColor poisonHeart;
    public static HeartType.HeartColor witherHeart;
    public static HeartType.HeartColor absorptionHeart;
    public static HeartType.HeartColor frozenHeart;

    public static final int defaultHeartSize = 9;
    public static int heartSize;

    public static final Map<String, Identifier> textures = new HashMap<>();
    private static final int heartsPerRow = 10;

    public static Identifier getTexture(int normalHealth, int maxHealth, int absorptionHealth, HeartType.Effect effect) {
        String healthId = normalHealth + "_" + (maxHealth - normalHealth) + "_" + absorptionHealth + "_" + effect;
        if (textures.containsKey(healthId)) return textures.get(healthId);

        int normalHearts = MathHelper.ceil(normalHealth / 2.0F);
        int maxHearts = MathHelper.ceil(maxHealth / 2.0F);
        int absorptionHearts = MathHelper.ceil(absorptionHealth / 2.0F);
        int totalHearts = maxHearts + absorptionHearts;
        int heartRows = (int) Math.ceil(totalHearts / (float) heartsPerRow);

        boolean lastNormalHalf = normalHealth % 2 == 1;
        boolean lastAbsorptionHalf = absorptionHealth % 2 == 1;

        int heartDensity = Math.max(heartsPerRow - (heartRows - 2), 3);
        int yPixelsTotal = (heartRows - 1) * heartDensity + heartSize;

        int xPixelsTotal = Math.min(totalHearts, heartsPerRow) * (heartSize -1) + 1;

        BufferedImage healthBar = new BufferedImage(xPixelsTotal, yPixelsTotal, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = healthBar.getGraphics();

        for (int heart = totalHearts - 1; heart >= 0; heart--) {
            addHeart(graphics, emptyTexture.fullHeartTexture(), heartRows, heartDensity, heart, heartSize);
            BufferedImage heartTexture = getHeartTexture(heart, totalHearts, maxHearts, normalHearts, lastNormalHalf, lastAbsorptionHalf, effect);

            if(heartTexture == null) continue;

            addHeart(graphics, heartTexture, heartRows, heartDensity, heart, heartSize);
        }

        graphics.dispose();

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            ImageIO.write(healthBar, "png", byteArrayOutputStream);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "hearts", NativeImage.read(byteArrayOutputStream.toByteArray()));
            texture.setFilter(false, false);
            texture.upload();

            Identifier identifier = Identifier.of(modId, healthId);
            textures.put(healthId, identifier);
            client.getTextureManager().registerTexture(identifier, texture);

            return identifier;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BufferedImage getHeartTexture(int currentHeart, int totalHearts, int maxNormalHearts, int normalHearts, boolean lastNormalHalf, boolean lastAbsorptionHalf, HeartType.Effect effect) {
        HeartType.HeartColor heartColor = null;
        boolean isHalf = false;
        if (currentHeart < normalHearts) {
            isHalf = currentHeart == normalHearts -1 && lastNormalHalf;
            switch (effect) {
                case none, absorption -> heartColor = normalHeart;
                case poison -> heartColor = poisonHeart;
                case wither -> heartColor = witherHeart;
                case frozen -> heartColor = frozenHeart;
            }
        } else if (currentHeart < maxNormalHearts) {
            heartColor = emptyTexture;
        } else {
            isHalf = currentHeart == totalHearts -1 && lastAbsorptionHalf;
            heartColor = absorptionHeart;
        }
        return isHalf ? heartColor.halfHeartTexture() : heartColor.fullHeartTexture();
    }

    private static void addHeart(Graphics graphics, Image image, int heartRows, int heartDensity, int heart, int heartSize) {
        graphics.drawImage(image, (heart % heartsPerRow) * (heartSize -1), (heartRows - (heart / heartsPerRow) - 1) * heartDensity, heartSize, heartSize, null);
    }
}
