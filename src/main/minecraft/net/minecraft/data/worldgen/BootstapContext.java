package net.minecraft.data.worldgen;

import com.mojang.serialization.Lifecycle;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public interface BootstapContext<T> {
   Holder.Reference<T> register(ResourceKey<T> pKey, T pValue, Lifecycle pLifecycle);

   default Holder.Reference<T> register(ResourceKey<T> pKey, T pValue) {
      return this.register(pKey, pValue, Lifecycle.stable());
   }

   <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> pRegistryKey);
}