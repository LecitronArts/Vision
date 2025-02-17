package net.optifine.util;

import net.minecraft.util.Mth;

public class MathUtilsTest {
   public static void main(String[] args) throws Exception {
      MathUtilsTest.OPER[] amathutilstest$oper = MathUtilsTest.OPER.values();

      for(int i = 0; i < amathutilstest$oper.length; ++i) {
         MathUtilsTest.OPER mathutilstest$oper = amathutilstest$oper[i];
         dbg("******** " + mathutilstest$oper + " ***********");
         test(mathutilstest$oper, false);
      }

   }

   private static void test(MathUtilsTest.OPER oper, boolean fast) {
      Mth.fastMath = fast;
      double d0;
      double d1;
      switch (oper) {
         case SIN:
         case COS:
            d0 = (double)-(float)Math.PI;
            d1 = (double)(float)Math.PI;
            break;
         case ASIN:
         case ACOS:
            d0 = -1.0D;
            d1 = 1.0D;
            break;
         default:
            return;
      }

      int i = 10;

      for(int j = 0; j <= i; ++j) {
         double d2 = d0 + (double)j * (d1 - d0) / (double)i;
         float f;
         float f1;
         switch (oper) {
            case SIN:
               f = (float)Math.sin(d2);
               f1 = Mth.sin((float)d2);
               break;
            case COS:
               f = (float)Math.cos(d2);
               f1 = Mth.cos((float)d2);
               break;
            case ASIN:
               f = (float)Math.asin(d2);
               f1 = MathUtils.asin((float)d2);
               break;
            case ACOS:
               f = (float)Math.acos(d2);
               f1 = MathUtils.acos((float)d2);
               break;
            default:
               return;
         }

         dbg(String.format("%.2f, Math: %f, Helper: %f, diff: %f", d2, f, f1, Math.abs(f - f1)));
      }

   }

   public static void dbg(String str) {
      System.out.println(str);
   }

   private static enum OPER {
      SIN,
      COS,
      ASIN,
      ACOS;
   }
}