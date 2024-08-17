package net.minecraft.world.entity.vehicle;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.IntFunction;
import javax.annotation.Nullable;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.fixes.versioned.visual.EntityRidingOffsetsPre1_20_2;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Unique;

public class Boat extends VehicleEntity implements VariantHolder<Boat.Type> {
   private static final EntityDataAccessor<Integer> DATA_ID_TYPE = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_ID_PADDLE_LEFT = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_ID_PADDLE_RIGHT = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_ID_BUBBLE_TIME = SynchedEntityData.defineId(Boat.class, EntityDataSerializers.INT);
   public static final int PADDLE_LEFT = 0;
   public static final int PADDLE_RIGHT = 1;
   private static final int TIME_TO_EJECT = 60;
   private static final float PADDLE_SPEED = ((float)Math.PI / 8F);
   public static final double PADDLE_SOUND_TIME = (double)((float)Math.PI / 4F);
   public static final int BUBBLE_TIME = 60;
   private final float[] paddlePositions = new float[2];
   private float invFriction;
   private float outOfControlTicks;
   private float deltaRotation;
   private int lerpSteps;
   private double lerpX;
   private double lerpY;
   private double lerpZ;
   private double lerpYRot;
   private double lerpXRot;
   private boolean inputLeft;
   private boolean inputRight;
   private boolean inputUp;
   private boolean inputDown;
   private double waterLevel;
   private float landFriction;
   private Boat.Status status;
   private Boat.Status oldStatus;
   private double lastYd;
   private boolean isAboveBubbleColumn;
   private boolean bubbleColumnDirectionIsDown;
   private float bubbleMultiplier;
   private float bubbleAngle;
   private float bubbleAngleO;
   @Unique
   private double viaFabricPlus$speedMultiplier = 0.07D;

   @Unique
   private int viaFabricPlus$boatInterpolationSteps;

   @Unique
   private Vec3 viaFabricPlus$boatVelocity = Vec3.ZERO;
   public Boat(EntityType<? extends Boat> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.blocksBuilding = true;
   }

   public Boat(Level pLevel, double pX, double pY, double pZ) {
      this(EntityType.BOAT, pLevel);
      this.setPos(pX, pY, pZ);
      this.xo = pX;
      this.yo = pY;
      this.zo = pZ;
   }

