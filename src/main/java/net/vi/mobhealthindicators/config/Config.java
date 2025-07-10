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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static net.vi.mobhealthindicators.MobHealthIndicators.client;
import static net.vi.mobhealthindicators.MobHealthIndicators.overrideFiltersKey;

public class Config {

    public static Config config;

    public static final int heightRange = 25;
    public static final int heightDivisor = 50;

    public static final boolean showHeartsDefault = true;
    public static final boolean dynamicBrightnessDefault = true;
    public static final int heightDefault = 0;
    public static final boolean renderOnTopOnHoverDefault = true;
    public static final ToggleableEntityList blackListDefault = new ToggleableEntityList(true, "minecraft:armor_stand", "minecraft:ender_dragon");
    public static final ToggleableEntityList whiteListDefault = new ToggleableEntityList(false, "minecraft:player");
    public static final boolean showHostileDefault = true;
    public static final boolean showPassiveDefault = true;
    public static final boolean showSelfDefault = false;
    public static final boolean onlyShowDamagedDefault = false;
    public static final boolean onlyShowOnHoverDefault = false;

    @Expose public boolean showHearts = showHeartsDefault;
    @Expose public boolean dynamicBrightness = dynamicBrightnessDefault;
    @Expose public int height = heightDefault;
    @Expose public boolean renderOnTopOnHover = renderOnTopOnHoverDefault;
    @Expose public ToggleableEntityList blackList = blackListDefault;
    @Expose public ToggleableEntityList whiteList = whiteListDefault;
    @Expose public boolean showHostile = showHostileDefault;
    @Expose public boolean showPassive = showPassiveDefault;
    @Expose public boolean showSelf = showSelfDefault;
    @Expose public boolean onlyShowDamaged = onlyShowDamagedDefault;
    @Expose public boolean onlyShowOnHover = onlyShowOnHoverDefault;

    public void setName(String name, Object value) {
        try {
            Field f = Config.class.getField(name);
            f.set(this, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public Object getName(String name) {
        try {
            Field f = Config.class.getField(name);
            return f.get(this);
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
        if(!showPassive && !(livingEntity instanceof Monster)) return false;

        return true;
    }

    public static class ToggleableEntityList {
        @Expose public HashSet<String> entityList;
        @Expose public boolean toggle;

        public ToggleableEntityList(boolean toggle, String... entityList) {
            this.entityList = Arrays.stream(entityList).collect(Collectors.toCollection(HashSet::new));
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
            if(!field.isAnnotationPresent(Expose.class)) continue;
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
                configFile.createNewFile();
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
                    if(Modifier.isStatic(f.getModifiers())) continue;

                    if(f.get(config) == null) f.set(config, Config.class.getField(f.getName() + "Default").get(null));
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
            e.printStackTrace();
        }
    }
}