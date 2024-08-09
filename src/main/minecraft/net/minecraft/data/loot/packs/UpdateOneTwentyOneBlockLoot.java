package net.minecraft.data.loot.packs;

import java.util.Set;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Blocks;

public class UpdateOneTwentyOneBlockLoot extends BlockLootSubProvider {
   protected UpdateOneTwentyOneBlockLoot() {
      super(Set.of(), FeatureFlagSet.of(FeatureFlags.UPDATE_1_21));
   }

   protected void generate() {
      this.dropSelf(Blocks.CRAFTER);
      this.dropSelf(Blocks.CHISELED_TUFF);
      this.dropSelf(Blocks.TUFF_STAIRS);
      this.dropSelf(Blocks.TUFF_WALL);
      this.dropSelf(Blocks.POLISHED_TUFF);
      this.dropSelf(Blocks.POLISHED_TUFF_STAIRS);
      this.dropSelf(Blocks.POLISHED_TUFF_WALL);
      this.dropSelf(Blocks.TUFF_BRICKS);
      this.dropSelf(Blocks.TUFF_BRICK_STAIRS);
      this.dropSelf(Blocks.TUFF_BRICK_WALL);
      this.dropSelf(Blocks.CHISELED_TUFF_BRICKS);
      this.add(Blocks.TUFF_SLAB, (p_309884_) -> {
         return this.createSlabItemTable(p_309884_);
      });
      this.add(Blocks.TUFF_BRICK_SLAB, (p_312067_) -> {
         return this.createSlabItemTable(p_312067_);
      });
      this.add(Blocks.POLISHED_TUFF_SLAB, (p_312759_) -> {
         return this.createSlabItemTable(p_312759_);
      });
      this.dropSelf(Blocks.CHISELED_COPPER);
      this.dropSelf(Blocks.EXPOSED_CHISELED_COPPER);
      this.dropSelf(Blocks.WEATHERED_CHISELED_COPPER);
      this.dropSelf(Blocks.OXIDIZED_CHISELED_COPPER);
      this.dropSelf(Blocks.WAXED_CHISELED_COPPER);
      this.dropSelf(Blocks.WAXED_EXPOSED_CHISELED_COPPER);
      this.dropSelf(Blocks.WAXED_WEATHERED_CHISELED_COPPER);
      this.dropSelf(Blocks.WAXED_OXIDIZED_CHISELED_COPPER);
      this.add(Blocks.COPPER_DOOR, (p_310248_) -> {
         return this.createDoorTable(p_310248_);
      });
      this.add(Blocks.EXPOSED_COPPER_DOOR, (p_310936_) -> {
         return this.createDoorTable(p_310936_);
      });
      this.add(Blocks.WEATHERED_COPPER_DOOR, (p_311974_) -> {
         return this.createDoorTable(p_311974_);
      });
      this.add(Blocks.OXIDIZED_COPPER_DOOR, (p_312652_) -> {
         return this.createDoorTable(p_312652_);
      });
      this.add(Blocks.WAXED_COPPER_DOOR, (p_311328_) -> {
         return this.createDoorTable(p_311328_);
      });
      this.add(Blocks.WAXED_EXPOSED_COPPER_DOOR, (p_310922_) -> {
         return this.createDoorTable(p_310922_);
      });
      this.add(Blocks.WAXED_WEATHERED_COPPER_DOOR, (p_310816_) -> {
         return this.createDoorTable(p_310816_);
      });
      this.add(Blocks.WAXED_OXIDIZED_COPPER_DOOR, (p_310706_) -> {
         return this.createDoorTable(p_310706_);
      });
      this.dropSelf(Blocks.COPPER_TRAPDOOR);
      this.dropSelf(Blocks.EXPOSED_COPPER_TRAPDOOR);
      this.dropSelf(Blocks.WEATHERED_COPPER_TRAPDOOR);
      this.dropSelf(Blocks.OXIDIZED_COPPER_TRAPDOOR);
      this.dropSelf(Blocks.WAXED_COPPER_TRAPDOOR);
      this.dropSelf(Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR);
      this.dropSelf(Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR);
      this.dropSelf(Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR);
      this.dropSelf(Blocks.COPPER_GRATE);
      this.dropSelf(Blocks.EXPOSED_COPPER_GRATE);
      this.dropSelf(Blocks.WEATHERED_COPPER_GRATE);
      this.dropSelf(Blocks.OXIDIZED_COPPER_GRATE);
      this.dropSelf(Blocks.WAXED_COPPER_GRATE);
      this.dropSelf(Blocks.WAXED_EXPOSED_COPPER_GRATE);
      this.dropSelf(Blocks.WAXED_WEATHERED_COPPER_GRATE);
      this.dropSelf(Blocks.WAXED_OXIDIZED_COPPER_GRATE);
      this.dropSelf(Blocks.COPPER_BULB);
      this.dropSelf(Blocks.EXPOSED_COPPER_BULB);
      this.dropSelf(Blocks.WEATHERED_COPPER_BULB);
      this.dropSelf(Blocks.OXIDIZED_COPPER_BULB);
      this.dropSelf(Blocks.WAXED_COPPER_BULB);
      this.dropSelf(Blocks.WAXED_EXPOSED_COPPER_BULB);
      this.dropSelf(Blocks.WAXED_WEATHERED_COPPER_BULB);
      this.dropSelf(Blocks.WAXED_OXIDIZED_COPPER_BULB);
      this.add(Blocks.TRIAL_SPAWNER, noDrop());
   }
}