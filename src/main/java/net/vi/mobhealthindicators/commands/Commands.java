package net.vi.mobhealthindicators.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
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

public class Commands {

    @Environment(EnvType.CLIENT)
    public static void registerCommands() {
        registerSubCommands("MobHealthIndicators");
        registerSubCommands("moi");
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

                .then(literal("filteringMechanism").executes(context -> {
                    sendMessage(Text.literal(String.valueOf(config.filteringMechanism)));
                    return 1;
                    }).then(argument("value", WhiteOrBlackListArgumentType.whiteOrBlackList()).executes(context -> {
                        config.filteringMechanism = context.getArgument("value", Config.FilteringMechanism.class);
                        Config.save();
                        return 1;
                    }))
                )

                .then(literal("blackList").executes(context -> {
                    sendMessage(Text.literal(config.blackList.toString()));
                    return 1;
                    }).then(literal("add").then(argument("value", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE)).executes(context -> {
                        String value = ((RegistryKey<EntityType<?>>) context.getArgument("value", RegistryEntry.Reference.class).getKey().get()).getValue().toString();

                        if(config.blackList.contains(value)) return 1;

                        config.blackList.add(value);
                        Config.save();
                        return 1;
                    })))

                    .then(literal("remove").then(argument("value", SpecificStringArgumentType.specificString(() -> config.blackList)).executes(context -> {
                        String value = context.getArgument("value", String.class);

                        config.blackList.remove(value);
                        Config.save();
                        return 1;
                    })))
                )

                .then(literal("whiteList").executes(context -> {
                    sendMessage(Text.literal(config.whiteList.toString()));
                    return 1;
                    }).then(literal("add").then(argument("value", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE)).executes(context -> {
                        String value = ((RegistryKey<EntityType<?>>) context.getArgument("value", RegistryEntry.Reference.class).getKey().get()).getValue().toString();

                        if(config.whiteList.contains(value)) return 1;

                        config.whiteList.add(value);
                        Config.save();
                        return 1;
                    })))

                    .then(literal("remove").then(argument("value", SpecificStringArgumentType.specificString(() -> config.whiteList)).executes(context -> {
                        String value = context.getArgument("value", String.class);

                        config.whiteList.remove(value);
                        Config.save();
                        return 1;
                    })))
                )
            )
        );
    }

    private static void sendMessage(Text message) {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(message, false);
    }
}
