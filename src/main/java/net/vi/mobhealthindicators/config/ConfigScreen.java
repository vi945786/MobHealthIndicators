package net.vi.mobhealthindicators.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.*;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Function;

import static net.vi.mobhealthindicators.MobHealthIndicators.*;
import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.config.Config.heightRange;

public class ConfigScreen {

    public static Screen getConfigScreen(Screen parent) {
        ConfigBuilder configBuilder = ConfigBuilder.create();
        configBuilder.setParentScreen(parent);
        configBuilder.setEditable(true);
        configBuilder.setSavingRunnable(Config::save);
        configBuilder.setTitle(Text.translatable("mobhealthindicators.name"));
        configBuilder.transparentBackground();

        ConfigEntryBuilder entryBuilder = configBuilder.entryBuilder();

        ConfigCategory display = configBuilder.getOrCreateCategory(Text.translatable("config.mobhealthindicators.category.display"));
            display.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config.mobhealthindicators.option.showhearts"), config.showHearts).setDefaultValue(Config.showHeartsDefault).setSaveConsumer(value -> config.showHearts=value).build());
            display.addEntry(entryBuilder.startBooleanToggle(Text.translatable("config.mobhealthindicators.option.dynamicbrightness"), config.dynamicBrightness).setDefaultValue(Config.dynamicBrightnessDefault).setSaveConsumer(value -> config.dynamicBrightness=value).build());
            display.addEntry(entryBuilder.startIntSlider(Text.translatable("config.mobhealthindicators.option.height"), config.height, -heightRange, heightRange).setDefaultValue(Config.heightDefault).setSaveConsumer(value -> config.height=value).build());

        ConfigCategory filter = configBuilder.getOrCreateCategory(Text.translatable("config.mobhealthindicators.category.filter"));
            filter.addEntry(startToggleableEntityDropdownList(Text.translatable("config.mobhealthindicators.option.blacklist"), config.blackList.entityList.stream().toList(), config.blackList.toggle).setDefaultValue(Config.blackListDefault.entityList.stream().toList()).setSaveConsumer((list,toggle) -> {config.blackList.entityList=new HashSet<>(list);config.blackList.toggle=toggle;}).build());
            filter.addEntry(startToggleableEntityDropdownList(Text.translatable("config.mobhealthindicators.option.whitelist"), config.whiteList.entityList.stream().toList(), config.whiteList.toggle).setDefaultValue(Config.whiteListDefault.entityList.stream().toList()).setSaveConsumer((list,toggle) -> {config.whiteList.entityList=new HashSet<>(list);config.whiteList.toggle=toggle;}).build());
            SubCategoryBuilder showFor = entryBuilder.startSubCategory(Text.translatable("config.mobhealthindicators.subcategory.showfor"));
                showFor.add(entryBuilder.startBooleanToggle(Text.translatable("config.mobhealthindicators.option.showhostile"), config.showHostile).setDefaultValue(Config.showHostileDefault).setSaveConsumer(value -> config.showHostile=value).build());
                showFor.add(entryBuilder.startBooleanToggle(Text.translatable("config.mobhealthindicators.option.showpassive"), config.showPassive).setDefaultValue(Config.showPassiveDefault).setSaveConsumer(value -> config.showPassive=value).build());
                showFor.add(entryBuilder.startBooleanToggle(Text.translatable("config.mobhealthindicators.option.showself"), config.showSelf).setDefaultValue(Config.showSelfDefault).setSaveConsumer(value -> config.showSelf=value).build());
                showFor.add(entryBuilder.startBooleanToggle(Text.translatable("config.mobhealthindicators.option.onlyshowDamaged"), config.onlyShowDamaged).setDefaultValue(Config.onlyShowDamagedDefault).setSaveConsumer(value -> config.onlyShowDamaged=value).build());
            filter.addEntry(showFor.build());

        ConfigCategory keybinds = configBuilder.getOrCreateCategory(Text.translatable("config.mobhealthindicators.category.keybinds"));
            keybinds.addEntry(entryBuilder.fillKeybindingField(Text.translatable(toggleKey.getTranslationKey()), toggleKey).build());
            keybinds.addEntry(entryBuilder.fillKeybindingField(Text.translatable(overrideFiltersKey.getTranslationKey()), overrideFiltersKey).build());

        return configBuilder.build();
    }

    public static DropdownListBuilder<String> startToggleableEntityDropdownList(Text fieldNameKey, List<String> value, boolean toggled) {
        DropdownListBuilder<String> entry = ConfigScreen.startToggleableDropdownList(fieldNameKey, value, toggled, (string) -> new DropdownBoxEntry.DefaultSelectionTopCellElement<>(string == null ? "" : string, s -> s, Text::literal), new DropdownBoxEntry.DefaultSelectionCellCreator<>());
        entry.setSelections(Registries.ENTITY_TYPE.getIds().stream().filter(ConfigScreen::isVanillaLivingEntity).map(Identifier::toString).sorted().toList());
        return entry;
    }

    private static final List<String> vanillaLivingEntities = Arrays.stream(new String[] {"allay", "armadillo", "armor_stand", "axolotl", "bat", "bee", "blaze", "bogged", "breeze", "camel", "cat", "cave_spider", "chicken", "cod", "cow", "creaking", "creeper", "dolphin", "donkey", "drowned", "elder_guardian", "enderman", "endermite", "ender_dragon", "evoker", "fox", "frog", "ghast", "giant", "glow_squid", "goat", "guardian", "hoglin", "horse", "husk", "illusioner", "iron_golem", "llama", "magma_cube", "mooshroom", "mule", "ocelot", "panda", "parrot", "phantom", "pig", "piglin", "piglin_brute", "pillager", "player", "polar_bear", "pufferfish", "rabbit", "ravager", "salmon", "sheep", "shulker", "silverfish", "skeleton", "skeleton_horse", "slime", "sniffer", "snow_golem", "spider", "squid", "stray", "strider", "tadpole", "trader_llama", "tropical_fish", "turtle", "vex", "villager", "vindicator", "wandering_trader", "warden", "witch", "wither", "wither_skeleton", "wolf", "zoglin", "zombie", "zombie_horse", "zombie_villager", "zombified_piglin"}).toList();
    public static boolean isVanillaLivingEntity(Identifier id) {
        if(!id.getNamespace().equals("minecraft")) return true;

        return vanillaLivingEntities.contains(id.getPath());
    }

    public static <T> DropdownListBuilder<T> startToggleableDropdownList(Text fieldNameKey, List<T> value, boolean toggled, Function<T, DropdownBoxEntry.SelectionTopCellElement<T>> topCellCreator, DropdownBoxEntry.SelectionCellCreator<T> cellCreator) {
        return new DropdownListBuilder<>(Text.translatable("text.cloth-config.reset_value"), fieldNameKey, value, toggled, topCellCreator, cellCreator);
    }
}
