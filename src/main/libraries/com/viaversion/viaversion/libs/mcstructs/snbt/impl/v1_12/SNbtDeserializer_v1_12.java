package com.viaversion.viaversion.libs.mcstructs.snbt.impl.v1_12;

import com.viaversion.viaversion.libs.mcstructs.snbt.ISNbtDeserializer;
import com.viaversion.viaversion.libs.mcstructs.snbt.exceptions.SNbtDeserializeException;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;

import java.util.regex.Pattern;

public class SNbtDeserializer_v1_12 implements ISNbtDeserializer<CompoundTag> {

    private static final Pattern BYTE_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORT_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)s", Pattern.CASE_INSENSITIVE);
    private static final Pattern INT_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)");
    private static final Pattern LONG_PATTERN = Pattern.compile("[-+]?(?:0|[1-9][0-9]*)l", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLOAT_PATTERN = Pattern.compile("[-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?f", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("[-+]?(?:[0-9]+[.]?|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?d", Pattern.CASE_INSENSITIVE);
    private static final Pattern SHORT_DOUBLE_PATTERN = Pattern.compile("[-+]?(?:[0-9]+[.]|[0-9]*[.][0-9]+)(?:e[-+]?[0-9]+)?", Pattern.CASE_INSENSITIVE);

    @Override
    public CompoundTag deserialize(String s) throws SNbtDeserializeException {
        StringReader_v1_12 reader = this.makeReader(s);
        CompoundTag compoundTag = this.readCompound(reader);
        reader.skipWhitespaces();
        if (reader.canRead()) throw this.makeException(reader, "Trailing data found");
        else return compoundTag;
    }

    @Override
    public Tag deserializeValue(String s) throws SNbtDeserializeException {
        return this.readValue(this.makeReader(s));
    }

    protected CompoundTag readCompound(final StringReader_v1_12 reader) throws SNbtDeserializeException {
        reader.jumpTo('{');
        CompoundTag compound = new CompoundTag();
        reader.skipWhitespaces();
        while (reader.canRead() && reader.peek() != '}') {
            String key = reader.readString();
            if (key == null) throw this.makeException(reader, "Expected key");
            if (key.isEmpty()) throw this.makeException(reader, "Expected non-empty key");
            reader.jumpTo(':');
            compound.put(key, this.readValue(reader));
            if (!this.hasNextValue(reader)) break;
            if (!reader.canRead()) throw this.makeException(reader, "Expected key");
        }
        reader.jumpTo('}');
        return compound;
    }

    protected Tag readListOrArray(final StringReader_v1_12 reader) throws SNbtDeserializeException {
        if (reader.canRead(2) && !this.isQuote(reader.charAt(1)) && reader.charAt(2) == ';') return this.readArray(reader);
        else return this.readList(reader);
    }

    protected ListTag<Tag> readList(final StringReader_v1_12 reader) throws SNbtDeserializeException {
        reader.jumpTo('[');
        reader.skipWhitespaces();
        if (!reader.canRead()) throw this.makeException(reader, "Expected value");
        ListTag<Tag> list = new ListTag<>();
        while (reader.peek() != ']') {
            Tag tag = this.readValue(reader);

            list.add(tag);
            if (!this.hasNextValue(reader)) break;
            if (!reader.canRead()) throw this.makeException(reader, "Expected value");
        }
        reader.jumpTo(']');
        return list;
    }

    protected <T extends NumberTag> ListTag<T> readPrimitiveList(final StringReader_v1_12 reader, final Class<T> primitiveType, final Class<? extends Tag> arrayType) throws SNbtDeserializeException {
        ListTag<T> list = new ListTag<>();
        while (true) {
            if (reader.peek() != ']') {
                Tag tag = this.readValue(reader);
                if (!primitiveType.isAssignableFrom(tag.getClass())) {
                    throw new SNbtDeserializeException("Unable to insert " + tag.getClass().getSimpleName() + " into " + arrayType.getSimpleName());
                }
                list.add((T) tag);
                if (this.hasNextValue(reader)) {
                    if (!reader.canRead()) throw this.makeException(reader, "Expected value");
                    continue;
                }
            }
            reader.jumpTo(']');
            return list;
        }
    }

    protected Tag readArray(final StringReader_v1_12 reader) throws SNbtDeserializeException {
        reader.jumpTo('[');
        char c = reader.read();
        reader.read();
        reader.skipWhitespaces();
        if (!reader.canRead()) throw this.makeException(reader, "Expected value");
        else if (c == 'B') {
            final ListTag<ByteTag> tags = this.readPrimitiveList(reader, ByteTag.class, ByteArrayTag.class);
            final byte[] array = new byte[tags.size()];
            for (int i = 0; i < tags.size(); i++) {
                array[i] = tags.get(i).asByte();
            }
            return new ByteArrayTag(array);
        } else if (c == 'L') {
            final ListTag<LongTag> tags = this.readPrimitiveList(reader, LongTag.class, LongArrayTag.class);
            final long[] array = new long[tags.size()];
            for (int i = 0; i < tags.size(); i++) {
                array[i] = tags.get(i).asLong();
            }
            return new LongArrayTag(array);
        } else if (c == 'I') {
            final ListTag<IntTag> tags = this.readPrimitiveList(reader, IntTag.class, IntArrayTag.class);
            final int[] array = new int[tags.size()];
            for (int i = 0; i < tags.size(); i++) {
                array[i] = tags.get(i).asInt();
            }
            return new IntArrayTag(array);
        } else throw new SNbtDeserializeException("Invalid array type '" + c + "' found");
    }

    protected Tag readValue(final StringReader_v1_12 reader) throws SNbtDeserializeException {
        reader.skipWhitespaces();
        if (!reader.canRead()) throw this.makeException(reader, "Expected value");
        char c = reader.peek();
        if (c == '{') return this.readCompound(reader);
        else if (c == '[') return this.readListOrArray(reader);
        else return this.readPrimitive(reader);
    }

    protected Tag readPrimitive(final StringReader_v1_12 reader) throws SNbtDeserializeException {
        reader.skipWhitespaces();
        if (this.isQuote(reader.peek())) return new StringTag(reader.readQuotedString());
        String value = reader.readUnquotedString();
        if (value.isEmpty()) throw this.makeException(reader, "Expected value");
        else return this.readPrimitive(value);
    }

    protected Tag readPrimitive(final String value) {
        try {
            if (FLOAT_PATTERN.matcher(value).matches()) return new FloatTag(Float.parseFloat(value.substring(0, value.length() - 1)));
            else if (BYTE_PATTERN.matcher(value).matches()) return new ByteTag(Byte.parseByte(value.substring(0, value.length() - 1)));
            else if (LONG_PATTERN.matcher(value).matches()) return new LongTag(Long.parseLong(value.substring(0, value.length() - 1)));
            else if (SHORT_PATTERN.matcher(value).matches()) return new ShortTag(Short.parseShort(value.substring(0, value.length() - 1)));
            else if (INT_PATTERN.matcher(value).matches()) return new IntTag(Integer.parseInt(value));
            else if (DOUBLE_PATTERN.matcher(value).matches()) return new DoubleTag(Double.parseDouble(value.substring(0, value.length() - 1)));
            else if (SHORT_DOUBLE_PATTERN.matcher(value).matches()) return new DoubleTag(Double.parseDouble(value));
            else if (value.equalsIgnoreCase("false")) return new ByteTag((byte) 0);
            else if (value.equalsIgnoreCase("true")) return new ByteTag((byte) 1);
        } catch (NumberFormatException ignored) {
        }
        return new StringTag(value);
    }

    protected boolean hasNextValue(final StringReader_v1_12 reader) {
        reader.skipWhitespaces();
        if (reader.canRead() && reader.peek() == ',') {
            reader.skip();
            reader.skipWhitespaces();
            return true;
        } else {
            return false;
        }
    }

    protected SNbtDeserializeException makeException(final StringReader_v1_12 reader, final String message) {
        return new SNbtDeserializeException(message, reader.getString(), reader.getIndex());
    }

    protected StringReader_v1_12 makeReader(final String string) {
        return new StringReader_v1_12(string);
    }

    protected boolean isQuote(final char c) {
        return c == '"';
    }

}
