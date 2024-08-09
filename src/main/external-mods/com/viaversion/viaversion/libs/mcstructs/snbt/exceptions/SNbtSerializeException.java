package com.viaversion.viaversion.libs.mcstructs.snbt.exceptions;

import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;

/**
 * This exception is thrown when the SNbt serializer encounters an invalid Nbt tag.
 */
public class SNbtSerializeException extends Exception {

    public SNbtSerializeException(final Tag type) {
        super("Unable to serialize nbt type " + type.getClass().getSimpleName());
    }

}
