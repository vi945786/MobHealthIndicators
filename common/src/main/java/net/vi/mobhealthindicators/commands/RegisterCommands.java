package net.vi.mobhealthindicators.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.vi.mobhealthindicators.config.screen.ConfigScreenHandler;
import net.vi.mobhealthindicators.config.Config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import static net.vi.mobhealthindicators.EntityTypeToEntity.getLivingEntities;
import static net.vi.mobhealthindicators.ModInit.client;
import static net.vi.mobhealthindicators.config.Config.config;

public class RegisterCommands {

    public static <T> List<LiteralArgumentBuilder<T>> registerCommands(Function<String, LiteralArgumentBuilder<T>> builderFunction) {
        List<LiteralArgumentBuilder<T>> builders = new ArrayList<>();

        builders.add(registerSubCommands(builderFunction.apply("MobHealthIndicators")));
        builders.add(registerSubCommands(builderFunction.apply("mhi")));

        return builders;
    }

    @SuppressWarnings("unchecked")
    private static <T> void registerSubCommandOfBool(LiteralArgumentBuilder<T> builder, String name) {
        builder.then((ArgumentBuilder<T, ?>) literal(name).executes(context -> {
            sendMessage(Component.literal(name + " is currently set to: " + Config.getName(name)));
            return 1;
            }).then(argument("value", BoolArgumentType.bool()).executes(context -> {
                boolean value = context.getArgument("value", boolean.class);
                Config.setName(name, value);
                Config.save();
                sendMessage(Component.literal("set " + name + " to " + value));
                return 1;
            }))
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> void registerSubCommandOfInt(LiteralArgumentBuilder<T> builder, String name, int min, int max) {
        builder.then((ArgumentBuilder<T, ?>) literal(name).executes(context -> {
            sendMessage(Component.literal(name + " is currently set to: " + Config.getName(name)));
            return 1;
            }).then(argument("value", IntegerArgumentType.integer(min, max)).executes(context -> {
                int value = context.getArgument("value", int.class);
                Config.setName(name, value);
                Config.save();
                sendMessage(Component.literal("set " + name + " to " + value));
                return 1;
            }))
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> void registerSubCommandOfToggleableEntityList(LiteralArgumentBuilder<T> builder, String name) {
        Config.ToggleableEntityList list = Config.getName(name);
        builder.then((ArgumentBuilder<T, ?>) literal(name).executes(context -> {
            sendMessage(Component.literal(name + " is currently " + (list.toggle ? "enabled" : "disabled") + " with entities: " + list.entityList));
            return 1;
            }).then(literal("enable").executes(context -> {
                list.toggle = true;
                Config.save();
                sendMessage(Component.literal("enabled " + name));
                return 1;
            }))

            .then(literal("disable").executes(context -> {
                list.toggle = false;
                Config.save();
                sendMessage(Component.literal("disabled " + name));
                return 1;
            }))

            .then(literal("add").then(argument("value", SpecificStringArgumentType.specificString(() -> getLivingEntities().stream().map(EntityType::getKey).map(ResourceLocation::toString).collect(Collectors.toSet()))).executes(context -> {
                String value = context.getArgument("value", String.class);

                list.entityList.add(value);
                Config.save();
                sendMessage(Component.literal(value + " added to " + name));
                return 1;
            })))

            .then(literal("remove").then(argument("value", SpecificStringArgumentType.specificString(() -> new HashSet<>(list.entityList))).executes(context -> {
                String value = context.getArgument("value", String.class);

                list.entityList.remove(value);
                Config.save();
                sendMessage(Component.literal(value + " removed from " + name));
                return 1;
            })))
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> LiteralArgumentBuilder<T> registerSubCommands(LiteralArgumentBuilder<T> builder) {
        builder.executes(context -> {
            sendMessage(Component.literal(config.toString()));
            return 1;
        });

        builder.then((ArgumentBuilder<T, ?>) literal("config").executes(context -> {
            client.schedule(() -> client.setScreen(ConfigScreenHandler.getConfigScreen(client.screen)));
            return 1;
        }));

        for(Field f : Config.class.getFields()) {
            if(!f.isAnnotationPresent(Command.class)) continue;

            String name = f.getName();
            Class<?> type = f.getType();

            if(type == int.class) {
                Config.Range range = f.getAnnotation(Config.Range.class);
                registerSubCommandOfInt(builder, name, range.min(), range.max());
            } else if(type == boolean.class) {
                registerSubCommandOfBool(builder, name);
            } else if(type == Config.ToggleableEntityList.class) {
                registerSubCommandOfToggleableEntityList(builder, name);
            }
        }

        return builder;
    }

    private static void sendMessage(Component message) {
        assert client.player != null;
        client.player.displayClientMessage(message, false);
    }
}
