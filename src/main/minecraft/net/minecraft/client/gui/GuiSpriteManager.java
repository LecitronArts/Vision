package net.minecraft.client.gui;

import java.util.Set;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiSpriteManager extends TextureAtlasHolder {
   private static final Set<MetadataSectionSerializer<?>> METADATA_SECTIONS = Set.of(AnimationMetadataSection.SERIALIZER, GuiMetadataSection.TYPE);

   public GuiSpriteManager(TextureManager pTextureManager) {
      super(pTextureManager, new ResourceLocation("textures/atlas/gui.png"), new ResourceLocation("gui"), METADATA_SECTIONS);
   }

   public TextureAtlasSprite getSprite(ResourceLocation pLocation) {
      return super.getSprite(pLocation);
   }

   public GuiSpriteScaling getSpriteScaling(TextureAtlasSprite pSprite) {
      return this.getMetadata(pSprite).scaling();
   }

   private GuiMetadataSection getMetadata(TextureAtlasSprite pSprite) {
      return pSprite.contents().metadata().getSection(GuiMetadataSection.TYPE).orElse(GuiMetadataSection.DEFAULT);
   }
}