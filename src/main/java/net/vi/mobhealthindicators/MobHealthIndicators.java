package net.vi.mobhealthindicators;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.vi.mobhealthindicators.commands.Commands;
import net.vi.mobhealthindicators.config.Config;

import static net.vi.mobhealthindicators.config.Config.config;

public class MobHealthIndicators implements ClientModInitializer {

    public static final String modId = "mobhealthindicators";
    public static boolean areShadersEnabled;
    public static Entity targetedEntity = null;

    public static final KeyMapping.Category category = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(modId, "name"));
    public static final KeyMapping toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key." + modId + ".toggle",
            InputConstants.UNKNOWN.getValue(),
            category
    ));
    public static final KeyMapping overrideFiltersKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key." + modId + ".overridefilters",
            InputConstants.UNKNOWN.getValue(),
            category
    ));


    @Override
    @SuppressWarnings("ConstantConditions")
    public void onInitializeClient() {
        Config.load();
        Commands.registerCommands();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EntityTypeToEntity.update();
            areShadersEnabled = FabricLoader.getInstance().isModLoaded("iris") && net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse();
            while (toggleKey.consumeClick()) {
                config.showHearts = !config.showHearts;
                sendMessage((config.showHearts ? "enabled" : "disabled") + "rendering");
                Config.save();
            }

            if(config.infiniteHoverRange) {
                if (client.getCameraEntity() != null) {
                    HitResult hitResult = client.gameRenderer.pick(client.getCameraEntity(), 10000, 10000, client.getFrameTimeNs());

                    if (hitResult instanceof EntityHitResult entityHitResult) {
                        targetedEntity = entityHitResult.getEntity();
                    } else {
                        targetedEntity = null;
                    }
                }
            } else {
                targetedEntity = client.crosshairPickEntity;
            }
        });
    }

    public static void sendMessage(String message, Object... args) {
        if(Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.translatable("message." + modId + "." + message, args), true);
        }
    }
}