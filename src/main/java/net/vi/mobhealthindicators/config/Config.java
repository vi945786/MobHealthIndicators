package net.vi.mobhealthindicators.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.mojang.serialization.Codec;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.StringIdentifiable;
import net.vi.mobhealthindicators.MobHealthIndicators;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

public class Config {

    public static Config config = new Config();

    public static boolean showHeartsDefault = true;
    public static boolean dynamicBrightnessDefault = true;
    public static FilteringMechanism filteringMechanismDefault = FilteringMechanism.BLACK_LIST;
    public static Set<String> blackListDefault = new HashSet<>() {{add("minecraft:armor_stand");}};
    public static Set<String> whiteListDefault = new HashSet<>() {{add("minecraft:player");}};

    @Expose public boolean showHearts = showHeartsDefault;
    @Expose public boolean dynamicBrightness = dynamicBrightnessDefault;
    @Expose public FilteringMechanism filteringMechanism = filteringMechanismDefault;
    @Expose public Set<String> blackList = blackListDefault;
    @Expose public Set<String> whiteList = whiteListDefault;

    public boolean shouldRender(LivingEntity livingEntity) {
        if(!showHearts) return false;

        return switch (filteringMechanism) {
            case BLACK_LIST -> blackList.stream().noneMatch(s -> s.equals(EntityType.getId(livingEntity.getType()).toString()));
            case WHITE_LIST -> whiteList.stream().anyMatch(s -> s.equals(EntityType.getId(livingEntity.getType()).toString()));
            case NONE -> true;
        };
    }

    public enum FilteringMechanism implements StringIdentifiable {
        BLACK_LIST(),
        WHITE_LIST(),
        NONE();

        public static final Codec<FilteringMechanism> CODEC = StringIdentifiable.createCodec(FilteringMechanism::values);

        @Override
        public String toString() {
            return I18n.translate("filteringmechanism.mobhealthindicators." + this.name().toLowerCase());
        }

        @Override
        public String asString() {
            return switch (this) {
                case BLACK_LIST -> "blackList";
                case WHITE_LIST -> "whiteList";
                case NONE -> "none";
            };
        }
    }

    @Override
    public String toString() {
        return "showHearts = " + showHearts + "\n" +
                "dynamicBrightness = " + dynamicBrightness + "\n" +
                "filteringMechanism = " + filteringMechanism + "\n" +
                "blackList = " + blackList + "\n" +
                "whiteList = " + whiteList;
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
            e.printStackTrace();
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