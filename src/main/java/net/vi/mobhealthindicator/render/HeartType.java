package net.vi.mobhealthindicator.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public enum HeartType {
    EMPTY("container"),
    RED_FULL("full"),
    RED_HALF("half"),
    YELLOW_FULL("absorbing_full"),
    YELLOW_HALF("absorbing_half");

    public final Identifier icon;

    HeartType(String heartIcon) {
        icon = new Identifier("minecraft", "textures/gui/sprites/hud/heart/" + heartIcon + ".png");
    }

    public BufferedImage getTexture() {
        try {
            return ImageIO.read(MinecraftClient.getInstance().getTextureManager().resourceContainer.open(icon));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}