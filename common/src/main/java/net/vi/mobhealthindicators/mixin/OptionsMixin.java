package net.vi.mobhealthindicators.mixin;

import com.google.common.collect.Lists;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.vi.mobhealthindicators.ModInit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Options.class)
public class OptionsMixin {
    @Mutable
    @Shadow
    @Final
    public KeyMapping[] keyMappings;

    @Inject(at = @At("HEAD"), method = "load()V")
    public void loadHook(CallbackInfo info) {
        List<KeyMapping> keys = Lists.newArrayList(keyMappings);
        keys.add(ModInit.toggleKey);
        keys.add(ModInit.overrideFiltersKey);
        keyMappings = keys.toArray(KeyMapping[]::new);
    }
}
