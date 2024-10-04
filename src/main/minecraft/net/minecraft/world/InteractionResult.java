package net.minecraft.world;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;

public enum InteractionResult {
   SUCCESS,
   CONSUME,
   CONSUME_PARTIAL,
   PASS,
   FAIL;

   public boolean consumesAction() {
      return this == SUCCESS || this == CONSUME || this == CONSUME_PARTIAL;
   }

   public boolean shouldSwing() {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_4)) {
         return (this.consumesAction());
      } else {
         return this == SUCCESS;
      }
   }

   public boolean shouldAwardStats() {
      return this == SUCCESS || this == CONSUME;
   }

   public static InteractionResult sidedSuccess(boolean pIsClientSide) {
      return pIsClientSide ? SUCCESS : CONSUME;
   }
}