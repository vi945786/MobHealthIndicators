package net.vi.mobhealthindicators.config.screen;

import me.shedaniel.clothconfig2.gui.entries.DropdownBoxEntry;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DropdownBoxEntryNoReset<T> extends DropdownBoxEntry<T> {

    public DropdownBoxEntryNoReset(Text fieldName, @Nullable Supplier<Optional<Text[]>> tooltipSupplier, boolean requiresRestart, @Nullable Supplier<T> defaultValue, @Nullable Consumer<T> saveConsumer, @Nullable Iterable<T> selections, @NotNull SelectionTopCellElement<T> topRenderer, @NotNull SelectionCellCreator<T> cellCreator) {
        super(fieldName, Text.empty(), tooltipSupplier, requiresRestart, defaultValue, saveConsumer, selections, topRenderer, cellCreator);
        resetButton.setWidth(0);
    }
}
