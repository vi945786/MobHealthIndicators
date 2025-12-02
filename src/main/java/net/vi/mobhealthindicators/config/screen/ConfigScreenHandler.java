package net.vi.mobhealthindicators.config.screen;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.*;
import me.shedaniel.clothconfig2.impl.builders.BooleanToggleBuilder;
import me.shedaniel.clothconfig2.impl.builders.IntSliderBuilder;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.vi.mobhealthindicators.config.Config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

import static net.vi.mobhealthindicators.EntityTypeToEntity.getLivingEntities;
import static net.vi.mobhealthindicators.MobHealthIndicators.*;

public class ConfigScreenHandler {

    public enum Category {
        DISPLAY,
        FILTER,
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigScreen {
        Category category();
        boolean tooltip() default false;
    }

    public static Screen getConfigScreen(Screen parent) {
        ConfigBuilder configBuilder = ConfigBuilder.create();
        configBuilder.setParentScreen(parent);
        configBuilder.setEditable(true);
        configBuilder.setSavingRunnable(Config::save);
        configBuilder.setTitle(Component.translatable(modId + ".name"));
        configBuilder.transparentBackground();

        ConfigEntryBuilder entryBuilder = configBuilder.entryBuilder();

        ConfigCategory display = configBuilder.getOrCreateCategory(Component.translatable("config." + modId + ".category.display"));
        ConfigCategory filter = configBuilder.getOrCreateCategory(Component.translatable("config." + modId + ".category.filter"));
        ConfigCategory keybinds = configBuilder.getOrCreateCategory(Component.translatable("config." + modId + ".category.keybinds"));
            keybinds.addEntry(getKeybindingField(entryBuilder, toggleKey));
            keybinds.addEntry(getKeybindingField(entryBuilder, overrideFiltersKey));

        for(Field f : Config.class.getFields()) {
            ConfigScreen annotation = f.getAnnotation(ConfigScreen.class);
            if(annotation == null) continue;

            String name = f.getName();
            Class<?> type = f.getType();
            ConfigCategory configCategory = null;
            AbstractConfigListEntry<?> entry = null;

            switch (annotation.category()) {
                case DISPLAY -> configCategory = display;
                case FILTER -> configCategory = filter;
            }

            if(type == int.class) {
                Config.Range range = f.getAnnotation(Config.Range.class);
                entry = getIntSlider(entryBuilder, name, range.min(), range.max(), annotation.tooltip());
            } else if(type == boolean.class) {
                entry = getBooleanToggle(entryBuilder, name, annotation.tooltip());
            } else if(type == Config.ToggleableEntityList.class) {
                entry = getToggleableEntityDropdownList(name, annotation.tooltip());
            }

            configCategory.addEntry(entry);
        }

        return configBuilder.build();
    }

    private static IntegerSliderEntry getIntSlider(ConfigEntryBuilder entryBuilder, String name, int min, int max, boolean tooltip) {
        IntSliderBuilder builder = entryBuilder.startIntSlider(Component.translatable("config."  + modId + ".option." + name.toLowerCase()), Config.getName(name), min, max);
        builder.setDefaultValue(() -> Config.getDefault(name));
        builder.setSaveConsumer(value -> Config.setName(name, value));
        if(tooltip) builder.setTooltip(Component.translatable("config."  + modId + ".option." + name.toLowerCase() + ".tooltip"));
        return builder.build();
    }

    private static BooleanListEntry getBooleanToggle(ConfigEntryBuilder entryBuilder, String name, boolean tooltip) {
        BooleanToggleBuilder builder = entryBuilder.startBooleanToggle(Component.translatable("config."  + modId + ".option." + name.toLowerCase()), Config.getName(name));
        builder.setDefaultValue(() -> Config.getDefault(name));
        builder.setSaveConsumer(value -> Config.setName(name, value));
        if(tooltip) builder.setTooltip(Component.translatable("config."  + modId + ".option." + name.toLowerCase() + ".tooltip"));
        return builder.build();
    }

    private static ToggleableNestedListListEntry<String, ?> getToggleableEntityDropdownList(String name, boolean tooltip) {
        Config.ToggleableEntityList toggleableEntityList = Config.getName(name);
        Config.ToggleableEntityList defaultToggleableEntityList = Config.getDefault(name);
        BetterDropdownNoRestListBuilder<String> builder = startToggleableEntityDropdownList(Component.translatable("config."  + modId + ".option." + name.toLowerCase()), toggleableEntityList.entityList.stream().toList(), toggleableEntityList.toggle);
        builder.setDefaultValue(() -> defaultToggleableEntityList.entityList.stream().toList());
        builder.setDefaultToggled(() -> defaultToggleableEntityList.toggle);
        builder.setSaveConsumer((list,toggle) -> {toggleableEntityList.entityList=list;toggleableEntityList.toggle=toggle;});
        if(tooltip) builder.setTooltip(Component.translatable("config."  + modId + ".option." + name.toLowerCase() + ".tooltip"));
        return builder.build();
    }

    private static KeyCodeEntry getKeybindingField(ConfigEntryBuilder entryBuilder, KeyMapping keyBinding) {
        return entryBuilder.fillKeybindingField(Component.translatable(keyBinding.getName()), keyBinding).build();
    }

    private static BetterDropdownNoRestListBuilder<String> startToggleableEntityDropdownList(Component fieldNameKey, List<String> value, boolean toggled) {
        BetterDropdownNoRestListBuilder<String> entry = ConfigScreenHandler.startToggleableDropdownList(fieldNameKey, value, toggled, (string) -> new BetterDropdownBoxEntry.DefaultSelectionTopCellElement<>(string == null ? "" : string, s -> s, Component::literal), new BetterDropdownBoxEntry.DefaultSelectionCellCreator<>());
        entry.setSelections(getLivingEntities().stream().map(EntityType::getKey).map(ResourceLocation::toString).sorted().toList());
        return entry;
    }

    public static <T> BetterDropdownNoRestListBuilder<T> startToggleableDropdownList(Component fieldNameKey, List<T> value, boolean toggled, Function<T, BetterDropdownBoxEntry.SelectionTopCellElement<T>> topCellCreator, BetterDropdownBoxEntry.SelectionCellCreator<T> cellCreator) {
        return new BetterDropdownNoRestListBuilder<>(Component.translatable("text.cloth-config.reset_value"), fieldNameKey, value, toggled, topCellCreator, cellCreator);
    }
}
