package com.viaversion.viaversion.libs.mcstructs.snbt.impl.v1_7;

import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.mcstructs.snbt.ISNbtSerializer;
import com.viaversion.viaversion.libs.mcstructs.snbt.exceptions.SNbtSerializeException;

import java.util.Map;

public class SNbtSerializer_v1_7 implements ISNbtSerializer {

    @Override
    public String serialize(Tag tag) throws SNbtSerializeException {
        if (tag instanceof ByteTag byteTag) {
            return byteTag.getValue() + "b";
        } else if (tag instanceof ShortTag shortTag) {
            return shortTag.getValue() + "s";
        } else if (tag instanceof IntTag intTag) {
            return String.valueOf(intTag.getValue());
        } else if (tag instanceof LongTag longTag) {
            return longTag.getValue() + "L";
        } else if (tag instanceof FloatTag floatTag) {
            return floatTag.getValue() + "f";
        } else if (tag instanceof DoubleTag doubleTag) {
            return doubleTag.getValue() + "d";
        } else if (tag instanceof StringTag stringTag) {
            return "\"" + stringTag.getValue() + "\"";
        } else if (tag instanceof ListTag<?> listTag) {
            StringBuilder out = new StringBuilder("[");
            for (int i = 0; i < listTag.size(); i++) out.append(i).append(":").append(this.serialize(listTag.get(i))).append(",");
            return out.append("]").toString();
        } else if (tag instanceof CompoundTag compoundTag) {
            StringBuilder out = new StringBuilder("{");
            for (Map.Entry<String, Tag> entry : compoundTag.getValue().entrySet()) out.append(entry.getKey()).append(":").append(this.serialize(entry.getValue())).append(",");
            return out.append("}").toString();
        } else if (tag instanceof IntArrayTag intArrayTag) {
            StringBuilder out = new StringBuilder("[");
            for (int i : intArrayTag.getValue()) out.append(i).append(",");
            return out.append("]").toString();
        }
        throw new SNbtSerializeException(tag);
    }

}
