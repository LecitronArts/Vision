package net.minecraft.client.renderer.texture;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceMetadata;

public final class MissingTextureAtlasSprite {
   private static final int MISSING_IMAGE_WIDTH = 16;
   private static final int MISSING_IMAGE_HEIGHT = 16;
   private static final String MISSING_TEXTURE_NAME = "missingno";
   private static final ResourceLocation MISSING_TEXTURE_LOCATION = new ResourceLocation("missingno");
   private static final ResourceMetadata SPRITE_METADATA = (new ResourceMetadata.Builder()).put(AnimationMetadataSection.SERIALIZER, new AnimationMetadataSection(ImmutableList.of(new AnimationFrame(0, -1)), 16, 16, 1, false)).build();
   @Nullable
   private static DynamicTexture missingTexture;

   private static NativeImage generateMissingImage(int pWidth, int pHeight) {
      NativeImage nativeimage = new NativeImage(pWidth, pHeight, false);
      int i = -16777216;
      int j = -524040;

      for(int k = 0; k < pHeight; ++k) {
         for(int l = 0; l < pWidth; ++l) {
            if (k < pHeight / 2 ^ l < pWidth / 2) {
               nativeimage.setPixelRGBA(l, k, -524040);
            } else {
               nativeimage.setPixelRGBA(l, k, -16777216);
            }
         }
      }

      return nativeimage;
   }

   public static SpriteContents create() {
      NativeImage nativeimage = generateMissingImage(16, 16);
      return new SpriteContents(MISSING_TEXTURE_LOCATION, new FrameSize(16, 16), nativeimage, SPRITE_METADATA);
   }

   public static ResourceLocation getLocation() {
      return MISSING_TEXTURE_LOCATION;
   }

   public static DynamicTexture getTexture() {
      if (missingTexture == null) {
         NativeImage nativeimage = generateMissingImage(16, 16);
         nativeimage.untrack();
         missingTexture = new DynamicTexture(nativeimage);
         Minecraft.getInstance().getTextureManager().register(MISSING_TEXTURE_LOCATION, missingTexture);
      }

      return missingTexture;
   }

   public static boolean isMisingSprite(TextureAtlasSprite sprite) {
      return sprite.getName() == MISSING_TEXTURE_LOCATION;
   }
}