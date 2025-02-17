package net.optifine.entity.model;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.PiglinModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.PiglinRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterPiglinBrute extends ModelAdapterPiglin {
   public ModelAdapterPiglinBrute() {
      super(EntityType.PIGLIN_BRUTE, "piglin_brute", 0.5F);
   }

   public Model makeModel() {
      return new PiglinModel(bakeModelLayer(ModelLayers.PIGLIN_BRUTE));
   }

   public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index) {
      EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
      PiglinRenderer piglinrenderer = new PiglinRenderer(entityrenderdispatcher.getContext(), ModelLayers.PIGLIN_BRUTE, ModelLayers.PIGLIN_BRUTE_INNER_ARMOR, ModelLayers.PIGLIN_BRUTE_OUTER_ARMOR, false);
      piglinrenderer.model = (PiglinModel)modelBase;
      piglinrenderer.shadowRadius = shadowSize;
      return piglinrenderer;
   }
}