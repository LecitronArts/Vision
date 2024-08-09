package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;

public record ConstantValue(float value) implements NumberProvider {
   public static final Codec<ConstantValue> CODEC = RecordCodecBuilder.create((p_299462_) -> {
      return p_299462_.group(Codec.FLOAT.fieldOf("value").forGetter(ConstantValue::value)).apply(p_299462_, ConstantValue::new);
   });
   public static final Codec<ConstantValue> INLINE_CODEC = Codec.FLOAT.xmap(ConstantValue::new, ConstantValue::value);

   public LootNumberProviderType getType() {
      return NumberProviders.CONSTANT;
   }

   public float getFloat(LootContext pLootContext) {
      return this.value;
   }

   public static ConstantValue exactly(float pValue) {
      return new ConstantValue(pValue);
   }

   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (pOther != null && this.getClass() == pOther.getClass()) {
         return Float.compare(((ConstantValue)pOther).value, this.value) == 0;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.value != 0.0F ? Float.floatToIntBits(this.value) : 0;
   }
}