package net.vi.mobhealthindicators.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.*;
import net.minecraft.client.gui.screen.Screen;
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
        DropdownListBuilder<String> entry = startDropdownList(fieldNameKey, value, (String string) -> new DropdownBoxEntry.DefaultSelectionTopCellElement<>(string == null ? "" : string, s -> s, Text::literal), new DropdownBoxEntry.DefaultSelectionCellCreator<>());
        entry.setSelections(Registries.ENTITY_TYPE.stream().map(EntityType::getId).map(Identifier::toString).sorted().toList());
        return entry;
    }

    public static <T> DropdownListBuilder<T> startDropdownList(Text fieldNameKey, List<T> value, Function<T, DropdownBoxEntry.SelectionTopCellElement<T>> topCellCreator, DropdownBoxEntry.SelectionCellCreator<T> cellCreator) {
        return new DropdownListBuilder<>(Text.translatable("text.cloth-config.reset_value"), fieldNameKey, value, topCellCreator, cellCreator);
    }
}
