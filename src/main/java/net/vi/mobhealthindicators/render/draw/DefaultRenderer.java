package net.vi.mobhealthindicators.render.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;

public class DefaultRenderer {

    public static void draw(MatrixStack matrixStack, NativeImageBackedTexture texture) {
        RenderLayer.MultiPhase renderLayer = (RenderLayer.MultiPhase) RenderLayer.getEntityAlpha(Identifier.of("minecraft", "textures/gui/sprites/hud/heart/container.png"));
        renderLayer.phases.phases.getFirst().beginAction = () -> RenderSystem.setShaderTexture(0, texture.getGlTexture());

        BufferBuilder bufferBuilder = new BufferBuilder(new BufferAllocator(256), VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        NativeImage image = texture.getImage();
        drawHeart(matrixStack.peek().getPositionMatrix(), bufferBuilder, image.getWidth() / 2f, image.getHeight() -heartSize, image.getWidth(), image.getHeight());

        renderLayer.draw(bufferBuilder.end());
    }

    private static void drawHeart(Matrix4f matrix4f, BufferBuilder bufferBuilder, float x, float y, float xOffset, float yOffset) {
        drawVertex(matrix4f, bufferBuilder, x - xOffset, y -yOffset, 0F, 1F);
        drawVertex(matrix4f, bufferBuilder, x, y - yOffset, 1F, 1F);
        drawVertex(matrix4f, bufferBuilder, x, y, 1F, 0F);
        drawVertex(matrix4f, bufferBuilder, x - xOffset, y, 0F, 0F);
    }

    private static void drawVertex(Matrix4f model, BufferBuilder bufferBuilder, float x, float y, float u, float v) {
        bufferBuilder.vertex(model, x, y, 0).color(1F, 1F, 1F, 1F).texture(u, v).overlay(0, 10).light(0, 0).normal(x, 0, 0);
    }
}