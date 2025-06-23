package net.vi.mobhealthindicators.render.draw;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.vi.mobhealthindicators.render.Renderer;
import org.joml.Matrix4f;

import java.util.function.Function;

import static net.minecraft.client.gl.RenderPipelines.MATRICES_SNIPPET;
import static net.vi.mobhealthindicators.render.TextureBuilder.heartSize;

public class DefaultRenderer extends Renderer {
    public static final DefaultRenderer INSTANCE = new DefaultRenderer();
    private static final RenderPipeline renderPipeline = RenderPipeline.builder(MATRICES_SNIPPET)
            .withLocation(Identifier.of("mobhealthindicators", "pipeline/default_indicators"))
            .withVertexShader(Identifier.of("mobhealthindicators", "default"))
            .withFragmentShader(Identifier.of("mobhealthindicators", "default"))
            .withSampler("Sampler0")
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .withPolygonMode(PolygonMode.FILL)
            .withVertexFormat(VertexFormats.POSITION_TEXTURE, VertexFormat.DrawMode.QUADS)
            .withCull(false)
            .build();
    private static final Function<AbstractTexture, RenderLayer.MultiPhase> renderLayerFactory = Util.memoize(abstractTexture -> {
            RenderLayer.MultiPhaseParameters multiPhase = RenderLayer.MultiPhaseParameters.builder().texture(new AbstractRenderLayerTexture(abstractTexture)).build(false);
            return RenderLayer.of("health_indicators_default", 256, renderPipeline, multiPhase);
    });

    public void draw(MatrixStack matrixStack, NativeImageBackedTexture texture, int light) {
        RenderLayer renderLayer = renderLayerFactory.apply(texture);
        BufferBuilder bufferBuilder = new BufferBuilder(new BufferAllocator(1536), VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

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
        bufferBuilder.vertex(model, x, y, 0).texture(u, v);
    }
}