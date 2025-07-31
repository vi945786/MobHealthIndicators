package net.vi.mobhealthindicators.config.screen;

import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.gui.AbstractParentElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Selectable;
import net.minecraft.text.Text;

@SuppressWarnings("unused")
public abstract class BetterBaseListCell extends AbstractParentElement implements Selectable {
    private Supplier<Optional<Text>> errorSupplier;

    public BetterBaseListCell() {
    }

    public final int getPreferredTextColor() {
        return this.getConfigError().isPresent() ? -43691 : -2039584;
    }

    public final Optional<Text> getConfigError() {
        return this.errorSupplier != null && this.errorSupplier.get().isPresent() ? this.errorSupplier.get() : this.getError();
    }

    public void setErrorSupplier(Supplier<Optional<Text>> errorSupplier) {
        this.errorSupplier = errorSupplier;
    }

    public abstract Optional<Text> getError();

    public abstract int getCellHeight();

    public abstract void render(DrawContext var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, boolean var9, float var10);

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

    public void lateRender(DrawContext graphics, int mouseX, int mouseY, float delta) {}

    public int getMorePossibleHeight() {
        return 0;
    }
}
