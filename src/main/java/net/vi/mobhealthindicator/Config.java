package net.vi.mobhealthindicator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config INSTANCE = new Config();

    private boolean renderingEnabled = true;
    private boolean heartStackingEnabled = true;
    private int heartOffset = 0;

    public static boolean getRendering() {
        return INSTANCE.renderingEnabled;
    }

    public static void setRendering(boolean renderingEnabled) {
        INSTANCE.renderingEnabled = renderingEnabled;
        save();
    }

    public static boolean getHeartStacking() {
        return INSTANCE.heartStackingEnabled;
    }

    public static void setHeartStacking(boolean heartStackingEnabled) {
        INSTANCE.heartStackingEnabled = heartStackingEnabled;
        save();
    }

    public static int getHeartOffset() {
        return INSTANCE.heartOffset;
    }

    public static void setHeartOffset(int heartOffset) {
        INSTANCE.heartOffset = heartOffset;
        save();
    }

    public static void load() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FabricLoader.getInstance().getConfigDir().resolve(MobHealthIndicator.configFile).toFile()))) {
            Config config = GSON.fromJson(reader, Config.class);
            if (config != null) {
                INSTANCE = config;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FabricLoader.getInstance().getConfigDir().resolve(MobHealthIndicator.configFile).toFile()))) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
