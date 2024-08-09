package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_5.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.viaversion.viaversion.libs.mcstructs.core.Identifier;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.mcstructs.snbt.SNbtSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.AHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.HoverEventAction;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.EntityHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.ItemHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.TextHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.ITypedSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3.json.JsonHoverEventSerializer_v1_20_3;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_5.TextComponentCodec_v1_20_5;

import java.util.UUID;

public class JsonHoverEventSerializer_v1_20_5 extends JsonHoverEventSerializer_v1_20_3 {

    private static final String ACTION = "action";
    private static final String CONTENTS = "contents";
    private static final String VALUE = "value";

    private final TextComponentCodec_v1_20_5 codec;
    private final ITypedSerializer<JsonElement, ATextComponent> textSerializer;
    private final SNbtSerializer<CompoundTag> sNbtSerializer;

    public JsonHoverEventSerializer_v1_20_5(final TextComponentCodec_v1_20_5 codec, final ITypedSerializer<JsonElement, ATextComponent> textSerializer, final SNbtSerializer<CompoundTag> sNbtSerializer) {
        super(codec, textSerializer, sNbtSerializer);
        this.codec = codec;
        this.textSerializer = textSerializer;
        this.sNbtSerializer = sNbtSerializer;
    }

    @Override
    public JsonElement serialize(AHoverEvent object) {
        JsonObject out = new JsonObject();
        out.addProperty(ACTION, object.getAction().getName());
        if (object instanceof TextHoverEvent textHoverEvent) {
            out.add("contents", this.textSerializer.serialize(textHoverEvent.getText()));
        } else if (object instanceof ItemHoverEvent itemHoverEvent) {
            JsonObject contents = new JsonObject();
            contents.addProperty("id", itemHoverEvent.getItem().get());
            if (itemHoverEvent.getCount() != 1) contents.addProperty("count", itemHoverEvent.getCount());
            if (itemHoverEvent.getNbt() != null) contents.add("components", this.codec.convertItemComponents(itemHoverEvent.getNbt()));
            out.add("contents", contents);
        } else if (object instanceof EntityHoverEvent entityHoverEvent) {
            JsonObject contents = new JsonObject();
            contents.addProperty("type", entityHoverEvent.getEntityType().get());
            JsonArray id = new JsonArray();
            id.add((int) (entityHoverEvent.getUuid().getMostSignificantBits() >> 32));
            id.add((int) (entityHoverEvent.getUuid().getMostSignificantBits() & 0xFFFF_FFFFL));
            id.add((int) (entityHoverEvent.getUuid().getLeastSignificantBits() >> 32));
            id.add((int) (entityHoverEvent.getUuid().getLeastSignificantBits() & 0xFFFF_FFFFL));
            contents.add("id", id);
            if (entityHoverEvent.getName() != null) contents.add("name", this.textSerializer.serialize(entityHoverEvent.getName()));
            out.add("contents", contents);
        } else {
            throw new IllegalArgumentException("Unknown hover event type: " + object.getClass().getName());
        }
        return out;
    }

    @Override
    public AHoverEvent deserialize(JsonElement object) {
        if (!object.isJsonObject()) throw new IllegalArgumentException("Element must be a json object");
        JsonObject obj = object.getAsJsonObject();

        HoverEventAction action = HoverEventAction.getByName(requiredString(obj, ACTION), false);
        if (action == null) throw new IllegalArgumentException("Unknown hover event action: " + obj.get("action").getAsString());
        if (!action.isUserDefinable()) throw new IllegalArgumentException("Hover event action is not user definable: " + action);

        if (obj.has("contents")) {
            switch (action) {
                case SHOW_TEXT:
                    return new TextHoverEvent(action, this.textSerializer.deserialize(obj.get("contents")));
                case SHOW_ITEM:
                    //If the item is not valid or air an exception will be thrown
                    if (obj.has("contents") && isString(obj.get("contents"))) {
                        Identifier id = Identifier.of(obj.get("contents").getAsString());
                        this.verifyItem(id);
                        return new ItemHoverEvent(action, id, 1, null);
                    } else if (obj.has("contents") && isObject(obj.get("contents"))) {
                        JsonObject contents = obj.getAsJsonObject("contents");
                        Identifier id = Identifier.of(requiredString(contents, "id"));
                        this.verifyItem(id);
                        Integer count = optionalInt(contents, "count");
                        JsonObject components = optionalObject(contents, "components");
                        return new ItemHoverEvent(
                                action,
                                id,
                                count == null ? 1 : count,
                                components == null ? null : this.codec.convertItemComponents(components)
                        );
                    } else {
                        throw new IllegalArgumentException("Expected string or json array for '" + CONTENTS + "' tag");
                    }
                case SHOW_ENTITY:
                    //If the entity is not valid an exception will be thrown
                    JsonObject contents = requiredObject(obj, CONTENTS);
                    Identifier type = Identifier.of(requiredString(contents, "type"));
                    this.codec.verifyEntity(type);
                    UUID id = this.getUUID(contents.get("id"));
                    ATextComponent name;
                    if (contents.has("name")) {
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
        } else if (obj.has(VALUE)) {
            ATextComponent value = this.textSerializer.deserialize(obj.get(VALUE));
            try {
                switch (action) {
                    case SHOW_TEXT:
                        return new TextHoverEvent(action, value);
                    case SHOW_ITEM:
                        CompoundTag parsed = this.sNbtSerializer.deserialize(value.asUnformattedString());
                        Identifier id = Identifier.of(requiredString(parsed, "id"));
                        this.verifyItem(id);
                        Integer count = optionalInt(parsed, "count");
                        CompoundTag components = optionalCompound(parsed, "components");
                        if (components != null) this.codec.verifyItemComponents(components);
                        return new ItemHoverEvent(
                                action,
                                id,
                                count == null ? 1 : count,
                                components
                        );
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

    protected void verifyItem(final Identifier id) {
        this.codec.verifyItem(id);
        if (id.equals(Identifier.of("minecraft:air"))) throw new IllegalArgumentException("Item hover component id is 'minecraft:air'");
    }

}
