package net.vi.mobhealthindicators.config;

import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry.SelectionCellCreator;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry.SelectionTopCellElement;
import me.shedaniel.clothconfig2.gui.entries.NestedListListEntry;

import me.shedaniel.clothconfig2.impl.builders.FieldBuilder;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DropdownListBuilder<T> extends FieldBuilder<List<T>, ToggleableNestedListListEntry<T, DropdownBoxEntry<T>>, DropdownListBuilder<T>> {
    protected List<T> value;
    protected Supplier<T> defaultEntryValue = null;
    protected Function<T, SelectionTopCellElement<T>> topCellCreator;
    protected SelectionCellCreator<T> cellCreator;
    protected Supplier<Optional<Text[]>> tooltipSupplier = Optional::empty;
    protected BiConsumer<List<T>, Boolean> saveConsumer = null;
    protected Iterable<T> selections = Collections.emptyList();
    protected boolean suggestionMode = true;
    protected boolean toggled;

    public DropdownListBuilder(Text resetButtonKey, Text fieldNameKey, List<T> value, boolean toggled, Function<T, SelectionTopCellElement<T>> topCellCreator, SelectionCellCreator<T> cellCreator) {
        super(resetButtonKey, fieldNameKey);
        this.value = value;
        this.toggled = toggled;
        this.topCellCreator = Objects.requireNonNull(topCellCreator);
        this.cellCreator = Objects.requireNonNull(cellCreator);
    }

    public DropdownListBuilder<T> setSelections(List<T> selections) {
        this.selections = selections;
        return this;
    }

    public DropdownListBuilder<T> setDefaultValue(Supplier<List<T>> defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public DropdownListBuilder<T> setDefaultValue(List<T> defaultValue) {
        this.defaultValue = () -> Objects.requireNonNull(defaultValue);
        return this;
    }

    public DropdownListBuilder<T> setDefaultEntryValue(Supplier<T> defaultEntryValue) {
        this.defaultEntryValue = defaultEntryValue;
        return this;
    }

    public DropdownListBuilder<T> setDefaultEntryValue(T defaultEntryValue) {
        this.defaultEntryValue = () -> Objects.requireNonNull(defaultEntryValue);
        return this;
    }

    public DropdownListBuilder<T> setSaveConsumer(BiConsumer<List<T>, Boolean> saveConsumer) {
        this.saveConsumer = saveConsumer;
        return this;
    }

    public DropdownListBuilder<T> setTooltipSupplier(Supplier<Optional<Text[]>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    public DropdownListBuilder<T> setTooltip(Optional<Text[]> tooltip) {
        this.tooltipSupplier = () -> tooltip;
        return this;
    }

    public DropdownListBuilder<T> setTooltip(Text... tooltip) {
        this.tooltipSupplier = () -> Optional.ofNullable(tooltip);
        return this;
    }

    public DropdownListBuilder<T> requireRestart() {
        requireRestart(true);
        return this;
    }

    public DropdownListBuilder<T> setErrorSupplier(Function<List<T>, Optional<Text>> errorSupplier) {
        this.errorSupplier = errorSupplier;
        return this;
    }

    public DropdownListBuilder<T> setSuggestionMode(boolean suggestionMode) {
        this.suggestionMode = suggestionMode;
        return this;
    }

    public boolean isSuggestionMode() {
        return suggestionMode;
    }

    @NotNull
    @Override
    public ToggleableNestedListListEntry<T, DropdownBoxEntry<T>> build() {
        ToggleableNestedListListEntry<T, DropdownBoxEntry<T>> listEntry = new ToggleableNestedListListEntry<>(
                getFieldNameKey(),
                value,
                toggled,
                false,
                tooltipSupplier,
                saveConsumer,
                defaultValue,
                getResetButtonKey(),
                true,
                false,
                (entryValue, list) -> {
                    Supplier<T> defaultValue = () -> entryValue;
                    if (entryValue == null) defaultValue = defaultEntryValue;
                    DropdownBoxEntry<T> entry = new DropdownBoxEntry<>(Text.empty(), Text.literal("mobhealthindicators.hidebutton"), null, isRequireRestart(), defaultValue, null, selections, topCellCreator.apply(entryValue), cellCreator);
                    entry.setSuggestionMode(suggestionMode);
                    return entry;
                });
        if (errorSupplier != null)
            listEntry.setErrorSupplier(() -> errorSupplier.apply(listEntry.getValue()));


        return finishBuilding(listEntry);
    }
}