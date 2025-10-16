package net.vi.mobhealthindicators.mixin;

import net.minecraft.entity.damage.DamageSources;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(World.class)
public interface WorldAccessor {
    @Mutable
    @Accessor("thread")
    void setThread(Thread thread);

    @Mutable
    @Accessor("debugWorld")
    void setDebugWorld(boolean debugWorld);

    @Mutable
    @Accessor("properties")
    void setProperties(MutableWorldProperties properties);

    @Mutable
    @Accessor("biomeAccess")
    void setBiomeAccess(BiomeAccess biomeAccess);

    @Mutable
    @Accessor("registryKey")
    void setRegistryKey(RegistryKey<World> registryKey);

    @Mutable
    @Accessor("dimensionEntry")
    void setDimensionEntry(RegistryEntry<DimensionType> dimensionEntry);

    @Mutable
    @Accessor("random")
    void setRandom(Random random);

    @Mutable
    @Accessor("threadSafeRandom")
    void setAsyncRandom(Random random);

    @Mutable
    @Accessor("blockEntityTickers")
    void setBlockEntityTickers(List<BlockEntityTickInvoker> list);

    @Mutable
    @Accessor("pendingBlockEntityTickers")
    void setPendingBlockEntityTickers(List<BlockEntityTickInvoker> list);

    @Mutable
    @Accessor("damageSources")
    void setDamageSources(DamageSources sources);
}
