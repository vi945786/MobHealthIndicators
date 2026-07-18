package net.vi.mobhealthindicators.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.vi.mobhealthindicators.render.Renderer;
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
     * Appends only the explicitly always-on-top bars after vanilla's transparent
     * targets, particles, clouds and weather have been composited into main.
     * Ordinary bars are drawn earlier by FeatureRenderDispatcherMixin.
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
        if (Renderer.hasOnTopCommands()) {
            FramePass healthBarPass = frameGraphBuilder.addPass("mobhealthindicators_on_top_health_bars");
            this.targets.main = healthBarPass.readsAndWrites(this.targets.main);
            ResourceHandle<RenderTarget> mainTarget = this.targets.main;

            healthBarPass.executes(() -> {
                RenderTarget renderTarget = mainTarget.get();
                GpuTextureView previousColorTarget = RenderSystem.outputColorTextureOverride;
                GpuTextureView previousDepthTarget = RenderSystem.outputDepthTextureOverride;

                RenderSystem.outputColorTextureOverride = renderTarget.getColorTextureView();
                RenderSystem.outputDepthTextureOverride = renderTarget.getDepthTextureView();

                try {
                    Renderer.flushOnTop();
                } finally {
                    RenderSystem.outputColorTextureOverride = previousColorTarget;
                    RenderSystem.outputDepthTextureOverride = previousDepthTarget;
                }
            });
        }

        try {
            original.call(frameGraphBuilder, graphicsResourceAllocator, inspector);
        } finally {
            Renderer.endFrame();
        }
    }
}
