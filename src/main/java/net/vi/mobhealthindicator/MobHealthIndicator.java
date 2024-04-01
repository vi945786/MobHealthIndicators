package net.vi.mobhealthindicator;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class MobHealthIndicator implements ModInitializer {

    public static final String modId = "mobhealthindicator";

    public static final KeyBinding renderKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key." + modId + ".togglerendering",
            InputUtil.GLFW_KEY_PERIOD,
            "key.categories." + modId
    ));

    public static boolean renderEnabled = true;

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (renderKey.wasPressed()) {
                renderEnabled = !renderEnabled;
                sendMessage((renderEnabled ? "enabled" : "disabled") + "rendering", client);
            }
        });
    }

    private void sendMessage(String message, MinecraftClient client, Object... args) {
        if(client.player != null) {
            client.player.sendMessage(Text.translatable(modId + "." + message, args), true);
        }
    }
}