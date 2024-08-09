package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_7;

import com.google.gson.*;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.StringComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.TranslationComponent;

import java.lang.reflect.Type;
import java.util.Map;

public class TextSerializer_v1_7 implements JsonSerializer<ATextComponent> {

    @Override
    public JsonElement serialize(ATextComponent src, Type typeOfSrc, JsonSerializationContext context) {
        if (src instanceof StringComponent && src.getStyle().isEmpty() && src.getSiblings().isEmpty()) {
            return new JsonPrimitive(((StringComponent) src).getText());
        } else {
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
            } else {
                throw new JsonParseException("Don't know how to serialize " + src + " as a Component");
            }

            return serializedComponent;
        }
    }

}
