package net.vi.mobhealthindicator.render;

import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.vi.mobhealthindicator.config.ModConfig;

public class Renderer {

    public static void render(MatrixStack matrixStack, NativeImageBackedTexture texture, ModConfig config, int light) {
        if(config.dynamicBrightness) {
            DynamicBrightnessRenderer.draw(matrixStack, texture, light);
        } else {
            DefaultRenderer.draw(matrixStack, texture);
        }
    }
}
