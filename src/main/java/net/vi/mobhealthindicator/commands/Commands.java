package net.vi.mobhealthindicator.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.vi.mobhealthindicator.MobHealthIndicator;
import net.vi.mobhealthindicator.config.ModConfig;

import java.util.Arrays;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.vi.mobhealthindicator.MobHealthIndicator.*;

public class Commands {

    @Environment(EnvType.CLIENT)
    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, commandRegistryAccess) ->
            dispatcher.register(literal(MobHealthIndicator.modId)
                .executes(context -> {
                    MinecraftClient client = context.getSource().getClient();
                    Screen configScreen = AutoConfig.getConfigScreen(ModConfig.class, client.currentScreen).get();
                    client.setScreen(configScreen);
                    return 1;
                })
                    .then(literal("showHearts").executes(context -> {
                        sendMessage(Text.literal(String.valueOf(getConfig().showHearts)));
                        return 1;
                        }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                            getConfig().showHearts = context.getArgument("value", boolean.class);
                            configHolder.save();
                            return 1;
                        }))
                    )

                    .then(literal("dynamicBrightness").executes(context -> {
                        sendMessage(Text.literal(String.valueOf(getConfig().dynamicBrightness)));
                        return 1;
                        }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                            getConfig().dynamicBrightness = context.getArgument("value", boolean.class);
                            configHolder.save();
                            return 1;
                        }))
                    )

                    .then(literal("filteringMechanism").executes(context -> {
                        sendMessage(Text.literal(String.valueOf(getConfig().filteringMechanism)));
                        return 1;
                        }).then(argument("value", WhiteOrBlackListArgumentType.whiteOrBlackList()).executes(context -> {
                            getConfig().filteringMechanism = context.getArgument("value", ModConfig.WhiteOrBlackList.class);
                            configHolder.save();
                            return 1;
                        }))
                    )

                    .then(literal("blackList").executes(context -> {
                        sendMessage(Text.literal(Arrays.toString(getConfig().blackList)));
                        return 1;
                        }).then(literal("add").then(argument("value", StringArgumentType.greedyString()).executes(context -> {
                            ModConfig config = getConfig();
                            config.blackList = Arrays.copyOf(config.blackList, config.blackList.length +1);
                            config.blackList[config.blackList.length -1] =  context.getArgument("value", String.class);
                            configHolder.save();
                            return 1;
                        })))

                        .then(literal("remove").then(argument("value", SpecificStringArgumentType.specificString(() -> getConfig().blackList)).executes(context -> {
                            ModConfig config = getConfig();
                            config.blackList = Arrays.copyOf(config.blackList, config.blackList.length -1);
                            configHolder.save();
                            return 1;
                        })))
                    )

                    .then(literal("whiteList").executes(context -> {
                        sendMessage(Text.literal(Arrays.toString(getConfig().whiteList)));
                        return 1;
                        }).then(literal("add").then(argument("value", StringArgumentType.greedyString()).executes(context -> {
                            ModConfig config = getConfig();
                            config.whiteList = Arrays.copyOf(config.whiteList, config.whiteList.length +1);
                            config.whiteList[config.whiteList.length -1] =  context.getArgument("value", String.class);
                            configHolder.save();
                            return 1;
                        })))

                        .then(literal("remove").then(argument("value", SpecificStringArgumentType.specificString(() -> getConfig().whiteList)).executes(context -> {
                            ModConfig config = getConfig();
                            config.whiteList = Arrays.copyOf(config.whiteList, config.whiteList.length -1);
                            configHolder.save();
                            return 1;
                        })))
                    )
            )
        );
    }

    private static void sendMessage(Text message) {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(message);
    }
}
