package com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl;

import com.viaversion.viaversion.libs.mcstructs.core.Identifier;
import com.viaversion.viaversion.libs.mcstructs.core.utils.ToString;
import com.viaversion.viaversion.libs.mcstructs.snbt.SNbtSerializer;
import com.viaversion.viaversion.libs.mcstructs.snbt.exceptions.SNbtSerializeException;
import com.viaversion.viaversion.libs.mcstructs.text.components.StringComponent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.AHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.HoverEventAction;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.TextComponentSerializer;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * The implementation for item hover events.
 */
public class ItemHoverEvent extends AHoverEvent {

    private Identifier item;
    private int count;
    @Nullable
    private CompoundTag nbt;

    public ItemHoverEvent(final Identifier item, final int count, final CompoundTag nbt) {
        this(HoverEventAction.SHOW_ITEM, item, count, nbt);
    }

    public ItemHoverEvent(final HoverEventAction action, final Identifier item, final int count, @Nullable final CompoundTag nbt) {
        super(action);

        this.item = item;
        this.count = count;
        this.nbt = nbt;
    }

    /**
     * @return The item of this hover event
     */
    public Identifier getItem() {
        return this.item;
    }

    /**
     * Set the item of this hover event.
     *
     * @param item The new item
     * @return This instance for chaining
     */
    public ItemHoverEvent setItem(final Identifier item) {
        this.item = item;
        return this;
    }

    /**
     * @return The count of this hover event
     */
    public int getCount() {
        return this.count;
    }

    /**
     * Set the count of this hover event.
     *
     * @param count The new count
     * @return This instance for chaining
     */
    public ItemHoverEvent setCount(final int count) {
        this.count = count;
        return this;
    }

    /**
     * @return The nbt of this hover event
     */
    @Nullable
    public CompoundTag getNbt() {
        return this.nbt;
    }

    /**
     * Set the nbt of this hover event.
     *
     * @param nbt The new nbt
     * @return This instance for chaining
     */
    public ItemHoverEvent setNbt(@Nullable final CompoundTag nbt) {
        this.nbt = nbt;
        return this;
    }

    @Override
    public TextHoverEvent toLegacy(TextComponentSerializer textComponentSerializer, SNbtSerializer<?> sNbtSerializer) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", this.item.getValue());
        tag.putByte("Count", (byte) this.count);
        if (this.nbt != null) tag.put("tag", this.nbt);
        try {
            return new TextHoverEvent(this.getAction(), new StringComponent(sNbtSerializer.serialize(tag)));
        } catch (SNbtSerializeException e) {
            throw new RuntimeException("This should never happen! Please report to the developer immediately!", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemHoverEvent that = (ItemHoverEvent) o;
        return this.count == that.count && this.getAction() == that.getAction() && Objects.equals(this.item, that.item) && Objects.equals(this.nbt, that.nbt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getAction(), this.item, this.count, this.nbt);
    }

    @Override
    public String toString() {
        return ToString.of(this)
                .add("action", this.action)
                .add("item", this.item)
                .add("count", this.count, count -> count != 1)
                .add("nbt", this.nbt, Objects::nonNull)
                .toString();
    }

}
