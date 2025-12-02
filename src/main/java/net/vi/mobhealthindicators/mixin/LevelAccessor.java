package net.vi.mobhealthindicators.mixin;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(Level.class)
public interface LevelAccessor {
    @Mutable
    @Accessor("thread")
    void setThread(Thread thread);

    @Mutable
    @Accessor("isDebug")
    void setIsDebug(boolean debugWorld);

    @Mutable
    @Accessor("levelData")
    void setLevelData(WritableLevelData levelData);

    @Mutable
    @Accessor("biomeManager")
    void setBiomeManager(BiomeManager biomeManager);

    @Mutable
    @Accessor("dimension")
    void setDimension(ResourceKey<Level> dimension);

    @Mutable
    @Accessor("dimensionTypeRegistration")
    void setDimensionTypeRegistration(Holder<DimensionType> dimensionTypeRegistration);

    @Mutable
    @Accessor("random")
    void setRandom(RandomSource random);

    @Mutable
    @Accessor("threadSafeRandom")
    void setThreadSafeRandom(RandomSource threadSafeRandom);

    @Mutable
    @Accessor("blockEntityTickers")
    void setBlockEntityTickers(List<TickingBlockEntity> blockEntityTickers);

    @Mutable
    @Accessor("pendingBlockEntityTickers")
    void setPendingBlockEntityTickers(List<TickingBlockEntity> pendingBlockEntityTickers);

    @Mutable
    @Accessor("damageSources")
    void setDamageSources(DamageSources damageSources);
}
