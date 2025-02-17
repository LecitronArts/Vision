package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetBannerPatternFunction extends LootItemConditionalFunction {
   private static final Codec<Pair<Holder<BannerPattern>, DyeColor>> PATTERN_CODEC = Codec.mapPair(BuiltInRegistries.BANNER_PATTERN.holderByNameCodec().fieldOf("pattern"), DyeColor.CODEC.fieldOf("color")).codec();
   public static final Codec<SetBannerPatternFunction> CODEC = RecordCodecBuilder.create((p_298703_) -> {
      return commonFields(p_298703_).and(p_298703_.group(PATTERN_CODEC.listOf().fieldOf("patterns").forGetter((p_299571_) -> {
         return p_299571_.patterns;
      }), Codec.BOOL.fieldOf("append").forGetter((p_301183_) -> {
         return p_301183_.append;
      }))).apply(p_298703_, SetBannerPatternFunction::new);
   });
   private final List<Pair<Holder<BannerPattern>, DyeColor>> patterns;
   private final boolean append;

   SetBannerPatternFunction(List<LootItemCondition> p_165276_, List<Pair<Holder<BannerPattern>, DyeColor>> p_298427_, boolean p_165277_) {
      super(p_165276_);
      this.patterns = p_298427_;
      this.append = p_165277_;
   }

   protected ItemStack run(ItemStack pStack, LootContext pContext) {
      CompoundTag compoundtag = BlockItem.getBlockEntityData(pStack);
      if (compoundtag == null) {
         compoundtag = new CompoundTag();
      }

      BannerPattern.Builder bannerpattern$builder = new BannerPattern.Builder();
      this.patterns.forEach(bannerpattern$builder::addPattern);
      ListTag listtag = bannerpattern$builder.toListTag();
      ListTag listtag1;
      if (this.append) {
         listtag1 = compoundtag.getList("Patterns", 10).copy();
         listtag1.addAll(listtag);
      } else {
         listtag1 = listtag;
      }

      compoundtag.put("Patterns", listtag1);
      BlockItem.setBlockEntityData(pStack, BlockEntityType.BANNER, compoundtag);
      return pStack;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_BANNER_PATTERN;
   }

   public static SetBannerPatternFunction.Builder setBannerPattern(boolean pAppend) {
      return new SetBannerPatternFunction.Builder(pAppend);
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetBannerPatternFunction.Builder> {
      private final ImmutableList.Builder<Pair<Holder<BannerPattern>, DyeColor>> patterns = ImmutableList.builder();
      private final boolean append;

      Builder(boolean pAppend) {
         this.append = pAppend;
      }

      protected SetBannerPatternFunction.Builder getThis() {
         return this;
      }

      public LootItemFunction build() {
         return new SetBannerPatternFunction(this.getConditions(), this.patterns.build(), this.append);
      }

      public SetBannerPatternFunction.Builder addPattern(ResourceKey<BannerPattern> pPattern, DyeColor pColor) {
         return this.addPattern(BuiltInRegistries.BANNER_PATTERN.getHolderOrThrow(pPattern), pColor);
      }

      public SetBannerPatternFunction.Builder addPattern(Holder<BannerPattern> pPattern, DyeColor pColor) {
         this.patterns.add(Pair.of(pPattern, pColor));
         return this;
      }
   }
}