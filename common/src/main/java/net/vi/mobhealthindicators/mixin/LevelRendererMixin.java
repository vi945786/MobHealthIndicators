package net.vi.mobhealthindicators.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.vi.mobhealthindicators.render.Renderer;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
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
     * GameRenderer switches to its hand/HUD projection after this method returns.
     * Capture the level projection, fog and model-view matrix so the late on-top
     * overlay can temporarily restore the exact world transform.
     */
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void mobhealthindicators$captureWorldRenderState(
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
        Renderer.captureWorldRenderState(modelViewMatrix, terrainFog);
    }
}
