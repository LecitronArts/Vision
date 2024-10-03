package com.viaversion.viaversion.libs.mcstructs.snbt;

import com.viaversion.viaversion.libs.mcstructs.snbt.exceptions.SNbtDeserializeException;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;

/**
 * The base SNbt deserializer interface.
 *
 * @param <T> The output type of the deserializer
 */
public interface ISNbtDeserializer<T extends Tag> {

    T deserialize(final String s) throws SNbtDeserializeException;

    Tag deserializeValue(final String s) throws SNbtDeserializeException;

}
