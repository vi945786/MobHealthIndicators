package net.vi.mobhealthindicators.config;

import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry.SelectionCellCreator;
import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry.SelectionTopCellElement;

import me.shedaniel.clothconfig2.impl.builders.FieldBuilder;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DropdownNoRestListBuilder<T> extends FieldBuilder<List<T>, ToggleableNestedListListEntry<T, DropdownBoxEntryNoReset<T>>, DropdownNoRestListBuilder<T>> {
    protected List<T> value;
    protected Supplier<T> defaultEntryValue = null;
    protected Function<T, SelectionTopCellElement<T>> topCellCreator;
    protected SelectionCellCreator<T> cellCreator;
    protected Supplier<Optional<Text[]>> tooltipSupplier = Optional::empty;
    protected BiConsumer<List<T>, Boolean> saveConsumer = null;
    protected Iterable<T> selections = Collections.emptyList();
    protected boolean suggestionMode = true;
    protected boolean toggled;

    public DropdownNoRestListBuilder(Text resetButtonKey, Text fieldNameKey, List<T> value, boolean toggled, Function<T, SelectionTopCellElement<T>> topCellCreator, SelectionCellCreator<T> cellCreator) {
        super(resetButtonKey, fieldNameKey);
        this.value = value;
        this.toggled = toggled;
        this.topCellCreator = Objects.requireNonNull(topCellCreator);
        this.cellCreator = Objects.requireNonNull(cellCreator);
    }

    public DropdownNoRestListBuilder<T> setSelections(List<T> selections) {
        this.selections = selections;
        return this;
    }

    public DropdownNoRestListBuilder<T> setDefaultValue(Supplier<List<T>> defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public DropdownNoRestListBuilder<T> setDefaultValue(List<T> defaultValue) {
        this.defaultValue = () -> Objects.requireNonNull(defaultValue);
        return this;
    }

    public DropdownNoRestListBuilder<T> setDefaultEntryValue(Supplier<T> defaultEntryValue) {
        this.defaultEntryValue = defaultEntryValue;
        return this;
    }

    public DropdownNoRestListBuilder<T> setDefaultEntryValue(T defaultEntryValue) {
        this.defaultEntryValue = () -> Objects.requireNonNull(defaultEntryValue);
        return this;
    }

    public DropdownNoRestListBuilder<T> setSaveConsumer(BiConsumer<List<T>, Boolean> saveConsumer) {
        this.saveConsumer = saveConsumer;
        return this;
    }

    public DropdownNoRestListBuilder<T> setTooltipSupplier(Supplier<Optional<Text[]>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    public DropdownNoRestListBuilder<T> setTooltip(Optional<Text[]> tooltip) {
        this.tooltipSupplier = () -> tooltip;
        return this;
    }

    public DropdownNoRestListBuilder<T> setTooltip(Text... tooltip) {
        this.tooltipSupplier = () -> Optional.ofNullable(tooltip);
        return this;
    }

    public DropdownNoRestListBuilder<T> requireRestart() {
        requireRestart(true);
        return this;
    }

    public DropdownNoRestListBuilder<T> setErrorSupplier(Function<List<T>, Optional<Text>> errorSupplier) {
        this.errorSupplier = errorSupplier;
        return this;
    }

    public DropdownNoRestListBuilder<T> setSuggestionMode(boolean suggestionMode) {
        this.suggestionMode = suggestionMode;
        return this;
    }

    public boolean isSuggestionMode() {
        return suggestionMode;
    }

    @NotNull
    @Override
    public ToggleableNestedListListEntry<T, DropdownBoxEntryNoReset<T>> build() {
        ToggleableNestedListListEntry<T, DropdownBoxEntryNoReset<T>> listEntry = new ToggleableNestedListListEntry<>(
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
                    DropdownBoxEntryNoReset<T> entry = new DropdownBoxEntryNoReset<>(Text.empty(), null, isRequireRestart(), defaultValue, null, selections, topCellCreator.apply(entryValue), cellCreator);
                    entry.setSuggestionMode(suggestionMode);
                    return entry;
                });
        if (errorSupplier != null)
            listEntry.setErrorSupplier(() -> errorSupplier.apply(listEntry.getValue()));


        return finishBuilding(listEntry);
    }
}