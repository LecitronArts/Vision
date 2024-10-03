package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3.nbt;

import com.viaversion.viaversion.libs.mcstructs.core.Identifier;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.mcstructs.snbt.SNbtSerializer;
import com.viaversion.viaversion.libs.mcstructs.snbt.exceptions.SNbtSerializeException;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.AHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.HoverEventAction;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.EntityHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.ItemHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.TextHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.ITypedSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.TextComponentCodec;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3.CodecUtils_v1_20_3;

import java.util.List;
import java.util.UUID;

public class NbtHoverEventSerializer_v1_20_3 implements ITypedSerializer<Tag, AHoverEvent>, CodecUtils_v1_20_3 {

    private static final String ACTION = "action";
    private static final String CONTENTS = "contents";
    private static final String VALUE = "value";

    private final TextComponentCodec codec;
    private final ITypedSerializer<Tag, ATextComponent> textSerializer;
    private final SNbtSerializer<CompoundTag> sNbtSerializer;

    public NbtHoverEventSerializer_v1_20_3(final TextComponentCodec codec, final ITypedSerializer<Tag, ATextComponent> textSerializer, final SNbtSerializer<CompoundTag> sNbtSerializer) {
        this.codec = codec;
        this.textSerializer = textSerializer;
        this.sNbtSerializer = sNbtSerializer;
    }

    @Override
    public Tag serialize(AHoverEvent object) {
        CompoundTag out = new CompoundTag();
        out.putString(ACTION, object.getAction().getName());
        if (object instanceof TextHoverEvent textHoverEvent) {
            out.put("contents", this.textSerializer.serialize(textHoverEvent.getText()));
        } else if (object instanceof ItemHoverEvent itemHoverEvent) {
            CompoundTag contents = new CompoundTag();
            contents.putString("id", itemHoverEvent.getItem().get());
            if (itemHoverEvent.getCount() != 1) contents.putInt("count", itemHoverEvent.getCount());
            if (itemHoverEvent.getNbt() != null) {
                try {
                    contents.putString("tag", this.sNbtSerializer.serialize(itemHoverEvent.getNbt()));
                } catch (SNbtSerializeException e) {
                    throw new IllegalStateException("Failed to serialize nbt", e);
                }
            }
            out.put("contents", contents);
        } else if (object instanceof EntityHoverEvent entityHoverEvent) {
            CompoundTag contents = new CompoundTag();
            contents.putString("type", entityHoverEvent.getEntityType().get());
            contents.put("id", new IntArrayTag(new int[]{
                    (int) (entityHoverEvent.getUuid().getMostSignificantBits() >> 32),
                    (int) (entityHoverEvent.getUuid().getMostSignificantBits() & 0xFFFF_FFFFL),
                    (int) (entityHoverEvent.getUuid().getLeastSignificantBits() >> 32),
                    (int) (entityHoverEvent.getUuid().getLeastSignificantBits() & 0xFFFF_FFFFL)
            }));
            if (entityHoverEvent.getName() != null) contents.put("name", this.textSerializer.serialize(entityHoverEvent.getName()));
            out.put("contents", contents);
        } else {
            throw new IllegalArgumentException("Unknown hover event type: " + object.getClass().getName());
        }
        return out;
    }

