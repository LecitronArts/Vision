package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.TickRateManager;

public record ClientboundTickingStepPacket(int tickSteps) implements Packet<ClientGamePacketListener> {
   public ClientboundTickingStepPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readVarInt());
   }

   public static ClientboundTickingStepPacket from(TickRateManager pTickRateManager) {
      return new ClientboundTickingStepPacket(pTickRateManager.frozenTicksToRun());
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.tickSteps);
   }

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleTickingStep(this);
   }
}