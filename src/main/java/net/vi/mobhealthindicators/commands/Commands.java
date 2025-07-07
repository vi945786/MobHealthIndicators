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
                    sendMessage(Text.literal("showHearts is currently set to: " + config.showHearts));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.showHearts = context.getArgument("value", boolean.class);
                        Config.save();
                        sendMessage(Text.literal("set showHearts to " + config.showHearts));
                        return 1;
                    }))
                )

                .then(literal("dynamicBrightness").executes(context -> {
                    sendMessage(Text.literal("dynamicBrightness is currently set to: " + config.dynamicBrightness));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.dynamicBrightness = context.getArgument("value", boolean.class);
                        Config.save();
                        sendMessage(Text.literal("set dynamicBrightness to " + config.dynamicBrightness));
                        return 1;
                    }))
                )

                .then(literal("height").executes(context -> {
                    sendMessage(Text.literal("height is currently set to: " + config.height));
                    return 1;
                    }).then(argument("value", IntegerArgumentType.integer(-heightRange, heightRange)).executes(context -> {
                        config.height = context.getArgument("value", int.class);
                        Config.save();
                        sendMessage(Text.literal("set height to " + config.height));
                        return 1;
                    }))
                )

                .then(literal("renderOnTopOnHover").executes(context -> {
                    sendMessage(Text.literal("renderOnTopOnHover is currently set to: " + config.renderOnTopOnHover));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.renderOnTopOnHover = context.getArgument("value", boolean.class);
                        Config.save();
                        sendMessage(Text.literal("set renderOnTopOnHover to " + config.renderOnTopOnHover));
                        return 1;
                    }))
                )

                .then(literal("blackList").executes(context -> {
                    sendMessage(Text.literal("blackList is currently " + (config.blackList.toggle ? "enabled" : "disabled") + " with entities: " + config.blackList.entityList));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.blackList.toggle = context.getArgument("value", boolean.class);
                        Config.save();
                        sendMessage(Text.literal("toggled blackList to " + config.blackList.toggle));
                        return 1;
                    }))

                    .then(literal("add").then(argument("value", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE)).executes(context -> {
                        String value = ((RegistryKey<EntityType<?>>) context.getArgument("value", RegistryEntry.Reference.class).getKey().get()).getValue().toString();

                        config.blackList.entityList.add(value);
                        Config.save();
                        sendMessage(Text.literal(value + " added to blackList"));
                        return 1;
                    })))

                    .then(literal("remove").then(argument("value", SpecificStringArgumentType.specificString(() -> config.blackList.entityList)).executes(context -> {
                        String value = context.getArgument("value", String.class);

                        config.blackList.entityList.remove(value);
                        Config.save();
                        sendMessage(Text.literal(value + " removed from blackList"));
                        return 1;
                    })))
                )

                .then(literal("whiteList").executes(context -> {
                    sendMessage(Text.literal("whiteList is currently " + (config.whiteList.toggle ? "enabled" : "disabled") + " with entities: " + config.whiteList.entityList));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.whiteList.toggle = context.getArgument("value", boolean.class);
                        Config.save();
                        sendMessage(Text.literal("toggled whiteList to " + config.whiteList.toggle));
                        return 1;
                    }))

                    .then(literal("add").then(argument("value", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE)).executes(context -> {
                        String value = ((RegistryKey<EntityType<?>>) context.getArgument("value", RegistryEntry.Reference.class).getKey().get()).getValue().toString();

                        config.whiteList.entityList.add(value);
                        Config.save();
                        sendMessage(Text.literal(value + " added to whiteList"));
                        return 1;
                    })))

                    .then(literal("remove").then(argument("value", SpecificStringArgumentType.specificString(() -> config.whiteList.entityList)).executes(context -> {
                        String value = context.getArgument("value", String.class);

                        config.whiteList.entityList.remove(value);
                        Config.save();
                        sendMessage(Text.literal(value + " removed from whiteList"));
                        return 1;
                    })))
                )

                .then(literal("showHostile").executes(context -> {
                    sendMessage(Text.literal("showHostile is currently set to: " + config.showHostile));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.showHostile = context.getArgument("value", boolean.class);
                        Config.save();
                        sendMessage(Text.literal("set showHostile to " + config.showHostile));
                        return 1;
                    }))
                )

                .then(literal("showPassive").executes(context -> {
                    sendMessage(Text.literal("showPassive is currently set to: " + config.showPassive));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.showPassive = context.getArgument("value", boolean.class);
                        Config.save();
                        sendMessage(Text.literal("set showPassive to " + config.showPassive));
                        return 1;
                    }))
                )

                .then(literal("showSelf").executes(context -> {
                    sendMessage(Text.literal("showSelf is currently set to: " + config.showSelf));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.showSelf = context.getArgument("value", boolean.class);
                        Config.save();
                        sendMessage(Text.literal("set showSelf to " + config.showSelf));
                        return 1;
                    }))
                )

                .then(literal("onlyShowDamaged").executes(context -> {
                    sendMessage(Text.literal("onlyShowDamaged is currently set to: " + config.onlyShowDamaged));
                    return 1;
                    }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                        config.onlyShowDamaged = context.getArgument("value", boolean.class);
                        Config.save();
                        sendMessage(Text.literal("set onlyShowDamaged to " + config.onlyShowDamaged));
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
