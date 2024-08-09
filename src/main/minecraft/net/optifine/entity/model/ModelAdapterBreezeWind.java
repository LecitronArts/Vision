package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BreezeModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.BreezeRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.layers.BreezeWindLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterBreezeWind extends ModelAdapterBreeze {
   public ModelAdapterBreezeWind() {
      super(EntityType.BREEZE, "breeze_wind", 0.0F);
   }

   public Model makeModel() {
      return new BreezeModel(bakeModelLayer(ModelLayers.BREEZE_WIND));
   }

   public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index) {
      EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
      BreezeRenderer breezerenderer = new BreezeRenderer(entityrenderdispatcher.getContext());
      breezerenderer.model = new BreezeModel<>(bakeModelLayer(ModelLayers.BREEZE_WIND));
      breezerenderer.shadowRadius = 0.0F;
      EntityRenderer entityrenderer = rendererCache.get(EntityType.BREEZE, index, () -> {
         return breezerenderer;
      });
      if (!(entityrenderer instanceof BreezeRenderer breezerenderer1)) {
         Config.warn("Not a RenderBreeze: " + entityrenderer);
         return null;
      } else {
         ResourceLocation resourcelocation = modelBase.locationTextureCustom != null ? modelBase.locationTextureCustom : new ResourceLocation("textures/entity/breeze/breeze_wind.png");
         BreezeWindLayer breezewindlayer = new BreezeWindLayer(breezerenderer1, entityrenderdispatcher.getContext().getModelSet(), resourcelocation);
         if (!Reflector.BreezeWindLayer_model.exists()) {
            Config.warn("Field not found: BreezeWindLayer.model");
            return null;
         } else {
            Reflector.setFieldValue(breezewindlayer, Reflector.BreezeWindLayer_model, modelBase);
            breezerenderer1.removeLayers(BreezeWindLayer.class);
            breezerenderer1.addLayer(breezewindlayer);
            return breezerenderer1;
         }
      }
   }

   public boolean setTextureLocation(IEntityRenderer er, ResourceLocation textureLocation) {
      return true;
   }
}