package com.viaversion.viaversion.libs.mcstructs.text.utils;

import com.google.gson.*;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@literal json <-> nbt} converter which is based on Minecraft's Codecs from 1.20.5.
 */
public class JsonNbtConverter {

    /**
     * Convert a {@link Tag} to a {@link JsonElement}.
     *
     * @param tag The tag to convert
     * @return The converted json element
     */
    @Nullable
    public static JsonElement toJson(@Nullable final Tag tag) {
        if (tag == null) return null;
        if (tag instanceof NumberTag) {
                return new JsonPrimitive(((NumberTag) tag).getValue());
        } else if (tag instanceof ByteArrayTag) {
                JsonArray byteArray = new JsonArray();
                for (byte b : ((ByteArrayTag) tag).getValue()) byteArray.add(b);
                return byteArray;
        } else if (tag instanceof StringTag) {
                return new JsonPrimitive(((StringTag) tag).getValue());
        } else if (tag instanceof ListTag<?>) {
                JsonArray list = new JsonArray();
                ListTag<Tag> listTag = ((ListTag) tag);
                for (Tag tagInList : listTag.getValue()) {
                    if (CompoundTag.class == listTag.getElementType()) {
                        CompoundTag compound = ((CompoundTag) tagInList);
                        if (compound.size() == 1) {
                            Tag wrappedTag = compound.get("");
                            if (wrappedTag != null) tagInList = wrappedTag;
                        }
                    }
                    list.add(toJson(tagInList));
                }
                return list;
        } else if (tag instanceof CompoundTag) {
                JsonObject compound = new JsonObject();
                for (Map.Entry<String, Tag> entry : ((CompoundTag) tag).getValue().entrySet()) compound.add(entry.getKey(), toJson(entry.getValue()));
                return compound;
        } else if (tag instanceof IntArrayTag) {
                JsonArray intArray = new JsonArray();
                for (int i : ((IntArrayTag) tag).getValue()) intArray.add(i);
                return intArray;
        } else if (tag instanceof LongArrayTag) {
                JsonArray longArray = new JsonArray();
                for (long l : ((LongArrayTag) tag).getValue()) longArray.add(l);
                return longArray;
        } else {
                throw new IllegalArgumentException("Unknown Nbt type: " + tag);
        }
    }

    /**
     * Convert a {@link JsonElement} to a {@link Tag}.
     *
     * @param element The element to convert
     * @return The converted nbt tag
     */
    @Nullable
    public static Tag toNbt(@Nullable final JsonElement element) {
        if (element == null) return null;
        if (element instanceof JsonObject) {
            JsonObject object = element.getAsJsonObject();
            CompoundTag compound = new CompoundTag();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) compound.put(entry.getKey(), toNbt(entry.getValue()));
            return compound;
        } else if (element instanceof JsonArray) {
            JsonArray array = element.getAsJsonArray();
            List<Tag> nbtTags = new ArrayList<>();
            Tag listType = null;
            boolean mixedList = false;
            for (JsonElement arrayElement : array) {
                Tag tag = toNbt(arrayElement);
                nbtTags.add(tag);
                listType = getListType(listType, tag);
                if (listType == null) mixedList = true;
            }
            if (listType == null) {
                return new ListTag<>();
            } else if (mixedList) { //Mixed list
                ListTag<CompoundTag> list = new ListTag<>();
                for (Tag tag : nbtTags) {
                    if (tag instanceof CompoundTag) list.add(((CompoundTag) tag));
                    else {
                        final CompoundTag entries = new CompoundTag();
                        entries.put("", tag);
                        list.add(entries);
                    }
                }
                return list;
            } else if (listType instanceof ByteTag) {
                byte[] bytes = new byte[nbtTags.size()];
                for (int i = 0; i < nbtTags.size(); i++) bytes[i] = ((NumberTag) nbtTags.get(i)).asByte();
                return new ByteArrayTag(bytes);
            } else if (listType instanceof IntTag) {
                int[] ints = new int[nbtTags.size()];
                for (int i = 0; i < nbtTags.size(); i++) ints[i] = ((NumberTag) nbtTags.get(i)).asInt();
                return new IntArrayTag(ints);
            } else if (listType instanceof LongTag) {
                long[] longs = new long[nbtTags.size()];
                for (int i = 0; i < nbtTags.size(); i++) longs[i] = ((NumberTag) nbtTags.get(i)).asLong();
                return new LongArrayTag(longs);
            } else {
                return new ListTag<>(nbtTags);
            }
        } else if (element instanceof JsonNull) {
            return null;
        } else if (element instanceof JsonPrimitive) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString()) {
                return new StringTag(primitive.getAsString());
            } else if (primitive.isBoolean()) {
                return new ByteTag(primitive.getAsBoolean());
            } else {
                BigDecimal number = primitive.getAsBigDecimal();
                try {
                    long l = number.longValueExact();
                    if ((byte) l == l) return new ByteTag((byte) l);
                    else if ((short) l == l) return new ShortTag((short) l);
                    else if ((int) l == l) return new IntTag((int) l);
                    else return new LongTag(l);
                } catch (ArithmeticException e) {
                    double d = number.doubleValue();
                    if ((float) d == d) return new FloatTag((float) d);
                    else return new DoubleTag(d);
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown JsonElement type: " + element.getClass().getName());
        }
    }

    private static Tag getListType(final Tag current, final Tag tag) {
        if (current == null) return tag;
        if (current != tag) return null; //Placeholder for mixed lists
        return current;
    }

}
