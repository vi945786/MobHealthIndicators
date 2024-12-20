package net.vi.mobhealthindicators.render.draw;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;

public class DefaultRenderer {

    public static void draw(MatrixStack matrixStack, NativeImageBackedTexture texture) {
        BufferBuilder bufferBuilder = new BufferBuilder(new BufferAllocator(256), VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        NativeImage image = texture.getImage();
        assert image != null;
        drawHeart(matrixStack.peek().getPositionMatrix(), bufferBuilder, image.getWidth() / 2f, image.getHeight() -heartSize, image.getWidth(), image.getHeight(), texture.getGlId());
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    private static void drawHeart(Matrix4f matrix4f, BufferBuilder bufferBuilder, float x, float y, float xOffset, float yOffset, int textureId) {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.enableDepthTest();

        drawVertex(matrix4f, bufferBuilder, x - xOffset, y -yOffset, 0F, 1F);
        drawVertex(matrix4f, bufferBuilder, x, y - yOffset, 1F, 1F);
        drawVertex(matrix4f, bufferBuilder, x, y, 1F, 0F);
        drawVertex(matrix4f, bufferBuilder, x - xOffset, y, 0F, 0F);
    }

    private static void drawVertex(Matrix4f model, BufferBuilder bufferBuilder, float x, float y, float u, float v) {
        bufferBuilder.vertex(model, x, y, 0).texture(u, v);
    }
}
