package com.viaversion.viaversion.libs.opennbt.conversion.converter;

import com.viaversion.viaversion.libs.opennbt.conversion.TagConverter;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteArrayTag;

/**
 * A converter that converts between ByteArrayTag and byte[].
 */
public class ByteArrayTagConverter implements TagConverter<ByteArrayTag, byte[]> {
    @Override
    public byte[] convert(ByteArrayTag tag) {
        return tag.getValue();
    }

    @Override
    public ByteArrayTag convert(byte[] value) {
        return new ByteArrayTag(value);
    }
}
