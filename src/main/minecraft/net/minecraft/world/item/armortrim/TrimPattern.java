package net.minecraft.world.item.armortrim;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

public record TrimPattern(ResourceLocation assetId, Holder<Item> templateItem, Component description, boolean decal) {
   public static final Codec<TrimPattern> DIRECT_CODEC = RecordCodecBuilder.create((p_309249_) -> {
      return p_309249_.group(ResourceLocation.CODEC.fieldOf("asset_id").forGetter(TrimPattern::assetId), RegistryFixedCodec.create(Registries.ITEM).fieldOf("template_item").forGetter(TrimPattern::templateItem), ComponentSerialization.CODEC.fieldOf("description").forGetter(TrimPattern::description), Codec.BOOL.fieldOf("decal").orElse(false).forGetter(TrimPattern::decal)).apply(p_309249_, TrimPattern::new);
   });
   public static final Codec<Holder<TrimPattern>> CODEC = RegistryFileCodec.create(Registries.TRIM_PATTERN, DIRECT_CODEC);

   public Component copyWithStyle(Holder<TrimMaterial> pTrimMaterial) {
      return this.description.copy().withStyle(pTrimMaterial.value().description().getStyle());
   }
}