package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public class ServerboundSeenAdvancementsPacket implements Packet<ServerGamePacketListener> {
   private final ServerboundSeenAdvancementsPacket.Action action;
   @Nullable
   private final ResourceLocation tab;

   public ServerboundSeenAdvancementsPacket(ServerboundSeenAdvancementsPacket.Action pAction, @Nullable ResourceLocation pTab) {
      this.action = pAction;
      this.tab = pTab;
   }

   public static ServerboundSeenAdvancementsPacket openedTab(AdvancementHolder pAdvancement) {
      return new ServerboundSeenAdvancementsPacket(ServerboundSeenAdvancementsPacket.Action.OPENED_TAB, pAdvancement.id());
   }

   public static ServerboundSeenAdvancementsPacket closedScreen() {
      return new ServerboundSeenAdvancementsPacket(ServerboundSeenAdvancementsPacket.Action.CLOSED_SCREEN, (ResourceLocation)null);
   }

   public ServerboundSeenAdvancementsPacket(FriendlyByteBuf pBuffer) {
      this.action = pBuffer.readEnum(ServerboundSeenAdvancementsPacket.Action.class);
      if (this.action == ServerboundSeenAdvancementsPacket.Action.OPENED_TAB) {
         this.tab = pBuffer.readResourceLocation();
      } else {
         this.tab = null;
      }

   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeEnum(this.action);
      if (this.action == ServerboundSeenAdvancementsPacket.Action.OPENED_TAB) {
         pBuffer.writeResourceLocation(this.tab);
      }

   }

   public void handle(ServerGamePacketListener pHandler) {
      pHandler.handleSeenAdvancements(this);
   }

   public ServerboundSeenAdvancementsPacket.Action getAction() {
      return this.action;
   }

   @Nullable
   public ResourceLocation getTab() {
      return this.tab;
   }

   public static enum Action {
      OPENED_TAB,
      CLOSED_SCREEN;
   }
}