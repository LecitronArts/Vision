package net.minecraft.nbt;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ListTag extends CollectionTag<Tag> {
   private static final int SELF_SIZE_IN_BYTES = 37;
   public static final TagType<ListTag> TYPE = new TagType.VariableSize<ListTag>() {
      public ListTag load(DataInput p_128792_, NbtAccounter p_128794_) throws IOException {
         p_128794_.pushDepth();

         ListTag listtag;
         try {
            listtag = loadList(p_128792_, p_128794_);
         } finally {
            p_128794_.popDepth();
         }

         return listtag;
      }

      private static ListTag loadList(DataInput p_301758_, NbtAccounter p_301694_) throws IOException {
         p_301694_.accountBytes(37L);
         byte b0 = p_301758_.readByte();
         int i = p_301758_.readInt();
         if (b0 == 0 && i > 0) {
            throw new NbtFormatException("Missing type on ListTag");
         } else {
            p_301694_.accountBytes(4L, (long)i);
            TagType<?> tagtype = TagTypes.getType(b0);
            List<Tag> list = Lists.newArrayListWithCapacity(i);

            for(int j = 0; j < i; ++j) {
               list.add(tagtype.load(p_301758_, p_301694_));
            }

            return new ListTag(list, b0);
         }
      }

      public StreamTagVisitor.ValueResult parse(DataInput p_197491_, StreamTagVisitor p_197492_, NbtAccounter p_301731_) throws IOException {
         p_301731_.pushDepth();

         StreamTagVisitor.ValueResult streamtagvisitor$valueresult;
         try {
            streamtagvisitor$valueresult = parseList(p_197491_, p_197492_, p_301731_);
         } finally {
            p_301731_.popDepth();
         }

         return streamtagvisitor$valueresult;
      }

      private static StreamTagVisitor.ValueResult parseList(DataInput p_301745_, StreamTagVisitor p_301695_, NbtAccounter p_301734_) throws IOException {
         p_301734_.accountBytes(37L);
         TagType<?> tagtype = TagTypes.getType(p_301745_.readByte());
         int i = p_301745_.readInt();
         switch (p_301695_.visitList(tagtype, i)) {
            case HALT:
               return StreamTagVisitor.ValueResult.HALT;
            case BREAK:
               tagtype.skip(p_301745_, i, p_301734_);
               return p_301695_.visitContainerEnd();
            default:
               p_301734_.accountBytes(4L, (long)i);
               int j = 0;

               while(true) {
                  label45: {
                     if (j < i) {
                        switch (p_301695_.visitElement(tagtype, j)) {
                           case HALT:
                              return StreamTagVisitor.ValueResult.HALT;
                           case BREAK:
                              tagtype.skip(p_301745_, p_301734_);
                              break;
                           case SKIP:
                              tagtype.skip(p_301745_, p_301734_);
                              break label45;
                           default:
                              switch (tagtype.parse(p_301745_, p_301695_, p_301734_)) {
                                 case HALT:
                                    return StreamTagVisitor.ValueResult.HALT;
                                 case BREAK:
                                    break;
                                 default:
                                    break label45;
                              }
                        }
                     }

                     int k = i - 1 - j;
                     if (k > 0) {
                        tagtype.skip(p_301745_, k, p_301734_);
                     }

                     return p_301695_.visitContainerEnd();
                  }

                  ++j;
               }
         }
      }

      public void skip(DataInput p_301743_, NbtAccounter p_301728_) throws IOException {
         p_301728_.pushDepth();

         try {
            TagType<?> tagtype = TagTypes.getType(p_301743_.readByte());
            int i = p_301743_.readInt();
            tagtype.skip(p_301743_, i, p_301728_);
         } finally {
            p_301728_.popDepth();
         }

      }

      public String getName() {
         return "LIST";
      }

      public String getPrettyName() {
         return "TAG_List";
      }
   };
   private final List<Tag> list;
   private byte type;

   ListTag(List<Tag> pList, byte pType) {
      this.list = pList;
      this.type = pType;
   }

   public ListTag() {
      this(Lists.newArrayList(), (byte)0);
   }

   public void write(DataOutput pOutput) throws IOException {
      if (this.list.isEmpty()) {
         this.type = 0;
      } else {
         this.type = this.list.get(0).getId();
      }

      pOutput.writeByte(this.type);
      pOutput.writeInt(this.list.size());

      for(Tag tag : this.list) {
         tag.write(pOutput);
      }

   }

   public int sizeInBytes() {
      int i = 37;
      i += 4 * this.list.size();

      for(Tag tag : this.list) {
         i += tag.sizeInBytes();
      }

      return i;
   }

   public byte getId() {
      return 9;
   }

   public TagType<ListTag> getType() {
      return TYPE;
   }

   public String toString() {
      return this.getAsString();
   }

   private void updateTypeAfterRemove() {
      if (this.list.isEmpty()) {
         this.type = 0;
      }

   }

   public Tag remove(int p_128751_) {
      Tag tag = this.list.remove(p_128751_);
      this.updateTypeAfterRemove();
      return tag;
   }

   public boolean isEmpty() {
      return this.list.isEmpty();
   }

   public CompoundTag getCompound(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag tag = this.list.get(pIndex);
         if (tag.getId() == 10) {
            return (CompoundTag)tag;
         }
      }

      return new CompoundTag();
   }

   public ListTag getList(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag tag = this.list.get(pIndex);
         if (tag.getId() == 9) {
            return (ListTag)tag;
         }
      }

      return new ListTag();
   }

   public short getShort(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag tag = this.list.get(pIndex);
         if (tag.getId() == 2) {
            return ((ShortTag)tag).getAsShort();
         }
      }

      return 0;
   }

   public int getInt(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag tag = this.list.get(pIndex);
         if (tag.getId() == 3) {
            return ((IntTag)tag).getAsInt();
         }
      }

      return 0;
   }

   public int[] getIntArray(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag tag = this.list.get(pIndex);
         if (tag.getId() == 11) {
            return ((IntArrayTag)tag).getAsIntArray();
         }
      }

      return new int[0];
   }

   public long[] getLongArray(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag tag = this.list.get(pIndex);
         if (tag.getId() == 12) {
            return ((LongArrayTag)tag).getAsLongArray();
         }
      }

      return new long[0];
   }

   public double getDouble(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag tag = this.list.get(pIndex);
         if (tag.getId() == 6) {
            return ((DoubleTag)tag).getAsDouble();
         }
      }

      return 0.0D;
   }

   public float getFloat(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag tag = this.list.get(pIndex);
         if (tag.getId() == 5) {
            return ((FloatTag)tag).getAsFloat();
         }
      }

      return 0.0F;
   }

   public String getString(int pIndex) {
      if (pIndex >= 0 && pIndex < this.list.size()) {
         Tag tag = this.list.get(pIndex);
         return tag.getId() == 8 ? tag.getAsString() : tag.toString();
      } else {
         return "";
      }
   }

   public int size() {
      return this.list.size();
   }

   public Tag get(int pIndex) {
      return this.list.get(pIndex);
   }

   public Tag set(int p_128760_, Tag p_128761_) {
      Tag tag = this.get(p_128760_);
      if (!this.setTag(p_128760_, p_128761_)) {
         throw new UnsupportedOperationException(String.format(Locale.ROOT, "Trying to add tag of type %d to list of %d", p_128761_.getId(), this.type));
      } else {
         return tag;
      }
   }

   public void add(int p_128753_, Tag p_128754_) {
      if (!this.addTag(p_128753_, p_128754_)) {
         throw new UnsupportedOperationException(String.format(Locale.ROOT, "Trying to add tag of type %d to list of %d", p_128754_.getId(), this.type));
      }
   }

   public boolean setTag(int pIndex, Tag pNbt) {
      if (this.updateType(pNbt)) {
         this.list.set(pIndex, pNbt);
         return true;
      } else {
         return false;
      }
   }

   public boolean addTag(int pIndex, Tag pNbt) {
      if (this.updateType(pNbt)) {
         this.list.add(pIndex, pNbt);
         return true;
      } else {
         return false;
      }
   }

   private boolean updateType(Tag pTag) {
      if (pTag.getId() == 0) {
         return false;
      } else if (this.type == 0) {
         this.type = pTag.getId();
         return true;
      } else {
         return this.type == pTag.getId();
      }
   }

   public ListTag copy() {
      Iterable<Tag> iterable = (Iterable<Tag>)(TagTypes.getType(this.type).isValue() ? this.list : Iterables.transform(this.list, Tag::copy));
      List<Tag> list = Lists.newArrayList(iterable);
      return new ListTag(list, this.type);
   }

   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         return pOther instanceof ListTag && Objects.equals(this.list, ((ListTag)pOther).list);
      }
   }

   public int hashCode() {
      return this.list.hashCode();
   }

   public void accept(TagVisitor pVisitor) {
      pVisitor.visitList(this);
   }

   public byte getElementType() {
      return this.type;
   }

   public void clear() {
      this.list.clear();
      this.type = 0;
   }

   public StreamTagVisitor.ValueResult accept(StreamTagVisitor pVisitor) {
      switch (pVisitor.visitList(TagTypes.getType(this.type), this.list.size())) {
         case HALT:
            return StreamTagVisitor.ValueResult.HALT;
         case BREAK:
            return pVisitor.visitContainerEnd();
         default:
            int i = 0;

            while(i < this.list.size()) {
               Tag tag = this.list.get(i);
               switch (pVisitor.visitElement(tag.getType(), i)) {
                  case HALT:
                     return StreamTagVisitor.ValueResult.HALT;
                  case BREAK:
                     return pVisitor.visitContainerEnd();
                  default:
                     switch (tag.accept(pVisitor)) {
                        case HALT:
                           return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                           return pVisitor.visitContainerEnd();
                     }
                  case SKIP:
                     ++i;
               }
            }

            return pVisitor.visitContainerEnd();
      }
   }
}