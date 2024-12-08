package net.vi.mobhealthindicators.render.draw;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.vi.mobhealthindicators.render.HeartType;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;

public class DynamicBrightnessRenderer {

    public static void draw(MatrixStack matrixStack, NativeImageBackedTexture texture, int light) {
        RenderLayer.MultiPhase renderLayer = (RenderLayer.MultiPhase) RenderLayer.getEntityCutoutNoCull(HeartType.EMPTY.icon);
        renderLayer.phases.phases.getFirst().beginAction = () -> {
            RenderSystem.setShaderTexture(0, texture.getGlId());
        };

        BufferBuilder bufferBuilder = new BufferBuilder(new BufferAllocator(256), VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        NativeImage image = texture.getImage();
        assert image != null;
        drawHeart(matrixStack.peek().getPositionMatrix(), bufferBuilder, image.getWidth() / 2f, image.getHeight() -heartSize, image.getWidth(), image.getHeight(), light);

        renderLayer.startDrawing();
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
        renderLayer.endDrawing();
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
