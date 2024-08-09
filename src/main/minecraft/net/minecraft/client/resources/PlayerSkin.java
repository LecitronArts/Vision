package net.minecraft.client.resources;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;

public record PlayerSkin(ResourceLocation texture, String textureUrl, ResourceLocation capeTexture, ResourceLocation elytraTexture, PlayerSkin.Model model, boolean secure) {
   public ResourceLocation capeTexture() {
      return this.capeTexture;
   }

   public ResourceLocation elytraTexture() {
      return this.elytraTexture;
   }

   public ResourceLocation texture() {
      return this.texture;
   }

   public String textureUrl() {
      return this.textureUrl;
   }

   public PlayerSkin.Model model() {
      return this.model;
   }

   public boolean secure() {
      return this.secure;
   }

   public static enum Model {
      SLIM("slim"),
      WIDE("default");

      private final String id;

      private Model(String pId) {
         this.id = pId;
      }

      public static PlayerSkin.Model byName(@Nullable String pName) {
         if (pName == null) {
            return WIDE;
         } else {
            PlayerSkin.Model playerskin$model;
            switch (pName) {
               case "slim":
                  playerskin$model = SLIM;
                  break;
               default:
                  playerskin$model = WIDE;
            }

            return playerskin$model;
         }
      }

      public String id() {
         return this.id;
      }
   }
}
