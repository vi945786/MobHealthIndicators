package net.vi.mobhealthindicator.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class DefaultRenderer {

    public static void draw(MatrixStack matrixStack, NativeImageBackedTexture texture) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        NativeImage image = texture.getImage();
        assert image != null;
        drawHeart(matrixStack.peek().getPositionMatrix(), bufferBuilder, image.getWidth() / 2f, image.getHeight(), image.getWidth(), image.getHeight(), texture.getGlId());
        tessellator.draw();
    }

    private static void drawHeart(Matrix4f matrix4f, VertexConsumer bufferBuilder, float x, float y, float xOffset, float yOffset, int textureId) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.enableDepthTest();

        drawVertex(matrix4f, bufferBuilder, x - xOffset, y -yOffset, 0F, 1F);
        drawVertex(matrix4f, bufferBuilder, x, y - yOffset, 1F, 1F);
        drawVertex(matrix4f, bufferBuilder, x, y, 1F, 0F);
        drawVertex(matrix4f, bufferBuilder, x - xOffset, y, 0F, 0F);
    }

    private static void drawVertex(Matrix4f model, VertexConsumer vertices, float x, float y, float u, float v) {
        vertices.vertex(model, x, y, 0).texture(u, v).next();
    }
}
