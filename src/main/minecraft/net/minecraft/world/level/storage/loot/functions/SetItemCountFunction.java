package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Set;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetItemCountFunction extends LootItemConditionalFunction {
   public static final Codec<SetItemCountFunction> CODEC = RecordCodecBuilder.create((p_297145_) -> {
      return commonFields(p_297145_).and(p_297145_.group(NumberProviders.CODEC.fieldOf("count").forGetter((p_297138_) -> {
         return p_297138_.value;
      }), Codec.BOOL.fieldOf("add").orElse(false).forGetter((p_297139_) -> {
         return p_297139_.add;
      }))).apply(p_297145_, SetItemCountFunction::new);
   });
   private final NumberProvider value;
   private final boolean add;

   private SetItemCountFunction(List<LootItemCondition> p_298181_, NumberProvider p_165410_, boolean p_165411_) {
      super(p_298181_);
      this.value = p_165410_;
      this.add = p_165411_;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_COUNT;
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.value.getReferencedContextParams();
   }

   public ItemStack run(ItemStack pStack, LootContext pContext) {
      int i = this.add ? pStack.getCount() : 0;
      pStack.setCount(Mth.clamp(i + this.value.getInt(pContext), 0, pStack.getMaxStackSize()));
      return pStack;
   }

   public static LootItemConditionalFunction.Builder<?> setCount(NumberProvider pCountValue) {
      return simpleBuilder((p_297144_) -> {
         return new SetItemCountFunction(p_297144_, pCountValue, false);
      });
   }

   public static LootItemConditionalFunction.Builder<?> setCount(NumberProvider pCountValue, boolean pAdd) {
      return simpleBuilder((p_297142_) -> {
         return new SetItemCountFunction(p_297142_, pCountValue, pAdd);
      });
   }
}