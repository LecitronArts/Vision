package net.minecraft.util;

import java.io.DataOutput;
import java.io.IOException;

public class DelegateDataOutput implements DataOutput {
   private final DataOutput parent;

   public DelegateDataOutput(DataOutput pParent) {
      this.parent = pParent;
   }

   public void write(int pValue) throws IOException {
      this.parent.write(pValue);
   }

   public void write(byte[] pData) throws IOException {
      this.parent.write(pData);
   }

   public void write(byte[] pData, int pOffset, int pLength) throws IOException {
      this.parent.write(pData, pOffset, pLength);
   }

   public void writeBoolean(boolean pValue) throws IOException {
      this.parent.writeBoolean(pValue);
   }

   public void writeByte(int pValue) throws IOException {
      this.parent.writeByte(pValue);
   }

   public void writeShort(int pValue) throws IOException {
      this.parent.writeShort(pValue);
   }

   public void writeChar(int pValue) throws IOException {
      this.parent.writeChar(pValue);
   }

   public void writeInt(int pValue) throws IOException {
      this.parent.writeInt(pValue);
   }

   public void writeLong(long pValue) throws IOException {
      this.parent.writeLong(pValue);
   }

   public void writeFloat(float pValue) throws IOException {
      this.parent.writeFloat(pValue);
   }

   public void writeDouble(double pValue) throws IOException {
      this.parent.writeDouble(pValue);
   }

   public void writeBytes(String pValue) throws IOException {
      this.parent.writeBytes(pValue);
   }

   public void writeChars(String pValue) throws IOException {
      this.parent.writeChars(pValue);
   }

   public void writeUTF(String pValue) throws IOException {
      this.parent.writeUTF(pValue);
   }
}