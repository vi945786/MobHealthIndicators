package net.vi.mobhealthindicators;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.vi.mobhealthindicators.commands.RegisterCommands;
import net.vi.mobhealthindicators.config.Config;

import static net.vi.mobhealthindicators.render.Renderer.FULL_BRIGHT_INDICATORS;
import static net.vi.mobhealthindicators.render.Renderer.FULL_BRIGHT_INDICATORS_PIPELINE;

public class ModInit {

    public static Minecraft client;

    public static final String modId = "mobhealthindicators";
    public static boolean isIrisLoaded;
    public static boolean areShadersEnabled;
    public static Entity targetedEntity = null;

    public static KeyMapping.Category category;
    public static KeyMapping toggleKey;
    public static KeyMapping overrideFiltersKey;

    public static void updateAreShadersEnabled() {
        ModInit.areShadersEnabled = isIrisLoaded && net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse();
    }

    public static void init() {
        Platform platform = Platform.getInstance();

        Config.load(platform);
        RegisterCommands.registerCommands(platform);

        isIrisLoaded = platform.isModLoaded("iris");

        category = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(modId, "name"));
        toggleKey = platform.registerKeyMapping(new KeyMapping(
                "key." + modId + ".toggle",
                InputConstants.UNKNOWN.getValue(),
                category
        ));
        overrideFiltersKey = platform.registerKeyMapping(new KeyMapping(
            "key." + modId + ".overridefilters",
            InputConstants.UNKNOWN.getValue(),
            category
        ));

        FULL_BRIGHT_INDICATORS_PIPELINE = platform.getFullBrightIndicatorsPipeline();
        FULL_BRIGHT_INDICATORS = platform.getFullBrightIndicatorsRenderTypeFunction();

        client = Minecraft.getInstance();
    }

    public static void sendMessage(String message, Object... args) {
        if(client.player != null) {
            client.player.displayClientMessage(Component.translatable("message." + modId + "." + message, args), true);
        }
    }
}
