package net.vi.mobhealthindicators.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.vi.mobhealthindicators.render.Renderer;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void mobhealthindicators$beginHealthBarFrame(
            GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline, CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog, Vector4f fogColor, boolean shouldRenderSky, ChunkSectionsToRender chunkSectionsToRender, CallbackInfo ci
    ) {
        Renderer.beginFrame(cameraState);
    }

    /**
     * Adds the health-bar pass immediately before the frame graph is executed.
     *
     * <p>The wrapped target lives in Mojang's frame-graph package instead of a
     * mapped Minecraft state package. This deliberately avoids depending on a
     * refmap for the injection descriptor, fixing NeoForge production crashes
     * where CameraRenderState is relocated to renderer/state/level.</p>
     */
    @WrapOperation(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;execute(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder$Inspector;)V"
            )
    )
    private void mobhealthindicators$executeWithHealthBarPass(
            FrameGraphBuilder frameGraphBuilder,
            GraphicsResourceAllocator graphicsResourceAllocator,
            FrameGraphBuilder.Inspector inspector,
            Operation<Void> original
    ) {
        FramePass healthBarPass = frameGraphBuilder.addPass("mobhealthindicators_health_bars");
        this.targets.main = healthBarPass.readsAndWrites(this.targets.main);
        ResourceHandle<RenderTarget> mainTarget = this.targets.main;

        healthBarPass.executes(() -> {
            RenderTarget renderTarget = mainTarget.get();
            RenderSystem.outputColorTextureOverride = renderTarget.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = renderTarget.getDepthTextureView();

            try {
                Renderer.flush();
            } finally {
                RenderSystem.outputColorTextureOverride = null;
                RenderSystem.outputDepthTextureOverride = null;
            }
        });

        try {
            original.call(frameGraphBuilder, graphicsResourceAllocator, inspector);
        } finally {
            Renderer.endFrame();
        }
    }
}
