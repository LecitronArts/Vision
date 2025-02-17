package net.minecraft.client.resources;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.optifine.util.TextureUtils;

public class PaintingTextureManager extends TextureAtlasHolder {
   private static final ResourceLocation BACK_SPRITE_LOCATION = new ResourceLocation("back");

   public PaintingTextureManager(TextureManager pTextureManager) {
      super(pTextureManager, new ResourceLocation("textures/atlas/paintings.png"), new ResourceLocation("paintings"));
   }

   public TextureAtlasSprite get(PaintingVariant pPaintingVariant) {
      TextureAtlasSprite textureatlassprite = this.getSprite(BuiltInRegistries.PAINTING_VARIANT.getKey(pPaintingVariant));
      return TextureUtils.getCustomSprite(textureatlassprite);
   }

   public TextureAtlasSprite getBackSprite() {
      return this.getSprite(BACK_SPRITE_LOCATION);
   }
}