package net.vi.mobhealthindicators.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import net.vi.mobhealthindicators.EntityTypeToEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.vi.mobhealthindicators.EntityTypeToEntity.ENTITY_TYPE_CLASS_MAP;
import static net.vi.mobhealthindicators.EntityTypeToEntity.isUpdating;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "<init>", at = @At("CTOR_HEAD"))
    @SuppressWarnings("unchecked")
    private void saveType(EntityType<?> type, World world, CallbackInfo ci) {
        if(isUpdating) {
            ENTITY_TYPE_CLASS_MAP.put(type, ((Class<? extends Entity>) ((Object) getClass())));
            throw new EntityTypeToEntity.ReturnException();
        }
    }
}
