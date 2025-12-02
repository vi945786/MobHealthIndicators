package net.vi.mobhealthindicators.mixin;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.vi.mobhealthindicators.render.HeartType;
import net.vi.mobhealthindicators.render.Renderer;
import net.vi.mobhealthindicators.render.TextureBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.WeakHashMap;

import static net.vi.mobhealthindicators.MobHealthIndicators.targetedEntity;
import static net.vi.mobhealthindicators.config.Config.config;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Shadow protected abstract boolean shouldShowName(Entity livingEntity, double d);

    @Unique private static WeakHashMap<EntityRenderState, Entity> entities = new WeakHashMap<>();
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    public void getAndUpdateRenderState(Entity entity, EntityRenderState reusedState, float partialTick, CallbackInfo ci) {
        entities.put(reusedState, entity);
    }

    @Inject(method = "submit", at = @At("TAIL"))
    public void renderHealth(EntityRenderState entityRenderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        Entity entity = entities.get(entityRenderState);

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        if (!(entity instanceof LivingEntity livingEntity) || !config.shouldRender(livingEntity, targetedEntity) || player == null || player.getVehicle() == livingEntity || livingEntity.isInvisibleTo(player) || ((minecraft.screen instanceof InventoryScreen || minecraft.screen instanceof CreativeModeInventoryScreen) && livingEntity == player)) {
            return;
        }

        int normalHealth = Mth.ceil(livingEntity.getHealth());
        int maxHealth = Mth.ceil(livingEntity.getMaxHealth());
        int absorptionHealth = Mth.ceil(livingEntity.getAbsorptionAmount());
        HeartType.Effect effect = HeartType.Effect.getEffect(livingEntity);

        double d = dispatcher.distanceToSqr(livingEntity);
        Renderer.render(poseStack, livingEntity, TextureBuilder.getTexture(normalHealth, maxHealth, absorptionHealth, effect), dispatcher.getPackedLightCoords(entity, minecraft.getFrameTimeNs()), d, this.shouldShowName(livingEntity, d), dispatcher);
    }
}