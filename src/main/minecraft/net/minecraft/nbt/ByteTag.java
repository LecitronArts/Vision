package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ByteTag extends NumericTag {
   private static final int SELF_SIZE_IN_BYTES = 9;
   public static final TagType<ByteTag> TYPE = new TagType.StaticSize<ByteTag>() {
      public ByteTag load(DataInput p_128297_, NbtAccounter p_128299_) throws IOException {
         return ByteTag.valueOf(readAccounted(p_128297_, p_128299_));
      }

      public StreamTagVisitor.ValueResult parse(DataInput p_197438_, StreamTagVisitor p_197439_, NbtAccounter p_301726_) throws IOException {
         return p_197439_.visit(readAccounted(p_197438_, p_301726_));
      }

      private static byte readAccounted(DataInput p_301730_, NbtAccounter p_301751_) throws IOException {
         p_301751_.accountBytes(9L);
         return p_301730_.readByte();
      }

      public int size() {
         return 1;
      }

      public String getName() {
         return "BYTE";
      }

      public String getPrettyName() {
         return "TAG_Byte";
      }

      public boolean isValue() {
         return true;
      }
   };
   public static final ByteTag ZERO = valueOf((byte)0);
   public static final ByteTag ONE = valueOf((byte)1);
   private final byte data;

   ByteTag(byte pData) {
      this.data = pData;
   }

   public static ByteTag valueOf(byte pData) {
      return ByteTag.Cache.cache[128 + pData];
   }

   public static ByteTag valueOf(boolean pData) {
      return pData ? ONE : ZERO;
   }

   public void write(DataOutput pOutput) throws IOException {
      pOutput.writeByte(this.data);
   }

   public int sizeInBytes() {
      return 9;
   }

   public byte getId() {
      return 1;
   }

   public TagType<ByteTag> getType() {
      return TYPE;
   }

   public ByteTag copy() {
      return this;
   }

   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         return pOther instanceof ByteTag && this.data == ((ByteTag)pOther).data;
      }
   }

   public int hashCode() {
      return this.data;
   }

   public void accept(TagVisitor pVisitor) {
      pVisitor.visitByte(this);
   }

   public long getAsLong() {
      return (long)this.data;
   }

   public int getAsInt() {
      return this.data;
   }

   public short getAsShort() {
      return (short)this.data;
   }

   public byte getAsByte() {
      return this.data;
   }

   public double getAsDouble() {
      return (double)this.data;
   }

   public float getAsFloat() {
      return (float)this.data;
   }

   public Number getAsNumber() {
      return this.data;
   }

   public StreamTagVisitor.ValueResult accept(StreamTagVisitor pVisitor) {
      return pVisitor.visit(this.data);
   }

   static class Cache {
      static final ByteTag[] cache = new ByteTag[256];

      private Cache() {
      }

      static {
         for(int i = 0; i < cache.length; ++i) {
            cache[i] = new ByteTag((byte)(i - 128));
         }

      }
   }
}