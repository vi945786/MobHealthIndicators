package net.vi.mobhealthindicators.config.screen;

import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BetterDropdownBoxEntryNoReset<T> extends BetterDropdownBoxEntry<T> {

    public BetterDropdownBoxEntryNoReset(Text fieldName, @Nullable Supplier<Optional<Text[]>> tooltipSupplier, boolean requiresRestart, @Nullable Supplier<T> defaultValue, @Nullable Consumer<T> saveConsumer, @Nullable Iterable<T> selections, @NotNull SelectionTopCellElement<T> topRenderer, @NotNull SelectionCellCreator<T> cellCreator) {
        super(fieldName, Text.empty(), tooltipSupplier, requiresRestart, defaultValue, saveConsumer, selections, topRenderer, cellCreator);
        resetButton.setWidth(0);
    }
}
