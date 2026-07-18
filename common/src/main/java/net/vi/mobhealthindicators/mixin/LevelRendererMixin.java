package net.vi.mobhealthindicators.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.vi.mobhealthindicators.render.Renderer;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Runs after Iris' LevelRenderer mixins so the final overlay is submitted only
 * after the shader pack has completed its composite and final passes.
 */
@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class LevelRendererMixin {

    /**
     * Minecraft 26.1 extracts entity render states before renderLevel builds the
     * frame graph, so collection must begin in the extraction stage.
     */
    @Inject(method = "extractLevel", at = @At("HEAD"))
    private void mobhealthindicators$beginHealthBarFrame(
            DeltaTracker deltaTracker,
            Camera camera,
            float deltaPartialTick,
            CallbackInfo ci
    ) {
        Renderer.beginFrame(camera);
    }

    /**
     * TAIL is after vanilla's frame graph and after Iris finalizes the level. The
     * world model-view matrix has already been popped by vanilla, so temporarily
     * restore the same matrix used by the level before drawing the overlay.
     */
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void mobhealthindicators$renderOnTopAfterLevel(
            GraphicsResourceAllocator resourceAllocator,
            DeltaTracker deltaTracker,
            boolean renderOutline,
            CameraRenderState cameraState,
            Matrix4fc modelViewMatrix,
            GpuBufferSlice terrainFog,
            Vector4f fogColor,
            boolean shouldRenderSky,
            ChunkSectionsToRender chunkSectionsToRender,
            CallbackInfo ci
    ) {
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.mul(modelViewMatrix);
        try {
            Renderer.flushOnTop();
        } finally {
            modelViewStack.popMatrix();
            Renderer.endFrame();
        }
    }
}
