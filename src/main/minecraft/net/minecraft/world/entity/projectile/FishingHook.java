package net.minecraft.world.entity.projectile;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class FishingHook extends Projectile {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final RandomSource syncronizedRandom = RandomSource.create();
   private boolean biting;
   private int outOfWaterTime;
   private static final int MAX_OUT_OF_WATER_TIME = 10;
   private static final EntityDataAccessor<Integer> DATA_HOOKED_ENTITY = SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_BITING = SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.BOOLEAN);
   private int life;
   private int nibble;
   private int timeUntilLured;
   private int timeUntilHooked;
   private float fishAngle;
   private boolean openWater = true;
   @Nullable
   private Entity hookedIn;
   private FishingHook.FishHookState currentState = FishingHook.FishHookState.FLYING;
   private final int luck;
   private final int lureSpeed;

   private FishingHook(EntityType<? extends FishingHook> pEntityType, Level pLevel, int pLuck, int pLureSpeed) {
      super(pEntityType, pLevel);
      this.noCulling = true;
      this.luck = Math.max(0, pLuck);
      this.lureSpeed = Math.max(0, pLureSpeed);
   }

   public FishingHook(EntityType<? extends FishingHook> pEntityType, Level pLevel) {
      this(pEntityType, pLevel, 0, 0);
   }

   public FishingHook(Player pPlayer, Level pLevel, int pLuck, int pLureSpeed) {
      this(EntityType.FISHING_BOBBER, pLevel, pLuck, pLureSpeed);
      this.setOwner(pPlayer);
      float f = pPlayer.getXRot();
      float f1 = pPlayer.getYRot();
      float f2 = Mth.cos(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
      float f3 = Mth.sin(-f1 * ((float)Math.PI / 180F) - (float)Math.PI);
      float f4 = -Mth.cos(-f * ((float)Math.PI / 180F));
      float f5 = Mth.sin(-f * ((float)Math.PI / 180F));
      double d0 = pPlayer.getX() - (double)f3 * 0.3D;
      double d1 = pPlayer.getEyeY();
      double d2 = pPlayer.getZ() - (double)f2 * 0.3D;
      this.moveTo(d0, d1, d2, f1, f);
      Vec3 vec3 = new Vec3((double)(-f3), (double)Mth.clamp(-(f5 / f4), -5.0F, 5.0F), (double)(-f2));
      double d3 = vec3.length();
      vec3 = vec3.multiply(0.6D / d3 + this.random.triangle(0.5D, 0.0103365D), 0.6D / d3 + this.random.triangle(0.5D, 0.0103365D), 0.6D / d3 + this.random.triangle(0.5D, 0.0103365D));
      this.setDeltaMovement(vec3);
      this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI)));
      this.setXRot((float)(Mth.atan2(vec3.y, vec3.horizontalDistance()) * (double)(180F / (float)Math.PI)));
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
   }

   protected void defineSynchedData() {
      this.getEntityData().define(DATA_HOOKED_ENTITY, 0);
      this.getEntityData().define(DATA_BITING, false);
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
      if (DATA_HOOKED_ENTITY.equals(pKey)) {
         int i = this.getEntityData().get(DATA_HOOKED_ENTITY);
         this.hookedIn = i > 0 ? this.level().getEntity(i - 1) : null;
      }

      if (DATA_BITING.equals(pKey)) {
         this.biting = this.getEntityData().get(DATA_BITING);
         if (this.biting) {
            this.setDeltaMovement(this.getDeltaMovement().x, (double)(-0.4F * Mth.nextFloat(this.syncronizedRandom, 0.6F, 1.0F)), this.getDeltaMovement().z);
         }
      }

      super.onSyncedDataUpdated(pKey);
   }

   public boolean shouldRenderAtSqrDistance(double pDistance) {
      double d0 = 64.0D;
      return pDistance < 4096.0D;
   }

   public void lerpTo(double pX, double pY, double pZ, float pYRot, float pXRot, int pSteps) {
   }

   public void tick() {
      this.syncronizedRandom.setSeed(this.getUUID().getLeastSignificantBits() ^ this.level().getGameTime());
      super.tick();
      Player player = this.getPlayerOwner();
      if (player == null) {
         this.discard();
      } else if (this.level().isClientSide || !this.shouldStopFishing(player)) {
         if (this.onGround()) {
            ++this.life;
            if (this.life >= 1200) {
               this.discard();
               return;
            }
         } else {
            this.life = 0;
         }

         float f = 0.0F;
         BlockPos blockpos = this.blockPosition();
         FluidState fluidstate = this.level().getFluidState(blockpos);
         if (fluidstate.is(FluidTags.WATER)) {
            f = fluidstate.getHeight(this.level(), blockpos);
         }

         boolean flag = f > 0.0F;
         if (this.currentState == FishingHook.FishHookState.FLYING) {
            if (this.hookedIn != null) {
               this.setDeltaMovement(Vec3.ZERO);
               this.currentState = FishingHook.FishHookState.HOOKED_IN_ENTITY;
               return;
            }

            if (flag) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.3D, 0.2D, 0.3D));
               this.currentState = FishingHook.FishHookState.BOBBING;
               return;
            }

            this.checkCollision();
         } else {
            if (this.currentState == FishingHook.FishHookState.HOOKED_IN_ENTITY) {
               if (this.hookedIn != null) {
                  if (!this.hookedIn.isRemoved() && this.hookedIn.level().dimension() == this.level().dimension()) {
                     this.setPos(this.hookedIn.getX(), this.hookedIn.getY(0.8D), this.hookedIn.getZ());
                  } else {
                     this.setHookedEntity((Entity)null);
                     this.currentState = FishingHook.FishHookState.FLYING;
                  }
               }

               return;
            }

            if (this.currentState == FishingHook.FishHookState.BOBBING) {
               Vec3 vec3 = this.getDeltaMovement();
               double d0 = this.getY() + vec3.y - (double)blockpos.getY() - (double)f;
               if (Math.abs(d0) < 0.01D) {
                  d0 += Math.signum(d0) * 0.1D;
               }

               this.setDeltaMovement(vec3.x * 0.9D, vec3.y - d0 * (double)this.random.nextFloat() * 0.2D, vec3.z * 0.9D);
               if (this.nibble <= 0 && this.timeUntilHooked <= 0) {
                  this.openWater = true;
               } else {
                  this.openWater = this.openWater && this.outOfWaterTime < 10 && this.calculateOpenWater(blockpos);
               }

               if (flag) {
                  this.outOfWaterTime = Math.max(0, this.outOfWaterTime - 1);
                  if (this.biting) {
                     this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.1D * (double)this.syncronizedRandom.nextFloat() * (double)this.syncronizedRandom.nextFloat(), 0.0D));
                  }

                  if (!this.level().isClientSide) {
                     this.catchingFish(blockpos);
                  }
               } else {
                  this.outOfWaterTime = Math.min(10, this.outOfWaterTime + 1);
               }
            }
         }

         if (!fluidstate.is(FluidTags.WATER)) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D, 0.0D));
         }

         this.move(MoverType.SELF, this.getDeltaMovement());
         this.updateRotation();
         if (this.currentState == FishingHook.FishHookState.FLYING && (this.onGround() || this.horizontalCollision)) {
            this.setDeltaMovement(Vec3.ZERO);
         }

         double d1 = 0.92D;
         this.setDeltaMovement(this.getDeltaMovement().scale(0.92D));
         this.reapplyPosition();
      }
   }

   private boolean shouldStopFishing(Player pPlayer) {
      ItemStack itemstack = pPlayer.getMainHandItem();
      ItemStack itemstack1 = pPlayer.getOffhandItem();
      boolean flag = itemstack.is(Items.FISHING_ROD);
      boolean flag1 = itemstack1.is(Items.FISHING_ROD);
      if (!pPlayer.isRemoved() && pPlayer.isAlive() && (flag || flag1) && !(this.distanceToSqr(pPlayer) > 1024.0D)) {
         return false;
      } else {
         this.discard();
         return true;
      }
   }

   private void checkCollision() {
      HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
      this.onHit(hitresult);
   }

   protected boolean canHitEntity(Entity p_37135_) {
      return super.canHitEntity(p_37135_) || p_37135_.isAlive() && p_37135_ instanceof ItemEntity;
   }

   protected void onHitEntity(EntityHitResult pResult) {
      super.onHitEntity(pResult);
      if (!this.level().isClientSide) {
         this.setHookedEntity(pResult.getEntity());
      }

   }

   protected void onHitBlock(BlockHitResult pResult) {
      super.onHitBlock(pResult);
      this.setDeltaMovement(this.getDeltaMovement().normalize().scale(pResult.distanceTo(this)));
   }

   private void setHookedEntity(@Nullable Entity pHookedEntity) {
      this.hookedIn = pHookedEntity;
      this.getEntityData().set(DATA_HOOKED_ENTITY, pHookedEntity == null ? 0 : pHookedEntity.getId() + 1);
   }

   private void catchingFish(BlockPos pPos) {
      ServerLevel serverlevel = (ServerLevel)this.level();
      int i = 1;
      BlockPos blockpos = pPos.above();
      if (this.random.nextFloat() < 0.25F && this.level().isRainingAt(blockpos)) {
         ++i;
      }

      if (this.random.nextFloat() < 0.5F && !this.level().canSeeSky(blockpos)) {
         --i;
      }

      if (this.nibble > 0) {
         --this.nibble;
         if (this.nibble <= 0) {
            this.timeUntilLured = 0;
            this.timeUntilHooked = 0;
            this.getEntityData().set(DATA_BITING, false);
         }
      } else if (this.timeUntilHooked > 0) {
         this.timeUntilHooked -= i;
         if (this.timeUntilHooked > 0) {
            this.fishAngle += (float)this.random.triangle(0.0D, 9.188D);
            float f = this.fishAngle * ((float)Math.PI / 180F);
            float f1 = Mth.sin(f);
            float f2 = Mth.cos(f);
            double d0 = this.getX() + (double)(f1 * (float)this.timeUntilHooked * 0.1F);
            double d1 = (double)((float)Mth.floor(this.getY()) + 1.0F);
            double d2 = this.getZ() + (double)(f2 * (float)this.timeUntilHooked * 0.1F);
            BlockState blockstate = serverlevel.getBlockState(BlockPos.containing(d0, d1 - 1.0D, d2));
            if (blockstate.is(Blocks.WATER)) {
               if (this.random.nextFloat() < 0.15F) {
                  serverlevel.sendParticles(ParticleTypes.BUBBLE, d0, d1 - (double)0.1F, d2, 1, (double)f1, 0.1D, (double)f2, 0.0D);
               }

               float f3 = f1 * 0.04F;
               float f4 = f2 * 0.04F;
               serverlevel.sendParticles(ParticleTypes.FISHING, d0, d1, d2, 0, (double)f4, 0.01D, (double)(-f3), 1.0D);
               serverlevel.sendParticles(ParticleTypes.FISHING, d0, d1, d2, 0, (double)(-f4), 0.01D, (double)f3, 1.0D);
            }
         } else {
            this.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
            double d3 = this.getY() + 0.5D;
            serverlevel.sendParticles(ParticleTypes.BUBBLE, this.getX(), d3, this.getZ(), (int)(1.0F + this.getBbWidth() * 20.0F), (double)this.getBbWidth(), 0.0D, (double)this.getBbWidth(), (double)0.2F);
            serverlevel.sendParticles(ParticleTypes.FISHING, this.getX(), d3, this.getZ(), (int)(1.0F + this.getBbWidth() * 20.0F), (double)this.getBbWidth(), 0.0D, (double)this.getBbWidth(), (double)0.2F);
            this.nibble = Mth.nextInt(this.random, 20, 40);
            this.getEntityData().set(DATA_BITING, true);
         }
      } else if (this.timeUntilLured > 0) {
         this.timeUntilLured -= i;
         float f5 = 0.15F;
         if (this.timeUntilLured < 20) {
            f5 += (float)(20 - this.timeUntilLured) * 0.05F;
         } else if (this.timeUntilLured < 40) {
            f5 += (float)(40 - this.timeUntilLured) * 0.02F;
         } else if (this.timeUntilLured < 60) {
            f5 += (float)(60 - this.timeUntilLured) * 0.01F;
         }

         if (this.random.nextFloat() < f5) {
            float f6 = Mth.nextFloat(this.random, 0.0F, 360.0F) * ((float)Math.PI / 180F);
            float f7 = Mth.nextFloat(this.random, 25.0F, 60.0F);
            double d4 = this.getX() + (double)(Mth.sin(f6) * f7) * 0.1D;
            double d5 = (double)((float)Mth.floor(this.getY()) + 1.0F);
            double d6 = this.getZ() + (double)(Mth.cos(f6) * f7) * 0.1D;
            BlockState blockstate1 = serverlevel.getBlockState(BlockPos.containing(d4, d5 - 1.0D, d6));
            if (blockstate1.is(Blocks.WATER)) {
               serverlevel.sendParticles(ParticleTypes.SPLASH, d4, d5, d6, 2 + this.random.nextInt(2), (double)0.1F, 0.0D, (double)0.1F, 0.0D);
            }
         }

         if (this.timeUntilLured <= 0) {
            this.fishAngle = Mth.nextFloat(this.random, 0.0F, 360.0F);
            this.timeUntilHooked = Mth.nextInt(this.random, 20, 80);
         }
      } else {
         this.timeUntilLured = Mth.nextInt(this.random, 100, 600);
         this.timeUntilLured -= this.lureSpeed * 20 * 5;
      }

   }

   private boolean calculateOpenWater(BlockPos pPos) {
      FishingHook.OpenWaterType fishinghook$openwatertype = FishingHook.OpenWaterType.INVALID;

      for(int i = -1; i <= 2; ++i) {
         FishingHook.OpenWaterType fishinghook$openwatertype1 = this.getOpenWaterTypeForArea(pPos.offset(-2, i, -2), pPos.offset(2, i, 2));
         switch (fishinghook$openwatertype1) {
            case INVALID:
               return false;
            case ABOVE_WATER:
               if (fishinghook$openwatertype == FishingHook.OpenWaterType.INVALID) {
                  return false;
               }
               break;
            case INSIDE_WATER:
               if (fishinghook$openwatertype == FishingHook.OpenWaterType.ABOVE_WATER) {
                  return false;
               }
         }

         fishinghook$openwatertype = fishinghook$openwatertype1;
      }

      return true;
   }

   private FishingHook.OpenWaterType getOpenWaterTypeForArea(BlockPos pFirstPos, BlockPos pSecondPos) {
      return BlockPos.betweenClosedStream(pFirstPos, pSecondPos).map(this::getOpenWaterTypeForBlock).reduce((p_37139_, p_37140_) -> {
         return p_37139_ == p_37140_ ? p_37139_ : FishingHook.OpenWaterType.INVALID;
      }).orElse(FishingHook.OpenWaterType.INVALID);
   }

   private FishingHook.OpenWaterType getOpenWaterTypeForBlock(BlockPos p_37164_) {
      BlockState blockstate = this.level().getBlockState(p_37164_);
      if (!blockstate.isAir() && !blockstate.is(Blocks.LILY_PAD)) {
         FluidState fluidstate = blockstate.getFluidState();
         return fluidstate.is(FluidTags.WATER) && fluidstate.isSource() && blockstate.getCollisionShape(this.level(), p_37164_).isEmpty() ? FishingHook.OpenWaterType.INSIDE_WATER : FishingHook.OpenWaterType.INVALID;
      } else {
         return FishingHook.OpenWaterType.ABOVE_WATER;
      }
   }

   public boolean isOpenWaterFishing() {
      return this.openWater;
   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
   }

   public void readAdditionalSaveData(CompoundTag pCompound) {
   }

   public int retrieve(ItemStack pStack) {
      Player player = this.getPlayerOwner();
      if (!this.level().isClientSide && player != null && !this.shouldStopFishing(player)) {
         int i = 0;
         if (this.hookedIn != null) {
            this.pullEntity(this.hookedIn);
            CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer)player, pStack, this, Collections.emptyList());
            this.level().broadcastEntityEvent(this, (byte)31);
            i = this.hookedIn instanceof ItemEntity ? 3 : 5;
         } else if (this.nibble > 0) {
            LootParams lootparams = (new LootParams.Builder((ServerLevel)this.level())).withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.TOOL, pStack).withParameter(LootContextParams.THIS_ENTITY, this).withLuck((float)this.luck + player.getLuck()).create(LootContextParamSets.FISHING);
            LootTable loottable = this.level().getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
            List<ItemStack> list = loottable.getRandomItems(lootparams);
            CriteriaTriggers.FISHING_ROD_HOOKED.trigger((ServerPlayer)player, pStack, this, list);

            for(ItemStack itemstack : list) {
               ItemEntity itementity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), itemstack);
               double d0 = player.getX() - this.getX();
               double d1 = player.getY() - this.getY();
               double d2 = player.getZ() - this.getZ();
               double d3 = 0.1D;
               itementity.setDeltaMovement(d0 * 0.1D, d1 * 0.1D + Math.sqrt(Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2)) * 0.08D, d2 * 0.1D);
               this.level().addFreshEntity(itementity);
               player.level().addFreshEntity(new ExperienceOrb(player.level(), player.getX(), player.getY() + 0.5D, player.getZ() + 0.5D, this.random.nextInt(6) + 1));
               if (itemstack.is(ItemTags.FISHES)) {
                  player.awardStat(Stats.FISH_CAUGHT, 1);
               }
            }

            i = 1;
         }

         if (this.onGround()) {
            i = 2;
         }

         this.discard();
         return i;
      } else {
         return 0;
      }
   }

   public void handleEntityEvent(byte pId) {
      if (pId == 31 && this.level().isClientSide && this.hookedIn instanceof Player && ((Player)this.hookedIn).isLocalPlayer()) {
         this.pullEntity(this.hookedIn);
      }

      super.handleEntityEvent(pId);
   }

   protected void pullEntity(Entity pEntity) {
      Entity entity = this.getOwner();
      if (entity != null) {
         Vec3 vec3 = (new Vec3(entity.getX() - this.getX(), entity.getY() - this.getY(), entity.getZ() - this.getZ())).scale(0.1D);
         pEntity.setDeltaMovement(pEntity.getDeltaMovement().add(vec3));
      }
   }

   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   public void remove(Entity.RemovalReason pReason) {
      this.updateOwnerInfo((FishingHook)null);
      super.remove(pReason);
   }

   public void onClientRemoval() {
      this.updateOwnerInfo((FishingHook)null);
   }

   public void setOwner(@Nullable Entity pOwner) {
      super.setOwner(pOwner);
      this.updateOwnerInfo(this);
   }

   private void updateOwnerInfo(@Nullable FishingHook pFishingHook) {
      Player player = this.getPlayerOwner();
      if (player != null) {
         player.fishing = pFishingHook;
      }

   }

   @Nullable
   public Player getPlayerOwner() {
      Entity entity = this.getOwner();
      return entity instanceof Player ? (Player)entity : null;
   }

   @Nullable
   public Entity getHookedIn() {
      return this.hookedIn;
   }

   public boolean canChangeDimensions() {
      return false;
   }

   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      Entity entity = this.getOwner();
      return new ClientboundAddEntityPacket(this, entity == null ? this.getId() : entity.getId());
   }

   public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
      super.recreateFromPacket(pPacket);
      if (this.getPlayerOwner() == null) {
         int i = pPacket.getData();
         LOGGER.error("Failed to recreate fishing hook on client. {} (id: {}) is not a valid owner.", this.level().getEntity(i), i);
         this.kill();
      }

   }

   static enum FishHookState {
      FLYING,
      HOOKED_IN_ENTITY,
      BOBBING;
   }

   static enum OpenWaterType {
      ABOVE_WATER,
      INSIDE_WATER,
      INVALID;
   }
}