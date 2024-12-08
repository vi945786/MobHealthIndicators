package net.vi.mobhealthindicators.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Function;

import static net.vi.mobhealthindicators.config.Config.config;

public class ConfigScreen implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::getConfigScreen;
    }

    public static Screen getConfigScreen(Screen parent) {
        ConfigBuilder configBuilder = ConfigBuilder.create();
        configBuilder.setParentScreen(parent);
        configBuilder.setEditable(true);
        configBuilder.setSavingRunnable(Config::save);

        configBuilder.setTitle(Text.translatable("config.mobhealthindicators.title"));
        configBuilder.transparentBackground();

        ConfigEntryBuilder entryBuilder = configBuilder.entryBuilder();

        ConfigCategory general = configBuilder.getOrCreateCategory(Text.translatable("config.mobhealthindicators.category.general"));

        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config.mobhealthindicators.option.showhearts"), config.showHearts).setDefaultValue(Config.showHeartsDefault).setSaveConsumer(value -> config.showHearts=value).build());
        general.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config.mobhealthindicators.option.dynamicbrightness"), config.dynamicBrightness).setDefaultValue(Config.dynamicBrightnessDefault).setSaveConsumer(value -> config.dynamicBrightness=value).build());
        general.addEntry(entryBuilder.startEnumSelector(Text.translatable("config.mobhealthindicators.option.filteringmechanism"), Config.FilteringMechanism.class, config.filteringMechanism).setDefaultValue(Config.filteringMechanismDefault).setSaveConsumer(value -> config.filteringMechanism=value).build());

        general.addEntry(startEntityDropdownList(Text.translatable("config.mobhealthindicators.option.blacklist"), config.blackList.stream().toList()).setDefaultValue(Config.blackListDefault.stream().toList()).setSaveConsumer(value -> config.blackList= new HashSet<>(value)).build());
        general.addEntry(startEntityDropdownList(Text.translatable("config.mobhealthindicators.option.whitelist"), config.whiteList.stream().toList()).setDefaultValue(Config.whiteListDefault.stream().toList()).setSaveConsumer(value -> config.whiteList= new HashSet<>(value)).build());

        return configBuilder.build();
    }

    public static DropdownListBuilder<String> startEntityDropdownList(Text fieldNameKey, List<String> value) {
        DropdownListBuilder<String> entry = ConfigScreen.startDropdownList(fieldNameKey, value, (string) -> new DropdownBoxEntry.DefaultSelectionTopCellElement<>(string == null ? "" : string, s -> s, Text::literal), new DropdownBoxEntry.DefaultSelectionCellCreator<>());
        entry.setSelections(Registries.ENTITY_TYPE.getIds().stream().filter(ConfigScreen::isVanillaLivingEntity).map(Identifier::toString).sorted().toList());
        return entry;
    }

    private static final List<String> vanillaLivingEntities = Arrays.stream(new String[] {"allay", "armadillo", "armor_stand", "axolotl", "bat", "bee", "blaze", "bogged", "breeze", "camel", "cat", "cave_spider", "chicken", "cod", "cow", "creaking", "creeper", "dolphin", "donkey", "drowned", "elder_guardian", "enderman", "endermite", "ender_dragon", "evoker", "fox", "frog", "ghast", "giant", "glow_squid", "goat", "guardian", "hoglin", "horse", "husk", "illusioner", "iron_golem", "llama", "magma_cube", "mooshroom", "mule", "ocelot", "panda", "parrot", "phantom", "pig", "piglin", "piglin_brute", "pillager", "player", "polar_bear", "pufferfish", "rabbit", "ravager", "salmon", "sheep", "shulker", "silverfish", "skeleton", "skeleton_horse", "slime", "sniffer", "snow_golem", "spider", "squid", "stray", "strider", "tadpole", "trader_llama", "tropical_fish", "turtle", "vex", "villager", "vindicator", "wandering_trader", "warden", "witch", "wither", "wither_skeleton", "wolf", "zoglin", "zombie", "zombie_horse", "zombie_villager", "zombified_piglin"}).toList();
    public static boolean isVanillaLivingEntity(Identifier id) {
        if(!id.getNamespace().equals("minecraft")) return true;

        return vanillaLivingEntities.contains(id.getPath());
    }

    public static <T> DropdownListBuilder<T> startDropdownList(Text fieldNameKey, List<T> value, Function<T, DropdownBoxEntry.SelectionTopCellElement<T>> topCellCreator, DropdownBoxEntry.SelectionCellCreator<T> cellCreator) {
        return new DropdownListBuilder<>(Text.translatable("text.cloth-config.reset_value"), fieldNameKey, value, topCellCreator, cellCreator);
    }
}
