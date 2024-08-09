package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.BiFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;

public class SequenceFunction implements LootItemFunction {
   public static final Codec<SequenceFunction> CODEC = RecordCodecBuilder.create((p_300473_) -> {
      return p_300473_.group(LootItemFunctions.CODEC.listOf().fieldOf("functions").forGetter((p_298675_) -> {
         return p_298675_.functions;
      })).apply(p_300473_, SequenceFunction::new);
   });
   public static final Codec<SequenceFunction> INLINE_CODEC = LootItemFunctions.CODEC.listOf().xmap(SequenceFunction::new, (p_298151_) -> {
      return p_298151_.functions;
   });
   private final List<LootItemFunction> functions;
   private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;

   private SequenceFunction(List<LootItemFunction> p_297875_) {
      this.functions = p_297875_;
      this.compositeFunction = LootItemFunctions.compose(p_297875_);
   }

   public static SequenceFunction of(List<LootItemFunction> pFunctions) {
      return new SequenceFunction(List.copyOf(pFunctions));
   }

   public ItemStack apply(ItemStack pStack, LootContext pContext) {
      return this.compositeFunction.apply(pStack, pContext);
   }

   public void validate(ValidationContext pContext) {
      LootItemFunction.super.validate(pContext);

      for(int i = 0; i < this.functions.size(); ++i) {
         this.functions.get(i).validate(pContext.forChild(".function[" + i + "]"));
      }

   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SEQUENCE;
   }
}