package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.tr7zw.entityculling.access.EntityRendererInter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.optifine.entity.model.IEntityRenderer;
import net.optifine.reflect.Reflector;
import net.optifine.reflect.ReflectorForge;
import net.optifine.util.Either;
import org.joml.Matrix4f;

public abstract class EntityRenderer<T extends Entity> implements IEntityRenderer, EntityRendererInter<T> {
   protected static final float NAMETAG_SCALE = 0.025F;
   protected final EntityRenderDispatcher entityRenderDispatcher;
   private final Font font;
   public float shadowRadius;
   public float shadowStrength = 1.0F;
   private EntityType entityType = null;
   private ResourceLocation locationTextureCustom = null;
   public float shadowOffsetX;
   public float shadowOffsetZ;

   protected EntityRenderer(EntityRendererProvider.Context pContext) {
      this.entityRenderDispatcher = pContext.getEntityRenderDispatcher();
      this.font = pContext.getFont();
   }

   public final int getPackedLightCoords(T pEntity, float pPartialTicks) {
      BlockPos blockpos = BlockPos.containing(pEntity.getLightProbePosition(pPartialTicks));
      return LightTexture.pack(this.getBlockLightLevel(pEntity, blockpos), this.getSkyLightLevel(pEntity, blockpos));
   }

   protected int getSkyLightLevel(T pEntity, BlockPos pPos) {
      return pEntity.level().getBrightness(LightLayer.SKY, pPos);
   }

   protected int getBlockLightLevel(T pEntity, BlockPos pPos) {
      return pEntity.isOnFire() ? 15 : pEntity.level().getBrightness(LightLayer.BLOCK, pPos);
   }

   public boolean shouldRender(T pLivingEntity, Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
      if (!pLivingEntity.shouldRender(pCamX, pCamY, pCamZ)) {
         return false;
      } else if (pLivingEntity.noCulling) {
         return true;
      } else {
         AABB aabb = pLivingEntity.getBoundingBoxForCulling().inflate(0.5D);
         if (aabb.hasNaN() || aabb.getSize() == 0.0D) {
            aabb = new AABB(pLivingEntity.getX() - 2.0D, pLivingEntity.getY() - 2.0D, pLivingEntity.getZ() - 2.0D, pLivingEntity.getX() + 2.0D, pLivingEntity.getY() + 2.0D, pLivingEntity.getZ() + 2.0D);
         }

         return pCamera.isVisible(aabb);
      }
   }

   public Vec3 getRenderOffset(T pEntity, float pPartialTicks) {
      return Vec3.ZERO;
   }

   public void render(T pEntity, float pEntityYaw, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      if (this.shouldShowName(pEntity)) {
         this.renderNameTag(pEntity, pEntity.getDisplayName(), pPoseStack, pBuffer, pPackedLight);
      }
   }

   protected boolean shouldShowName(T pEntity) {
      return pEntity.shouldShowName() || pEntity.hasCustomName() && pEntity == this.entityRenderDispatcher.crosshairPickEntity;
   }

   public abstract ResourceLocation getTextureLocation(T pEntity);

   public Font getFont() {
      return this.font;
   }

   protected void renderNameTag(T pEntity, Component pDisplayName, PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight) {
      double d0 = this.entityRenderDispatcher.distanceToSqr(pEntity);
      boolean flag = !(d0 > 4096.0D);
      if (Reflector.ForgeHooksClient_isNameplateInRenderDistance.exists()) {
         flag = Reflector.ForgeHooksClient_isNameplateInRenderDistance.callBoolean(pEntity, d0);
      }

      if (flag) {
         boolean flag1 = !pEntity.isDiscrete();
         float f = pEntity.getNameTagOffsetY();
         int i = "deadmau5".equals(pDisplayName.getString()) ? -10 : 0;
         pPoseStack.pushPose();
         pPoseStack.translate(0.0F, f, 0.0F);
         pPoseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
         pPoseStack.scale(-0.025F, -0.025F, 0.025F);
         Matrix4f matrix4f = pPoseStack.last().pose();
         float f1 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
         int j = (int)(f1 * 255.0F) << 24;
         Font font = this.getFont();
         float f2 = (float)(-font.width(pDisplayName) / 2);
         font.drawInBatch(pDisplayName, f2, (float)i, 553648127, false, matrix4f, pBuffer, flag1 ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, j, pPackedLight);
         if (flag1) {
            font.drawInBatch(pDisplayName, f2, (float)i, -1, false, matrix4f, pBuffer, Font.DisplayMode.NORMAL, 0, pPackedLight);
         }

         pPoseStack.popPose();
      }

   }

   public Either<EntityType, BlockEntityType> getType() {
      return this.entityType == null ? null : Either.makeLeft(this.entityType);
   }

   public void setType(Either<EntityType, BlockEntityType> type) {
      this.entityType = type.getLeft().get();
   }

   public ResourceLocation getLocationTextureCustom() {
      return this.locationTextureCustom;
   }

   public void setLocationTextureCustom(ResourceLocation locationTextureCustom) {
      this.locationTextureCustom = locationTextureCustom;
   }


   @Override
   public boolean shadowShouldShowName(T entity) {
      return shouldShowName(entity);
   }

   @Override
   public void shadowRenderNameTag(T entity, Component component, PoseStack poseStack,
                                   MultiBufferSource multiBufferSource, int light) {
      renderNameTag(entity, component, poseStack, multiBufferSource, light);
   }
}