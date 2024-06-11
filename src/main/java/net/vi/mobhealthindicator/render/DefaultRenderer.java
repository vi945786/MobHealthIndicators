package net.vi.mobhealthindicator.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class DefaultRenderer implements Renderer {

    private BufferBuilder vertexConsumer;
    private Tessellator tessellator;
    private MatrixStack matrixStack;
    private HeartType type;
    private Matrix4f matrix4f;

    public DefaultRenderer(MatrixStack matrixStack) {
        this.matrixStack = matrixStack;
    }

    @Override
    public void init() {
        tessellator = Tessellator.getInstance();
        vertexConsumer = tessellator.getBuffer();
    }

    @Override
    public void startRendering(HeartType type) {
        this.type = type;

        matrixStack.push();
        vertexConsumer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        matrix4f = matrixStack.peek().getPositionMatrix();
    }

    @Override
    public void render(float x) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, type.icon);
        RenderSystem.enableDepthTest();

        drawVertex(matrix4f, vertexConsumer, x, 0F - heartSize, minU, maxV);
        drawVertex(matrix4f, vertexConsumer, x - heartSize, 0F - heartSize, maxU, maxV);
        drawVertex(matrix4f, vertexConsumer, x - heartSize, 0F, maxU, minV);
        drawVertex(matrix4f, vertexConsumer, x, 0F, minU, minV);
    }

    private static void drawVertex(Matrix4f model, VertexConsumer vertices, float x, float y, float u, float v) {
        vertices.vertex(model, x, y, 0.0F).texture(u, v).next();
    }

    @Override
    public void endRendering() {
        tessellator.draw();
        matrixStack.pop();
    }
}
