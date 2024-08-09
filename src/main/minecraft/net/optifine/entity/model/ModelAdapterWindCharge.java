package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.WindChargeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.WindChargeRenderer;
import net.minecraft.world.entity.EntityType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterWindCharge extends ModelAdapter {
   private static Map<String, String> mapParts = makeMapParts();

   public ModelAdapterWindCharge() {
      super(EntityType.WIND_CHARGE, "wind_charge", 0.0F);
   }

   public Model makeModel() {
      return new WindChargeModel(bakeModelLayer(ModelLayers.WIND_CHARGE));
   }

   public ModelPart getModelRenderer(Model model, String modelPart) {
      if (!(model instanceof WindChargeModel windchargemodel)) {
         return null;
      } else if (mapParts.containsKey(modelPart)) {
         String s = mapParts.get(modelPart);
         return windchargemodel.root().getChildModelDeep(s);
      } else {
         return null;
      }
   }

   public String[] getModelRendererNames() {
      return mapParts.keySet().toArray(new String[0]);
   }

   private static Map<String, String> makeMapParts() {
      Map<String, String> map = new LinkedHashMap<>();
      map.put("core", "projectile");
      map.put("wind", "wind");
      map.put("cube1", "cube_r1");
      map.put("cube2", "cube_r2");
      map.put("charge", "wind_charge");
      return map;
   }

   public IEntityRenderer makeEntityRender(Model modelBase, float shadowSize, RendererCache rendererCache, int index) {
      EntityRenderDispatcher entityrenderdispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
      WindChargeRenderer windchargerenderer = new WindChargeRenderer(entityrenderdispatcher.getContext());
      if (!Reflector.RenderWindCharge_model.exists()) {
         Config.warn("Field not found: RenderWindCharge.model");
         return null;
      } else {
         Reflector.setFieldValue(windchargerenderer, Reflector.RenderWindCharge_model, modelBase);
         windchargerenderer.shadowRadius = shadowSize;
         return windchargerenderer;
      }
   }
}