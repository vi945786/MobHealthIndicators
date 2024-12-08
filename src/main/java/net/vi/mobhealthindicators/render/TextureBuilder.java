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
    public static BufferedImage redFullTexture;
    public static BufferedImage redHalfTexture;
    public static BufferedImage yellowFullTexture;
    public static BufferedImage yellowHalfTexture;

    public static int heartSize;

    public static final Map<String, NativeImageBackedTexture> textures = new HashMap<>();
    private static final int heartsPerRow = 10;

    public static NativeImageBackedTexture getTexture(int redHealth, int maxHealth, int yellowHealth) {

        int redHearts = MathHelper.ceil(redHealth / 2.0F);
        boolean lastRedHalf = (redHealth & 1) == 1;
        int normalHearts = MathHelper.ceil(maxHealth / 2.0F);
        int yellowHearts = MathHelper.ceil(yellowHealth / 2.0F);
        boolean lastYellowHalf = (yellowHealth & 1) == 1;
        int totalHearts = normalHearts + yellowHearts;
        int heartRows = (int) Math.ceil(totalHearts / (float) heartsPerRow);

        int heartDensity = Math.max(heartsPerRow - (heartRows - 2), 3);
        int yPixelsTotal = (heartRows - 1) * heartDensity + heartSize;

        int xPixelsTotal = Math.min(totalHearts, heartsPerRow) * (heartSize -1) + 1;

        BufferedImage healthBar = new BufferedImage(xPixelsTotal, yPixelsTotal, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = healthBar.getGraphics();

        for (int heart = totalHearts - 1; heart >= 0; heart--) {

            addHeart(graphics, emptyTexture, heartRows, heartDensity, heart, heartSize);

            if (heart < redHearts) {
                if (heart == redHearts - 1 && lastRedHalf) {
                    addHeart(graphics, redHalfTexture, heartRows, heartDensity, heart, heartSize);
                } else {
                    addHeart(graphics, redFullTexture, heartRows, heartDensity, heart, heartSize);
                }
            } else if (heart >= normalHearts) {
                if (heart == totalHearts - 1 && lastYellowHalf) {
                    addHeart(graphics, yellowHalfTexture, heartRows, heartDensity, heart, heartSize);
                } else {
                    addHeart(graphics, yellowFullTexture, heartRows, heartDensity, heart, heartSize);
                }
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

    private static void addHeart(Graphics graphics, Image image, int heartRows, int heartDensity, int heart, int heartSize) {
        graphics.drawImage(image, (heart % heartsPerRow) * (heartSize -1), (heartRows - (heart / heartsPerRow) - 1) * heartDensity, heartSize, heartSize, null);
    }
}
