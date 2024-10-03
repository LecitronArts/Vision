package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_5.nbt;

import com.viaversion.viaversion.libs.mcstructs.core.Identifier;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import com.viaversion.viaversion.libs.mcstructs.snbt.SNbtSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.AHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.HoverEventAction;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.EntityHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.ItemHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.TextHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.ITypedSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3.nbt.NbtHoverEventSerializer_v1_20_3;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_5.TextComponentCodec_v1_20_5;

import java.util.UUID;

public class NbtHoverEventSerializer_v1_20_5 extends NbtHoverEventSerializer_v1_20_3 {

    private static final String ACTION = "action";
    private static final String CONTENTS = "contents";
    private static final String VALUE = "value";

    private final TextComponentCodec_v1_20_5 codec;
    private final ITypedSerializer<Tag, ATextComponent> textSerializer;
    private final SNbtSerializer<CompoundTag> sNbtSerializer;

    public NbtHoverEventSerializer_v1_20_5(final TextComponentCodec_v1_20_5 codec, final ITypedSerializer<Tag, ATextComponent> textSerializer, final SNbtSerializer<CompoundTag> sNbtSerializer) {
        super(codec, textSerializer, sNbtSerializer);
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
            if (itemHoverEvent.getNbt() != null) contents.put("components", itemHoverEvent.getNbt());
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
                    //If the item is not valid or air an exception will be thrown
                    if ((tag.get("contents") instanceof StringTag)) {
                        Identifier id = Identifier.of((tag.get("contents") instanceof StringTag ? ((StringTag) tag.get("contents")).getValue() : ""));
                        this.verifyItem(id);
                        return new ItemHoverEvent(action, id, 1, null);
                    } else if ((tag.get("contents") instanceof CompoundTag)) {
                        return this.parseItemHoverEvent(action, (tag.get("contents") instanceof CompoundTag ? ((CompoundTag) tag.get("contents")) : new CompoundTag()));
                    } else {
                        throw new IllegalArgumentException("Expected string or compound tag for '" + CONTENTS + "' tag");
                    }
                case SHOW_ENTITY:
                    //If the entity is not valid an exception will be thrown
                    CompoundTag contents = requiredCompound(tag, CONTENTS);
                    Identifier type = Identifier.of(requiredString(contents, "type"));
                    this.codec.verifyEntity(type);
                    UUID id = this.getUUID(contents.get("id"));
                    ATextComponent name;
                    if (contents.contains("name")) {
                        try {
                            name = this.textSerializer.deserialize(contents.get("name"));
                        } catch (Throwable t) {
                            name = null;
                        }
                    } else {
                        name = null;
                    }
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
                        return this.parseItemHoverEvent(action, this.sNbtSerializer.deserialize(value.asUnformattedString()));
                    case SHOW_ENTITY:
                        CompoundTag parsed = this.sNbtSerializer.deserialize(value.asUnformattedString());
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

    protected ItemHoverEvent parseItemHoverEvent(final HoverEventAction action, final CompoundTag tag) {
        Identifier id = Identifier.of(requiredString(tag, "id"));
        this.verifyItem(id);
        Integer count = optionalInt(tag, "count");
        CompoundTag components = optionalCompound(tag, "components");
        if (components != null) this.codec.verifyItemComponents(components);
        return new ItemHoverEvent(
                action,
                id,
                count == null ? 1 : count,
                components
        );
    }

    protected void verifyItem(final Identifier id) {
        this.codec.verifyItem(id);
        if (id.equals(Identifier.of("minecraft:air"))) throw new IllegalArgumentException("Item hover component id is 'minecraft:air'");
    }

}
