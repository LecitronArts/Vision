package net.minecraft.world.entity.ai.control;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MoveControl implements Control {
   public static final float MIN_SPEED = 5.0E-4F;
   public static final float MIN_SPEED_SQR = 2.5000003E-7F;
   protected static final int MAX_TURN = 90;
   protected final Mob mob;
   protected double wantedX;
   protected double wantedY;
   protected double wantedZ;
   protected double speedModifier;
   protected float strafeForwards;
   protected float strafeRight;
   protected MoveControl.Operation operation = MoveControl.Operation.WAIT;

   public MoveControl(Mob pMob) {
      this.mob = pMob;
   }

   public boolean hasWanted() {
      return this.operation == MoveControl.Operation.MOVE_TO;
   }

   public double getSpeedModifier() {
      return this.speedModifier;
   }

   public void setWantedPosition(double pX, double pY, double pZ, double pSpeed) {
      this.wantedX = pX;
      this.wantedY = pY;
      this.wantedZ = pZ;
      this.speedModifier = pSpeed;
      if (this.operation != MoveControl.Operation.JUMPING) {
         this.operation = MoveControl.Operation.MOVE_TO;
      }

   }

   public void strafe(float pForward, float pStrafe) {
      this.operation = MoveControl.Operation.STRAFE;
      this.strafeForwards = pForward;
      this.strafeRight = pStrafe;
      this.speedModifier = 0.25D;
   }

   public void tick() {
      if (this.operation == MoveControl.Operation.STRAFE) {
         float f = (float)this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
         float f1 = (float)this.speedModifier * f;
         float f2 = this.strafeForwards;
         float f3 = this.strafeRight;
         float f4 = Mth.sqrt(f2 * f2 + f3 * f3);
         if (f4 < 1.0F) {
            f4 = 1.0F;
         }

         f4 = f1 / f4;
         f2 *= f4;
         f3 *= f4;
         float f5 = Mth.sin(this.mob.getYRot() * ((float)Math.PI / 180F));
         float f6 = Mth.cos(this.mob.getYRot() * ((float)Math.PI / 180F));
         float f7 = f2 * f6 - f3 * f5;
         float f8 = f3 * f6 + f2 * f5;
         if (!this.isWalkable(f7, f8)) {
            this.strafeForwards = 1.0F;
            this.strafeRight = 0.0F;
         }

         this.mob.setSpeed(f1);
         this.mob.setZza(this.strafeForwards);
         this.mob.setXxa(this.strafeRight);
         this.operation = MoveControl.Operation.WAIT;
      } else if (this.operation == MoveControl.Operation.MOVE_TO) {
         this.operation = MoveControl.Operation.WAIT;
         double d0 = this.wantedX - this.mob.getX();
         double d1 = this.wantedZ - this.mob.getZ();
         double d2 = this.wantedY - this.mob.getY();
         double d3 = d0 * d0 + d2 * d2 + d1 * d1;
         if (d3 < (double)2.5000003E-7F) {
            this.mob.setZza(0.0F);
            return;
         }

         float f9 = (float)(Mth.atan2(d1, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
         this.mob.setYRot(this.rotlerp(this.mob.getYRot(), f9, 90.0F));
         this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
         BlockPos blockpos = this.mob.blockPosition();
         BlockState blockstate = this.mob.level().getBlockState(blockpos);
         VoxelShape voxelshape = blockstate.getCollisionShape(this.mob.level(), blockpos);
         if (d2 > (double)this.mob.maxUpStep() && d0 * d0 + d1 * d1 < (double)Math.max(1.0F, this.mob.getBbWidth()) || !voxelshape.isEmpty() && this.mob.getY() < voxelshape.max(Direction.Axis.Y) + (double)blockpos.getY() && !blockstate.is(BlockTags.DOORS) && !blockstate.is(BlockTags.FENCES)) {
            this.mob.getJumpControl().jump();
            this.operation = MoveControl.Operation.JUMPING;
         }
      } else if (this.operation == MoveControl.Operation.JUMPING) {
         this.mob.setSpeed((float)(this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
         if (this.mob.onGround()) {
            this.operation = MoveControl.Operation.WAIT;
         }
      } else {
         this.mob.setZza(0.0F);
      }

   }

   private boolean isWalkable(float pRelativeX, float pRelativeZ) {
      PathNavigation pathnavigation = this.mob.getNavigation();
      if (pathnavigation != null) {
         NodeEvaluator nodeevaluator = pathnavigation.getNodeEvaluator();
         if (nodeevaluator != null && nodeevaluator.getBlockPathType(this.mob.level(), Mth.floor(this.mob.getX() + (double)pRelativeX), this.mob.getBlockY(), Mth.floor(this.mob.getZ() + (double)pRelativeZ)) != BlockPathTypes.WALKABLE) {
            return false;
         }
      }

      return true;
   }

   protected float rotlerp(float pSourceAngle, float pTargetAngle, float pMaximumChange) {
      float f = Mth.wrapDegrees(pTargetAngle - pSourceAngle);
      if (f > pMaximumChange) {
         f = pMaximumChange;
      }

      if (f < -pMaximumChange) {
         f = -pMaximumChange;
      }

      float f1 = pSourceAngle + f;
      if (f1 < 0.0F) {
         f1 += 360.0F;
      } else if (f1 > 360.0F) {
         f1 -= 360.0F;
      }

      return f1;
   }

   public double getWantedX() {
      return this.wantedX;
   }

   public double getWantedY() {
      return this.wantedY;
   }

   public double getWantedZ() {
      return this.wantedZ;
   }

   protected static enum Operation {
      WAIT,
      MOVE_TO,
      STRAFE,
      JUMPING;
   }
}