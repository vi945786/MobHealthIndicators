package net.vi.mobhealthindicators;

import com.mojang.serialization.Lifecycle;
import com.sun.tools.attach.VirtualMachine;
import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import it.unimi.dsi.fastutil.objects.ObjectIterators;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageScaling;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.spawn.SpawnConditionSelectors;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.FuelRegistry;
import net.minecraft.item.map.MapState;
import net.minecraft.particle.BlockParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.ServerRecipeManager;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryInfo;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.entry.RegistryEntryOwner;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import net.minecraft.util.ModelAndTexture;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.function.LazyIterationConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.light.ChunkSkyLight;
import net.minecraft.world.chunk.light.LightSourceView;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.explosion.ExplosionBehavior;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.QueryableTickScheduler;
import net.minecraft.world.tick.TickManager;
import net.vi.mobhealthindicators.mixin.ReferenceAccessor;
import net.vi.mobhealthindicators.mixin.WorldAccessor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.tools.agent.MixinAgent;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static net.vi.mobhealthindicators.MobHealthIndicators.client;
import static net.vi.mobhealthindicators.config.Config.config;
import static net.vi.mobhealthindicators.config.Config.save;

public class EntityTypeToEntity {
    private static final Map<EntityType<?>, Class<? extends Entity>> ENTITY_TYPE_CLASS_MAP = new HashMap<>();
    private static int tick = 0;
    public static boolean isUpdating = false;
    public static boolean firstUpdate = true;

    public static void addEntity(EntityType<?> type, Class<? extends Entity> entityClass) {
        ENTITY_TYPE_CLASS_MAP.put(type, entityClass);
        config.entityTypeToEntity.put(EntityType.getId(type).toString(), entityClass.getName());
    }

    public static List<EntityType<?>> getLivingEntities() {
        return Registries.ENTITY_TYPE.stream().filter(EntityTypeToEntity::isLivingEntity).toList();
    }

    public static boolean isLivingEntity(EntityType<?> type) {
        return !EntityTypeToEntity.ENTITY_TYPE_CLASS_MAP.containsKey(type) || LivingEntity.class.isAssignableFrom(EntityTypeToEntity.ENTITY_TYPE_CLASS_MAP.get(type));
    }

