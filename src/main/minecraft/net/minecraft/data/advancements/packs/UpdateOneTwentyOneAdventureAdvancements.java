package net.minecraft.data.advancements.packs;

import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.advancements.AdvancementSubProvider;
import net.minecraft.world.entity.EntityType;

public class UpdateOneTwentyOneAdventureAdvancements implements AdvancementSubProvider {
   public void generate(HolderLookup.Provider p_312808_, Consumer<AdvancementHolder> p_313044_) {
      AdvancementHolder advancementholder = AdvancementSubProvider.createPlaceholder("adventure/root");
      VanillaAdventureAdvancements.createMonsterHunterAdvancement(advancementholder, p_313044_, Stream.concat(VanillaAdventureAdvancements.MOBS_TO_KILL.stream(), Stream.of(EntityType.BREEZE)).collect(Collectors.toList()));
   }
}