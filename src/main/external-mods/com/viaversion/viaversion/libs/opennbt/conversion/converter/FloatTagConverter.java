package com.viaversion.viaversion.libs.opennbt.conversion.converter;

import com.viaversion.viaversion.libs.opennbt.conversion.TagConverter;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.FloatTag;

/**
 * A converter that converts between FloatTag and float.
 */
public class FloatTagConverter implements TagConverter<FloatTag, Float> {
    @Override
    public Float convert(FloatTag tag) {
        return tag.getValue();
    }

    @Override
    public FloatTag convert(Float value) {
        return new FloatTag(value);
    }
}