   protected float getEyeHeight(Pose pPose, EntityDimensions pSize) {
      return pSize.height;
   }

   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.EVENTS;
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_ID_TYPE, Boat.Type.OAK.ordinal());
      this.entityData.define(DATA_ID_PADDLE_LEFT, false);
      this.entityData.define(DATA_ID_PADDLE_RIGHT, false);
      this.entityData.define(DATA_ID_BUBBLE_TIME, 0);
   }

   public boolean canCollideWith(Entity pEntity) {
      return canVehicleCollide(this, pEntity);
   }

   public static boolean canVehicleCollide(Entity pVehicle, Entity pEntity) {
      return (pEntity.canBeCollidedWith() || pEntity.isPushable()) && !pVehicle.isPassengerOfSameVehicle(pEntity);
   }

   public boolean canBeCollidedWith() {
      return true;
   }

   public boolean isPushable() {
      return true;
   }

   protected Vec3 getRelativePortalPosition(Direction.Axis pAxis, BlockUtil.FoundRectangle pPortal) {
      return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(pAxis, pPortal));
   }

   protected Vector3f getPassengerAttachmentPoint(Entity pEntity, EntityDimensions pDimensions, float pScale) {
      float f = this.getSinglePassengerXOffset();
      if (this.getPassengers().size() > 1) {
         int i = this.getPassengers().indexOf(pEntity);
         if (i == 0) {
            f = 0.2F;
         } else {
            f = -0.6F;
         }

         if (pEntity instanceof Animal) {
            f += 0.2F;
         }
      }

      return new Vector3f(0.0F, this.getVariant() == Boat.Type.BAMBOO ? pDimensions.height * 0.8888889F : pDimensions.height / 3.0F, f);
   }

   public void onAboveBubbleCol(boolean pDownwards) {
      if (!this.level().isClientSide) {
         this.isAboveBubbleColumn = true;
         this.bubbleColumnDirectionIsDown = pDownwards;
         if (this.getBubbleTime() == 0) {
            this.setBubbleTime(60);
         }
      }

      this.level().addParticle(ParticleTypes.SPLASH, this.getX() + (double)this.random.nextFloat(), this.getY() + 0.7D, this.getZ() + (double)this.random.nextFloat(), 0.0D, 0.0D, 0.0D);
      if (this.random.nextInt(20) == 0) {
         this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), this.getSwimSplashSound(), this.getSoundSource(), 1.0F, 0.8F + 0.4F * this.random.nextFloat(), false);
         this.gameEvent(GameEvent.SPLASH, this.getControllingPassenger());
      }

   }

   public void push(Entity pEntity) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         super.push(pEntity);
         return;
      }
      if (pEntity instanceof Boat) {
         if (pEntity.getBoundingBox().minY < this.getBoundingBox().maxY) {
            super.push(pEntity);
         }
      } else if (pEntity.getBoundingBox().minY <= this.getBoundingBox().minY) {
         super.push(pEntity);
      }

   }

   public Item getDropItem() {
      Item item;
      switch (this.getVariant()) {
         case SPRUCE:
            item = Items.SPRUCE_BOAT;
            break;
         case BIRCH:
            item = Items.BIRCH_BOAT;
            break;
         case JUNGLE:
            item = Items.JUNGLE_BOAT;
            break;
         case ACACIA:
            item = Items.ACACIA_BOAT;
            break;
         case CHERRY:
            item = Items.CHERRY_BOAT;
            break;
         case DARK_OAK:
            item = Items.DARK_OAK_BOAT;
            break;
         case MANGROVE:
            item = Items.MANGROVE_BOAT;
            break;
         case BAMBOO:
            item = Items.BAMBOO_RAFT;
            break;
         default:
            item = Items.OAK_BOAT;
      }

      return item;
   }

   public void animateHurt(float pYaw) {
      this.setHurtDir(-this.getHurtDir());
      this.setHurtTime(10);
      this.setDamage(this.getDamage() * 11.0F);
   }

   public boolean isPickable() {
      return !this.isRemoved();
   }

   public void lerpTo(double pX, double pY, double pZ, float pYRot, float pXRot, int pSteps) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {

         if (/*interpolate &&*/ this.isVehicle() && ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_7_6)) {
            this.xo = pX;
            this.yo = pY;
            this.zo = pZ;
            this.viaFabricPlus$boatInterpolationSteps = 0;
            this.setPos(pX, pY, pZ);
            this.setRot(pYRot, pXRot);
            this.setDeltaMovement(Vec3.ZERO);
            this.viaFabricPlus$boatVelocity = Vec3.ZERO;
         } else {
            if (!this.isVehicle()) {
               this.viaFabricPlus$boatInterpolationSteps = pSteps + 5;
            } else {
               if (this.distanceToSqr(pX, pY, pZ) <= 1) {
                  return;
               }
               this.viaFabricPlus$boatInterpolationSteps = 3;
            }

            this.lerpX = pX;
            this.lerpY = pY;
            this.lerpZ = pZ;
            this.lerpYRot = pYRot;
            this.lerpXRot = pXRot;
            this.setDeltaMovement(this.viaFabricPlus$boatVelocity);
         }
         return;
      }
      this.lerpX = pX;
      this.lerpY = pY;
      this.lerpZ = pZ;
      this.lerpYRot = (double)pYRot;
      this.lerpXRot = (double)pXRot;
      this.lerpSteps = 10;
   }

   public double lerpTargetX() {
      return this.lerpSteps > 0 ? this.lerpX : this.getX();
   }

   public double lerpTargetY() {
      return this.lerpSteps > 0 ? this.lerpY : this.getY();
   }

   public double lerpTargetZ() {
      return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
   }

   public float lerpTargetXRot() {
      return this.lerpSteps > 0 ? (float)this.lerpXRot : this.getXRot();
   }

   public float lerpTargetYRot() {
      return this.lerpSteps > 0 ? (float)this.lerpYRot : this.getYRot();
   }

   public Direction getMotionDirection() {
      return this.getDirection().getClockWise();
   }

   public void tick() {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         super.tick();

         if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
         }
         if (this.getDamage() > 0) {
            this.setDamage(this.getDamage() - 1);
         }
         this.xo = this.getX();
         this.yo = this.getY();
         this.zo = this.getZ();

         final int yPartitions = 5;
         double percentSubmerged = 0;
         for (int partitionIndex = 0; partitionIndex < yPartitions; partitionIndex++) {
            final double minY = this.getBoundingBox().minY + this.getBoundingBox().getYsize() * partitionIndex / yPartitions - 0.125;
            final double maxY = this.getBoundingBox().minY + this.getBoundingBox().getYsize() * (partitionIndex + 1) / yPartitions - 0.125;
            final AABB box = new AABB(this.getBoundingBox().minX, minY, this.getBoundingBox().minZ, this.getBoundingBox().maxX, maxY, this.getBoundingBox().maxZ);
            if (BlockPos.betweenClosedStream(box).anyMatch(pos -> this.level().getFluidState(pos).is(FluidTags.WATER))) {
               percentSubmerged += 1.0 / yPartitions;
            }
         }

         final double oldHorizontalSpeed = this.getDeltaMovement().horizontalDistance();
         if (oldHorizontalSpeed > (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_7_6) ? 0.2625D : 0.2975D)) {
            final double rx = Math.cos(this.getYRot() * Math.PI / 180);
            final double rz = Math.sin(this.getYRot() * Math.PI / 180);
            for (int i = 0; i < 1 + oldHorizontalSpeed * 60; i++) {
               final double dForward = this.random.nextFloat() * 2 - 1;
               final double dSideways = (this.random.nextInt(2) * 2 - 1) * 0.7D;
               if (this.random.nextBoolean()) {
                  final double x = this.getX() - rx * dForward * 0.8 + rz * dSideways;
                  final double z = this.getZ() - rz * dForward * 0.8 - rx * dSideways;
                  this.level().addParticle(ParticleTypes.SPLASH, x, this.getY() - 0.125D, z, this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z);
               } else {
                  final double x = this.getX() + rx + rz * dForward * 0.7;
                  final double z = this.getZ() + rz - rx * dForward * 0.7;
                  this.level().addParticle(ParticleTypes.SPLASH, x, this.getY() - 0.125D, z, this.getDeltaMovement().x, this.getDeltaMovement().y, this.getDeltaMovement().z);
               }
            }
         }

         if (this.level().isClientSide && !this.isVehicle()) {
            if (this.viaFabricPlus$boatInterpolationSteps > 0) {
               final double newX = this.getX() + (this.lerpX - this.getX()) / this.viaFabricPlus$boatInterpolationSteps;
               final double newY = this.getY() + (this.lerpY - this.getY()) / this.viaFabricPlus$boatInterpolationSteps;
               final double newZ = this.getZ() + (this.lerpZ - this.getZ()) / this.viaFabricPlus$boatInterpolationSteps;
               final double newYaw = this.getYRot() + Mth.wrapDegrees(this.lerpYRot - this.getYRot()) / this.viaFabricPlus$boatInterpolationSteps;
               final double newPitch = this.getXRot() + (this.lerpXRot - this.getXRot()) / this.viaFabricPlus$boatInterpolationSteps;
               this.viaFabricPlus$boatInterpolationSteps--;
               this.setPos(newX, newY, newZ);
               this.setRot((float) newYaw, (float) newPitch);
            } else {
               this.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y, this.getZ() + this.getDeltaMovement().z);
               if (this.onGround()) {
                  this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
               }
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.99D, 0.95D, 0.99D));
            }
         } else {
            if (percentSubmerged < 1) {
               final double normalizedDistanceFromMiddle = percentSubmerged * 2 - 1;
               this.setDeltaMovement(this.getDeltaMovement().add(0, 0.04D * normalizedDistanceFromMiddle, 0));
            } else {
               if (this.getDeltaMovement().y < 0) {
                  this.setDeltaMovement(this.getDeltaMovement().multiply(1, 0.5D, 1));
               }
               this.setDeltaMovement(this.getDeltaMovement().add(0, 0.007D, 0));
            }

            if (this.getControllingPassenger() != null) {
               final LivingEntity passenger = this.getControllingPassenger();
               if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_5_2)) {
                  final double xAcceleration = passenger.getDeltaMovement().x * this.viaFabricPlus$speedMultiplier;
                  final double zAcceleration = passenger.getDeltaMovement().z * this.viaFabricPlus$speedMultiplier;
                  this.setDeltaMovement(this.getDeltaMovement().add(xAcceleration, 0, zAcceleration));
               } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_6_4)) {
                  if (passenger.zza > 0) {
                     final double xAcceleration = -Math.sin(passenger.getYRot() * Math.PI / 180) * this.viaFabricPlus$speedMultiplier * 0.05D;
                     final double zAcceleration = Math.cos(passenger.getYRot() * Math.PI / 180) * this.viaFabricPlus$speedMultiplier * 0.05D;
                     this.setDeltaMovement(this.getDeltaMovement().add(xAcceleration, 0, zAcceleration));
                  }
               } else {
                  final float boatAngle = passenger.getYRot() - passenger.xxa * 90F;
                  final double xAcceleration = -Math.sin(boatAngle * Math.PI / 180) * this.viaFabricPlus$speedMultiplier * passenger.zza * 0.05D;
                  final double zAcceleration = Math.cos(boatAngle * Math.PI / 180) * this.viaFabricPlus$speedMultiplier * passenger.zza * 0.05D;
                  this.setDeltaMovement(this.getDeltaMovement().add(xAcceleration, 0, zAcceleration));
               }
            }

            double newHorizontalSpeed = this.getDeltaMovement().horizontalDistance();
            if (newHorizontalSpeed > 0.35D) {
               final double multiplier = 0.35D / newHorizontalSpeed;
               this.setDeltaMovement(this.getDeltaMovement().multiply(multiplier, 1, multiplier));
               newHorizontalSpeed = 0.35D;
            }

            if (newHorizontalSpeed > oldHorizontalSpeed && this.viaFabricPlus$speedMultiplier < 0.35D) {
               this.viaFabricPlus$speedMultiplier += (0.35D - this.viaFabricPlus$speedMultiplier) / 35;
               if (this.viaFabricPlus$speedMultiplier > 0.35D) {
                  this.viaFabricPlus$speedMultiplier = 0.35D;
               }
            } else {
               this.viaFabricPlus$speedMultiplier -= (this.viaFabricPlus$speedMultiplier - 0.07D) / 35;
               if (this.viaFabricPlus$speedMultiplier < 0.07D) {
                  this.viaFabricPlus$speedMultiplier = 0.07D;
               }
            }

            if (ProtocolTranslator.getTargetVersion().newerThan(LegacyProtocolVersion.r1_6_4)) {
               for (int i = 0; i < 4; i++) {
                  final int dx = Mth.floor(this.getX() + ((i % 2) - 0.5D) * 0.8D);
                  //noinspection IntegerDivisionInFloatingPointContext
                  final int dz = Mth.floor(this.getZ() + ((i / 2) - 0.5D) * 0.8D);
                  for (int ddy = 0; ddy < 2; ddy++) {
                     final int dy = Mth.floor(this.getY()) + ddy;
                     final BlockPos pos = new BlockPos(dx, dy, dz);
                     final Block block = this.level().getBlockState(pos).getBlock();
                     if (block == Blocks.SNOW) {
                        this.level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        this.horizontalCollision = false;
                     } else if (block == Blocks.LILY_PAD) {
                        this.level().destroyBlock(pos, true);
                        this.horizontalCollision = false;
                     }
                  }
               }
            }

            if (this.onGround()) {
               this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
            }

            this.move(MoverType.SELF, this.getDeltaMovement());

            if (!this.horizontalCollision || oldHorizontalSpeed <= 0.2975D) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.99D, 0.95D, 0.99D));
            }

            this.setXRot(0);
            final double deltaX = this.xo - this.getX();
            final double deltaZ = this.zo - this.getZ();
            if (deltaX * deltaX + deltaZ * deltaZ > 0.001D) {
               final double yawDelta = Mth.clamp(Mth.wrapDegrees((Mth.atan2(deltaZ, deltaX) * 180 / Math.PI) - this.getYRot()), -20, 20);
               this.setYRot((float) (this.getYRot() + yawDelta));
            }
         }
         return;
      }
      this.oldStatus = this.status;
      this.status = this.getStatus();
      if (this.status != Boat.Status.UNDER_WATER && this.status != Boat.Status.UNDER_FLOWING_WATER) {
         this.outOfControlTicks = 0.0F;
      } else {
         ++this.outOfControlTicks;
      }

      if (!this.level().isClientSide && this.outOfControlTicks >= 60.0F) {
         this.ejectPassengers();
      }

      if (this.getHurtTime() > 0) {
         this.setHurtTime(this.getHurtTime() - 1);
      }

      if (this.getDamage() > 0.0F) {
         this.setDamage(this.getDamage() - 1.0F);
      }

      super.tick();
      this.tickLerp();
      if (this.isControlledByLocalInstance()) {
         if (!(this.getFirstPassenger() instanceof Player)) {
            this.setPaddleState(false, false);
         }

         this.floatBoat();
         if (this.level().isClientSide) {
            this.controlBoat();
            this.level().sendPacketToServer(new ServerboundPaddleBoatPacket(this.getPaddleState(0), this.getPaddleState(1)));
         }

         this.move(MoverType.SELF, this.getDeltaMovement());
      } else {
         this.setDeltaMovement(Vec3.ZERO);
      }

      this.tickBubbleColumn();

      for(int i = 0; i <= 1; ++i) {
         if (this.getPaddleState(i)) {
            if (!this.isSilent() && (double)(this.paddlePositions[i] % ((float)Math.PI * 2F)) <= (double)((float)Math.PI / 4F) && (double)((this.paddlePositions[i] + ((float)Math.PI / 8F)) % ((float)Math.PI * 2F)) >= (double)((float)Math.PI / 4F)) {
               SoundEvent soundevent = this.getPaddleSound();
               if (soundevent != null) {
                  Vec3 vec3 = this.getViewVector(1.0F);
                  double d0 = i == 1 ? -vec3.z : vec3.z;
                  double d1 = i == 1 ? vec3.x : -vec3.x;
                  this.level().playSound((Player)null, this.getX() + d0, this.getY(), this.getZ() + d1, soundevent, this.getSoundSource(), 1.0F, 0.8F + 0.4F * this.random.nextFloat());
               }
            }

            this.paddlePositions[i] += ((float)Math.PI / 8F);
         } else {
            this.paddlePositions[i] = 0.0F;
         }
      }

      this.checkInsideBlocks();
      List<Entity> list = this.level().getEntities(this, this.getBoundingBox().inflate((double)0.2F, (double)-0.01F, (double)0.2F), EntitySelector.pushableBy(this));
      if (!list.isEmpty()) {
         boolean flag = !this.level().isClientSide && !(this.getControllingPassenger() instanceof Player);

         for(Entity entity : list) {
            if (!entity.hasPassenger(this)) {
               if (flag && this.getPassengers().size() < this.getMaxPassengers() && !entity.isPassenger() && this.hasEnoughSpaceFor(entity) && entity instanceof LivingEntity && !(entity instanceof WaterAnimal) && !(entity instanceof Player)) {
                  entity.startRiding(this);
               } else {
                  this.push(entity);
               }
            }
         }
      }

   }

   private void tickBubbleColumn() {
      if (this.level().isClientSide) {
         int i = this.getBubbleTime();
         if (i > 0) {
            this.bubbleMultiplier += 0.05F;
         } else {
            this.bubbleMultiplier -= 0.1F;
         }

         this.bubbleMultiplier = Mth.clamp(this.bubbleMultiplier, 0.0F, 1.0F);
         this.bubbleAngleO = this.bubbleAngle;
         this.bubbleAngle = 10.0F * (float)Math.sin((double)(0.5F * (float)this.level().getGameTime())) * this.bubbleMultiplier;
      } else {
         if (!this.isAboveBubbleColumn) {
            this.setBubbleTime(0);
         }

         int k = this.getBubbleTime();
         if (k > 0) {
            --k;
            this.setBubbleTime(k);
            int j = 60 - k - 1;
            if (j > 0 && k == 0) {
               this.setBubbleTime(0);
               Vec3 vec3 = this.getDeltaMovement();
               if (this.bubbleColumnDirectionIsDown) {
                  this.setDeltaMovement(vec3.add(0.0D, -0.7D, 0.0D));
                  this.ejectPassengers();
               } else {
                  this.setDeltaMovement(vec3.x, this.hasPassenger((p_150274_) -> {
                     return p_150274_ instanceof Player;
                  }) ? 2.7D : 0.6D, vec3.z);
               }
            }

            this.isAboveBubbleColumn = false;
         }
      }

   }
   @Override
   public void lerpMotion(double x, double y, double z) {
      super.lerpMotion(x, y, z);

      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         this.viaFabricPlus$boatVelocity = new Vec3(x, y, z);
      }
   }
   @Nullable
   protected SoundEvent getPaddleSound() {
      switch (this.getStatus()) {
         case IN_WATER:
         case UNDER_WATER:
         case UNDER_FLOWING_WATER:
            return SoundEvents.BOAT_PADDLE_WATER;
         case ON_LAND:
            return SoundEvents.BOAT_PADDLE_LAND;
         case IN_AIR:
         default:
            return null;
      }
   }

   private void tickLerp() {
      if (this.isControlledByLocalInstance()) {
         this.lerpSteps = 0;
         this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
      }

      if (this.lerpSteps > 0) {
         this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
         --this.lerpSteps;
      }
   }

   public void setPaddleState(boolean pLeft, boolean pRight) {
      this.entityData.set(DATA_ID_PADDLE_LEFT, pLeft);
      this.entityData.set(DATA_ID_PADDLE_RIGHT, pRight);
   }

   public float getRowingTime(int pSide, float pLimbSwing) {
      return this.getPaddleState(pSide) ? Mth.clampedLerp(this.paddlePositions[pSide] - ((float)Math.PI / 8F), this.paddlePositions[pSide], pLimbSwing) : 0.0F;
   }

   private Boat.Status getStatus() {
      Boat.Status boat$status = this.isUnderwater();
      if (boat$status != null) {
         this.waterLevel = this.getBoundingBox().maxY;
         return boat$status;
      } else if (this.checkInWater()) {
         return Boat.Status.IN_WATER;
      } else {
         float f = this.getGroundFriction();
         if (f > 0.0F) {
            this.landFriction = f;
            return Boat.Status.ON_LAND;
         } else {
            return Boat.Status.IN_AIR;
         }
      }
   }

   public float getWaterLevelAbove() {
      AABB aabb = this.getBoundingBox();
      int i = Mth.floor(aabb.minX);
      int j = Mth.ceil(aabb.maxX);
      int k = Mth.floor(aabb.maxY);
      int l = Mth.ceil(aabb.maxY - this.lastYd);
      int i1 = Mth.floor(aabb.minZ);
      int j1 = Mth.ceil(aabb.maxZ);
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      label39:
      for(int k1 = k; k1 < l; ++k1) {
         float f = 0.0F;

         for(int l1 = i; l1 < j; ++l1) {
            for(int i2 = i1; i2 < j1; ++i2) {
               blockpos$mutableblockpos.set(l1, k1, i2);
               FluidState fluidstate = this.level().getFluidState(blockpos$mutableblockpos);
               if (fluidstate.is(FluidTags.WATER)) {
                  f = Math.max(f, fluidstate.getHeight(this.level(), blockpos$mutableblockpos));
               }

               if (f >= 1.0F) {
                  continue label39;
               }
            }
         }

         if (f < 1.0F) {
            return (float)blockpos$mutableblockpos.getY() + f;
         }
      }

      return (float)(l + 1);
   }

   public float getGroundFriction() {
      AABB aabb = this.getBoundingBox();
      AABB aabb1 = new AABB(aabb.minX, aabb.minY - 0.001D, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ);
      int i = Mth.floor(aabb1.minX) - 1;
      int j = Mth.ceil(aabb1.maxX) + 1;
      int k = Mth.floor(aabb1.minY) - 1;
      int l = Mth.ceil(aabb1.maxY) + 1;
      int i1 = Mth.floor(aabb1.minZ) - 1;
      int j1 = Mth.ceil(aabb1.maxZ) + 1;
      VoxelShape voxelshape = Shapes.create(aabb1);
      float f = 0.0F;
      int k1 = 0;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for(int l1 = i; l1 < j; ++l1) {
         for(int i2 = i1; i2 < j1; ++i2) {
            int j2 = (l1 != i && l1 != j - 1 ? 0 : 1) + (i2 != i1 && i2 != j1 - 1 ? 0 : 1);
            if (j2 != 2) {
               for(int k2 = k; k2 < l; ++k2) {
                  if (j2 <= 0 || k2 != k && k2 != l - 1) {
                     blockpos$mutableblockpos.set(l1, k2, i2);
                     BlockState blockstate = this.level().getBlockState(blockpos$mutableblockpos);
                     if (!(blockstate.getBlock() instanceof WaterlilyBlock) && Shapes.joinIsNotEmpty(blockstate.getCollisionShape(this.level(), blockpos$mutableblockpos).move((double)l1, (double)k2, (double)i2), voxelshape, BooleanOp.AND)) {
                        f += blockstate.getBlock().getFriction();
                        ++k1;
                     }
                  }
               }
            }
         }
      }

      return f / (float)k1;
   }

   private boolean checkInWater() {
      AABB aabb = this.getBoundingBox();
      int i = Mth.floor(aabb.minX);
      int j = Mth.ceil(aabb.maxX);
      int k = Mth.floor(aabb.minY);
      int l = Mth.ceil(aabb.minY + 0.001D);
      int i1 = Mth.floor(aabb.minZ);
      int j1 = Mth.ceil(aabb.maxZ);
      boolean flag = false;
      this.waterLevel = -Double.MAX_VALUE;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for(int k1 = i; k1 < j; ++k1) {
         for(int l1 = k; l1 < l; ++l1) {
            for(int i2 = i1; i2 < j1; ++i2) {
               blockpos$mutableblockpos.set(k1, l1, i2);
               FluidState fluidstate = this.level().getFluidState(blockpos$mutableblockpos);
               if (fluidstate.is(FluidTags.WATER)) {
                  float f = (float)l1 + fluidstate.getHeight(this.level(), blockpos$mutableblockpos);
                  this.waterLevel = Math.max((double)f, this.waterLevel);
                  flag |= aabb.minY < (double)f;
               }
            }
         }
      }

      return flag;
   }

   @Nullable
   private Boat.Status isUnderwater() {
      AABB aabb = this.getBoundingBox();
      double d0 = aabb.maxY + 0.001D;
      int i = Mth.floor(aabb.minX);
      int j = Mth.ceil(aabb.maxX);
      int k = Mth.floor(aabb.maxY);
      int l = Mth.ceil(d0);
      int i1 = Mth.floor(aabb.minZ);
      int j1 = Mth.ceil(aabb.maxZ);
      boolean flag = false;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for(int k1 = i; k1 < j; ++k1) {
         for(int l1 = k; l1 < l; ++l1) {
            for(int i2 = i1; i2 < j1; ++i2) {
               blockpos$mutableblockpos.set(k1, l1, i2);
               FluidState fluidstate = this.level().getFluidState(blockpos$mutableblockpos);
               if (fluidstate.is(FluidTags.WATER) && d0 < (double)((float)blockpos$mutableblockpos.getY() + fluidstate.getHeight(this.level(), blockpos$mutableblockpos))) {
                  if (!fluidstate.isSource()) {
                     return Boat.Status.UNDER_FLOWING_WATER;
                  }

                  flag = true;
               }
            }
         }
      }

      return flag ? Boat.Status.UNDER_WATER : null;
   }

   private void floatBoat() {
      double d0 = (double)-0.04F;
      double d1 = this.isNoGravity() ? 0.0D : (double)-0.04F;
      double d2 = 0.0D;
      this.invFriction = 0.05F;
      if (this.oldStatus == Boat.Status.IN_AIR && this.status != Boat.Status.IN_AIR && this.status != Boat.Status.ON_LAND) {
         this.waterLevel = this.getY(1.0D);
         this.setPos(this.getX(), (double)(this.getWaterLevelAbove() - this.getBbHeight()) + 0.101D, this.getZ());
         this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
         this.lastYd = 0.0D;
         this.status = Boat.Status.IN_WATER;
      } else {
         if (this.status == Boat.Status.IN_WATER) {
            d2 = (this.waterLevel - this.getY()) / (double)this.getBbHeight();
            this.invFriction = 0.9F;
         } else if (this.status == Boat.Status.UNDER_FLOWING_WATER) {
            d1 = -7.0E-4D;
            this.invFriction = 0.9F;
         } else if (this.status == Boat.Status.UNDER_WATER) {
            d2 = (double)0.01F;
            this.invFriction = 0.45F;
         } else if (this.status == Boat.Status.IN_AIR) {
            this.invFriction = 0.9F;
         } else if (this.status == Boat.Status.ON_LAND) {
            this.invFriction = this.landFriction;
            if (this.getControllingPassenger() instanceof Player) {
               this.landFriction /= 2.0F;
            }
         }

         Vec3 vec3 = this.getDeltaMovement();
         this.setDeltaMovement(vec3.x * (double)this.invFriction, vec3.y + d1, vec3.z * (double)this.invFriction);
         this.deltaRotation *= this.invFriction;
         if (d2 > 0.0D) {
            Vec3 vec31 = this.getDeltaMovement();
            this.setDeltaMovement(vec31.x, (vec31.y + d2 * 0.06153846016296973D) * 0.75D, vec31.z);
         }
      }

   }

   private void controlBoat() {
      if (this.isVehicle()) {
         float f = 0.0F;
         if (this.inputLeft) {
            --this.deltaRotation;
         }

         if (this.inputRight) {
            ++this.deltaRotation;
         }

         if (this.inputRight != this.inputLeft && !this.inputUp && !this.inputDown) {
            f += 0.005F;
         }

         this.setYRot(this.getYRot() + this.deltaRotation);
         if (this.inputUp) {
            f += 0.04F;
         }

         if (this.inputDown) {
            f -= 0.005F;
         }

         this.setDeltaMovement(this.getDeltaMovement().add((double)(Mth.sin(-this.getYRot() * ((float)Math.PI / 180F)) * f), 0.0D, (double)(Mth.cos(this.getYRot() * ((float)Math.PI / 180F)) * f)));
         this.setPaddleState(this.inputRight && !this.inputLeft || this.inputUp, this.inputLeft && !this.inputRight || this.inputUp);
      }
   }

   protected float getSinglePassengerXOffset() {
      return 0.0F;
   }

   public boolean hasEnoughSpaceFor(Entity pEntity) {
      return pEntity.getBbWidth() < this.getBbWidth();
   }

   protected void positionRider(Entity pPassenger, Entity.MoveFunction pCallback) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         final Vec3 newPosition = new Vec3(EntityRidingOffsetsPre1_20_2.getMountedHeightOffset(this, pPassenger)).add(this.position());
         pCallback.accept(pPassenger, newPosition.x, newPosition.y + EntityRidingOffsetsPre1_20_2.getHeightOffset(pPassenger), newPosition.z);
         return;
      }
      super.positionRider(pPassenger, pCallback);
      if (!pPassenger.getType().is(EntityTypeTags.CAN_TURN_IN_BOATS)) {
         pPassenger.setYRot(pPassenger.getYRot() + this.deltaRotation);
         pPassenger.setYHeadRot(pPassenger.getYHeadRot() + this.deltaRotation);
         this.clampRotation(pPassenger);
         if (pPassenger instanceof Animal && this.getPassengers().size() == this.getMaxPassengers()) {
            int i = pPassenger.getId() % 2 == 0 ? 90 : 270;
            pPassenger.setYBodyRot(((Animal)pPassenger).yBodyRot + (float)i);
            pPassenger.setYHeadRot(pPassenger.getYHeadRot() + (float)i);
         }

      }
   }

   public Vec3 getDismountLocationForPassenger(LivingEntity pLivingEntity) {
      Vec3 vec3 = getCollisionHorizontalEscapeVector((double)(this.getBbWidth() * Mth.SQRT_OF_TWO), (double)pLivingEntity.getBbWidth(), pLivingEntity.getYRot());
      double d0 = this.getX() + vec3.x;
      double d1 = this.getZ() + vec3.z;
      BlockPos blockpos = BlockPos.containing(d0, this.getBoundingBox().maxY, d1);
      BlockPos blockpos1 = blockpos.below();
      if (!this.level().isWaterAt(blockpos1)) {
         List<Vec3> list = Lists.newArrayList();
         double d2 = this.level().getBlockFloorHeight(blockpos);
         if (DismountHelper.isBlockFloorValid(d2)) {
            list.add(new Vec3(d0, (double)blockpos.getY() + d2, d1));
         }

         double d3 = this.level().getBlockFloorHeight(blockpos1);
         if (DismountHelper.isBlockFloorValid(d3)) {
            list.add(new Vec3(d0, (double)blockpos1.getY() + d3, d1));
         }

         for(Pose pose : pLivingEntity.getDismountPoses()) {
            for(Vec3 vec31 : list) {
               if (DismountHelper.canDismountTo(this.level(), vec31, pLivingEntity, pose)) {
                  pLivingEntity.setPose(pose);
                  return vec31;
               }
            }
         }
      }

      return super.getDismountLocationForPassenger(pLivingEntity);
   }

   protected void clampRotation(Entity pEntityToUpdate) {
      pEntityToUpdate.setYBodyRot(this.getYRot());
      float f = Mth.wrapDegrees(pEntityToUpdate.getYRot() - this.getYRot());
      float f1 = Mth.clamp(f, -105.0F, 105.0F);
      pEntityToUpdate.yRotO += f1 - f;
      pEntityToUpdate.setYRot(pEntityToUpdate.getYRot() + f1 - f);
      pEntityToUpdate.setYHeadRot(pEntityToUpdate.getYRot());
   }

   public void onPassengerTurned(Entity pEntityToUpdate) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         super.onPassengerTurned(pEntityToUpdate);
         return;
      }
      this.clampRotation(pEntityToUpdate);
   }

   protected void addAdditionalSaveData(CompoundTag pCompound) {
      pCompound.putString("Type", this.getVariant().getSerializedName());
   }

   protected void readAdditionalSaveData(CompoundTag pCompound) {
      if (pCompound.contains("Type", 8)) {
         this.setVariant(Boat.Type.byName(pCompound.getString("Type")));
      }

   }

   public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
      if (pPlayer.isSecondaryUseActive()) {
         return InteractionResult.PASS;
      } else if (this.outOfControlTicks < 60.0F) {
         if (!this.level().isClientSide) {
            return pPlayer.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
         } else {
            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_6_4)) {
         super.checkFallDamage(pY, pOnGround, pState, pPos);
         return;
      } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         this.status = Boat.Status.ON_LAND;
      }
      this.lastYd = this.getDeltaMovement().y;
      if (!this.isPassenger()) {
         if (pOnGround) {
            if (this.fallDistance > 3.0F) {
               if (this.status != Boat.Status.ON_LAND) {
                  this.resetFallDistance();
                  return;
               }

               this.causeFallDamage(this.fallDistance, 1.0F, this.damageSources().fall());
               if (!this.level().isClientSide && !this.isRemoved()) {
                  this.kill();
                  if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                     for(int i = 0; i < 3; ++i) {
                        this.spawnAtLocation(this.getVariant().getPlanks());
                     }

                     for(int j = 0; j < 2; ++j) {
                        this.spawnAtLocation(Items.STICK);
                     }
                  }
               }
            }

            this.resetFallDistance();
         } else if (!this.level().getFluidState(this.blockPosition().below()).is(FluidTags.WATER) && pY < 0.0D) {
            this.fallDistance -= (float)pY;
         }

      }
   }

   public boolean getPaddleState(int pSide) {
      return this.entityData.<Boolean>get(pSide == 0 ? DATA_ID_PADDLE_LEFT : DATA_ID_PADDLE_RIGHT) && this.getControllingPassenger() != null;
   }

   private void setBubbleTime(int pBubbleTime) {
      this.entityData.set(DATA_ID_BUBBLE_TIME, pBubbleTime);
   }

   private int getBubbleTime() {
      return this.entityData.get(DATA_ID_BUBBLE_TIME);
   }

   public float getBubbleAngle(float pPartialTicks) {
      return Mth.lerp(pPartialTicks, this.bubbleAngleO, this.bubbleAngle);
   }

   public void setVariant(Boat.Type pVariant) {
      this.entityData.set(DATA_ID_TYPE, pVariant.ordinal());
   }

   public Boat.Type getVariant() {
      return Boat.Type.byId(this.entityData.get(DATA_ID_TYPE));
   }

   protected boolean canAddPassenger(Entity pPassenger) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         return (super.canAddPassenger(pPassenger));
      }
      return this.getPassengers().size() < this.getMaxPassengers() && !this.isEyeInFluid(FluidTags.WATER);
   }

   protected int getMaxPassengers() {
      return 2;
   }

   @Nullable
   public LivingEntity getControllingPassenger() {
      Entity entity = this.getFirstPassenger();
      LivingEntity livingentity1;
      if (entity instanceof LivingEntity livingentity) {
         livingentity1 = livingentity;
      } else {
         livingentity1 = super.getControllingPassenger();
      }

      return livingentity1;
   }

   public void setInput(boolean pInputLeft, boolean pInputRight, boolean pInputUp, boolean pInputDown) {
      this.inputLeft = pInputLeft;
      this.inputRight = pInputRight;
      this.inputUp = pInputUp;
      this.inputDown = pInputDown;
   }

   protected Component getTypeName() {
      return Component.translatable(this.getDropItem().getDescriptionId());
   }

   public boolean isUnderWater() {
      return this.status == Boat.Status.UNDER_WATER || this.status == Boat.Status.UNDER_FLOWING_WATER;
   }

   public ItemStack getPickResult() {
      return new ItemStack(this.getDropItem());
   }

   public static enum Status {
      IN_WATER,
      UNDER_WATER,
      UNDER_FLOWING_WATER,
      ON_LAND,
      IN_AIR;
   }

   public static enum Type implements StringRepresentable {
      OAK(Blocks.OAK_PLANKS, "oak"),
      SPRUCE(Blocks.SPRUCE_PLANKS, "spruce"),
      BIRCH(Blocks.BIRCH_PLANKS, "birch"),
      JUNGLE(Blocks.JUNGLE_PLANKS, "jungle"),
      ACACIA(Blocks.ACACIA_PLANKS, "acacia"),
      CHERRY(Blocks.CHERRY_PLANKS, "cherry"),
      DARK_OAK(Blocks.DARK_OAK_PLANKS, "dark_oak"),
      MANGROVE(Blocks.MANGROVE_PLANKS, "mangrove"),
      BAMBOO(Blocks.BAMBOO_PLANKS, "bamboo");

      private final String name;
      private final Block planks;
      public static final StringRepresentable.EnumCodec<Boat.Type> CODEC = StringRepresentable.fromEnum(Boat.Type::values);
      private static final IntFunction<Boat.Type> BY_ID = ByIdMap.continuous(Enum::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

      private Type(Block pPlanks, String pName) {
         this.name = pName;
         this.planks = pPlanks;
      }

      public String getSerializedName() {
         return this.name;
      }

      public String getName() {
         return this.name;
      }

      public Block getPlanks() {
         return this.planks;
      }

      public String toString() {
         return this.name;
      }

      public static Boat.Type byId(int pId) {
         return BY_ID.apply(pId);
      }

      public static Boat.Type byName(String pName) {
         return CODEC.byName(pName, OAK);
      }
   }
}