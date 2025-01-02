package net.vi.mobhealthindicators.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.mojang.serialization.Codec;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.MathHelper;
import net.vi.mobhealthindicators.MobHealthIndicators;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static net.vi.mobhealthindicators.MobHealthIndicators.client;
import static net.vi.mobhealthindicators.MobHealthIndicators.overrideFiltersKey;

public class Config {

    public static Config config;

    public static final boolean showHeartsDefault = true;
    public static final boolean dynamicBrightnessDefault = true;
    public static final ToggleableEntityList blackListDefault = new ToggleableEntityList(true, "minecraft:armor_stand");
    public static final ToggleableEntityList whiteListDefault = new ToggleableEntityList(false, "minecraft:player");
    public static final boolean showHostileDefault = true;
    public static final boolean showPassiveDefault = true;
    public static final boolean showSelfDefault = false;
    public static final boolean onlyShowDamagedDefault = false;

    @Expose public boolean showHearts = showHeartsDefault;
    @Expose public boolean dynamicBrightness = dynamicBrightnessDefault;
    @Expose public ToggleableEntityList blackList = blackListDefault;
    @Expose public ToggleableEntityList whiteList = whiteListDefault;
    @Expose public boolean showHostile = showHostileDefault;
    @Expose public boolean showPassive = showPassiveDefault;
    @Expose public boolean showSelf = showSelfDefault;
    @Expose public boolean onlyShowDamaged = onlyShowDamagedDefault;

    public boolean shouldRender(LivingEntity livingEntity) {
        if(overrideFiltersKey.isPressed()) return true;

        if(!showHearts) return false;

        if(showSelf && livingEntity == client.player) return true;
        if(!showSelf && livingEntity == client.player) return false;
        if(onlyShowDamaged && MathHelper.ceil(livingEntity.getHealth()) >= MathHelper.ceil(livingEntity.getMaxHealth())) return false;
        if(whiteList.toggle && whiteList.entityList.stream().anyMatch(s -> s.equals(EntityType.getId(livingEntity.getType()).toString()))) return true;
        if(blackList.toggle && blackList.entityList.stream().anyMatch(s -> s.equals(EntityType.getId(livingEntity.getType()).toString()))) return false;
        if(!showHostile && livingEntity instanceof Monster) return false;
        if(!showPassive && !(livingEntity instanceof Monster)) return false;

        return true;
    }

    public static class ToggleableEntityList {
        @Expose public HashSet<String> entityList;
        @Expose public boolean toggle;

        public ToggleableEntityList() {}

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
        return "showHearts = " + showHearts + "\n" +
                "dynamicBrightness = " + dynamicBrightness + "\n" +
                "blackList = " + blackList + "\n" +
                "whiteList = " + whiteList + "\n" +
                "showHostile = " + showHostile + "\n" +
                "showPassive = " + showPassive + "\n" +
                "showSelf = " + showSelf + "\n" +
                "onlyShowDamaged = " + onlyShowDamaged;
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