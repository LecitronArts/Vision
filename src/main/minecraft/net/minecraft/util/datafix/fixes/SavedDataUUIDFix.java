package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class SavedDataUUIDFix extends AbstractUUIDFix {
   private static final Logger LOGGER = LogUtils.getLogger();

   public SavedDataUUIDFix(Schema pOutputSchema) {
      super(pOutputSchema, References.SAVED_DATA_RAIDS);
   }

   protected TypeRewriteRule makeRule() {
      return this.fixTypeEverywhereTyped("SavedDataUUIDFix", this.getInputSchema().getType(this.typeReference), (p_145672_) -> {
         return p_145672_.update(DSL.remainderFinder(), (p_296635_) -> {
            return p_296635_.update("data", (p_145674_) -> {
               return p_145674_.update("Raids", (p_145676_) -> {
                  return p_145676_.createList(p_145676_.asStream().map((p_145678_) -> {
                     return p_145678_.update("HeroesOfTheVillage", (p_145680_) -> {
                        return p_145680_.createList(p_145680_.asStream().map((p_145682_) -> {
                           return createUUIDFromLongs(p_145682_, "UUIDMost", "UUIDLeast").orElseGet(() -> {
                              LOGGER.warn("HeroesOfTheVillage contained invalid UUIDs.");
                              return p_145682_;
                           });
                        }));
                     });
                  }));
               });
            });
         });
      });
   }
}