package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Matrix4f;

public abstract class RenderStateShard {
   private static final float VIEW_SCALE_Z_EPSILON = 0.99975586F;
   public static final double MAX_ENCHANTMENT_GLINT_SPEED_MILLIS = 8.0D;
   protected final String name;
   private final Runnable setupState;
   private final Runnable clearState;
   protected static final RenderStateShard.TransparencyStateShard NO_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("no_transparency", () -> {
      RenderSystem.disableBlend();
   }, () -> {
   });
   protected static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("additive_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   protected static final RenderStateShard.TransparencyStateShard LIGHTNING_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("lightning_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   protected static final RenderStateShard.TransparencyStateShard GLINT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("glint_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   protected static final RenderStateShard.TransparencyStateShard CRUMBLING_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("crumbling_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   protected static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY = new RenderStateShard.TransparencyStateShard("translucent_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   protected static final RenderStateShard.ShaderStateShard NO_SHADER = new RenderStateShard.ShaderStateShard();
   protected static final RenderStateShard.ShaderStateShard POSITION_COLOR_LIGHTMAP_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorLightmapShader);
   protected static final RenderStateShard.ShaderStateShard POSITION_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionShader);
   protected static final RenderStateShard.ShaderStateShard POSITION_COLOR_TEX_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorTexShader);
   protected static final RenderStateShard.ShaderStateShard POSITION_TEX_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexShader);
   protected static final RenderStateShard.ShaderStateShard POSITION_COLOR_TEX_LIGHTMAP_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorTexLightmapShader);
   protected static final RenderStateShard.ShaderStateShard POSITION_COLOR_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_SOLID_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeSolidShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_CUTOUT_MIPPED_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeCutoutMippedShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_CUTOUT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeCutoutShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TRANSLUCENT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTranslucentShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TRANSLUCENT_MOVING_BLOCK_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTranslucentMovingBlockShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ARMOR_CUTOUT_NO_CULL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeArmorCutoutNoCullShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_SOLID_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntitySolidShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityCutoutShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityCutoutNoCullShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_CUTOUT_NO_CULL_Z_OFFSET_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityCutoutNoCullZOffsetShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ITEM_ENTITY_TRANSLUCENT_CULL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeItemEntityTranslucentCullShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_CULL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentCullShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityTranslucentEmissiveShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_SMOOTH_CUTOUT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntitySmoothCutoutShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_BEACON_BEAM_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeBeaconBeamShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_DECAL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityDecalShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_NO_OUTLINE_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityNoOutlineShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_SHADOW_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityShadowShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_ALPHA_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityAlphaShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_EYES_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEyesShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENERGY_SWIRL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEnergySwirlShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_LEASH_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLeashShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_WATER_MASK_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeWaterMaskShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_OUTLINE_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeOutlineShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ARMOR_GLINT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeArmorGlintShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ARMOR_ENTITY_GLINT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeArmorEntityGlintShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_GLINT_TRANSLUCENT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeGlintTranslucentShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_GLINT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeGlintShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_GLINT_DIRECT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeGlintDirectShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_GLINT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityGlintShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEntityGlintDirectShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_CRUMBLING_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeCrumblingShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTextShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_BACKGROUND_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTextBackgroundShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_INTENSITY_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTextIntensityShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_SEE_THROUGH_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTextSeeThroughShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_BACKGROUND_SEE_THROUGH_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTextBackgroundSeeThroughShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TEXT_INTENSITY_SEE_THROUGH_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTextIntensitySeeThroughShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_LIGHTNING_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLightningShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_TRIPWIRE_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeTripwireShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_END_PORTAL_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEndPortalShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_END_GATEWAY_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEndGatewayShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_LINES_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeLinesShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_GUI_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeGuiShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_GUI_OVERLAY_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeGuiOverlayShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_GUI_TEXT_HIGHLIGHT_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeGuiTextHighlightShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_GUI_GHOST_RECIPE_OVERLAY_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeGuiGhostRecipeOverlayShader);
   protected static final RenderStateShard.ShaderStateShard RENDERTYPE_BREEZE_WIND_SHADER = new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeBreezeWindShader);
   protected static final RenderStateShard.TextureStateShard BLOCK_SHEET_MIPPED = new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, true);
   protected static final RenderStateShard.TextureStateShard BLOCK_SHEET = new RenderStateShard.TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false);
   protected static final RenderStateShard.EmptyTextureStateShard NO_TEXTURE = new RenderStateShard.EmptyTextureStateShard();
   protected static final RenderStateShard.TexturingStateShard DEFAULT_TEXTURING = new RenderStateShard.TexturingStateShard("default_texturing", () -> {
   }, () -> {
   });
   protected static final RenderStateShard.TexturingStateShard GLINT_TEXTURING = new RenderStateShard.TexturingStateShard("glint_texturing", () -> {
      setupGlintTexturing(8.0F);
   }, () -> {
      RenderSystem.resetTextureMatrix();
   });
   protected static final RenderStateShard.TexturingStateShard ENTITY_GLINT_TEXTURING = new RenderStateShard.TexturingStateShard("entity_glint_texturing", () -> {
      setupGlintTexturing(0.16F);
   }, () -> {
      RenderSystem.resetTextureMatrix();
   });
   protected static final RenderStateShard.LightmapStateShard LIGHTMAP = new RenderStateShard.LightmapStateShard(true);
   protected static final RenderStateShard.LightmapStateShard NO_LIGHTMAP = new RenderStateShard.LightmapStateShard(false);
   protected static final RenderStateShard.OverlayStateShard OVERLAY = new RenderStateShard.OverlayStateShard(true);
   protected static final RenderStateShard.OverlayStateShard NO_OVERLAY = new RenderStateShard.OverlayStateShard(false);
   protected static final RenderStateShard.CullStateShard CULL = new RenderStateShard.CullStateShard(true);
   protected static final RenderStateShard.CullStateShard NO_CULL = new RenderStateShard.CullStateShard(false);
   protected static final RenderStateShard.DepthTestStateShard NO_DEPTH_TEST = new RenderStateShard.DepthTestStateShard("always", 519);
   protected static final RenderStateShard.DepthTestStateShard EQUAL_DEPTH_TEST = new RenderStateShard.DepthTestStateShard("==", 514);
   protected static final RenderStateShard.DepthTestStateShard LEQUAL_DEPTH_TEST = new RenderStateShard.DepthTestStateShard("<=", 515);
   protected static final RenderStateShard.DepthTestStateShard GREATER_DEPTH_TEST = new RenderStateShard.DepthTestStateShard(">", 516);
   protected static final RenderStateShard.WriteMaskStateShard COLOR_DEPTH_WRITE = new RenderStateShard.WriteMaskStateShard(true, true);
   protected static final RenderStateShard.WriteMaskStateShard COLOR_WRITE = new RenderStateShard.WriteMaskStateShard(true, false);
   protected static final RenderStateShard.WriteMaskStateShard DEPTH_WRITE = new RenderStateShard.WriteMaskStateShard(false, true);
   protected static final RenderStateShard.LayeringStateShard NO_LAYERING = new RenderStateShard.LayeringStateShard("no_layering", () -> {
   }, () -> {
   });
   protected static final RenderStateShard.LayeringStateShard POLYGON_OFFSET_LAYERING = new RenderStateShard.LayeringStateShard("polygon_offset_layering", () -> {
      RenderSystem.polygonOffset(-1.0F, -10.0F);
      RenderSystem.enablePolygonOffset();
   }, () -> {
      RenderSystem.polygonOffset(0.0F, 0.0F);
      RenderSystem.disablePolygonOffset();
   });
   protected static final RenderStateShard.LayeringStateShard VIEW_OFFSET_Z_LAYERING = new RenderStateShard.LayeringStateShard("view_offset_z_layering", () -> {
      PoseStack posestack = RenderSystem.getModelViewStack();
      posestack.pushPose();
      posestack.scale(0.99975586F, 0.99975586F, 0.99975586F);
      RenderSystem.applyModelViewMatrix();
   }, () -> {
      PoseStack posestack = RenderSystem.getModelViewStack();
      posestack.popPose();
      RenderSystem.applyModelViewMatrix();
   });
   protected static final RenderStateShard.OutputStateShard MAIN_TARGET = new RenderStateShard.OutputStateShard("main_target", () -> {
   }, () -> {
   });
   protected static final RenderStateShard.OutputStateShard OUTLINE_TARGET = new RenderStateShard.OutputStateShard("outline_target", () -> {
      Minecraft.getInstance().levelRenderer.entityTarget().bindWrite(false);
   }, () -> {
      Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
   });
   protected static final RenderStateShard.OutputStateShard TRANSLUCENT_TARGET = new RenderStateShard.OutputStateShard("translucent_target", () -> {
      if (Minecraft.useShaderTransparency()) {
         Minecraft.getInstance().levelRenderer.getTranslucentTarget().bindWrite(false);
      }

   }, () -> {
      if (Minecraft.useShaderTransparency()) {
         Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
      }

   });
   protected static final RenderStateShard.OutputStateShard PARTICLES_TARGET = new RenderStateShard.OutputStateShard("particles_target", () -> {
      if (Minecraft.useShaderTransparency()) {
         Minecraft.getInstance().levelRenderer.getParticlesTarget().bindWrite(false);
      }

   }, () -> {
      if (Minecraft.useShaderTransparency()) {
         Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
      }

   });
   protected static final RenderStateShard.OutputStateShard WEATHER_TARGET = new RenderStateShard.OutputStateShard("weather_target", () -> {
      if (Minecraft.useShaderTransparency()) {
         Minecraft.getInstance().levelRenderer.getWeatherTarget().bindWrite(false);
      }

   }, () -> {
      if (Minecraft.useShaderTransparency()) {
         Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
      }

   });
   protected static final RenderStateShard.OutputStateShard CLOUDS_TARGET = new RenderStateShard.OutputStateShard("clouds_target", () -> {
      if (Minecraft.useShaderTransparency()) {
         Minecraft.getInstance().levelRenderer.getCloudsTarget().bindWrite(false);
      }

   }, () -> {
      if (Minecraft.useShaderTransparency()) {
         Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
      }

   });
   protected static final RenderStateShard.OutputStateShard ITEM_ENTITY_TARGET = new RenderStateShard.OutputStateShard("item_entity_target", () -> {
      if (Minecraft.useShaderTransparency()) {
         Minecraft.getInstance().levelRenderer.getItemEntityTarget().bindWrite(false);
      }

   }, () -> {
      if (Minecraft.useShaderTransparency()) {
         Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
      }

   });
   protected static final RenderStateShard.LineStateShard DEFAULT_LINE = new RenderStateShard.LineStateShard(OptionalDouble.of(1.0D));
   protected static final RenderStateShard.ColorLogicStateShard NO_COLOR_LOGIC = new RenderStateShard.ColorLogicStateShard("no_color_logic", () -> {
      RenderSystem.disableColorLogicOp();
   }, () -> {
   });
   protected static final RenderStateShard.ColorLogicStateShard OR_REVERSE_COLOR_LOGIC = new RenderStateShard.ColorLogicStateShard("or_reverse", () -> {
      RenderSystem.enableColorLogicOp();
      RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
   }, () -> {
      RenderSystem.disableColorLogicOp();
   });

   public RenderStateShard(String pName, Runnable pSetupState, Runnable pClearState) {
      this.name = pName;
      this.setupState = pSetupState;
      this.clearState = pClearState;
   }

   public void setupRenderState() {
      this.setupState.run();
   }

   public void clearRenderState() {
      this.clearState.run();
   }

   public String toString() {
      return this.name;
   }

   private static void setupGlintTexturing(float pScale) {
      long i = (long)((double)Util.getMillis() * Minecraft.getInstance().options.glintSpeed().get() * 8.0D);
      float f = (float)(i % 110000L) / 110000.0F;
      float f1 = (float)(i % 30000L) / 30000.0F;
      Matrix4f matrix4f = (new Matrix4f()).translation(-f, f1, 0.0F);
      matrix4f.rotateZ(0.17453292F).scale(pScale);
      RenderSystem.setTextureMatrix(matrix4f);
   }

   public String getName() {
      return this.name;
   }

   static class BooleanStateShard extends RenderStateShard {
      private final boolean enabled;

      public BooleanStateShard(String pName, Runnable pSetupState, Runnable pClearState, boolean pEnabled) {
         super(pName, pSetupState, pClearState);
         this.enabled = pEnabled;
      }

      public String toString() {
         return this.name + "[" + this.enabled + "]";
      }
   }

   protected static class ColorLogicStateShard extends RenderStateShard {
      public ColorLogicStateShard(String p_286784_, Runnable p_286884_, Runnable p_286375_) {
         super(p_286784_, p_286884_, p_286375_);
      }
   }

   protected static class CullStateShard extends RenderStateShard.BooleanStateShard {
      public CullStateShard(boolean pUseCull) {
         super("cull", () -> {
            if (!pUseCull) {
               RenderSystem.disableCull();
            }

         }, () -> {
            if (!pUseCull) {
               RenderSystem.enableCull();
            }

         }, pUseCull);
      }
   }

   protected static class DepthTestStateShard extends RenderStateShard {
      private final String functionName;

      public DepthTestStateShard(String pFunctionName, int pDepthFunc) {
         super("depth_test", () -> {
            if (pDepthFunc != 519) {
               RenderSystem.enableDepthTest();
               RenderSystem.depthFunc(pDepthFunc);
            }

         }, () -> {
            if (pDepthFunc != 519) {
               RenderSystem.disableDepthTest();
               RenderSystem.depthFunc(515);
            }

         });
         this.functionName = pFunctionName;
      }

      public String toString() {
         return this.name + "[" + this.functionName + "]";
      }
   }

   protected static class EmptyTextureStateShard extends RenderStateShard {
      public EmptyTextureStateShard(Runnable pSetupState, Runnable pClearState) {
         super("texture", pSetupState, pClearState);
      }

      EmptyTextureStateShard() {
         super("texture", () -> {
         }, () -> {
         });
      }

      protected Optional<ResourceLocation> cutoutTexture() {
         return Optional.empty();
      }

      public boolean isBlur() {
         return false;
      }

      public boolean isMipmap() {
         return false;
      }
   }

   protected static class LayeringStateShard extends RenderStateShard {
      public LayeringStateShard(String p_110267_, Runnable p_110268_, Runnable p_110269_) {
         super(p_110267_, p_110268_, p_110269_);
      }
   }

   protected static class LightmapStateShard extends RenderStateShard.BooleanStateShard {
      public LightmapStateShard(boolean pUseLightmap) {
         super("lightmap", () -> {
            if (pUseLightmap) {
               Minecraft.getInstance().gameRenderer.lightTexture().turnOnLightLayer();
            }

         }, () -> {
            if (pUseLightmap) {
               Minecraft.getInstance().gameRenderer.lightTexture().turnOffLightLayer();
            }

         }, pUseLightmap);
      }
   }

   protected static class LineStateShard extends RenderStateShard {
      private final OptionalDouble width;

      public LineStateShard(OptionalDouble pWidth) {
         super("line_width", () -> {
            if (!Objects.equals(pWidth, OptionalDouble.of(1.0D))) {
               if (pWidth.isPresent()) {
                  RenderSystem.lineWidth((float)pWidth.getAsDouble());
               } else {
                  RenderSystem.lineWidth(Math.max(2.5F, (float)Minecraft.getInstance().getWindow().getWidth() / 1920.0F * 2.5F));
               }
            }

         }, () -> {
            if (!Objects.equals(pWidth, OptionalDouble.of(1.0D))) {
               RenderSystem.lineWidth(1.0F);
            }

         });
         this.width = pWidth;
      }

      public String toString() {
         return this.name + "[" + (this.width.isPresent() ? this.width.getAsDouble() : "window_scale") + "]";
      }
   }

   protected static class MultiTextureStateShard extends RenderStateShard.EmptyTextureStateShard {
      private final Optional<ResourceLocation> cutoutTexture;

      MultiTextureStateShard(ImmutableList<Triple<ResourceLocation, Boolean, Boolean>> pTextures) {
         super(() -> {
            int i = 0;

            for(Triple<ResourceLocation, Boolean, Boolean> triple : pTextures) {
               TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
               texturemanager.getTexture(triple.getLeft()).setFilter(triple.getMiddle(), triple.getRight());
               RenderSystem.setShaderTexture(i++, triple.getLeft());
            }

         }, () -> {
         });
         this.cutoutTexture = pTextures.stream().findFirst().map(Triple::getLeft);
      }

      protected Optional<ResourceLocation> cutoutTexture() {
         return this.cutoutTexture;
      }

      public static RenderStateShard.MultiTextureStateShard.Builder builder() {
         return new RenderStateShard.MultiTextureStateShard.Builder();
      }

      public static final class Builder {
         private final ImmutableList.Builder<Triple<ResourceLocation, Boolean, Boolean>> builder = new ImmutableList.Builder<>();

         public RenderStateShard.MultiTextureStateShard.Builder add(ResourceLocation pTexture, boolean pBlur, boolean pMipmap) {
            this.builder.add(Triple.of(pTexture, pBlur, pMipmap));
            return this;
         }

         public RenderStateShard.MultiTextureStateShard build() {
            return new RenderStateShard.MultiTextureStateShard(this.builder.build());
         }
      }
   }

   protected static final class OffsetTexturingStateShard extends RenderStateShard.TexturingStateShard {
      public OffsetTexturingStateShard(float pU, float pV) {
         super("offset_texturing", () -> {
            RenderSystem.setTextureMatrix((new Matrix4f()).translation(pU, pV, 0.0F));
         }, () -> {
            RenderSystem.resetTextureMatrix();
         });
      }
   }

   protected static class OutputStateShard extends RenderStateShard {
      public OutputStateShard(String p_110300_, Runnable p_110301_, Runnable p_110302_) {
         super(p_110300_, p_110301_, p_110302_);
      }
   }

   protected static class OverlayStateShard extends RenderStateShard.BooleanStateShard {
      public OverlayStateShard(boolean pUseOverlay) {
         super("overlay", () -> {
            if (pUseOverlay) {
               Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor();
            }

         }, () -> {
            if (pUseOverlay) {
               Minecraft.getInstance().gameRenderer.overlayTexture().teardownOverlayColor();
            }

         }, pUseOverlay);
      }
   }

   protected static class ShaderStateShard extends RenderStateShard {
      private final Optional<Supplier<ShaderInstance>> shader;

      public ShaderStateShard(Supplier<ShaderInstance> pShader) {
         super("shader", () -> {
            RenderSystem.setShader(pShader);
         }, () -> {
         });
         this.shader = Optional.of(pShader);
      }

      public ShaderStateShard() {
         super("shader", () -> {
            RenderSystem.setShader(() -> {
               return null;
            });
         }, () -> {
         });
         this.shader = Optional.empty();
      }

      public String toString() {
         return this.name + "[" + this.shader + "]";
      }
   }

   protected static class TextureStateShard extends RenderStateShard.EmptyTextureStateShard {
      private final Optional<ResourceLocation> texture;
      private final boolean blur;
      private final boolean mipmap;

      public TextureStateShard(ResourceLocation pTexture, boolean pBlur, boolean pMipmap) {
         super(() -> {
            TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
            texturemanager.getTexture(pTexture).setFilter(pBlur, pMipmap);
            RenderSystem.setShaderTexture(0, pTexture);
         }, () -> {
         });
         this.texture = Optional.of(pTexture);
         this.blur = pBlur;
         this.mipmap = pMipmap;
      }

      public String toString() {
         return this.name + "[" + this.texture + "(blur=" + this.blur + ", mipmap=" + this.mipmap + ")]";
      }

      protected Optional<ResourceLocation> cutoutTexture() {
         return this.texture;
      }

      public boolean isBlur() {
         return this.blur;
      }

      public boolean isMipmap() {
         return this.mipmap;
      }
   }

   protected static class TexturingStateShard extends RenderStateShard {
      public TexturingStateShard(String p_110349_, Runnable p_110350_, Runnable p_110351_) {
         super(p_110349_, p_110350_, p_110351_);
      }
   }

   protected static class TransparencyStateShard extends RenderStateShard {
      public TransparencyStateShard(String p_110353_, Runnable p_110354_, Runnable p_110355_) {
         super(p_110353_, p_110354_, p_110355_);
      }
   }

   protected static class WriteMaskStateShard extends RenderStateShard {
      private final boolean writeColor;
      private final boolean writeDepth;

      public WriteMaskStateShard(boolean pWriteColor, boolean pWriteDepth) {
         super("write_mask_state", () -> {
            if (!pWriteDepth) {
               RenderSystem.depthMask(pWriteDepth);
            }

            if (!pWriteColor) {
               RenderSystem.colorMask(pWriteColor, pWriteColor, pWriteColor, pWriteColor);
            }

         }, () -> {
            if (!pWriteDepth) {
               RenderSystem.depthMask(true);
            }

            if (!pWriteColor) {
               RenderSystem.colorMask(true, true, true, true);
            }

         });
         this.writeColor = pWriteColor;
         this.writeDepth = pWriteDepth;
      }

      public String toString() {
         return this.name + "[writeColor=" + this.writeColor + ", writeDepth=" + this.writeDepth + "]";
      }
   }
}