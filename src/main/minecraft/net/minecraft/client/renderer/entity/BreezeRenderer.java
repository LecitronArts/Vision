package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.BreezeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.BreezeEyesLayer;
import net.minecraft.client.renderer.entity.layers.BreezeWindLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreezeRenderer extends MobRenderer<Breeze, BreezeModel<Breeze>> {
   private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation("textures/entity/breeze/breeze.png");
   private static final ResourceLocation WIND_TEXTURE_LOCATION = new ResourceLocation("textures/entity/breeze/breeze_wind.png");

   public BreezeRenderer(EntityRendererProvider.Context p_311628_) {
      super(p_311628_, new BreezeModel<>(p_311628_.bakeLayer(ModelLayers.BREEZE)), 0.8F);
      this.addLayer(new BreezeWindLayer(this, p_311628_.getModelSet(), WIND_TEXTURE_LOCATION));
      this.addLayer(new BreezeEyesLayer(this, p_311628_.getModelSet(), TEXTURE_LOCATION));
   }

   public ResourceLocation getTextureLocation(Breeze p_312626_) {
      return TEXTURE_LOCATION;
   }
}