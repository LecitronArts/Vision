package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import net.minecraft.Util;

public class EntityHorseSplitFix extends EntityRenameFix {
   public EntityHorseSplitFix(Schema pOutputSchema, boolean pChangesType) {
      super("EntityHorseSplitFix", pOutputSchema, pChangesType);
   }

   protected Pair<String, Typed<?>> fix(String pEntityName, Typed<?> pTyped) {
      Dynamic<?> dynamic = pTyped.get(DSL.remainderFinder());
      if (Objects.equals("EntityHorse", pEntityName)) {
         int i = dynamic.get("Type").asInt(0);
         String s1;
         switch (i) {
            case 1:
               s1 = "Donkey";
               break;
            case 2:
               s1 = "Mule";
               break;
            case 3:
               s1 = "ZombieHorse";
               break;
            case 4:
               s1 = "SkeletonHorse";
               break;
            default:
               s1 = "Horse";
         }

         String s = s1;
         dynamic.remove("Type");
         Type<?> type = this.getOutputSchema().findChoiceType(References.ENTITY).types().get(s);
         return Pair.of(s, Util.writeAndReadTypedOrThrow(pTyped, type, (p_308980_) -> {
            return p_308980_;
         }));
      } else {
         return Pair.of(pEntityName, pTyped);
      }
   }
}