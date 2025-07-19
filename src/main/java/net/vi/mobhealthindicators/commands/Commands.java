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
import net.vi.mobhealthindicators.config.screen.ConfigScreenHandler;
import net.vi.mobhealthindicators.config.Config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.HashSet;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.vi.mobhealthindicators.config.Config.*;

public class Commands {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Command {}

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
        Config.ToggleableEntityList list = config.getName(name);
        argumentBuilder.then(literal(name).executes(context -> {
            sendMessage(Text.literal(name + " is currently " + (list.toggle ? "enabled" : "disabled") + " with entities: " + list.entityList));
            return 1;
            }).then(literal("set").then(argument("value", BoolArgumentType.bool()).executes(context -> {
                list.toggle = context.getArgument("value", boolean.class);
                Config.save();
                sendMessage(Text.literal("toggled blackList to " + list.toggle));
                return 1;
            })))

            .then(literal("add").then(argument("value", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENTITY_TYPE)).executes(context -> {
                String value = ((RegistryKey<EntityType<?>>) context.getArgument("value", RegistryEntry.Reference.class).getKey().get()).getValue().toString();

                list.entityList.add(value);
                Config.save();
                sendMessage(Text.literal(value + " added to blackList"));
                return 1;
            })))

            .then(literal("remove").then(argument("value", SpecificStringArgumentType.specificString(() -> new HashSet<>(list.entityList))).executes(context -> {
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
                client.send(() -> client.setScreen(ConfigScreenHandler.getConfigScreen(client.currentScreen)));
                return 1;
            }));

            for(Field f : Config.class.getFields()) {
                if(!f.isAnnotationPresent(Command.class)) continue;

                String name = f.getName();
                Class<?> type = f.getType();

                if(type == int.class) {
                    registerSubCommandOfInt(builder, name, config.getStatic(name + "Max"), config.getStatic(name + "Min"));
                } else if(type == boolean.class) {
                    registerSubCommandOfBool(builder, name);
                } else if(type == Config.ToggleableEntityList.class) {
                    registerSubCommandOfToggleableEntityList(builder, registryAccess, name);
                }
            }

            dispatcher.register(builder);
        });
    }

    private static void sendMessage(Text message) {
        assert MinecraftClient.getInstance().player != null;
        MinecraftClient.getInstance().player.sendMessage(message, false);
    }
}
