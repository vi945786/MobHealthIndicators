package net.vi.mobhealthindicators;

import com.mojang.serialization.Lifecycle;
import com.sun.tools.attach.VirtualMachine;
import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import net.minecraft.core.*;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.WolfVariant;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.FuelValues;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickContainerAccess;
import net.vi.mobhealthindicators.mixin.EntityTypeAccessor;
import net.vi.mobhealthindicators.mixin.LevelAccessor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.tools.agent.MixinAgent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import static net.vi.mobhealthindicators.ModInit.client;
import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.config.Config.save;

public class EntityTypeToEntity {
    private static final Map<EntityType<?>, Class<? extends Entity>> ENTITY_TYPE_CLASS_MAP = new HashMap<>();
    private static int tick = 0;
    public static boolean isUpdating = false;
    public static boolean firstUpdate = true;

    public static void addEntity(EntityType<?> type, Class<? extends Entity> entityClass) {
        ENTITY_TYPE_CLASS_MAP.put(type, entityClass);
        config.entityTypeToEntity.put(EntityType.getKey(type).toString(), entityClass.getName());
    }

    public static List<EntityType<?>> getLivingEntities() {
        return BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(EntityTypeToEntity::isLivingEntity)
                .toList();
    }

    public static boolean isLivingEntity(EntityType<?> type) {
        return !ENTITY_TYPE_CLASS_MAP.containsKey(type) || LivingEntity.class.isAssignableFrom(ENTITY_TYPE_CLASS_MAP.get(type));
    }

