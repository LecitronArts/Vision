package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class ClientboundMapItemDataPacket implements Packet<ClientGamePacketListener> {
   private final int mapId;
   private final byte scale;
   private final boolean locked;
   @Nullable
   private final List<MapDecoration> decorations;
   @Nullable
   private final MapItemSavedData.MapPatch colorPatch;

   public ClientboundMapItemDataPacket(int pMapId, byte pScale, boolean pLocked, @Nullable Collection<MapDecoration> pDecorations, @Nullable MapItemSavedData.MapPatch pColorPatch) {
      this.mapId = pMapId;
      this.scale = pScale;
      this.locked = pLocked;
      this.decorations = pDecorations != null ? Lists.newArrayList(pDecorations) : null;
      this.colorPatch = pColorPatch;
   }

   public ClientboundMapItemDataPacket(FriendlyByteBuf pBuffer) {
      this.mapId = pBuffer.readVarInt();
      this.scale = pBuffer.readByte();
      this.locked = pBuffer.readBoolean();
      this.decorations = pBuffer.readNullable((p_237731_) -> {
         return p_237731_.readList((p_178981_) -> {
            MapDecoration.Type mapdecoration$type = p_178981_.readEnum(MapDecoration.Type.class);
            byte b0 = p_178981_.readByte();
            byte b1 = p_178981_.readByte();
            byte b2 = (byte)(p_178981_.readByte() & 15);
            Component component = p_178981_.readNullable(FriendlyByteBuf::readComponentTrusted);
            return new MapDecoration(mapdecoration$type, b0, b1, b2, component);
         });
      });
      int i = pBuffer.readUnsignedByte();
      if (i > 0) {
         int j = pBuffer.readUnsignedByte();
         int k = pBuffer.readUnsignedByte();
         int l = pBuffer.readUnsignedByte();
         byte[] abyte = pBuffer.readByteArray();
         this.colorPatch = new MapItemSavedData.MapPatch(k, l, i, j, abyte);
      } else {
         this.colorPatch = null;
      }

   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.mapId);
      pBuffer.writeByte(this.scale);
      pBuffer.writeBoolean(this.locked);
      pBuffer.writeNullable(this.decorations, (p_237728_, p_237729_) -> {
         p_237728_.writeCollection(p_237729_, (p_296401_, p_296402_) -> {
            p_296401_.writeEnum(p_296402_.type());
            p_296401_.writeByte(p_296402_.x());
            p_296401_.writeByte(p_296402_.y());
            p_296401_.writeByte(p_296402_.rot() & 15);
            p_296401_.writeNullable(p_296402_.name(), FriendlyByteBuf::writeComponent);
         });
      });
      if (this.colorPatch != null) {
         pBuffer.writeByte(this.colorPatch.width);
         pBuffer.writeByte(this.colorPatch.height);
         pBuffer.writeByte(this.colorPatch.startX);
         pBuffer.writeByte(this.colorPatch.startY);
         pBuffer.writeByteArray(this.colorPatch.mapColors);
      } else {
         pBuffer.writeByte(0);
      }

   }

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleMapItemData(this);
   }

   public int getMapId() {
      return this.mapId;
   }

   public void applyToMap(MapItemSavedData pMapdata) {
      if (this.decorations != null) {
         pMapdata.addClientSideDecorations(this.decorations);
      }

      if (this.colorPatch != null) {
         this.colorPatch.applyToMap(pMapdata);
      }

   }

   public byte getScale() {
      return this.scale;
   }

   public boolean isLocked() {
      return this.locked;
   }
}