package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import net.minecraft.util.datafix.ComponentDataFixUtils;

public class TeamDisplayNameFix extends DataFix {
   public TeamDisplayNameFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   protected TypeRewriteRule makeRule() {
      Type<Pair<String, Dynamic<?>>> type = DSL.named(References.TEAM.typeName(), DSL.remainderType());
      if (!Objects.equals(type, this.getInputSchema().getType(References.TEAM))) {
         throw new IllegalStateException("Team type is not what was expected.");
      } else {
         return this.fixTypeEverywhere("TeamDisplayNameFix", type, (p_17011_) -> {
            return (p_145726_) -> {
               return p_145726_.mapSecond((p_309000_) -> {
                  return p_309000_.update("DisplayName", ComponentDataFixUtils::wrapLiteralStringAsComponent);
               });
            };
         });
      }
   }
}