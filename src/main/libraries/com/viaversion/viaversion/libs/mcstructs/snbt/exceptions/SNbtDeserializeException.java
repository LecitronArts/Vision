package com.viaversion.viaversion.libs.mcstructs.snbt.exceptions;

/**
 * This exception is thrown when the SNbt deserializer encounters an invalid SNbt tag.
 */
public class SNbtDeserializeException extends Exception {

    private static String trim(final String rawTag, final int position) {
        StringBuilder out = new StringBuilder();
        int end = Math.min(rawTag.length(), position);
        if (end > 35) out.append("...");
        out.append(rawTag, Math.max(0, end - 35), end).append("<--[HERE]");
        return out.toString();
    }


    public SNbtDeserializeException(final String message) {
        super(message);
    }

    public SNbtDeserializeException(final String message, final String rawTag, final int position) {
        super(message + " at: " + trim(rawTag, position));
    }

}
