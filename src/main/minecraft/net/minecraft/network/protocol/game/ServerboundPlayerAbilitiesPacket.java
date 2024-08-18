package net.minecraft.network.protocol.game;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.player.Abilities;

public class ServerboundPlayerAbilitiesPacket implements Packet<ServerGamePacketListener> {
   private static final int FLAG_FLYING = 2;
   private final boolean isFlying;
   private Abilities viaFabricPlus$abilities;
   public ServerboundPlayerAbilitiesPacket(Abilities pAbilities) {
      this.isFlying = pAbilities.flying;
      this.viaFabricPlus$abilities =pAbilities;
   }

   public ServerboundPlayerAbilitiesPacket(FriendlyByteBuf pBuffer) {
      byte b0 = pBuffer.readByte();
      this.isFlying = (b0 & 2) != 0;
   }

   public void write(FriendlyByteBuf pBuffer) {
      byte b0 = 0;
      if (this.isFlying) {
         b0 = (byte)(b0 | 2);
      }
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
         if (viaFabricPlus$abilities.invulnerable) b0 |= 1;
         if (viaFabricPlus$abilities.mayfly) b0 |= 4;
         if (viaFabricPlus$abilities.instabuild) b0 |= 8;
      }

      pBuffer.writeByte(b0);
   }

   public void handle(ServerGamePacketListener pHandler) {
      pHandler.handlePlayerAbilities(this);
   }

   public boolean isFlying() {
      return this.isFlying;
   }
}