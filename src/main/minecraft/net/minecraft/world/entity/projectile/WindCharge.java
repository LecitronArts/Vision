package net.minecraft.world.entity.projectile;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class WindCharge extends AbstractHurtingProjectile implements ItemSupplier {
   public static final WindCharge.WindChargeExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new WindCharge.WindChargeExplosionDamageCalculator();

   public WindCharge(EntityType<? extends WindCharge> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public WindCharge(EntityType<? extends WindCharge> pEntityType, Breeze pOwner, Level pLevel) {
      super(pEntityType, pOwner.getX(), pOwner.getSnoutYPosition(), pOwner.getZ(), pLevel);
      this.setOwner(pOwner);
   }

   protected AABB makeBoundingBox() {
      float f = this.getType().getDimensions().width / 2.0F;
      float f1 = this.getType().getDimensions().height;
      float f2 = 0.15F;
      return new AABB(this.position().x - (double)f, this.position().y - (double)0.15F, this.position().z - (double)f, this.position().x + (double)f, this.position().y - (double)0.15F + (double)f1, this.position().z + (double)f);
   }

   protected float getEyeHeight(Pose pPose, EntityDimensions pDimensions) {
      return 0.0F;
   }

   public boolean canCollideWith(Entity pEntity) {
      return pEntity instanceof WindCharge ? false : super.canCollideWith(pEntity);
   }

   protected boolean canHitEntity(Entity pTarget) {
      return pTarget instanceof WindCharge ? false : super.canHitEntity(pTarget);
   }

   protected void onHitEntity(EntityHitResult pResult) {
      super.onHitEntity(pResult);
      if (!this.level().isClientSide) {
         Entity entity1 = pResult.getEntity();
         DamageSources damagesources = this.damageSources();
         Entity entity = this.getOwner();
         LivingEntity livingentity1;
         if (entity instanceof LivingEntity) {
            LivingEntity livingentity = (LivingEntity)entity;
            livingentity1 = livingentity;
         } else {
            livingentity1 = null;
         }

         entity1.hurt(damagesources.mobProjectile(this, livingentity1), 1.0F);
         this.explode();
      }
   }

   private void explode() {
      this.level().explode(this, (DamageSource)null, EXPLOSION_DAMAGE_CALCULATOR, this.getX(), this.getY(), this.getZ(), (float)(3.0D + this.random.nextDouble()), false, Level.ExplosionInteraction.BLOW, ParticleTypes.GUST, ParticleTypes.GUST_EMITTER, SoundEvents.WIND_BURST);
   }

   protected void onHitBlock(BlockHitResult pResult) {
      super.onHitBlock(pResult);
      this.explode();
      this.discard();
   }

   protected void onHit(HitResult pResult) {
      super.onHit(pResult);
      if (!this.level().isClientSide) {
         this.discard();
      }

   }

   protected boolean shouldBurn() {
      return false;
   }

   public ItemStack getItem() {
      return ItemStack.EMPTY;
   }

   protected float getInertia() {
      return 1.0F;
   }

   protected float getLiquidInertia() {
      return this.getInertia();
   }

   @Nullable
   protected ParticleOptions getTrailParticle() {
      return null;
   }

   protected ClipContext.Block getClipType() {
      return ClipContext.Block.OUTLINE;
   }

   public static final class WindChargeExplosionDamageCalculator extends ExplosionDamageCalculator {
      public boolean shouldDamageEntity(Explosion p_309489_, Entity p_312366_) {
         return false;
      }
   }
}