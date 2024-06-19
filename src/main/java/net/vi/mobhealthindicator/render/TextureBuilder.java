package net.vi.mobhealthindicator.render;

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

    public static BufferedImage EmptyTexture;
    public static BufferedImage RedFullTexture;
    public static BufferedImage RedHalfTexture;
    public static BufferedImage YellowFullTexture;
    public static BufferedImage YellowHalfTexture;

    public static final Map<String, NativeImageBackedTexture> textures = new HashMap<>();
    private static final int heartsPerRow = 10;

    public static NativeImageBackedTexture getTexture(int redHealth, int maxHealth, int yellowHealth) {

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
            return new NativeImageBackedTexture(NativeImage.read(byteArrayOutputStream.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addHeart(Graphics graphics, Image image, int heartRows, int heartDensity, int heart) {
        graphics.drawImage(image, (heart % heartsPerRow) * 8, (heartRows - (heart / heartsPerRow) - 1) * heartDensity, 9, 9, null);
    }
}
