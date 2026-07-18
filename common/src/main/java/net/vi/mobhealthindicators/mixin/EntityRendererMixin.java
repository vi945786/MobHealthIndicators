package net.vi.mobhealthindicators.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.vi.mobhealthindicators.render.HeartType;
import net.vi.mobhealthindicators.render.Renderer;
import net.vi.mobhealthindicators.render.TextureBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.vi.mobhealthindicators.ModInit.client;
import static net.vi.mobhealthindicators.ModInit.targetedEntity;
import static net.vi.mobhealthindicators.config.Config.config;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Shadow
    public abstract Vec3 getRenderOffset(EntityRenderState renderState);

    @Inject(
            method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At("RETURN")
    )
    private void mobhealthindicators$captureHealthBar(
            Entity entity,
            float partialTick,
            CallbackInfoReturnable<EntityRenderState> cir
    ) {
        if (!Renderer.isCollecting() || config == null) return;
        if (!(entity instanceof LivingEntity livingEntity)) return;

        LocalPlayer player = client.player;
        if (player == null
                || player.getVehicle() == livingEntity
                || livingEntity.isInvisibleTo(player)
                || !config.shouldRender(livingEntity, targetedEntity)) {
            return;
        }

        int health = Mth.ceil(livingEntity.getHealth());
        int maxHealth = Mth.ceil(livingEntity.getMaxHealth());
        int absorption = Mth.ceil(livingEntity.getAbsorptionAmount());
        HeartType.Effect effect = HeartType.Effect.getEffect(livingEntity);
        Identifier texture = TextureBuilder.getTexture(health, maxHealth, absorption, effect);

        EntityRenderState renderState = cir.getReturnValue();
        Renderer.queue(
                livingEntity,
                renderState,
                this.getRenderOffset(renderState),
                texture,
                targetedEntity == livingEntity
        );
    }
}

//package net.vi.mobhealthindicators.mixin;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
//import net.minecraft.client.gui.screens.inventory.InventoryScreen;
//import net.minecraft.client.player.LocalPlayer;
//import net.minecraft.client.renderer.SubmitNodeCollector;
//import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
//import net.minecraft.client.renderer.entity.EntityRenderer;
//import net.minecraft.client.renderer.entity.state.EntityRenderState;
//import net.minecraft.client.renderer.rendertype.RenderType;
//import net.minecraft.client.renderer.rendertype.RenderTypes;
//import net.minecraft.client.renderer.state.level.CameraRenderState;
//import net.minecraft.resources.Identifier;
//import net.minecraft.util.Mth;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.LivingEntity;
//import net.vi.mobhealthindicators.render.HeartType;
//import net.vi.mobhealthindicators.render.Renderer;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.Unique;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//import java.util.WeakHashMap;
//
//import static net.vi.mobhealthindicators.ModInit.*;
//import static net.vi.mobhealthindicators.config.Config.config;
//import static net.vi.mobhealthindicators.render.Renderer.*;
//import static net.vi.mobhealthindicators.render.TextureBuilder.getTexture;
//
//@Mixin(EntityRenderer.class)
//public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
//
//    @Shadow
//    protected abstract boolean shouldShowName(Entity livingEntity, double d);
//
//    @Unique
//    private static WeakHashMap<EntityRenderState, Entity> entities = new WeakHashMap<>();
//
//    @Inject(method = "extractRenderState", at = @At("RETURN"))
//    public void getAndUpdateRenderState(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo ci) {
//        entities.put(state, entity);
//    }
//
//    @Inject(method = "submit", at = @At("TAIL"))
//    public void renderHealth(S state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
//
//        LocalPlayer player = client.player;
//
//        Entity entity = entities.get(state);
//
//        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
//        if (!(entity instanceof LivingEntity livingEntity) || !config.shouldRender(livingEntity, targetedEntity) || player == null || player.getVehicle() == livingEntity || livingEntity.isInvisibleTo(player) || ((client.screen instanceof InventoryScreen || client.screen instanceof CreativeModeInventoryScreen) && livingEntity == player)) {
//            return;
//        }
//
//        int normalHealth = Mth.ceil(livingEntity.getHealth());
//        int maxHealth = Mth.ceil(livingEntity.getMaxHealth());
//        int absorptionHealth = Mth.ceil(livingEntity.getAbsorptionAmount());
//        HeartType.Effect effect = HeartType.Effect.getEffect(livingEntity);
//
//        double d = dispatcher.distanceToSqr(livingEntity);
//
//        boolean isTargeted = livingEntity == targetedEntity && config.renderOnTopOnHover;
//        Identifier texture = getTexture(normalHealth, maxHealth, absorptionHealth, effect, isTargeted);
//        RenderType renderType = config.dynamicBrightness ? RenderTypes.entityCutout(texture) : FULL_BRIGHT_RENDER_TYPE.apply(texture);
//
////        if (isTargeted) renderType.state.outputTarget = OutputTarget.OUTLINE_TARGET;
//
//        if (isTargeted) targetedEntityRenderData = new Renderer.RenderData(poseStack.last(), renderType, livingEntity, texture, dispatcher.getPackedLightCoords(entity, client.getDeltaTracker().getGameTimeDeltaPartialTick(true)), d, this.shouldShowName(livingEntity, d), dispatcher);
//        else Renderer.render(poseStack.last(), renderType, livingEntity, texture, dispatcher.getPackedLightCoords(entity, client.getDeltaTracker().getGameTimeDeltaPartialTick(true)), d, this.shouldShowName(livingEntity, d), dispatcher);
//    }
//}
