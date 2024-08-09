package net.minecraft.network;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

public interface PacketListener {
   PacketFlow flow();

   ConnectionProtocol protocol();

   void onDisconnect(Component pReason);

   boolean isAcceptingMessages();

   default boolean shouldHandleMessage(Packet<?> pPacket) {
      return this.isAcceptingMessages();
   }

   default boolean shouldPropagateHandlingExceptions() {
      return true;
   }

   default void fillCrashReport(CrashReport pCrashReport) {
      CrashReportCategory crashreportcategory = pCrashReport.addCategory("Connection");
      crashreportcategory.setDetail("Protocol", () -> {
         return this.protocol().id();
      });
      crashreportcategory.setDetail("Flow", () -> {
         return this.flow().toString();
      });
      this.fillListenerSpecificCrashDetails(crashreportcategory);
   }

   default void fillListenerSpecificCrashDetails(CrashReportCategory pCrashReportCategory) {
   }
}