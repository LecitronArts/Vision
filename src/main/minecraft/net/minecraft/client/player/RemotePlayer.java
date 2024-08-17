package net.minecraft.client.player;

import com.mojang.authlib.GameProfile;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RemotePlayer extends AbstractClientPlayer {
   private Vec3 lerpDeltaMovement = Vec3.ZERO;
   private int lerpDeltaMovementSteps;

   public RemotePlayer(ClientLevel pClientLevel, GameProfile pGameProfile) {
      super(pClientLevel, pGameProfile);
      this.setMaxUpStep(1.0F);
      this.noPhysics = true;
   }

   public boolean shouldRenderAtSqrDistance(double pDistance) {
      double d0 = this.getBoundingBox().getSize() * 10.0D;
      if (Double.isNaN(d0)) {
         d0 = 1.0D;
      }

      d0 *= 64.0D * getViewScale();
      return pDistance < d0 * d0;
   }

   public boolean hurt(DamageSource pSource, float pAmount) {
      return true;
   }

   public void tick() {
      super.tick();
      this.calculateEntityAnimation(false);
   }

   public void aiStep() {
      if (this.lerpSteps > 0) {
         this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
         --this.lerpSteps;
      }

      if (this.lerpHeadSteps > 0) {
         this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
         --this.lerpHeadSteps;
      }

      if (this.lerpDeltaMovementSteps > 0) {
         this.addDeltaMovement(new Vec3((this.lerpDeltaMovement.x - this.getDeltaMovement().x) / (double)this.lerpDeltaMovementSteps, (this.lerpDeltaMovement.y - this.getDeltaMovement().y) / (double)this.lerpDeltaMovementSteps, (this.lerpDeltaMovement.z - this.getDeltaMovement().z) / (double)this.lerpDeltaMovementSteps));
         --this.lerpDeltaMovementSteps;
      }

      this.oBob = this.bob;
      this.updateSwingTime();
      float f;
      if (this.onGround() && !this.isDeadOrDying()) {
         f = (float)Math.min(0.1D, this.getDeltaMovement().horizontalDistance());
      } else {
         f = 0.0F;
      }

      this.bob += (f - this.bob) * 0.4F;
      this.level().getProfiler().push("push");
      this.pushEntities();
      this.level().getProfiler().pop();
   }

   public void lerpMotion(double pX, double pY, double pZ) {
      this.lerpDeltaMovement = new Vec3(pX, pY, pZ);
      this.lerpDeltaMovementSteps = this.getType().updateInterval() + 1;
   }

   protected void updatePlayerPose() {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)) {
         super.updatePlayerPose();
      }
   }

   public void sendSystemMessage(Component pComponent) {
      Minecraft minecraft = Minecraft.getInstance();
      minecraft.gui.getChat().addMessage(pComponent);
   }

   public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
      super.recreateFromPacket(pPacket);
      this.setOldPosAndRot();
   }
}