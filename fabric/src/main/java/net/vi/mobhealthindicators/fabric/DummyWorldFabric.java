package net.vi.mobhealthindicators.fabric;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.vi.mobhealthindicators.EntityTypeToEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class DummyWorldFabric extends EntityTypeToEntity.DummyWorld {

    public static void init() {
        EntityTypeToEntity.DummyWorld.factory = DummyWorldFabric::new;
    }

    protected DummyWorldFabric(WritableLevelData properties, ResourceKey<Level> levelKey, RegistryAccess registryAccess, Holder<DimensionType> dimensionType, boolean isClient, boolean debugWorld, long seed) {
        super(properties, levelKey, registryAccess, dimensionType, isClient, debugWorld, seed);
    }

    @Override public @NotNull Collection<EnderDragonPart> dragonParts() { return List.of(); }
}
