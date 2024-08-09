package net.minecraft.commands.functions;

import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.execution.UnboundEntryAction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record PlainTextFunction<T>(ResourceLocation id, List<UnboundEntryAction<T>> entries) implements CommandFunction<T>, InstantiatedFunction<T> {
   public InstantiatedFunction<T> instantiate(@Nullable CompoundTag p_311629_, CommandDispatcher<T> p_311161_, T p_313022_) throws FunctionInstantiationException {
      return this;
   }

   public ResourceLocation id() {
      return this.id;
   }

   public List<UnboundEntryAction<T>> entries() {
      return this.entries;
   }
}