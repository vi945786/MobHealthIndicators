package net.vi.mobhealthindicator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.Codec;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.StringIdentifiable;
import net.vi.mobhealthindicator.MobHealthIndicator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class Config {
    public static Config config = new Config();

    public boolean showHearts = true;
    public boolean dynamicBrightness = true;
    public WhiteOrBlackList filteringMechanism = WhiteOrBlackList.blackList;
    public String[] blackList = new String[] {"minecraft:armor_stand"};
    public String[] whiteList = new String[] {"minecraft:player"};

    public boolean shouldRender(LivingEntity livingEntity) {
        if(!showHearts) return false;

        return switch (filteringMechanism) {
            case blackList -> Arrays.stream(blackList).noneMatch(s -> s.equals(EntityType.getId(livingEntity.getType()).toString()));
            case whiteList -> Arrays.stream(whiteList).anyMatch(s -> s.equals(EntityType.getId(livingEntity.getType()).toString()));
            case none -> true;
        };
    }

    public enum WhiteOrBlackList implements StringIdentifiable {
        blackList,
        whiteList,
        none;

        public static final Codec<WhiteOrBlackList> CODEC = StringIdentifiable.createCodec(WhiteOrBlackList::values);

        @Override
        public String asString() {
            return toString();
        }
    }

    @Override
    public String toString() {
        return "showHearts = " + showHearts + "\n" +
               "dynamicBrightness = " + dynamicBrightness + "\n" +
               "filteringMechanism = " + filteringMechanism + "\n" +
               "blackList = " + Arrays.toString(blackList) + "\n" +
               "whiteList = " + Arrays.toString(whiteList) + "\n";
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File configFile = FabricLoader.getInstance().getConfigDir().resolve(MobHealthIndicator.modId + ".json").toFile();

    public static void init() {
        if(!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        load();
    }

    public static void load() {
        try (FileReader reader = new FileReader(configFile)) {
            Config config = GSON.fromJson(reader, Config.class);
            if (config != null) {
                if(config.filteringMechanism == null) config.filteringMechanism = Config.config.filteringMechanism;
                if(config.blackList == null) config.blackList = Config.config.blackList;
                if(config.whiteList == null) config.whiteList = Config.config.whiteList;

                Config.config = config;
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
