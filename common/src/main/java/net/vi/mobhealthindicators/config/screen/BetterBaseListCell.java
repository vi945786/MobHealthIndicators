package net.vi.mobhealthindicators.config.screen;

import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

@SuppressWarnings("unused")
public abstract class BetterBaseListCell extends AbstractContainerEventHandler implements NarratableEntry {
    private Supplier<Optional<Component>> errorSupplier;

    public BetterBaseListCell() {
    }

    public final int getPreferredTextColor() {
        return this.getConfigError().isPresent() ? -43691 : -2039584;
    }

    public final Optional<Component> getConfigError() {
        return this.errorSupplier != null && this.errorSupplier.get().isPresent() ? this.errorSupplier.get() : this.getError();
    }

    public void setErrorSupplier(Supplier<Optional<Component>> errorSupplier) {
        this.errorSupplier = errorSupplier;
    }

    public abstract Optional<Component> getError();

    public abstract int getCellHeight();

    public abstract void extractRenderState(GuiGraphicsExtractor graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta);

    public void updateBounds(boolean expanded, int x, int y, int entryWidth, int entryHeight) {
    }

    public void updateSelected(boolean isSelected) {
    }

    public boolean isRequiresRestart() {
        return false;
    }

    public boolean isEdited() {
        return this.getConfigError().isPresent();
    }

    public void onAdd() {
    }

    public void onDelete() {
    }

    public void lateRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {}

    public int getMorePossibleHeight() {
        return 0;
    }
}
