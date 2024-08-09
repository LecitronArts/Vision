package net.minecraft.world.entity.item;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PrimedTnt extends Entity implements TraceableEntity {
   private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(PrimedTnt.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE_ID = SynchedEntityData.defineId(PrimedTnt.class, EntityDataSerializers.BLOCK_STATE);
   private static final int DEFAULT_FUSE_TIME = 80;
   private static final String TAG_BLOCK_STATE = "block_state";
   public static final String TAG_FUSE = "fuse";
   @Nullable
   private LivingEntity owner;

   public PrimedTnt(EntityType<? extends PrimedTnt> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.blocksBuilding = true;
   }

   public PrimedTnt(Level pLevel, double pX, double pY, double pZ, @Nullable LivingEntity pOwner) {
      this(EntityType.TNT, pLevel);
      this.setPos(pX, pY, pZ);
      double d0 = pLevel.random.nextDouble() * (double)((float)Math.PI * 2F);
      this.setDeltaMovement(-Math.sin(d0) * 0.02D, (double)0.2F, -Math.cos(d0) * 0.02D);
      this.setFuse(80);
      this.xo = pX;
      this.yo = pY;
      this.zo = pZ;
      this.owner = pOwner;
   }

   protected void defineSynchedData() {
      this.entityData.define(DATA_FUSE_ID, 80);
      this.entityData.define(DATA_BLOCK_STATE_ID, Blocks.TNT.defaultBlockState());
   }

   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   public boolean isPickable() {
      return !this.isRemoved();
   }

   public void tick() {
      if (!this.isNoGravity()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
      }

      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
      if (this.onGround()) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
      }

      int i = this.getFuse() - 1;
      this.setFuse(i);
      if (i <= 0) {
         this.discard();
         if (!this.level().isClientSide) {
            this.explode();
         }
      } else {
         this.updateInWaterStateAndDoFluidPushing();
         if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.0D, 0.0D);
         }
      }

   }

   private void explode() {
      float f = 4.0F;
      this.level().explode(this, this.getX(), this.getY(0.0625D), this.getZ(), 4.0F, Level.ExplosionInteraction.TNT);
   }

   protected void addAdditionalSaveData(CompoundTag pCompound) {
      pCompound.putShort("fuse", (short)this.getFuse());
      pCompound.put("block_state", NbtUtils.writeBlockState(this.getBlockState()));
   }

   protected void readAdditionalSaveData(CompoundTag pCompound) {
      this.setFuse(pCompound.getShort("fuse"));
      if (pCompound.contains("block_state", 10)) {
         this.setBlockState(NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), pCompound.getCompound("block_state")));
      }

   }

   @Nullable
   public LivingEntity getOwner() {
      return this.owner;
   }

   public void restoreFrom(Entity pEntity) {
      super.restoreFrom(pEntity);
      if (pEntity instanceof PrimedTnt primedtnt) {
         this.owner = primedtnt.owner;
      }

   }

   protected float getEyeHeight(Pose pPose, EntityDimensions pSize) {
      return 0.15F;
   }

   public void setFuse(int pLife) {
      this.entityData.set(DATA_FUSE_ID, pLife);
   }

   public int getFuse() {
      return this.entityData.get(DATA_FUSE_ID);
   }

   public void setBlockState(BlockState pBlockState) {
      this.entityData.set(DATA_BLOCK_STATE_ID, pBlockState);
   }

   public BlockState getBlockState() {
      return this.entityData.get(DATA_BLOCK_STATE_ID);
   }
}