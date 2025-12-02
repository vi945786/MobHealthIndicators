package net.vi.mobhealthindicators.mixin;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Invoker("pick")
    HitResult invokePick(Entity entity, double blockInteractionRange, double entityInteractionRange, float partialTick);
}
