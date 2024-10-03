package com.viaversion.viaversion.libs.mcstructs.snbt.impl.v1_12;

import com.viaversion.viaversion.libs.mcstructs.snbt.ISNbtSerializer;
import com.viaversion.viaversion.libs.mcstructs.snbt.exceptions.SNbtSerializeException;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;

import java.util.Map;
import java.util.regex.Pattern;

public class SNbtSerializer_v1_12 implements ISNbtSerializer {

    private static final Pattern ESCAPE_PATTERN = Pattern.compile("[A-Za-z0-9._+-]+");

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
        } else if (tag instanceof ByteArrayTag byteArrayTag) {
            StringBuilder out = new StringBuilder("[B;");
            for (int i = 0; i < byteArrayTag.length(); i++) {
                if (i != 0) out.append(",");
                out.append(byteArrayTag.getValue(i)).append("B");
            }
            return out.append("]").toString();
        } else if (tag instanceof StringTag stringTag) {
            return this.escape(stringTag.getValue());
        } else if (tag instanceof ListTag<?> listTag) {
            StringBuilder out = new StringBuilder("[");
            for (int i = 0; i < listTag.size(); i++) {
                if (i != 0) out.append(",");
                out.append(this.serialize(listTag.get(i)));
            }
            return out.append("]").toString();
        } else if (tag instanceof CompoundTag compoundTag) {
            StringBuilder out = new StringBuilder("{");
            for (Map.Entry<String, Tag> entry : compoundTag.getValue().entrySet()) {
                if (out.length() != 1) out.append(",");
                out.append(this.checkEscape(entry.getKey())).append(":").append(this.serialize(entry.getValue()));
            }
            return out.append("}").toString();
        } else if (tag instanceof IntArrayTag intArrayTag) {
            StringBuilder out = new StringBuilder("[I;");
            for (int i = 0; i < intArrayTag.length(); i++) {
                if (i != 0) out.append(",");
                out.append(intArrayTag.getValue(i));
            }
            return out.append("]").toString();
        } else if (tag instanceof LongArrayTag longArrayTag) {
            StringBuilder out = new StringBuilder("[L;");
            for (int i = 0; i < longArrayTag.length(); i++) {
                if (i != 0) out.append(",");
                out.append(longArrayTag.getValue(i)).append("L");
            }
            return out.append("]").toString();
        }
        throw new SNbtSerializeException(tag);
    }

    protected String checkEscape(final String s) {
        if (ESCAPE_PATTERN.matcher(s).matches()) return s;
        return this.escape(s);
    }

    protected String escape(final String s) {
        StringBuilder out = new StringBuilder("\"");
        char[] chars = s.toCharArray();
        for (char c : chars) {
            if (c == '\\' || c == '"') out.append("\\");
            out.append(c);
        }
        return out.append("\"").toString();
    }

}
