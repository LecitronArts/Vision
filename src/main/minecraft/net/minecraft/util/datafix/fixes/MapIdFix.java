package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;

public class MapIdFix extends DataFix {
   public MapIdFix(Schema pOutputSchema, boolean pChangesType) {
      super(pOutputSchema, pChangesType);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped("Map id fix", this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA), (p_296632_) -> {
         return p_296632_.update(DSL.remainderFinder(), (p_145512_) -> {
            return p_145512_.createMap(ImmutableMap.of(p_145512_.createString("data"), p_145512_));
         });
      });
   }
}