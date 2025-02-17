package net.minecraft.world.entity.projectile;

import com.google.common.base.MoreObjects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class Projectile extends Entity implements TraceableEntity {
   @Nullable
   private UUID ownerUUID;
   @Nullable
   private Entity cachedOwner;
   private boolean leftOwner;
   private boolean hasBeenShot;

   Projectile(EntityType<? extends Projectile> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public void setOwner(@Nullable Entity pOwner) {
      if (pOwner != null) {
         this.ownerUUID = pOwner.getUUID();
         this.cachedOwner = pOwner;
      }

   }

   @Nullable
   public Entity getOwner() {
      if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
         return this.cachedOwner;
      } else {
         if (this.ownerUUID != null) {
            Level level = this.level();
            if (level instanceof ServerLevel) {
               ServerLevel serverlevel = (ServerLevel)level;
               this.cachedOwner = serverlevel.getEntity(this.ownerUUID);
               return this.cachedOwner;
            }
         }

         return null;
      }
   }

   public Entity getEffectSource() {
      return MoreObjects.firstNonNull(this.getOwner(), this);
   }

   protected void addAdditionalSaveData(CompoundTag pCompound) {
      if (this.ownerUUID != null) {
         pCompound.putUUID("Owner", this.ownerUUID);
      }

      if (this.leftOwner) {
         pCompound.putBoolean("LeftOwner", true);
      }

      pCompound.putBoolean("HasBeenShot", this.hasBeenShot);
   }

   protected boolean ownedBy(Entity pEntity) {
      return pEntity.getUUID().equals(this.ownerUUID);
   }

   protected void readAdditionalSaveData(CompoundTag pCompound) {
      if (pCompound.hasUUID("Owner")) {
         this.ownerUUID = pCompound.getUUID("Owner");
         this.cachedOwner = null;
      }

      this.leftOwner = pCompound.getBoolean("LeftOwner");
      this.hasBeenShot = pCompound.getBoolean("HasBeenShot");
   }

   public void restoreFrom(Entity pEntity) {
      super.restoreFrom(pEntity);
      if (pEntity instanceof Projectile projectile) {
         this.cachedOwner = projectile.cachedOwner;
      }

   }

   public void tick() {
      if (!this.hasBeenShot) {
         this.gameEvent(GameEvent.PROJECTILE_SHOOT, this.getOwner());
         this.hasBeenShot = true;
      }

      if (!this.leftOwner) {
         this.leftOwner = this.checkLeftOwner();
      }

      super.tick();
   }

   private boolean checkLeftOwner() {
      Entity entity = this.getOwner();
      if (entity != null) {
         for(Entity entity1 : this.level().getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D), (p_37272_) -> {
            return !p_37272_.isSpectator() && p_37272_.isPickable();
         })) {
            if (entity1.getRootVehicle() == entity.getRootVehicle()) {
               return false;
            }
         }
      }

      return true;
   }

   public void shoot(double pX, double pY, double pZ, float pVelocity, float pInaccuracy) {
      Vec3 vec3 = (new Vec3(pX, pY, pZ)).normalize().add(this.random.triangle(0.0D, 0.0172275D * (double)pInaccuracy), this.random.triangle(0.0D, 0.0172275D * (double)pInaccuracy), this.random.triangle(0.0D, 0.0172275D * (double)pInaccuracy)).scale((double)pVelocity);
      this.setDeltaMovement(vec3);
      double d0 = vec3.horizontalDistance();
      this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI)));
      this.setXRot((float)(Mth.atan2(vec3.y, d0) * (double)(180F / (float)Math.PI)));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   public void shootFromRotation(Entity pShooter, float pX, float pY, float pZ, float pVelocity, float pInaccuracy) {
      float f = -Mth.sin(pY * ((float)Math.PI / 180F)) * Mth.cos(pX * ((float)Math.PI / 180F));
      float f1 = -Mth.sin((pX + pZ) * ((float)Math.PI / 180F));
      float f2 = Mth.cos(pY * ((float)Math.PI / 180F)) * Mth.cos(pX * ((float)Math.PI / 180F));
      this.shoot((double)f, (double)f1, (double)f2, pVelocity, pInaccuracy);
      Vec3 vec3 = pShooter.getDeltaMovement();
      this.setDeltaMovement(this.getDeltaMovement().add(vec3.x, pShooter.onGround() ? 0.0D : vec3.y, vec3.z));
   }

   protected void onHit(HitResult pResult) {
      HitResult.Type hitresult$type = pResult.getType();
      if (hitresult$type == HitResult.Type.ENTITY) {
         this.onHitEntity((EntityHitResult)pResult);
         this.level().gameEvent(GameEvent.PROJECTILE_LAND, pResult.getLocation(), GameEvent.Context.of(this, (BlockState)null));
      } else if (hitresult$type == HitResult.Type.BLOCK) {
         BlockHitResult blockhitresult = (BlockHitResult)pResult;
         this.onHitBlock(blockhitresult);
         BlockPos blockpos = blockhitresult.getBlockPos();
         this.level().gameEvent(GameEvent.PROJECTILE_LAND, blockpos, GameEvent.Context.of(this, this.level().getBlockState(blockpos)));
      }

   }

   protected void onHitEntity(EntityHitResult pResult) {
   }

   protected void onHitBlock(BlockHitResult pResult) {
      BlockState blockstate = this.level().getBlockState(pResult.getBlockPos());
      blockstate.onProjectileHit(this.level(), blockstate, pResult, this);
   }

   public void lerpMotion(double pX, double pY, double pZ) {
      this.setDeltaMovement(pX, pY, pZ);
      if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
         double d0 = Math.sqrt(pX * pX + pZ * pZ);
         this.setXRot((float)(Mth.atan2(pY, d0) * (double)(180F / (float)Math.PI)));
         this.setYRot((float)(Mth.atan2(pX, pZ) * (double)(180F / (float)Math.PI)));
         this.xRotO = this.getXRot();
         this.yRotO = this.getYRot();
         this.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
      }

   }

   protected boolean canHitEntity(Entity pTarget) {
      if (!pTarget.canBeHitByProjectile()) {
         return false;
      } else {
         Entity entity = this.getOwner();
         return entity == null || this.leftOwner || !entity.isPassengerOfSameVehicle(pTarget);
      }
   }

   protected void updateRotation() {
      Vec3 vec3 = this.getDeltaMovement();
      double d0 = vec3.horizontalDistance();
      this.setXRot(lerpRotation(this.xRotO, (float)(Mth.atan2(vec3.y, d0) * (double)(180F / (float)Math.PI))));
      this.setYRot(lerpRotation(this.yRotO, (float)(Mth.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI))));
   }

   protected static float lerpRotation(float pCurrentRotation, float pTargetRotation) {
      while(pTargetRotation - pCurrentRotation < -180.0F) {
         pCurrentRotation -= 360.0F;
      }

      while(pTargetRotation - pCurrentRotation >= 180.0F) {
         pCurrentRotation += 360.0F;
      }

      return Mth.lerp(0.2F, pCurrentRotation, pTargetRotation);
   }

   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      Entity entity = this.getOwner();
      return new ClientboundAddEntityPacket(this, entity == null ? 0 : entity.getId());
   }

   public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
      super.recreateFromPacket(pPacket);
      Entity entity = this.level().getEntity(pPacket.getData());
      if (entity != null) {
         this.setOwner(entity);
      }

   }

   public boolean mayInteract(Level pLevel, BlockPos pPos) {
      Entity entity = this.getOwner();
      if (entity instanceof Player) {
         return entity.mayInteract(pLevel, pPos);
      } else {
         return entity == null || pLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
      }
   }

   public boolean mayBreak(Level pLevel) {
      return this.getType().is(EntityTypeTags.IMPACT_PROJECTILES) && pLevel.getGameRules().getBoolean(GameRules.RULE_PROJECTILESCANBREAKBLOCKS);
   }
}