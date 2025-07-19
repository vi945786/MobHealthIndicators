package net.vi.mobhealthindicators;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.vi.mobhealthindicators.commands.Commands;
import net.vi.mobhealthindicators.config.Config;

import static net.vi.mobhealthindicators.config.Config.config;

public class MobHealthIndicators implements ClientModInitializer {

    public static final String modId = "mobhealthindicators";
    public static final MinecraftClient client = MinecraftClient.getInstance();
    public static boolean areShadersEnabled;
    public static Entity targetedEntity = null;

    public static final KeyBinding toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + modId + ".toggle",
            InputUtil.UNKNOWN_KEY.getCode(),
            modId + ".name"
    ));
    public static final KeyBinding overrideFiltersKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + modId + ".overridefilters",
            InputUtil.UNKNOWN_KEY.getCode(),
            modId + ".name"
    ));

    @Override
    public void onInitializeClient() {
        Config.load();
        Commands.registerCommands();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            areShadersEnabled = FabricLoader.getInstance().isModLoaded("iris") && net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse();
            while (toggleKey.wasPressed()) {
                config.showHearts = !config.showHearts;
                sendMessage((config.showHearts ? "enabled" : "disabled") + "rendering");
            }

            if(config.infiniteHoverRange) {
                if (client.cameraEntity != null) {
                    HitResult hitResult = client.gameRenderer.findCrosshairTarget(client.cameraEntity, 10000, 10000, client.getRenderTickCounter().getTickProgress(true));

                    if (hitResult instanceof EntityHitResult entityHitResult) {
                        targetedEntity = entityHitResult.getEntity();
                    } else {
                        targetedEntity = null;
                    }
                }
            } else {
                targetedEntity = client.targetedEntity;
            }
        });
    }

    public static void sendMessage(String message, Object... args) {
        if(client.player != null) {
            client.player.sendMessage(Text.translatable("message." + modId + "." + message, args), true);
        }
    }
}