    @Override
    public AHoverEvent deserialize(Tag object) {
        if (!(object instanceof CompoundTag tag)) throw new IllegalArgumentException("Nbt tag is not a compound tag");

        HoverEventAction action = HoverEventAction.getByName(requiredString(tag, ACTION), false);
        if (action == null) throw new IllegalArgumentException("Unknown hover event action: " + (tag.get("action") instanceof StringTag ? ((StringTag) tag.get("action")).getValue() : ""));
        if (!action.isUserDefinable()) throw new IllegalArgumentException("Hover event action is not user definable: " + action);

        if (tag.contains("contents")) {
            switch (action) {
                case SHOW_TEXT:
                    return new TextHoverEvent(action, this.textSerializer.deserialize(tag.get("contents")));
                case SHOW_ITEM:
                    //The item id does not have to be a valid item. Minecraft defaults to air if the item is invalid
                    if ((tag.get("contents") instanceof StringTag)) {
                        return new ItemHoverEvent(action, Identifier.of((tag.get("contents") instanceof StringTag ? ((StringTag) tag.get("contents")).getValue() : "")), 1, null);
                    } else if ((tag.get("contents") instanceof CompoundTag)) {
                        CompoundTag contents = (tag.get("contents") instanceof CompoundTag ? ((CompoundTag) tag.get("contents")) : new CompoundTag());
                        String id = requiredString(contents, "id");
                        Integer count = optionalInt(contents, "count");
                        String itemTag = optionalString(contents, "tag");
                        try {
                            return new ItemHoverEvent(
                                    action,
                                    Identifier.of(id),
                                    count == null ? 1 : count,
                                    itemTag == null ? null : this.sNbtSerializer.deserialize(itemTag)
                            );
                        } catch (Throwable t) {
                            this.sneak(t);
                        }
                    } else {
                        throw new IllegalArgumentException("Expected string or compound tag for '" + CONTENTS + "' tag");
                    }
                case SHOW_ENTITY:
                    CompoundTag contents = requiredCompound(tag, CONTENTS);
                    Identifier type = Identifier.of(requiredString(contents, "type"));
                    UUID id = this.getUUID(contents.get("id"));
                    ATextComponent name = contents.contains("name") ? this.textSerializer.deserialize(contents.get("name")) : null;
                    return new EntityHoverEvent(action, type, id, name);

                default:
                    throw new IllegalArgumentException("Unknown hover event action: " + action);
            }
        } else if (tag.contains(VALUE)) {
            ATextComponent value = this.textSerializer.deserialize(tag.get(VALUE));
            try {
                switch (action) {
                    case SHOW_TEXT:
                        return new TextHoverEvent(action, value);
                    case SHOW_ITEM:
                        CompoundTag parsed = this.sNbtSerializer.deserialize(value.asUnformattedString());
                        Identifier id = Identifier.of((parsed.get("id") instanceof StringTag ? ((StringTag) parsed.get("id")).getValue() : ""));
                        int count = (parsed.get("Count") instanceof ByteTag ? ((ByteTag) parsed.get("Count")).asByte() : 0);
                        CompoundTag itemTag = (parsed.get("tag") instanceof CompoundTag ? ((CompoundTag) parsed.get("tag")) : null);
                        return new ItemHoverEvent(action, id, count, itemTag);
                    case SHOW_ENTITY:
                        parsed = this.sNbtSerializer.deserialize(value.asUnformattedString());
                        ATextComponent name = this.codec.deserializeJson((parsed.get("name") instanceof StringTag ? ((StringTag) parsed.get("name")).getValue() : ""));
                        Identifier type = Identifier.of((parsed.get("type") instanceof StringTag ? ((StringTag) parsed.get("type")).getValue() : ""));
                        UUID uuid = UUID.fromString((parsed.get("id") instanceof StringTag ? ((StringTag) parsed.get("id")).getValue() : ""));
                        return new EntityHoverEvent(action, type, uuid, name);

                    default:
                        throw new IllegalArgumentException("Unknown hover event action: " + action);
                }
            } catch (Throwable t) {
                this.sneak(t);
            }
        }

        throw new IllegalArgumentException("Missing '" + CONTENTS + "' or '" + VALUE + "' tag");
    }

    protected <T extends Throwable> void sneak(final Throwable t) throws T {
        throw (T) t;
    }

    protected UUID getUUID(final Tag tag) {
        if (!(tag instanceof IntArrayTag) && !(tag instanceof ListTag) && !(tag instanceof StringTag)) {
            throw new IllegalArgumentException("Expected int array, list or string tag for 'id' tag");
        }
        int[] value;
        if (tag instanceof StringTag) {
            return UUID.fromString(((StringTag) tag).getValue());
        } else if (tag instanceof IntArrayTag) {
            value = ((IntArrayTag) tag).getValue();
            if (value.length != 4) throw new IllegalArgumentException("Expected int array with 4 values for 'id' tag");
        } else {
            ListTag<?> list = ((ListTag) tag);
            if (list.size() != 4) throw new IllegalArgumentException("Expected list with 4 values for 'id' tag");
            if (!list.getElementType().isAssignableFrom(com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag.class)) throw new IllegalArgumentException("Expected list with number values for 'id' tag");
            List<Tag> values = unwrapMarkers(list);
            value = new int[4];
            for (int i = 0; i < 4; i++) {
                value[i] = ((NumberTag) values.get(i)).asInt();
            }
        }
        return new UUID((long) value[0] << 32 | (long) value[1] & 0xFFFF_FFFFL, (long) value[2] << 32 | (long) value[3] & 0xFFFF_FFFFL);
    }

}