    @SuppressWarnings("all")
    private static void firstUpdate() {
        config.entityTypeToEntity.forEach((typeId, entityClassName) -> {
            try {
                EntityType<?> type = Registries.ENTITY_TYPE.getOptionalValue(Identifier.of(typeId)).get();
                Class<? extends Entity> entityClass = (Class<? extends Entity>) Class.forName(entityClassName);

                if(type == null || entityClass == null) return;
                addEntity(type, entityClass);
            } catch (Throwable ignored) {}
        });

        List<Class<?>> classes;
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
            classes = Arrays.stream((Class<?>[]) agent.getAllLoadedClasses()).toList();
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
                    if(entityTypeClass == Entity.class) {
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
        if(tick++ % 20 != 0) return;

        int mapSize = ENTITY_TYPE_CLASS_MAP.size();
        if(firstUpdate) {
            firstUpdate = false;
            firstUpdate();
        }
        isUpdating = true;
        Registries.ENTITY_TYPE.stream().filter(entityType -> !ENTITY_TYPE_CLASS_MAP.containsKey(entityType)).forEach(EntityTypeToEntity::updateEntityType);
        isUpdating = false;
        if(mapSize != ENTITY_TYPE_CLASS_MAP.size()) save();
    }

    private static <T extends Entity> void updateEntityType(EntityType<T> entityType) {
        try {
            entityType.factory.create(entityType, null);
            if(!ENTITY_TYPE_CLASS_MAP.containsKey(entityType)) throw new RuntimeException();
        } catch (ReturnException ignored) {
        } catch (Throwable e1) {
            try {
                entityType.factory.create(entityType, DummyWorld.INSTANCE_UNSAFE);
                if(!ENTITY_TYPE_CLASS_MAP.containsKey(entityType)) throw new RuntimeException();
            } catch (ReturnException ignored) {
            } catch (Throwable e2) {
                try {
                    entityType.factory.create(entityType, DummyWorld.INSTANCE_REGULAR);
                    if(!ENTITY_TYPE_CLASS_MAP.containsKey(entityType)) throw new RuntimeException();
                } catch (ReturnException ignored) {
                } catch (Throwable e3) {
                    try {
                        if(client.world != null) {
                            System.out.println(entityType);
                            entityType.factory.create(entityType, client.world);
                            if (!ENTITY_TYPE_CLASS_MAP.containsKey(entityType)) throw new RuntimeException();
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    public static final class ReturnException extends RuntimeException {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    @ApiStatus.Internal
    public static final class DummyWorld extends World implements LightSourceView {
        public static final World INSTANCE_UNSAFE;
        public static final World INSTANCE_REGULAR;
        static final Scoreboard SCOREBOARD = new Scoreboard();

        static final DynamicRegistryManager FALLBACK_REGISTRY_MANAGER = new DynamicRegistryManager.Immutable() {
            private static final Map<RegistryKey<?>, Registry<?>> REGISTRIES = new HashMap<>();
            @Override
            public Optional<Registry> getOptional(RegistryKey key) {
                var x = Registries.REGISTRIES.get(key);
                if (x != null) {
                    return Optional.of(x);
                }

                var reg = REGISTRIES.get(key);

                if (reg != null) {
                    return Optional.of(reg);
                }

                return Optional.empty();
            }

            @Override
            public Stream<Entry<?>> streamAllRegistries() {
                return Stream.empty();
            }

            public static void addRegistry(DummyRegistry<?> registry) {
                REGISTRIES.put(registry.getKey(), registry);
            }

            static {
                addRegistry(new DummyRegistry<>(RegistryKeys.DAMAGE_TYPE, Identifier.of("dummy","damage"), new DamageType("", DamageScaling.NEVER, 0)));
                addRegistry(new DummyRegistry<>(RegistryKeys.BANNER_PATTERN, Identifier.of("dummy","pattern"), new BannerPattern(Identifier.of("dummy","pattern"), "")));
                addRegistry(new DummyRegistry<>(RegistryKeys.PAINTING_VARIANT, Identifier.of("dummy","painting"), new PaintingVariant(1, 1, Identifier.of("dummy","painting"), Optional.empty(), Optional.empty())));
                addRegistry(new DummyRegistry<>(RegistryKeys.WOLF_VARIANT, Identifier.of("dummy","wolf"), new WolfVariant(new WolfVariant.WolfAssetInfo(new AssetInfo.TextureAssetInfo(Identifier.of("dummy","wolf")), new AssetInfo.TextureAssetInfo(Identifier.of("dummy","wolf")), new AssetInfo.TextureAssetInfo(Identifier.of("dummy","wolf"))), SpawnConditionSelectors.EMPTY)));
                addRegistry(new DummyRegistry<>(RegistryKeys.COW_VARIANT, Identifier.of("dummy","cow"), new CowVariant(new ModelAndTexture<>(CowVariant.Model.NORMAL, new AssetInfo.TextureAssetInfo(Identifier.of("dummy", "wolf"))), SpawnConditionSelectors.EMPTY)));
                addRegistry(new DummyRegistry<>(RegistryKeys.PIG_VARIANT, Identifier.of("dummy","pig"), new PigVariant(new ModelAndTexture<>(PigVariant.Model.NORMAL, new AssetInfo.TextureAssetInfo(Identifier.of("dummy", "wolf"))), SpawnConditionSelectors.EMPTY)));
                addRegistry(new DummyRegistry<>(RegistryKeys.CHICKEN_VARIANT, Identifier.of("dummy","chicken"), new ChickenVariant(new ModelAndTexture<>(ChickenVariant.Model.NORMAL, new AssetInfo.TextureAssetInfo(Identifier.of("dummy", "wolf"))), SpawnConditionSelectors.EMPTY)));
                addRegistry(new DummyRegistry<>(RegistryKeys.CAT_VARIANT, Identifier.of("dummy","cat"), new CatVariant(new AssetInfo.TextureAssetInfo(Identifier.of("dummy", "cat")), SpawnConditionSelectors.EMPTY)));
                addRegistry(new DummyRegistry<>(RegistryKeys.FROG_VARIANT, Identifier.of("dummy","frog"), new FrogVariant(new AssetInfo.TextureAssetInfo(Identifier.of("dummy", "frog")), SpawnConditionSelectors.EMPTY)));
                addRegistry(new DummyRegistry<>(RegistryKeys.WOLF_SOUND_VARIANT, Identifier.of("dummy","wolf"), SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.Type.CLASSIC)));
            }
        };
        static final ServerRecipeManager RECIPE_MANAGER = new ServerRecipeManager(FALLBACK_REGISTRY_MANAGER);
        private static final FeatureSet FEATURES = FeatureFlags.FEATURE_MANAGER.getFeatureSet();
        private static final FuelRegistry FUEL_REGISTRY = new FuelRegistry.Builder(FALLBACK_REGISTRY_MANAGER, FeatureSet.empty()).build();
        final ChunkManager chunkManager = new ChunkManager() {
            private LightingProvider lightingProvider = null;

            @Nullable @Override public Chunk getChunk(int x, int z, ChunkStatus leastStatus, boolean create) {return null;}
            @Override public void tick(BooleanSupplier shouldKeepTicking, boolean tickChunks) {}
            @Override public String getDebugString() {return "Potato";}
            @Override public int getLoadedChunkCount() {return 0;}
            @Override public BlockView getWorld() {return DummyWorld.this;}
            @Override public LightingProvider getLightingProvider() {
                if (this.lightingProvider == null) {
                    this.lightingProvider = new LightingProvider(new ChunkProvider() {
                        @Override public @NotNull LightSourceView getChunk(int chunkX, int chunkZ) {return DummyWorld.this;}
                        @Override public BlockView getWorld() {return DummyWorld.this;}
                    }, false, false);
                }

                return this.lightingProvider;
            }
        };
        private static final EntityLookup<Entity> ENTITY_LOOKUP = new EntityLookup<>() {
            @Nullable @Override public Entity get(int id) {return null;}
            @Nullable @Override public Entity get(UUID uuid) {return null;}
            @Override public Iterable<Entity> iterate() {return ObjectIterators::emptyIterator;}
            @Override public <U extends Entity> void forEach(TypeFilter<Entity, U> filter, LazyIterationConsumer<U> consumer) {}
            @Override public void forEachIntersects(Box box, Consumer<Entity> action) {}
            @Override public <U extends Entity> void forEachIntersects(TypeFilter<Entity, U> filter, Box box, LazyIterationConsumer<U> consumer) {}
        };
        private static final QueryableTickScheduler<?> DUMMY_SCHEDULER = new QueryableTickScheduler<>() {
            @Override public boolean isTicking(BlockPos pos, Object type) {return false;}
            @Override public void scheduleTick(OrderedTick<Object> orderedTick) {}
            @Override public boolean isQueued(BlockPos pos, Object type) {return false;}
            @Override public int getTickCount() {return 0;}
        };

        static {
            World worldUnsafe, worldDefault;

            @SuppressWarnings("deprecation")
            var dimType = RegistryEntry.Reference.intrusive(new RegistryEntryOwner<>() {}, new DimensionType(OptionalLong.empty(), true, false, false, true, 1.0D, true, false, -64, 384, 384, BlockTags.INFINIBURN_OVERWORLD, DimensionTypes.OVERWORLD_ID, 0.0F, Optional.empty(), new DimensionType.MonsterSettings(false, true, UniformIntProvider.create(0, 7), 0)));
            ((ReferenceAccessor) dimType).callSetRegistryKey(RegistryKey.of(RegistryKeys.DIMENSION_TYPE, Identifier.of("overworld")));
            try {
                worldUnsafe = (DummyWorld) UnsafeAccess.UNSAFE.allocateInstance(DummyWorld.class);
                //noinspection ConstantConditions
                var accessor = (WorldAccessor) worldUnsafe;
                accessor.setBiomeAccess(new BiomeAccess(worldUnsafe, 1L));
                accessor.setDebugWorld(true);
                accessor.setProperties(new DummyWorldProperties());
                accessor.setRegistryKey(RegistryKey.of(RegistryKeys.WORLD, Identifier.of("dummy","world")));
                accessor.setDimensionEntry(dimType);
                accessor.setThread(Thread.currentThread());
                accessor.setRandom(net.minecraft.util.math.random.Random.create());
                //noinspection deprecation
                accessor.setAsyncRandom(Random.createThreadSafe());
                accessor.setBlockEntityTickers(new ArrayList<>());
                accessor.setPendingBlockEntityTickers(new ArrayList<>());
                try {
                    accessor.setDamageSources(new DamageSources(FALLBACK_REGISTRY_MANAGER));
                } catch (Throwable ignored) {}

            } catch (Throwable e) {
                worldUnsafe = null;
            }

            try {
                worldDefault = new DummyWorld(
                        new DummyWorldProperties(),
                        RegistryKey.of(RegistryKeys.WORLD, Identifier.of("dummy", "world")),
                        dimType,
                        false,
                        true,
                        1
                );
            } catch (Throwable e) {
                worldDefault = null;
            }

            //noinspection ConstantConditions
            INSTANCE_UNSAFE = worldUnsafe;
            INSTANCE_REGULAR = worldDefault;
        }

        private final TickManager tickManager = new TickManager();
        private DummyWorld(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> dimensionType, boolean isClient, boolean debugWorld, long seed) {super(properties, registryRef, FALLBACK_REGISTRY_MANAGER, dimensionType, isClient, debugWorld, seed, 0);}
        @Override public void updateListeners(BlockPos pos, BlockState oldState, BlockState newState, int flags) {}
        @Override public void playSound(@Nullable Entity source, double x, double y, double z, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed) {}
        @Override public void playSoundFromEntity(@Nullable Entity source, Entity entity, RegistryEntry<SoundEvent> sound, SoundCategory category, float volume, float pitch, long seed) {}

        @Override
        public void createExplosion(@Nullable Entity entity, @Nullable DamageSource damageSource, @Nullable ExplosionBehavior behavior, double x, double y, double z, float power, boolean createFire, ExplosionSourceType explosionSourceType, ParticleEffect smallParticle, ParticleEffect largeParticle, Pool<BlockParticleEffect> blockParticles, RegistryEntry<SoundEvent> soundEvent) {}

        @Override public String asString() {return "DummyWorld!";}

        @Override
        public void setSpawnPoint(WorldProperties.SpawnPoint spawnPoint) {

        }

        @Override
        public WorldProperties.SpawnPoint getSpawnPoint() {
            return null;
        }

        @Nullable @Override public Entity getEntityById(int id) {return null;}
        @Override public Collection<EnderDragonPart> getEnderDragonParts() {return List.of();}
        @Override public TickManager getTickManager() {return this.tickManager;}
        @Nullable @Override public MapState getMapState(MapIdComponent id) {return null;}
        @Override public void setBlockBreakingInfo(int entityId, BlockPos pos, int progress) {}
        @Override public Scoreboard getScoreboard() {return SCOREBOARD;}
        @Override public RecipeManager getRecipeManager() {return RECIPE_MANAGER;}
        @Override protected EntityLookup<Entity> getEntityLookup() {return ENTITY_LOOKUP;}
        @Override public QueryableTickScheduler<Block> getBlockTickScheduler() {return (QueryableTickScheduler<Block>) DUMMY_SCHEDULER;}
        @Override public QueryableTickScheduler<Fluid> getFluidTickScheduler() {return (QueryableTickScheduler<Fluid>) DUMMY_SCHEDULER;}
        @Override public ChunkManager getChunkManager() {return chunkManager;}
        @Override public void syncWorldEvent(@Nullable Entity source, int eventId, BlockPos pos, int data) {}
        @Override public void emitGameEvent(RegistryEntry<GameEvent> event, Vec3d emitterPos, GameEvent.Emitter emitter) {}
        @Override public DynamicRegistryManager getRegistryManager() {return FALLBACK_REGISTRY_MANAGER;}
        @Override public BrewingRecipeRegistry getBrewingRecipeRegistry() {return null;}
        @Override public FuelRegistry getFuelRegistry() {return FUEL_REGISTRY;}
        @Override public FeatureSet getEnabledFeatures() {return FEATURES;}
        @Override public float getBrightness(Direction direction, boolean shaded) {return 0;}
        @Override public List<? extends PlayerEntity> getPlayers() {return Collections.emptyList();}
        @Override public RegistryEntry<Biome> getGeneratorStoredBiome(int biomeX, int biomeY, int biomeZ) {return null;}
        @Override public int getSeaLevel() {return 0;}
        @Override public void forEachLightSource(BiConsumer<BlockPos, BlockState> callback) {}
        @Override public ChunkSkyLight getChunkSkyLight() {return null;}

        @Override
        public WorldBorder getWorldBorder() {
            return null;
        }

        static class DummyWorldProperties implements MutableWorldProperties {
            @Override public SpawnPoint getSpawnPoint() {return SpawnPoint.DEFAULT;}
            @Override public long getTime() {return 0;}
            @Override public long getTimeOfDay() {return 0;}
            @Override public boolean isThundering() {return false;}
            @Override public boolean isRaining() {return false;}
            @Override public void setRaining(boolean raining) {}
            @Override public boolean isHardcore() {return false;}
            @Override public Difficulty getDifficulty() {return Difficulty.NORMAL;}
            @Override public boolean isDifficultyLocked() {return false;}
            @Override public void setSpawnPoint(SpawnPoint spawnPoint) {}
        }
    }

    public record DummyRegistry<T>(RegistryKey<? extends Registry<T>> registryKey, Identifier defaultId, T defaultValue) implements Registry<T>, RegistryEntryOwner<T> {
        @Override public RegistryKey<? extends Registry<T>> getKey() {return registryKey;}
        @Nullable @Override public Identifier getId(T value) {return defaultId;}
        @Override public Optional<RegistryKey<T>> getKey(T entry) {return Optional.of(RegistryKey.of(registryKey, defaultId));}
        @Override public int getRawId(@Nullable T value) {return 0;}
        @Nullable @Override public T get(int index) {return defaultValue;}
        @Override public int size() {return 1;}
        @Nullable @Override public T get(@Nullable RegistryKey<T> key) {return defaultValue;}
        @Nullable @Override public T get(@Nullable Identifier id) {return defaultValue;}
        @Override public Optional<RegistryEntryInfo> getEntryInfo(RegistryKey<T> key) {return Optional.of(RegistryEntryInfo.DEFAULT);}
        @Override public Lifecycle getLifecycle() {return Lifecycle.experimental();}
        @Override public Optional<RegistryEntry.Reference<T>> getDefaultEntry() {return Optional.of(createEntry(defaultValue));}
        @Override public Set<Identifier> getIds() {return Set.of(defaultId);}
        @Override public Set<Map.Entry<RegistryKey<T>, T>> getEntrySet() {return Set.of();}
        @Override public Set<RegistryKey<T>> getKeys() {return Set.of();}
        @Override public Optional<RegistryEntry.Reference<T>> getRandom(Random random) {return Optional.empty();}
        @Override public boolean containsId(Identifier id) {return true;}
        @Override public boolean contains(RegistryKey<T> key) {return true;}
        @Override public Registry<T> freeze() {return this;}
        @SuppressWarnings("deprecation") @Override public RegistryEntry.Reference<T> createEntry(T value) {return RegistryEntry.Reference.intrusive(this, value);}
        @Override public Optional<RegistryEntry.Reference<T>> getEntry(int rawId) {return getDefaultEntry();}
        @Override public Optional<RegistryEntry.Reference<T>> getEntry(Identifier id) {return getDefaultEntry();}
        @Override public RegistryEntry<T> getEntry(T value) {return RegistryEntry.of(value);}
        @Override public Stream<RegistryEntry.Reference<T>> streamEntries() {return Stream.empty();}
        @Override public Stream<RegistryEntryList.Named<T>> getTags() {return Stream.empty();}
        @Override public Stream<RegistryEntryList.Named<T>> streamTags() {return Stream.empty();}
        @Override public PendingTagLoad<T> startTagReload(TagGroupLoader.RegistryTags<T> tags) {return null;}
        @Override public Optional<RegistryEntry.Reference<T>> getOptional(RegistryKey<T> key) {return getEntry(key.getValue());}
        @Override public Optional<RegistryEntryList.Named<T>> getOptional(TagKey<T> tag) {return Optional.empty();}

        @NotNull @Override public Iterator<T> iterator() {
            return new Iterator<>() {
                @Override public boolean hasNext() {return false;}
                @Override public T next() {return null;}
            };
        }
    }
}
