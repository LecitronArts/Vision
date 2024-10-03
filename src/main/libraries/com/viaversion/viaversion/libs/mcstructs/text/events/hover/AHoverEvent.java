package com.viaversion.viaversion.libs.mcstructs.text.events.hover;

import com.viaversion.viaversion.libs.mcstructs.snbt.SNbtSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.TextHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.TextComponentSerializer;

/**
 * The abstract class for hover events.<br>
 * Until minecraft 1.16 hover events always used the {@link com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.TextHoverEvent} implementation.
 */
public abstract class AHoverEvent {

    protected HoverEventAction action;

    public AHoverEvent(final HoverEventAction action) {
        this.action = action;
    }

    /**
     * @return The action of this hover event
     */
    public HoverEventAction getAction() {
        return this.action;
    }

    /**
     * Converts this hover event to a legacy text hover event.
     *
     * @param textComponentSerializer The serializer to use for text components
     * @param sNbtSerializer          The serializer to use for nbt tags
     * @return The converted hover event
     */
    public abstract TextHoverEvent toLegacy(final TextComponentSerializer textComponentSerializer, final SNbtSerializer<?> sNbtSerializer);

    @Override
    public abstract boolean equals(final Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

}
