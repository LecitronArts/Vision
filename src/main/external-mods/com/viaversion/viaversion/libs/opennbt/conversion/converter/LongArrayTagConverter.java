package com.viaversion.viaversion.libs.opennbt.conversion.converter;

import com.viaversion.viaversion.libs.opennbt.conversion.TagConverter;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.LongArrayTag;

/**
 * A converter that converts between LongArrayTag and long[].
 */
public class LongArrayTagConverter implements TagConverter<LongArrayTag, long[]> {
    @Override
    public long[] convert(LongArrayTag tag) {
        return tag.getValue();
    }

    @Override
    public LongArrayTag convert(long[] value) {
        return new LongArrayTag(value);
    }
}
