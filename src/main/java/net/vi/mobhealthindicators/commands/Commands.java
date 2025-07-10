package net.vi.mobhealthindicators.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
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
import static net.vi.mobhealthindicators.config.Config.*;

public class Commands {

    @Environment(EnvType.CLIENT)
    public static void registerCommands() {
        registerSubCommands("MobHealthIndicators");
        registerSubCommands("mhi");
    }

    private static void registerSubCommandOfBool(ArgumentBuilder argumentBuilder, String name) {
        argumentBuilder.then(literal(name).executes(context -> {
            sendMessage(Text.literal(name + " is currently set to: " + config.getName(name)));
            return 1;
            }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                config.setName(name, context.getArgument("value", boolean.class));
                Config.save();
                sendMessage(Text.literal("set " + name + " to " + config.getName(name)));
                return 1;
            }))
        );
    }

    private static void registerSubCommandOfInt(ArgumentBuilder argumentBuilder, String name, int max, int min) {
        argumentBuilder.then(literal(name).executes(context -> {
            sendMessage(Text.literal(name + " is currently set to: " + config.getName(name)));
            return 1;
            }).then(argument("value", IntegerArgumentType.integer(min, max)).executes(context -> {
                config.setName(name, context.getArgument("value", int.class));
                Config.save();
                sendMessage(Text.literal("set " + name + " to " + config.getName(name)));
                return 1;
            }))
        );
    }

    private static void registerSubCommandOfToggleableEntityList(ArgumentBuilder argumentBuilder, CommandRegistryAccess registryAccess, String name) {
        Config.ToggleableEntityList list = (Config.ToggleableEntityList) config.getName(name);
        argumentBuilder.then(literal(name).executes(context -> {
            sendMessage(Text.literal(name + " is currently toggled to: " + list.toggle));
            return 1;
            }).then(literal("set").executes(context -> {
                sendMessage(Text.literal(name + " is currently " + (list.toggle ? "enabled" : "disabled") + " with entities: " + list.entityList));
                return 1;
                }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                    list.toggle = context.getArgument("value", boolean.class);
                    Config.save();
                    sendMessage(Text.literal("toggled blackList to " + list));
                    return 1;
                })))

            .then(literal("add").then(argument("value", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE)).executes(context -> {
                String value = ((RegistryKey<EntityType<?>>) context.getArgument("value", RegistryEntry.Reference.class).getKey().get()).getValue().toString();

                list.entityList.add(value);
                Config.save();
                sendMessage(Text.literal(value + " added to blackList"));
                return 1;
            })))

            .then(literal("remove").then(argument("value", SpecificStringArgumentType.specificString(() -> list.entityList)).executes(context -> {
                String value = context.getArgument("value", String.class);

                list.entityList.remove(value);
                Config.save();
                sendMessage(Text.literal(value + " removed from blackList"));
                return 1;
            })))
        );
    }

    private static void registerSubCommands(String mainCommand) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder builder = literal(mainCommand).executes(context -> {
                sendMessage(Text.literal(config.toString()));
                return 1;
            });

            builder.then(literal("config").executes(context -> {
                MinecraftClient client = context.getSource().getClient();
                client.send(() -> client.setScreen(ConfigScreen.getConfigScreen(client.currentScreen)));
                return 1;
            }));

            registerSubCommandOfBool(builder, "showHearts");
            registerSubCommandOfBool(builder, "dynamicBrightness");
            registerSubCommandOfInt(builder, "height", heightRange, -heightRange);
            registerSubCommandOfBool(builder, "renderOnTopOnHover");
            registerSubCommandOfToggleableEntityList(builder, registryAccess, "blackList");
            registerSubCommandOfToggleableEntityList(builder, registryAccess, "whiteList");
            registerSubCommandOfBool(builder, "showHostile");
            registerSubCommandOfBool(builder, "showPassive");
            registerSubCommandOfBool(builder, "showSelf");
            registerSubCommandOfBool(builder, "onlyShowDamaged");
            registerSubCommandOfBool(builder, "onlyShowOnHover");

            dispatcher.register(builder);
        });
    }

    private static void sendMessage(Text message) {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(message, false);
    }
}
