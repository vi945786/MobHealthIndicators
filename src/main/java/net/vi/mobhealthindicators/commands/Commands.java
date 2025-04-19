package net.vi.mobhealthindicators.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.vi.mobhealthindicators.config.ConfigScreen;
import net.vi.mobhealthindicators.config.Config;

import java.util.Set;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.config.Config.heightRange;

public class Commands {

    @Environment(EnvType.CLIENT)
    public static void registerCommands() {
        registerSubCommands("MobHealthIndicators");
        registerSubCommands("mhi");
    }

    private static void registerSubCommands(String mainCommand) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(literal(mainCommand).executes(context -> {
                sendMessage(Text.literal(config.toString()));
                return 1;
            })
                .then(literal("config").executes(context -> {
                    MinecraftClient client = context.getSource().getClient();
                    client.send(() -> client.setScreen(ConfigScreen.getConfigScreen(client.currentScreen)));
                    return 1;
                }))

                .then(literal("showHearts").executes(context -> {
                    sendMessage(Text.literal(String.valueOf(config.showHearts)));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.showHearts = context.getArgument("value", boolean.class);
                        Config.save();
                        return 1;
                    }))
                )

                .then(literal("dynamicBrightness").executes(context -> {
                    sendMessage(Text.literal(String.valueOf(config.dynamicBrightness)));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.dynamicBrightness = context.getArgument("value", boolean.class);
                        Config.save();
                        return 1;
                    }))
                )

                .then(literal("height").executes(context -> {
                    sendMessage(Text.literal(String.valueOf(config.height)));
                    return 1;
                    }).then(argument("value", IntegerArgumentType.integer(-heightRange, heightRange)).executes(context -> {
                        config.height = context.getArgument("value", int.class);
                        Config.save();
                        return 1;
                    }))
                )

                .then(literal("blackList").executes(context -> {
                    sendMessage(Text.literal(config.blackList.toString()));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.blackList.toggle = context.getArgument("value", boolean.class);
                        Config.save();
                        return 1;
                    }))

                    .then(literal("add").then(argument("value", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE)).executes(context -> {
                        String value = ((RegistryKey<EntityType<?>>) context.getArgument("value", RegistryEntry.Reference.class).getKey().get()).getValue().toString();

                        if(config.blackList.entityList.contains(value)) return 1;

                        config.blackList.entityList.add(value);
                        Config.save();
                        return 1;
                    })))

                    .then(literal("remove").then(argument("value", SpecificStringArgumentType.specificString(() -> config.blackList.entityList)).executes(context -> {
                        String value = context.getArgument("value", String.class);

                        config.blackList.entityList.remove(value);
                        Config.save();
                        return 1;
                    })))
                )

                .then(literal("whiteList").executes(context -> {
                    sendMessage(Text.literal(config.whiteList.toString()));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.whiteList.toggle = context.getArgument("value", boolean.class);
                        Config.save();
                        return 1;
                    }))

                    .then(literal("add").then(argument("value", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE)).executes(context -> {
                        String value = ((RegistryKey<EntityType<?>>) context.getArgument("value", RegistryEntry.Reference.class).getKey().get()).getValue().toString();

                        if(config.whiteList.entityList.contains(value)) return 1;

                        config.whiteList.entityList.add(value);
                        Config.save();
                        return 1;
                    })))

                    .then(literal("remove").then(argument("value", SpecificStringArgumentType.specificString(() -> config.whiteList.entityList)).executes(context -> {
                        String value = context.getArgument("value", String.class);

                        config.whiteList.entityList.remove(value);
                        Config.save();
                        return 1;
                    })))
                )

                .then(literal("showHostile").executes(context -> {
                    sendMessage(Text.literal(String.valueOf(config.showHostile)));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.showHostile = context.getArgument("value", boolean.class);
                        Config.save();
                        return 1;
                    }))
                )

                .then(literal("showPassive").executes(context -> {
                    sendMessage(Text.literal(String.valueOf(config.showPassive)));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.showPassive = context.getArgument("value", boolean.class);
                        Config.save();
                        return 1;
                    }))
                )

                .then(literal("showSelf").executes(context -> {
                    sendMessage(Text.literal(String.valueOf(config.showSelf)));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.showSelf = context.getArgument("value", boolean.class);
                        Config.save();
                        return 1;
                    }))
                )

                .then(literal("onlyShowDamaged").executes(context -> {
                    sendMessage(Text.literal(String.valueOf(config.onlyShowDamaged)));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.onlyShowDamaged = context.getArgument("value", boolean.class);
                        Config.save();
                        return 1;
                    }))
                )
            )
        );
    }

    private static void sendMessage(Text message) {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(message, false);
    }
}
