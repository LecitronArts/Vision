package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_16;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.viaversion.viaversion.libs.mcstructs.snbt.SNbtSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.AHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.EntityHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.ItemHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.impl.TextHoverEvent;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.TextComponentSerializer;

import java.lang.reflect.Type;

public class HoverEventSerializer_v1_16 implements JsonSerializer<AHoverEvent> {

    private final TextComponentSerializer textComponentSerializer;
    private final SNbtSerializer<?> sNbtSerializer;

    public HoverEventSerializer_v1_16(final TextComponentSerializer textComponentSerializer, final SNbtSerializer<?> sNbtSerializer) {
        this.textComponentSerializer = textComponentSerializer;
        this.sNbtSerializer = sNbtSerializer;
    }

    @Override
    public JsonElement serialize(AHoverEvent src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject serializedHoverEvent = new JsonObject();

        serializedHoverEvent.addProperty("action", src.getAction().getName());
        if (src instanceof TextHoverEvent textHoverEvent) {
            serializedHoverEvent.add("contents", this.textComponentSerializer.serializeJson(textHoverEvent.getText()));
        } else if (src instanceof ItemHoverEvent itemHoverEvent) {
            JsonObject serializedItem = new JsonObject();
            serializedItem.addProperty("id", itemHoverEvent.getItem().get());
            if (itemHoverEvent.getCount() != 1) serializedItem.addProperty("count", itemHoverEvent.getCount());
            if (itemHoverEvent.getNbt() != null) serializedItem.addProperty("tag", this.sNbtSerializer.trySerialize(itemHoverEvent.getNbt()));
            serializedHoverEvent.add("contents", serializedItem);
        } else if (src instanceof EntityHoverEvent entityHoverEvent) {
            JsonObject serializedEntity = new JsonObject();
            serializedEntity.addProperty("type", entityHoverEvent.getEntityType().get());
            serializedEntity.addProperty("id", entityHoverEvent.getUuid().toString());
            if (entityHoverEvent.getName() != null) serializedEntity.add("name", this.textComponentSerializer.serializeJson(entityHoverEvent.getName()));
            serializedHoverEvent.add("contents", serializedEntity);
        }

        return serializedHoverEvent;
    }

}
