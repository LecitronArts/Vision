package com.viaversion.viaversion.libs.opennbt.conversion.converter;

import com.viaversion.viaversion.libs.opennbt.conversion.TagConverter;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;

/**
 * A converter that converts between StringTag and String.
 */
public class StringTagConverter implements TagConverter<StringTag, String> {
    @Override
    public String convert(StringTag tag) {
        return tag.getValue();
    }

    @Override
    public StringTag convert(String value) {
        return new StringTag(value);
    }
}