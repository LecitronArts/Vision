package com.viaversion.viaversion.libs.mcstructs.snbt;

import com.viaversion.viaversion.libs.mcstructs.snbt.exceptions.SNbtSerializeException;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;

/**
 * The base SNbt serializer interface.
 */
public interface ISNbtSerializer {

    String serialize(final Tag tag) throws SNbtSerializeException;

}
