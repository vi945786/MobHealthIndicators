package net.vi.mobhealthindicators;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.vi.mobhealthindicators.config.Config;

public class ModInit {

    public static Minecraft client;

    public static final String modId = "mobhealthindicators";
    public static boolean isIrisLoaded;
    public static boolean areShadersEnabled;
    public static Entity targetedEntity = null;

    private static final KeyMapping.Category category = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath(modId, "name"));
    public final static KeyMapping toggleKey = new KeyMapping(
            "key." +modId + ".toggle",
            InputConstants.UNKNOWN.getValue(),
            category
    );
    public final static KeyMapping overrideFiltersKey = new KeyMapping(
            "key." + modId + ".overridefilters",
            InputConstants.UNKNOWN.getValue(),
            category
    );

    public static void updateAreShadersEnabled() {
        ModInit.areShadersEnabled = isIrisLoaded && net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse();
    }

    public static void init() {
        Platform platform = Platform.getInstance();
        Config.load(platform);
        isIrisLoaded = platform.isModLoaded("iris");

        client = Minecraft.getInstance();
    }

    public static void sendMessage(String message, ChatFormatting... style) {
        if(client.player != null) {
            client.player.displayClientMessage(Component.translatable("message." + modId + "." + message).withStyle(style), true);
        }
    }
}
