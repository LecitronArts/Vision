package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
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
import org.slf4j.Logger;

public class SetItemDamageFunction extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<SetItemDamageFunction> CODEC = RecordCodecBuilder.create((p_297150_) -> {
      return commonFields(p_297150_).and(p_297150_.group(NumberProviders.CODEC.fieldOf("damage").forGetter((p_297149_) -> {
         return p_297149_.damage;
      }), Codec.BOOL.fieldOf("add").orElse(false).forGetter((p_297151_) -> {
         return p_297151_.add;
      }))).apply(p_297150_, SetItemDamageFunction::new);
   });
   private final NumberProvider damage;
   private final boolean add;

   private SetItemDamageFunction(List<LootItemCondition> p_300823_, NumberProvider p_165428_, boolean p_165429_) {
      super(p_300823_);
      this.damage = p_165428_;
      this.add = p_165429_;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_DAMAGE;
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.damage.getReferencedContextParams();
   }

   public ItemStack run(ItemStack pStack, LootContext pContext) {
      if (pStack.isDamageableItem()) {
         int i = pStack.getMaxDamage();
         float f = this.add ? 1.0F - (float)pStack.getDamageValue() / (float)i : 0.0F;
         float f1 = 1.0F - Mth.clamp(this.damage.getFloat(pContext) + f, 0.0F, 1.0F);
         pStack.setDamageValue(Mth.floor(f1 * (float)i));
      } else {
         LOGGER.warn("Couldn't set damage of loot item {}", (Object)pStack);
      }

      return pStack;
   }

   public static LootItemConditionalFunction.Builder<?> setDamage(NumberProvider pDamageValue) {
      return simpleBuilder((p_297153_) -> {
         return new SetItemDamageFunction(p_297153_, pDamageValue, false);
      });
   }

   public static LootItemConditionalFunction.Builder<?> setDamage(NumberProvider pDamageValue, boolean pAdd) {
      return simpleBuilder((p_297148_) -> {
         return new SetItemDamageFunction(p_297148_, pDamageValue, pAdd);
      });
   }
}