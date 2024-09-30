package net.minecraft.client.player;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KeyboardInput extends Input {
   private final Options options;

   public KeyboardInput(Options pOptions) {
      this.options = pOptions;
   }

   private static float calculateImpulse(boolean pInput, boolean pOtherInput) {
      if (pInput == pOtherInput) {
         return 0.0F;
      } else {
         return pInput ? 1.0F : -1.0F;
      }
   }

   public void tick(boolean pIsSneaking, float pSneakingSpeedMultiplier) {
      //pIsSneaking = changeSneakSlowdownCondition(pIsSneaking);
      this.up = this.options.keyUp.isDown();
      this.down = this.options.keyDown.isDown();
      this.left = this.options.keyLeft.isDown();
      this.right = this.options.keyRight.isDown();
      this.forwardImpulse = calculateImpulse(this.up, this.down);
      this.leftImpulse = calculateImpulse(this.left, this.right);
      this.jumping = this.options.keyJump.isDown();
      this.shiftKeyDown = this.options.keyShift.isDown();
      if (changeSneakSlowdownCondition(pIsSneaking)) {
         this.leftImpulse *= pSneakingSpeedMultiplier;
         this.forwardImpulse *= pSneakingSpeedMultiplier;
      }

   }
   private boolean changeSneakSlowdownCondition(boolean slowDown) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)) {
         return this.shiftKeyDown;
      } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_4)) {
         return !Minecraft.getInstance().player.isSpectator() && (this.shiftKeyDown || slowDown);
      } else {
         return slowDown;
      }
   }
}