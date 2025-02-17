package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_15;

import com.google.gson.*;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.*;
import com.viaversion.viaversion.libs.mcstructs.text.components.nbt.BlockNbtComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.nbt.EntityNbtComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.nbt.StorageNbtComponent;

import java.lang.reflect.Type;
import java.util.Map;

public class TextSerializer_v1_15 implements JsonSerializer<ATextComponent> {

    @Override
    public JsonElement serialize(ATextComponent src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject serializedComponent = new JsonObject();

        if (!src.getStyle().isEmpty()) {
            JsonElement serializedStyle = context.serialize(src.getStyle());
            if (serializedStyle.isJsonObject()) {
                JsonObject serializedStyleObject = serializedStyle.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : serializedStyleObject.entrySet()) serializedComponent.add(entry.getKey(), entry.getValue());
            }
        }
        if (!src.getSiblings().isEmpty()) {
            JsonArray siblings = new JsonArray();
            for (ATextComponent sibling : src.getSiblings()) siblings.add(this.serialize(sibling, sibling.getClass(), context));
            serializedComponent.add("extra", siblings);
        }

        if (src instanceof StringComponent) {
            serializedComponent.addProperty("text", ((StringComponent) src).getText());
        } else if (src instanceof TranslationComponent translationComponent) {
            serializedComponent.addProperty("translate", translationComponent.getKey());
            if (translationComponent.getArgs().length > 0) {
                JsonArray with = new JsonArray();
                Object[] args = translationComponent.getArgs();
                for (Object arg : args) {
                    if (arg instanceof ATextComponent) with.add(this.serialize((ATextComponent) arg, arg.getClass(), context));
                    else with.add(new JsonPrimitive(String.valueOf(arg)));
                }
                serializedComponent.add("with", with);
            }
        } else if (src instanceof ScoreComponent scoreComponent) {
            JsonObject serializedScore = new JsonObject();
            serializedScore.addProperty("name", scoreComponent.getName());
            serializedScore.addProperty("objective", scoreComponent.getObjective());
            serializedScore.addProperty("value", scoreComponent.getValue());
            serializedComponent.add("score", serializedScore);
        } else if (src instanceof SelectorComponent) {
            serializedComponent.addProperty("selector", ((SelectorComponent) src).getSelector());
        } else if (src instanceof KeybindComponent) {
            serializedComponent.addProperty("keybind", ((KeybindComponent) src).getKeybind());
        } else if (src instanceof NbtComponent nbtComponent) {
            serializedComponent.addProperty("nbt", nbtComponent.getComponent());
            serializedComponent.addProperty("interpret", nbtComponent.isResolve());
            if (nbtComponent instanceof BlockNbtComponent) serializedComponent.addProperty("block", ((BlockNbtComponent) nbtComponent).getPos());
            else if (nbtComponent instanceof EntityNbtComponent) serializedComponent.addProperty("entity", ((EntityNbtComponent) nbtComponent).getSelector());
            else if (nbtComponent instanceof StorageNbtComponent) serializedComponent.addProperty("storage", ((StorageNbtComponent) nbtComponent).getId().get());
            else throw new JsonParseException("Don't know how to serialize " + src + " as a Component");
        } else {
            throw new JsonParseException("Don't know how to serialize " + src + " as a Component");
        }

        return serializedComponent;
    }

}
