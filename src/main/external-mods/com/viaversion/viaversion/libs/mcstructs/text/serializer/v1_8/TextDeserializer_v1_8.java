package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_8;

import com.google.gson.*;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.Style;
import com.viaversion.viaversion.libs.mcstructs.text.components.ScoreComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.SelectorComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.StringComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.TranslationComponent;

import java.lang.reflect.Type;

import static com.viaversion.viaversion.libs.mcstructs.text.utils.JsonUtils.getString;

public class TextDeserializer_v1_8 implements JsonDeserializer<ATextComponent> {

    @Override
    public ATextComponent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            return new StringComponent(json.getAsString());
        } else if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            ATextComponent component = null;

            for (JsonElement element : array) {
                ATextComponent serializedElement = this.deserialize(element, element.getClass(), context);
                if (component == null) component = serializedElement;
                else component.append(serializedElement);
            }

            return component;
        } else if (json.isJsonObject()) {
            JsonObject rawComponent = json.getAsJsonObject();
            ATextComponent component;

            if (rawComponent.has("text")) {
                component = new StringComponent(rawComponent.get("text").getAsString());
            } else if (rawComponent.has("translate")) {
                String translate = rawComponent.get("translate").getAsString();
                if (rawComponent.has("with")) {
                    JsonArray with = rawComponent.getAsJsonArray("with");
                    Object[] args = new Object[with.size()];
                    for (int i = 0; i < with.size(); i++) {
                        ATextComponent element = this.deserialize(with.get(i), typeOfT, context);
                        args[i] = element;
                        if (element instanceof StringComponent stringComponent) {
                            if (stringComponent.getStyle().isEmpty() && stringComponent.getSiblings().isEmpty()) args[i] = stringComponent.getText();
                        }
                    }
                    component = new TranslationComponent(translate, args);
                } else {
                    component = new TranslationComponent(translate);
                }
            } else if (rawComponent.has("score")) {
                JsonObject score = rawComponent.getAsJsonObject("score");
                if (!score.has("name") || !score.has("objective")) throw new JsonParseException("A score component needs at least a name and an objective");

                component = new ScoreComponent(getString(score, "name"), getString(score, "objective"));
                if (score.has("value")) ((ScoreComponent) component).setValue(getString(score, "value"));
            } else if (rawComponent.has("selector")) {
                component = new SelectorComponent(getString(rawComponent, "selector"), null);
            } else {
                throw new JsonParseException("Don't know how to turn " + json + " into a Component");
            }

            if (rawComponent.has("extra")) {
                JsonArray extra = rawComponent.getAsJsonArray("extra");
                if (extra.isEmpty()) throw new JsonParseException("Unexpected empty array of components");
                for (JsonElement element : extra) component.append(this.deserialize(element, typeOfT, context));
            }

            Style newStyle = context.deserialize(rawComponent, Style.class);
            if (newStyle != null) component.setStyle(newStyle);
            return component;
        } else {
            throw new JsonParseException("Don't know how to turn " + json + " into a Component");
        }
    }

}
