package com.viaversion.viaversion.libs.opennbt.conversion.converter;

import com.viaversion.viaversion.libs.opennbt.conversion.TagConverter;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.LongTag;

/**
 * A converter that converts between LongTag and long.
 */
public class LongTagConverter implements TagConverter<LongTag, Long> {
    @Override
    public Long convert(LongTag tag) {
        return tag.getValue();
    }

    @Override
    public LongTag convert(Long value) {
        return new LongTag(value);
    }
}
