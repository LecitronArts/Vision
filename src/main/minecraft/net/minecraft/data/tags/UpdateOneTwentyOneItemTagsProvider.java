package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class UpdateOneTwentyOneItemTagsProvider extends ItemTagsProvider {
   public UpdateOneTwentyOneItemTagsProvider(PackOutput p_309895_, CompletableFuture<HolderLookup.Provider> p_312150_, CompletableFuture<TagsProvider.TagLookup<Item>> p_311361_, CompletableFuture<TagsProvider.TagLookup<Block>> p_310346_) {
      super(p_309895_, p_312150_, p_311361_, p_310346_);
   }

   protected void addTags(HolderLookup.Provider p_312918_) {
      this.tag(ItemTags.STAIRS).add(Items.TUFF_STAIRS, Items.POLISHED_TUFF_STAIRS, Items.TUFF_BRICK_STAIRS);
      this.tag(ItemTags.SLABS).add(Items.TUFF_SLAB, Items.POLISHED_TUFF_SLAB, Items.TUFF_BRICK_SLAB);
      this.tag(ItemTags.WALLS).add(Items.TUFF_WALL, Items.POLISHED_TUFF_WALL, Items.TUFF_BRICK_WALL);
   }
}