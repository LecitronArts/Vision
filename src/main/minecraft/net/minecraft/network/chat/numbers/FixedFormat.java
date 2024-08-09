package net.minecraft.network.chat.numbers;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;

public class FixedFormat implements NumberFormat {
   public static final NumberFormatType<FixedFormat> TYPE = new NumberFormatType<FixedFormat>() {
      private static final MapCodec<FixedFormat> CODEC = ComponentSerialization.CODEC.fieldOf("value").xmap(FixedFormat::new, (p_311625_) -> {
         return p_311625_.value;
      });

      public MapCodec<FixedFormat> mapCodec() {
         return CODEC;
      }

      public void writeToStream(FriendlyByteBuf p_311899_, FixedFormat p_310760_) {
         p_311899_.writeComponent(p_310760_.value);
      }

      public FixedFormat readFromStream(FriendlyByteBuf p_312644_) {
         Component component = p_312644_.readComponentTrusted();
         return new FixedFormat(component);
      }
   };
   final Component value;

   public FixedFormat(Component pValue) {
      this.value = pValue;
   }

   public MutableComponent format(int pNumber) {
      return this.value.copy();
   }

   public NumberFormatType<FixedFormat> type() {
      return TYPE;
   }
}