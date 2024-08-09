package net.minecraft.network.protocol.handshake;

import net.minecraft.network.ConnectionProtocol;

public enum ClientIntent {
   STATUS,
   LOGIN;

   private static final int STATUS_ID = 1;
   private static final int LOGIN_ID = 2;

   public static ClientIntent byId(int pId) {
      ClientIntent clientintent;
      switch (pId) {
         case 1:
            clientintent = STATUS;
            break;
         case 2:
            clientintent = LOGIN;
            break;
         default:
            throw new IllegalArgumentException("Unknown connection intent: " + pId);
      }

      return clientintent;
   }

   public int id() {
      byte b0;
      switch (this) {
         case STATUS:
            b0 = 1;
            break;
         case LOGIN:
            b0 = 2;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return b0;
   }

   public ConnectionProtocol protocol() {
      ConnectionProtocol connectionprotocol;
      switch (this) {
         case STATUS:
            connectionprotocol = ConnectionProtocol.STATUS;
            break;
         case LOGIN:
            connectionprotocol = ConnectionProtocol.LOGIN;
            break;
         default:
            throw new IncompatibleClassChangeError();
      }

      return connectionprotocol;
   }
}