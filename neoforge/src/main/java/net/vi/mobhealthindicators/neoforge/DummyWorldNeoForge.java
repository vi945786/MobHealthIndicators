package net.vi.mobhealthindicators.neoforge;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.neoforged.neoforge.entity.PartEntity;
import net.vi.mobhealthindicators.EntityTypeToEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class DummyWorldNeoForge extends EntityTypeToEntity.DummyWorld {

    public static void init() {
        EntityTypeToEntity.DummyWorld.factory = DummyWorldNeoForge::new;
    }

    protected DummyWorldNeoForge(WritableLevelData properties, ResourceKey<Level> levelKey, RegistryAccess registryAccess, Holder<DimensionType> dimensionType, boolean isClient, boolean debugWorld, long seed) {
        super(properties, levelKey, registryAccess, dimensionType, isClient, debugWorld, seed);
    }

    @Override public @NotNull Collection<PartEntity<?>> dragonParts() { return List.of(); }
    @Override public void setDayTimeFraction(float v) {}
    @Override public float getDayTimeFraction() { return 0; }
    @Override public float getDayTimePerTick() { return 0; }
    @Override public void setDayTimePerTick(float v) {}
}

