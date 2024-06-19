package net.vi.mobhealthindicator.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.serialization.Codec;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.StringIdentifiable;
import net.vi.mobhealthindicator.MobHealthIndicator;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;

public class Config {
    public static Config config = new Config();

    public boolean showHearts = true;
    public boolean dynamicBrightness = true;
    public WhiteOrBlackList filteringMechanism = WhiteOrBlackList.BLACK_LIST;
    public String[] blackList = new String[] {"minecraft:armor_stand"};
    public String[] whiteList = new String[] {"minecraft:player"};

    public boolean shouldRender(LivingEntity livingEntity) {
        if(!showHearts) return false;

        return switch (filteringMechanism) {
            case BLACK_LIST -> Arrays.stream(blackList).noneMatch(s -> s.equals(EntityType.getId(livingEntity.getType()).toString()));
            case WHITE_LIST -> Arrays.stream(whiteList).anyMatch(s -> s.equals(EntityType.getId(livingEntity.getType()).toString()));
            case NONE -> true;
        };
    }

    public enum WhiteOrBlackList implements StringIdentifiable {
        BLACK_LIST("blackList"),
        WHITE_LIST("whiteList"),
        NONE("none");

        public static final Codec<WhiteOrBlackList> CODEC = StringIdentifiable.createCodec(WhiteOrBlackList::values);

        public final String name;

        WhiteOrBlackList(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

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

    public static void load() {
        try (FileReader reader = new FileReader(FabricLoader.getInstance().getConfigDir().resolve(MobHealthIndicator.modId + ".json").toFile())) {
            Config config = GSON.fromJson(reader, Config.class);
            if (config != null) {
                Config.config = config;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FabricLoader.getInstance().getConfigDir().resolve(MobHealthIndicator.modId + ".json").toFile())) {
            GSON.toJson(config, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
