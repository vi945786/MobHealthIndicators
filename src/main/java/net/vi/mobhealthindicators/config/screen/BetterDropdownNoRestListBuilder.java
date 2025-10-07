package net.vi.mobhealthindicators.config.screen;

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

public class BetterDropdownNoRestListBuilder<T> extends FieldBuilder<List<T>, ToggleableNestedListListEntry<T, BetterDropdownBoxEntryNoReset<T>>, BetterDropdownNoRestListBuilder<T>> {
    protected List<T> value;
    protected Supplier<T> defaultEntryValue = null;
    protected Function<T, BetterDropdownBoxEntry.SelectionTopCellElement<T>> topCellCreator;
    protected BetterDropdownBoxEntry.SelectionCellCreator<T> cellCreator;
    protected Supplier<Optional<Text[]>> tooltipSupplier = Optional::empty;
    protected BiConsumer<List<T>, Boolean> saveConsumer = null;
    protected Iterable<T> selections = Collections.emptyList();
    protected boolean suggestionMode = true;
    protected boolean toggled;
    protected Supplier<Boolean> defaultToggled = null;

    public BetterDropdownNoRestListBuilder(Text resetButtonKey, Text fieldNameKey, List<T> value, boolean toggled, Function<T, BetterDropdownBoxEntry.SelectionTopCellElement<T>> topCellCreator, BetterDropdownBoxEntry.SelectionCellCreator<T> cellCreator) {
        super(resetButtonKey, fieldNameKey);
        this.value = value;
        this.toggled = toggled;
        this.topCellCreator = Objects.requireNonNull(topCellCreator);
        this.cellCreator = Objects.requireNonNull(cellCreator);
    }

    public BetterDropdownNoRestListBuilder<T> setSelections(List<T> selections) {
        this.selections = selections;
        return this;
    }

    public BetterDropdownNoRestListBuilder<T> setDefaultValue(Supplier<List<T>> defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public BetterDropdownNoRestListBuilder<T> setDefaultValue(List<T> defaultValue) {
        this.defaultValue = () -> Objects.requireNonNull(defaultValue);
        return this;
    }

    public BetterDropdownNoRestListBuilder<T> setDefaultToggled(Supplier<Boolean> defaultToggled) {
        this.defaultToggled = defaultToggled;
        return this;
    }

    public BetterDropdownNoRestListBuilder<T> setDefaultToggled(boolean defaultToggled) {
        this.defaultToggled = () -> defaultToggled;
        return this;
    }

    public BetterDropdownNoRestListBuilder<T> setDefaultEntryValue(Supplier<T> defaultEntryValue) {
        this.defaultEntryValue = defaultEntryValue;
        return this;
    }

    public BetterDropdownNoRestListBuilder<T> setDefaultEntryValue(T defaultEntryValue) {
        this.defaultEntryValue = () -> Objects.requireNonNull(defaultEntryValue);
        return this;
    }

    public BetterDropdownNoRestListBuilder<T> setSaveConsumer(BiConsumer<List<T>, Boolean> saveConsumer) {
        this.saveConsumer = saveConsumer;
        return this;
    }

    public BetterDropdownNoRestListBuilder<T> setTooltipSupplier(Supplier<Optional<Text[]>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    public BetterDropdownNoRestListBuilder<T> setTooltip(Optional<Text[]> tooltip) {
        this.tooltipSupplier = () -> tooltip;
        return this;
    }

    public BetterDropdownNoRestListBuilder<T> setTooltip(Text... tooltip) {
        this.tooltipSupplier = () -> Optional.ofNullable(tooltip);
        return this;
    }

    public BetterDropdownNoRestListBuilder<T> requireRestart() {
        requireRestart(true);
        return this;
    }

    public BetterDropdownNoRestListBuilder<T> setErrorSupplier(Function<List<T>, Optional<Text>> errorSupplier) {
        this.errorSupplier = errorSupplier;
        return this;
    }

    public BetterDropdownNoRestListBuilder<T> setSuggestionMode(boolean suggestionMode) {
        this.suggestionMode = suggestionMode;
        return this;
    }

    public boolean isSuggestionMode() {
        return suggestionMode;
    }

    @NotNull
    @Override
    public ToggleableNestedListListEntry<T, BetterDropdownBoxEntryNoReset<T>> build() {
        ToggleableNestedListListEntry<T, BetterDropdownBoxEntryNoReset<T>> listEntry = new ToggleableNestedListListEntry<>(
                getFieldNameKey(),
                value,
                toggled,
                false,
                tooltipSupplier,
                saveConsumer,
                defaultValue,
                defaultToggled,
                getResetButtonKey(),
                true,
                false,
                (entryValue, list) -> {
                    Supplier<T> defaultValue = () -> entryValue;
                    if (entryValue == null) defaultValue = defaultEntryValue;
                    BetterDropdownBoxEntryNoReset<T> entry = new BetterDropdownBoxEntryNoReset<>(Text.empty(), null, isRequireRestart(), defaultValue, null, selections, topCellCreator.apply(entryValue), cellCreator);
                    entry.setSuggestionMode(suggestionMode);
                    return entry;
                });
        if (errorSupplier != null)
            listEntry.setErrorSupplier(() -> errorSupplier.apply(listEntry.getValue()));


        return finishBuilding(listEntry);
    }
}