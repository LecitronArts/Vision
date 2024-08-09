package net.minecraft.network.chat.numbers;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class BlankFormat implements NumberFormat {
   public static final BlankFormat INSTANCE = new BlankFormat();
   public static final NumberFormatType<BlankFormat> TYPE = new NumberFormatType<BlankFormat>() {
      private static final MapCodec<BlankFormat> CODEC = MapCodec.unit(BlankFormat.INSTANCE);

      public MapCodec<BlankFormat> mapCodec() {
         return CODEC;
      }

      public void writeToStream(FriendlyByteBuf p_313166_, BlankFormat p_309506_) {
      }

      public BlankFormat readFromStream(FriendlyByteBuf p_311507_) {
         return BlankFormat.INSTANCE;
      }
   };

   public MutableComponent format(int p_310442_) {
      return Component.empty();
   }

   public NumberFormatType<BlankFormat> type() {
      return TYPE;
   }
}