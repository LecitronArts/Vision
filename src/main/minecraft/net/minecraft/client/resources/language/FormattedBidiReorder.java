package net.minecraft.client.resources.language;

import com.google.common.collect.Lists;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.BidiRun;
import java.util.List;

import icyllis.modernui.mc.text.FormattedTextWrapper;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.SubStringSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FormattedBidiReorder {
   public static FormattedCharSequence reorder(FormattedText pText, boolean pDefaultRightToLeft) {
      return new FormattedTextWrapper(pText);
   }

   private static String shape(String p_118930_) {
      try {
         return (new ArabicShaping(8)).shape(p_118930_);
      } catch (Exception exception) {
         return p_118930_;
      }
   }
}