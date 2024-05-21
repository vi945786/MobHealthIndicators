package net.vi.mobhealthindicator.render;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.vi.mobhealthindicator.HeartType;
import org.joml.Matrix4f;

public class DynamicBrightnessRenderer implements Renderer {

    private static class RenderLayers {
        public final RenderLayer renderLayerEmpty;
        public final RenderLayer renderLayerRedFull;
        public final RenderLayer renderLayerRedHalf;
        public final RenderLayer renderLayerYellowFull;
        public final RenderLayer renderLayerYellowHalf;

        public RenderLayers(EntityModel<?> model) {
            renderLayerEmpty = model.getLayer(HeartType.EMPTY.icon);
            renderLayerRedFull = model.getLayer(HeartType.RED_FULL.icon);
            renderLayerRedHalf = model.getLayer(HeartType.RED_HALF.icon);
            renderLayerYellowFull = model.getLayer(HeartType.YELLOW_FULL.icon);
            renderLayerYellowHalf = model.getLayer(HeartType.YELLOW_HALF.icon);
        }
    }

    private MatrixStack matrixStack;
    private VertexConsumerProvider vertexConsumerProvider;
    private EntityModel<?> model;
    private int light;
    private RenderLayers renderLayers;
    private RenderLayer renderLayer;
    private VertexConsumer bufferBuilder;
    private Matrix4f matrix4f;

    public DynamicBrightnessRenderer(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, EntityModel<?> model, int light) {
        this.matrixStack = matrixStack;
        this.vertexConsumerProvider = vertexConsumerProvider;
        this.model = model;
        this.light = light;
    }

    @Override
    public void init() {
        renderLayers = new RenderLayers(model);

        renderLayer = renderLayers.renderLayerEmpty;
        renderLayer.startDrawing();
        bufferBuilder = vertexConsumerProvider.getBuffer(renderLayer);
    }

    @Override
    public void startRendering(HeartType type) {
        if (type.equals(HeartType.EMPTY)) {
            renderLayer = renderLayers.renderLayerEmpty;
        } else if (type.equals(HeartType.RED_FULL)) {
            renderLayer = renderLayers.renderLayerRedFull;
        } else if (type.equals(HeartType.RED_HALF)) {
            renderLayer = renderLayers.renderLayerRedHalf;
        } else if (type.equals(HeartType.YELLOW_FULL)) {
            renderLayer = renderLayers.renderLayerYellowFull;
        } else if (type.equals(HeartType.YELLOW_HALF)) {
            renderLayer = renderLayers.renderLayerYellowHalf;
        }
        bufferBuilder = vertexConsumerProvider.getBuffer(renderLayer);

        matrixStack.push();
        renderLayer.startDrawing();

        matrix4f = matrixStack.peek().getPositionMatrix();
    }

    @Override
    public void render(float x) {
        drawVertex(matrix4f, bufferBuilder, x, 0F - heartSize, minU, maxV, light);
        drawVertex(matrix4f, bufferBuilder, x - heartSize, 0F - heartSize, maxU, maxV, light);
        drawVertex(matrix4f, bufferBuilder, x - heartSize, 0F, maxU, minV, light);
        drawVertex(matrix4f, bufferBuilder, x, 0F, minU, minV, light);
    }

    private static void drawVertex(Matrix4f model, VertexConsumer vertices, float x, float y, float u, float v, int light) {
        vertices.vertex(model, x, y, 0.0F).color(1F, 1F, 1F, 1F).texture(u, v).overlay(0, 10).light(light).normal(x, y, 0.0F).next();
    }

    @Override
    public void endRendering() {
        renderLayer.endDrawing();
        matrixStack.pop();
    }
}