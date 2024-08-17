package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.util.PngInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Unique;

@OnlyIn(Dist.CLIENT)
public class ServerData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_ICON_SIZE = 1024;
   public String name;
   public String ip;
   public Component status;
   public Component motd;
   @Nullable
   public ServerStatus.Players players;
   public long ping;
   public int protocol = SharedConstants.getCurrentVersion().getProtocolVersion();
   public Component version = Component.literal(SharedConstants.getCurrentVersion().getName());
   public boolean pinged;
   public List<Component> playerList = Collections.emptyList();
   private ServerData.ServerPackStatus packStatus = ServerData.ServerPackStatus.PROMPT;
   @Nullable
   private byte[] iconBytes;
   private ServerData.Type type;
   private boolean enforcesSecureChat;
   private ProtocolVersion viaFabricPlus$forcedVersion = null;
   private boolean viaFabricPlus$passedDirectConnectScreen;
   private ProtocolVersion viaFabricPlus$translatingVersion;

   public ServerData(String pName, String pIp, ServerData.Type pType) {
      this.name = pName;
      this.ip = pIp;
      this.type = pType;
   }

   public CompoundTag write() {
      CompoundTag compoundtag = new CompoundTag();
      compoundtag.putString("name", this.name);
      compoundtag.putString("ip", this.ip);
      if (this.iconBytes != null) {
         compoundtag.putString("icon", Base64.getEncoder().encodeToString(this.iconBytes));
      }

      if (this.packStatus == ServerData.ServerPackStatus.ENABLED) {
         compoundtag.putBoolean("acceptTextures", true);
      } else if (this.packStatus == ServerData.ServerPackStatus.DISABLED) {
         compoundtag.putBoolean("acceptTextures", false);
      }

      if (viaFabricPlus$forcedVersion != null) {
         compoundtag.putString("viafabricplus_forcedversion", viaFabricPlus$forcedVersion.getName());
      }
      return compoundtag;
   }

   public ServerData.ServerPackStatus getResourcePackStatus() {
      return this.packStatus;
   }

   public void setResourcePackStatus(ServerData.ServerPackStatus pPackStatus) {
      this.packStatus = pPackStatus;
   }

   public static ServerData read(CompoundTag pNbtCompound) {
      ServerData serverdata = new ServerData(pNbtCompound.getString("name"), pNbtCompound.getString("ip"), ServerData.Type.OTHER);
      if (pNbtCompound.contains("icon", 8)) {
         try {
            byte[] abyte = Base64.getDecoder().decode(pNbtCompound.getString("icon"));
            serverdata.setIconBytes(validateIcon(abyte));
         } catch (IllegalArgumentException illegalargumentexception) {
            LOGGER.warn("Malformed base64 server icon", (Throwable)illegalargumentexception);
         }
      }

      if (pNbtCompound.contains("acceptTextures", 1)) {
         if (pNbtCompound.getBoolean("acceptTextures")) {
            serverdata.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
         } else {
            serverdata.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
         }
      } else {
         serverdata.setResourcePackStatus(ServerData.ServerPackStatus.PROMPT);
      }

      if (pNbtCompound.contains("viafabricplus_forcedversion")) {
         final ProtocolVersion version = ProtocolVersion.getClosest(pNbtCompound.getString("viafabricplus_forcedversion"));
         if (version != null) {
            ( serverdata).viaFabricPlus$forceVersion(version);
         }
      }
      return serverdata;
   }

   @Nullable
   public byte[] getIconBytes() {
      return this.iconBytes;
   }

   public void setIconBytes(@Nullable byte[] pIconBytes) {
      this.iconBytes = pIconBytes;
   }

   public boolean isLan() {
      return this.type == ServerData.Type.LAN;
   }

   public boolean isRealm() {
      return this.type == ServerData.Type.REALM;
   }

   public ServerData.Type type() {
      return this.type;
   }

   public void setEnforcesSecureChat(boolean pEnforcesSecureChat) {
      this.enforcesSecureChat = pEnforcesSecureChat;
   }

   public boolean enforcesSecureChat() {
      return this.enforcesSecureChat;
   }

   public void copyNameIconFrom(ServerData pOther) {
      this.ip = pOther.ip;
      this.name = pOther.name;
      this.iconBytes = pOther.iconBytes;
   }

   public void copyFrom(ServerData pServerData) {
      this.copyNameIconFrom(pServerData);
      this.setResourcePackStatus(pServerData.getResourcePackStatus());
      this.type = pServerData.type;
      this.enforcesSecureChat = pServerData.enforcesSecureChat;
      viaFabricPlus$forceVersion((pServerData).viaFabricPlus$forcedVersion());
   }

   @Nullable
   public static byte[] validateIcon(@Nullable byte[] pIcon) {
      if (pIcon != null) {
         try {
            PngInfo pnginfo = PngInfo.fromBytes(pIcon);
            if (pnginfo.width() <= 1024 && pnginfo.height() <= 1024) {
               return pIcon;
            }
         } catch (IOException ioexception) {
            LOGGER.warn("Failed to decode server icon", (Throwable)ioexception);
         }
      }

      return null;
   }

   @OnlyIn(Dist.CLIENT)
   public static enum ServerPackStatus {
      ENABLED("enabled"),
      DISABLED("disabled"),
      PROMPT("prompt");

      private final Component name;

      private ServerPackStatus(String pName) {
         this.name = Component.translatable("addServer.resourcePack." + pName);
      }

      public Component getName() {
         return this.name;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Type {
      LAN,
      REALM,
      OTHER;
   }


   public ProtocolVersion viaFabricPlus$forcedVersion() {
      return viaFabricPlus$forcedVersion;
   }


   public void viaFabricPlus$forceVersion(ProtocolVersion version) {
      viaFabricPlus$forcedVersion = version;
   }


   public boolean viaFabricPlus$passedDirectConnectScreen() {
      return viaFabricPlus$passedDirectConnectScreen;
   }


   public void viaFabricPlus$passDirectConnectScreen(boolean state) {
      viaFabricPlus$passedDirectConnectScreen = state;
   }


   public ProtocolVersion viaFabricPlus$translatingVersion() {
      return viaFabricPlus$translatingVersion;
   }


   public void viaFabricPlus$setTranslatingVersion(ProtocolVersion version) {
      viaFabricPlus$translatingVersion = version;
   }
}