    @SuppressWarnings("all")
    private static void firstUpdate() {
        config.entityTypeToEntity.forEach((typeId, entityClassName) -> {
            try {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
                        .getOptional(ResourceLocation.parse(typeId))
                        .orElse(null);

                @SuppressWarnings("unchecked")
                Class<? extends Entity> entityClass =
                        (Class<? extends Entity>) Class.forName(entityClassName);

                if (type == null || entityClass == null) return;
                addEntity(type, entityClass);
            } catch (Throwable ignored) {}
        });

        List<Class> classes;
        try {
            String location = new File(MixinAgent.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getPath();
            String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

            Field allowAttachSelfField = Class.forName("sun.tools.attach.HotSpotVirtualMachine").getDeclaredField("ALLOW_ATTACH_SELF");
            UnsafeAccess.UNSAFE.putBoolean(UnsafeAccess.UNSAFE.staticFieldBase(allowAttachSelfField), UnsafeAccess.UNSAFE.staticFieldOffset(allowAttachSelfField), true);

            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(location);

            Field agentField = MixinAgent.class.getDeclaredField("instrumentation");
            agentField.setAccessible(true);
            Instrumentation agent = (Instrumentation) agentField.get(null);
            classes = Arrays.stream((Class[]) agent.getAllLoadedClasses()).toList();
        } catch (Throwable e1) {
            try {
                Method getFields = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
                UnsafeAccess.UNSAFE.putBoolean(getFields, 12, true);
                Field classesField = Arrays.stream((Field[]) getFields.invoke(ClassLoader.class, (Object) false)).filter(field -> field.getName().equals("classes")).findFirst().orElse(null);
                if(classesField == null) return;
                UnsafeAccess.UNSAFE.putBoolean(classesField, 12, true);
                classes = new ArrayList<>(((List<Class<?>>) classesField.get(Thread.currentThread().getContextClassLoader())));
            } catch (Throwable e2) {
                return;
            }
        }

        for (Class<?> c : classes) {
            try {
                for (Field field : c.getDeclaredFields()) {
                    if (!EntityType.class.isAssignableFrom(field.getType())) {
                        continue;
                    }
                    if (!(field.getGenericType() instanceof ParameterizedType type)) {
                        continue;
                    }
                    Type[] typeArgs = type.getActualTypeArguments();
                    if (typeArgs.length != 1) {
                        continue;
                    }
                    if (!(typeArgs[0] instanceof Class<?> entityTypeClass)) {
                        continue;
                    }
                    if (entityTypeClass == Entity.class) {
                        continue;
                    }

                    EntityType<?> entityType;
                    try {
                        entityType = (EntityType<?>) field.get(null);
                    } catch (ReflectiveOperationException e) {
                        throw new AssertionError(e);
                    }

                    addEntity(entityType, entityTypeClass.asSubclass(Entity.class));
                }
            } catch (Throwable ignored) {}
        }
    }

    public static void update() {
        if (tick++ % 20 != 0) return;

        int mapSize = ENTITY_TYPE_CLASS_MAP.size();
        if (firstUpdate) {
            firstUpdate = false;
            firstUpdate();
        }
        isUpdating = true;
        BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(entityType -> !ENTITY_TYPE_CLASS_MAP.containsKey(entityType))
                .forEach(EntityTypeToEntity::updateEntityType);
        isUpdating = false;
        if (mapSize != ENTITY_TYPE_CLASS_MAP.size()) save();
    }

    private static <T extends Entity> void updateEntityType(EntityType<T> entityType) {
        EntityTypeAccessor<T> accessor = (EntityTypeAccessor<T>) entityType;

        try {
            accessor.getFactory().create(entityType, null);
            if (!ENTITY_TYPE_CLASS_MAP.containsKey(entityType)) throw new RuntimeException();
        } catch (ReturnException ignored) {
        } catch (Throwable e1) {
            try {
                accessor.getFactory().create(entityType, DummyWorld.INSTANCE_UNSAFE);
                if (!ENTITY_TYPE_CLASS_MAP.containsKey(entityType)) throw new RuntimeException();
            } catch (ReturnException ignored) {
            } catch (Throwable e2) {
                try {
                    accessor.getFactory().create(entityType, DummyWorld.INSTANCE_REGULAR);
                    if (!ENTITY_TYPE_CLASS_MAP.containsKey(entityType)) throw new RuntimeException();
                } catch (ReturnException ignored) {
                } catch (Throwable e3) {
                    try {
                        if (client.level != null) {
                            accessor.getFactory().create(entityType, client.level);
                            if (!ENTITY_TYPE_CLASS_MAP.containsKey(entityType)) throw new RuntimeException();
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    public static final class ReturnException extends RuntimeException {}

    @ApiStatus.Internal
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static abstract class DummyWorld extends Level implements LightChunkGetter {
        @FunctionalInterface
        protected interface DummyWorldFactory {
            DummyWorld create(WritableLevelData properties, ResourceKey<Level> levelKey, RegistryAccess registryAccess, Holder<DimensionType> dimensionType, boolean isClient, boolean debugWorld, long seed);
        }

        protected static DummyWorldFactory factory;

        public static final Level INSTANCE_UNSAFE;
        public static final Level INSTANCE_REGULAR;

        private static final Scoreboard SCOREBOARD = new Scoreboard();

        private static final class FallbackRegistryAccess implements RegistryAccess.Frozen {
            private static final Map<net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<?>>,
                net.minecraft.core.Registry<?>> REGISTRIES = new HashMap<>();

            static void addRegistry(DummyRegistry<?> registry) {
                REGISTRIES.put(registry.key(), registry);
            }

            @Override public <E> @NotNull Optional<Registry<E>> lookup(ResourceKey<? extends Registry<? extends E>> registryKey) {
                return Optional.ofNullable((Registry<E>) REGISTRIES.get(registryKey));
            }
            @Override public @NotNull Stream<RegistryEntry<?>> registries() { return Stream.empty(); }
            @Override public RegistryAccess.@NotNull Frozen freeze() {return this; }
        }

        private static final FallbackRegistryAccess FALLBACK_REGISTRY_ACCESS = new FallbackRegistryAccess();
        private static final FeatureFlagSet FEATURES = FeatureFlags.VANILLA_SET;
        private final ChunkSource chunkSource =
                new ChunkSource() {
                    private LevelLightEngine lightEngine;

                    @Override public @Nullable LevelChunk getChunk(int x, int z, ChunkStatus leastStatus, boolean create) { return null; }
                    @Override public void tick(BooleanSupplier shouldKeepTicking, boolean tickChunks) {}
                    @Override public @NotNull String gatherStats() { return "DummyChunkSource"; }
                    @Override public int getLoadedChunksCount() { return 0; }
                    @Override public @NotNull Level getLevel() { return DummyWorld.this; }
                    @Override public @NotNull LevelLightEngine getLightEngine() {
                        if (lightEngine == null) {
                            lightEngine = new LevelLightEngine(
                                new LightChunkGetter() {
                                    @Override public @Nullable LightChunk getChunkForLighting(int chunkX, int chunkZ) { return null; }
                                    @Override public @NotNull BlockGetter getLevel() { return DummyWorld.this; }
                                }, false, false
                            );
                        }
                        return lightEngine;
                    }
                };

        private static final EntityLookup<Entity> ENTITY_LOOKUP = new EntityLookup<>();
        private static final TickContainerAccess<?> DUMMY_SCHEDULER = new TickContainerAccess<>() {
            @Override public boolean hasScheduledTick(BlockPos pos, Object type) { return false; }
            @Override public void schedule(ScheduledTick<Object> tick) {}
            @Override public int count() { return 0; }
        };

        private final TickRateManager tickManager = new TickRateManager();

        static {
            Level worldUnsafe;
            Level worldDefault;

            Holder<DimensionType> dimType =
                    Holder.direct(
                            new DimensionType(
                                    OptionalLong.empty(),
                                    true,
                                    false,
                                    false,
                                    true,
                                    1.0D,
                                    true,
                                    false,
                                    -64,
                                    384,
                                    384,
                                    BlockTags.INFINIBURN_OVERWORLD,
                                    BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                                    0.0F,
                                    Optional.empty(),
                                    new DimensionType.MonsterSettings(
                                            false, true,
                                            UniformInt.of(0, 7),
                                            0
                                    )
                            )
                    );

            try {
                worldUnsafe = (DummyWorld) UnsafeAccess.UNSAFE.allocateInstance(DummyWorld.class);
                LevelAccessor accessor = (LevelAccessor) worldUnsafe;

                accessor.setBiomeManager(new BiomeManager(worldUnsafe, 1L));
                accessor.setIsDebug(true);
                accessor.setLevelData(new DummyWorldProperties());
                accessor.setDimension(Level.OVERWORLD);
                accessor.setDimensionTypeRegistration(dimType);
                accessor.setThread(Thread.currentThread());
                accessor.setThreadSafeRandom(RandomSource.createThreadSafe());
                accessor.setRandom(RandomSource.create());
                accessor.setBlockEntityTickers(new ArrayList<>());
                accessor.setPendingBlockEntityTickers(new ArrayList<>());
                try {
                    accessor.setDamageSources(new DamageSources(FALLBACK_REGISTRY_ACCESS));
                } catch (Throwable ignored) {}
            } catch (Throwable e) {
                worldUnsafe = null;
            }

            try {
                DummyWorldProperties props = new DummyWorldProperties();
                ResourceKey<Level> levelKey =
                        ResourceKey.create(
                                Registries.DIMENSION,
                                ResourceLocation.fromNamespaceAndPath("dummy", "world")
                        );

                worldDefault = factory.create(
                        props,
                        levelKey,
                        FALLBACK_REGISTRY_ACCESS,
                        dimType,
                        false,
                        true,
                        1L
                );
            } catch (Throwable e) {
                worldDefault = null;
            }

            INSTANCE_UNSAFE = worldUnsafe;
            INSTANCE_REGULAR = worldDefault;

            FallbackRegistryAccess.addRegistry(new DummyRegistry<>(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath("dummy", "damage"),
                    new DamageType("", DamageScaling.NEVER, 0)
            ));

            FallbackRegistryAccess.addRegistry(new DummyRegistry<>(
                    Registries.BANNER_PATTERN,
                    ResourceLocation.fromNamespaceAndPath("dummy", "pattern"),
                    new BannerPattern(ResourceLocation.fromNamespaceAndPath("dummy", "pattern"), "")
            ));

            FallbackRegistryAccess.addRegistry(new DummyRegistry<>(
                    Registries.PAINTING_VARIANT,
                    ResourceLocation.fromNamespaceAndPath("dummy", "painting"),
                    new PaintingVariant(
                            1, 1,
                            ResourceLocation.fromNamespaceAndPath("dummy", "painting"),
                            Optional.empty(),
                            Optional.empty()
                    )
            ));

            FallbackRegistryAccess.addRegistry(new DummyRegistry<>(
                    Registries.WOLF_VARIANT,
                    ResourceLocation.fromNamespaceAndPath("dummy", "wolf"),
                    new WolfVariant(
                            new WolfVariant.AssetInfo(
                                    new ClientAsset.ResourceTexture(ResourceLocation.fromNamespaceAndPath("dummy", "wolf")),
                                    new ClientAsset.ResourceTexture(ResourceLocation.fromNamespaceAndPath("dummy", "wolf")),
                                    new ClientAsset.ResourceTexture(ResourceLocation.fromNamespaceAndPath("dummy", "wolf"))
                            ),
                            SpawnPrioritySelectors.EMPTY
                    )
            ));
        }

        protected DummyWorld(WritableLevelData properties, ResourceKey<Level> levelKey, RegistryAccess registryAccess, Holder<DimensionType> dimensionType, boolean isClient, boolean debugWorld, long seed) {
            super(properties, levelKey, registryAccess, dimensionType, isClient, debugWorld, seed, 0);
        }

        @Override public void sendBlockUpdated(BlockPos pos, BlockState oldState, BlockState newState, int flags) {}
        @Override public void playSeededSound(@Nullable Entity entity, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {}
        @Override public void playSeededSound(@Nullable Entity entity, Entity sourceEntity, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) {}
        @Override public void explode(@Nullable Entity entity, double x, double y, double z, float power, ExplosionInteraction mode) {}
        @Override public void explode(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionDamageCalculator explosionDamageCalculator, double v, double v1, double v2, float v3, boolean b, ExplosionInteraction explosionInteraction, ParticleOptions particleOptions, ParticleOptions particleOptions1, WeightedList<ExplosionParticleInfo> weightedList, Holder<SoundEvent> holder) {}
        @Override public @NotNull String gatherChunkSourceStats() { return ""; }
        @Override public void setRespawnData(LevelData.RespawnData respawnData) {}
        @Override public LevelData.RespawnData getRespawnData() { return null; }
        @Override public String toString() { return "DummyWorld"; }
        @Override public @Nullable Entity getEntity(int id) { return null; }
        @Override public @NotNull TickRateManager tickRateManager() { return this.tickManager; }
        @Override public @Nullable MapItemSavedData getMapData(MapId mapId) { return null; }
        @Override public void destroyBlockProgress(int breakerId, BlockPos pos, int progress) {}
        @Override public @NotNull Scoreboard getScoreboard() { return SCOREBOARD; }
        @Override public RecipeAccess recipeAccess() { return null; }
        @Override protected @NotNull LevelEntityGetter<Entity> getEntities() { return new LevelEntityGetterAdapter(ENTITY_LOOKUP, new EntitySectionStorage<>(null, null)); }
        @Override public LevelTicks<Block> getBlockTicks() { return (LevelTicks<Block>) DUMMY_SCHEDULER; }
        @Override public LevelTicks<Fluid> getFluidTicks() { return (LevelTicks<Fluid>) DUMMY_SCHEDULER; }
        @Override public @NotNull ChunkSource getChunkSource() { return chunkSource; }
        @Override public void levelEvent(@Nullable Entity entity, int type, BlockPos pos, int data) {}
        @Override public void gameEvent(Holder<GameEvent> gameEvent, Vec3 pos, GameEvent.Context context) {}
        @Override public @NotNull RegistryAccess registryAccess() { return FALLBACK_REGISTRY_ACCESS; }
        @Override public PotionBrewing potionBrewing() { return null; }
        @Override public FuelValues fuelValues() { return null; }
        @Override public FeatureFlagSet enabledFeatures() { return FEATURES; }
        @Override public float getShade(Direction direction, boolean shaded) { return 0.0F; }
        @Override public List<? extends Player> players() { return Collections.emptyList(); }
        @Override public Holder<Biome> getUncachedNoiseBiome(int x, int y, int z) { return null; }
        @Override public int getSeaLevel() { return 0; }
        @Override public @Nullable LightChunk getChunkForLighting(int chunkX, int chunkZ) { return null; }
        @Override public void onLightUpdate(LightLayer lightType, SectionPos pos) {}
        @Override public BlockGetter getLevel() { return null; }
        @Override public @NotNull WorldBorder getWorldBorder() { return new WorldBorder(); }

        static class DummyWorldProperties implements WritableLevelData {
            private long gameTime = 0;
            private long dayTime = 0;

            @Override public RespawnData getRespawnData() { return null; }
            @Override public long getGameTime() { return gameTime; }
            @Override public long getDayTime() { return dayTime; }
            @Override public boolean isThundering() { return false; }
            @Override public boolean isRaining() { return false; }
            @Override public void setRaining(boolean raining) {}
            @Override public boolean isHardcore() { return false; }
            @Override public @NotNull Difficulty getDifficulty() { return Difficulty.NORMAL; }
            @Override public boolean isDifficultyLocked() { return false; }
            @Override public void setSpawn(RespawnData respawnData) {}
        }
    }

    public static final class DummyRegistry<T> extends MappedRegistry<T> {
        public DummyRegistry(ResourceKey<? extends Registry<T>> registryKey, ResourceLocation defaultId, T defaultValue) {
            super(registryKey, Lifecycle.experimental(), false);
            ResourceKey<T> key = ResourceKey.create(registryKey, defaultId);
            this.register(key, defaultValue, RegistrationInfo.BUILT_IN);
        }
    }
}

