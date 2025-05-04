package net.vi.mobhealthindicators.render.draw;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Util;
import net.vi.mobhealthindicators.render.Renderer;
import org.joml.Matrix4f;

import java.util.function.Function;

import static net.minecraft.client.render.RenderPhase.*;
import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;

public class DynamicBrightnessRenderer extends Renderer {
    public static final DynamicBrightnessRenderer INSTANCE = new DynamicBrightnessRenderer();
    private static final Function<AbstractTexture, RenderLayer.MultiPhase> renderLayerFactory = Util.memoize(abstractTexture -> {
            RenderLayer.MultiPhaseParameters multiPhase = RenderLayer.MultiPhaseParameters.builder().texture(new AbstractRenderLayerTexture(abstractTexture)).lightmap(ENABLE_LIGHTMAP).build(false);
            return RenderLayer.of("health_indicators_dynamic_brightness", 256, RenderPipelines.ENTITY_CUTOUT, multiPhase);
    });

    public void draw(MatrixStack matrixStack, NativeImageBackedTexture texture, int light) {
        RenderLayer renderLayer = renderLayerFactory.apply(texture);
        BufferBuilder bufferBuilder = new BufferBuilder(new BufferAllocator(256), VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        NativeImage image = texture.getImage();
        drawHeart(matrixStack.peek().getPositionMatrix(), bufferBuilder, image.getWidth() / 2f, image.getHeight() -heartSize, image.getWidth(), image.getHeight(), light);

        renderLayer.draw(bufferBuilder.end());
    }

    private static void drawHeart(Matrix4f matrix4f, BufferBuilder bufferBuilder, float x, float y, float xOffset, float yOffset, int light) {
        drawVertex(matrix4f, bufferBuilder, x - xOffset, y -yOffset, 0F, 1F, light);
        drawVertex(matrix4f, bufferBuilder, x, y - yOffset, 1F, 1F, light);
        drawVertex(matrix4f, bufferBuilder, x, y, 1F, 0F, light);
        drawVertex(matrix4f, bufferBuilder, x - xOffset, y, 0F, 0F, light);
    }

    private static void drawVertex(Matrix4f model, BufferBuilder bufferBuilder, float x, float y, float u, float v, int light) {
        bufferBuilder.vertex(model, x, y, 0).color(1F, 1F, 1F, 1F).texture(u, v).overlay(0, 10).light(light).normal(x, 0, 0);
    }
}
