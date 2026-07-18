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
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.vi.mobhealthindicators.render.Renderer;
import org.joml.Matrix4f;
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
            GraphicsResourceAllocator graphicsResourceAllocator,
            DeltaTracker deltaTracker,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f worldModelViewMatrix,
            Matrix4f projectionMatrix,
            Matrix4f cullingProjectionMatrix,
            GpuBufferSlice shaderFog,
            Vector4f fogColor,
            boolean renderSky,
            CallbackInfo ci
    ) {
        Renderer.beginFrame(camera);
    }

    @WrapOperation(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;addLateDebugPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Matrix4f;)V"
            )
    )
    private void mobhealthindicators$insertHealthBarPass(
            LevelRenderer levelRenderer,
            FrameGraphBuilder frameGraphBuilder,
            CameraRenderState cameraRenderState,
            GpuBufferSlice shaderFog,
            Matrix4f worldModelViewMatrix,
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
                Renderer.endFrame();
            }
        });

        original.call(levelRenderer, frameGraphBuilder, cameraRenderState, shaderFog, worldModelViewMatrix);
    }
}
