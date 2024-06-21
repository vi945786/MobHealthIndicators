package net.vi.mobhealthindicator.commands;

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
import net.vi.mobhealthindicator.MobHealthIndicator;
import net.vi.mobhealthindicator.config.Config;

import java.util.Arrays;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.vi.mobhealthindicator.config.Config.config;

public class Commands {

    @Environment(EnvType.CLIENT)
    @SuppressWarnings("unchecked")
    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(literal("MobHealthIndicators").executes(context -> {
                sendMessage(Text.literal(config.toString()));
                return 1;
            })

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
                        config.filteringMechanism = context.getArgument("value", Config.WhiteOrBlackList.class);
                        Config.save();
                        return 1;
                    }))
                )

                .then(literal("blackList").executes(context -> {
                    sendMessage(Text.literal(Arrays.toString(config.blackList)));
                    return 1;
                    }).then(literal("add").then(argument("value", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE)).executes(context -> {
                        String value = ((RegistryKey<EntityType<?>>) context.getArgument("value", RegistryEntry.Reference.class).getKey().get()).getValue().toString();

                        if(Arrays.asList(config.blackList).contains(value)) return 1;

                        config.blackList = Arrays.copyOf(config.blackList, config.blackList.length +1);
                        config.blackList[config.blackList.length -1] = value;
                        Config.save();
                        return 1;
                    })))

                    .then(literal("remove").then(argument("value", SpecificStringArgumentType.specificString(() -> config.blackList)).executes(context -> {
                        config.blackList = Arrays.copyOf(config.blackList, config.blackList.length -1);
                        Config.save();
                        return 1;
                    })))
                )

                .then(literal("whiteList").executes(context -> {
                    sendMessage(Text.literal(Arrays.toString(config.whiteList)));
                    return 1;
                    }).then(literal("add").then(argument("value", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE)).executes(context -> {
                        String value = ((RegistryKey<EntityType<?>>) context.getArgument("value", RegistryEntry.Reference.class).getKey().get()).getValue().toString();

                        if(Arrays.asList(config.whiteList).contains(value)) return 1;

                        config.whiteList = Arrays.copyOf(config.whiteList, config.whiteList.length +1);
                        config.whiteList[config.whiteList.length -1] = value;
                        Config.save();
                        return 1;
                    })))

                    .then(literal("remove").then(argument("value", SpecificStringArgumentType.specificString(() -> config.whiteList)).executes(context -> {
                        config.whiteList = Arrays.copyOf(config.whiteList, config.whiteList.length -1);
                        Config.save();
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
