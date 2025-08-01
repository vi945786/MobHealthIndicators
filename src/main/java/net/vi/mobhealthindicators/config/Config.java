package net.vi.mobhealthindicators.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.util.math.MathHelper;
import net.vi.mobhealthindicators.MobHealthIndicators;
import net.vi.mobhealthindicators.render.HeartType;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.*;

import static net.vi.mobhealthindicators.MobHealthIndicators.client;
import static net.vi.mobhealthindicators.MobHealthIndicators.overrideFiltersKey;
import static net.vi.mobhealthindicators.commands.Commands.Command;
import static net.vi.mobhealthindicators.config.screen.ConfigScreenHandler.ConfigScreen;
import static net.vi.mobhealthindicators.config.screen.ConfigScreenHandler.Category;

public class Config {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Range {
        int min();
        int max();
    }

    public static Config config;
    public static Config defaults = new Config();

    @Expose @Command @ConfigScreen(category = Category.DISPLAY)
    public boolean showHearts = true;
    @Expose @Command @ConfigScreen(category = Category.DISPLAY, tooltip = true)
    public boolean dynamicBrightness = true;
    @Expose @Command @ConfigScreen(category = Category.DISPLAY) @Range(min = -25, max = 25)
    public int height = 0;
    @Expose @Command @ConfigScreen(category = Category.DISPLAY, tooltip = true)
    public boolean renderOnTopOnHover = true;
    @Expose @Command @ConfigScreen(category = Category.DISPLAY, tooltip = true)
    public boolean infiniteHoverRange = false;

    @Expose @Command @ConfigScreen(category = Category.FILTER)
    public ToggleableEntityList blackList = new ToggleableEntityList(true, "minecraft:armor_stand");
    @Expose @Command @ConfigScreen(category = Category.FILTER)
    public ToggleableEntityList whiteList = new ToggleableEntityList(false, "minecraft:player");
    @Expose @Command @ConfigScreen(category = Category.FILTER)
    public boolean showHostile = true;
    @Expose @Command @ConfigScreen(category = Category.FILTER)
    public boolean showPassive = true;
    @Expose @Command @ConfigScreen(category = Category.FILTER)
    public boolean showSelf = false;
    @Expose @Command @ConfigScreen(category = Category.FILTER)
    public boolean onlyShowDamaged = false;
    @Expose @Command @ConfigScreen(category = Category.FILTER, tooltip = true)
    public boolean onlyShowOnHover = false;

    @Expose
    public HashMap<String, String> entityTypeToEntity = new HashMap<>();

    public static void setName(String name, Object value) {
        try {
            Field f = Config.class.getField(name);
            f.set(config, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getDefault(String name) {
        return get(name, defaults);
    }

    public static <T> T getName(String name) {
        return get(name, config);
    }

    @SuppressWarnings("unchecked")
    private static <T> T get(String name, Object instance) {
        try {
            Field f = Config.class.getField(name);
            return (T) f.get(instance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    public boolean shouldRender(LivingEntity livingEntity, Entity targetedEntity) {
        if(overrideFiltersKey.isPressed()) return true;

        if(!showHearts) return false;

        if(!showSelf && livingEntity == client.player) return false;
        if(onlyShowOnHover && targetedEntity != livingEntity) return false;
        if(onlyShowDamaged && MathHelper.ceil(livingEntity.getHealth()) >= MathHelper.ceil(livingEntity.getMaxHealth()) && !HeartType.Effect.hasAbnormalHearts(livingEntity)) return false;

        if(whiteList.toggle && whiteList.entityList.stream().anyMatch(s -> s.equals(EntityType.getId(livingEntity.getType()).toString()))) return true;
        if(blackList.toggle && blackList.entityList.stream().anyMatch(s -> s.equals(EntityType.getId(livingEntity.getType()).toString()))) return false;
        if(!showHostile && livingEntity instanceof Monster) return false;
        return showPassive || livingEntity instanceof Monster;
    }

    public static class ToggleableEntityList {
        @Expose public List<String> entityList;
        @Expose public boolean toggle;

        public ToggleableEntityList(boolean toggle, String... entityList) {
            this.entityList = new ArrayList<>(Arrays.stream(entityList).toList());
            this.toggle = toggle;
        }

        @Override
        public String toString() {
            return entityList.toString() + ", " + toggle;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Field field : config.getClass().getFields()) {
            if(!field.isAnnotationPresent(Command.class)) continue;
            try {
                sb.append(field.getName())
                  .append(" = ")
                  .append(field.get(config));
            } catch (IllegalAccessException e) {
                sb.append(field.getName()).append("=N/A, ");
            }
            sb.append(", \n");
        }
        if (sb.lastIndexOf(", \n") == sb.length() - 3) {
            sb.setLength(sb.length() - 3);
        }
        return sb.toString();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();
    private static final File configFile = FabricLoader.getInstance().getConfigDir().resolve(MobHealthIndicators.modId + ".json").toFile();

    static {
        if(!configFile.exists()) {
            try {
                if(!configFile.createNewFile()) {
                    throw new IOException("Failed to create config file.");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void load() {
        try (FileReader reader = new FileReader(configFile)) {
            Config config = GSON.fromJson(reader, Config.class);
            if (config != null) {
                for(Field f : Config.class.getFields()) {
                    if(f.isAnnotationPresent(Expose.class) && f.get(config) == null) f.set(config, f.get(defaults));
                }
                Config.config = config;
            } else {
                Config.config = new Config();
                save();
            }
        } catch (Exception e) {
            Config.config = new Config();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(config, writer);
        } catch (Exception e) {
            System.err.println("Could not write to the config file.");
        }
    }
}