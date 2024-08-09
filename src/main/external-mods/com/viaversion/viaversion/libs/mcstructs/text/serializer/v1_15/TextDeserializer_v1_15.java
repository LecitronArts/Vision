package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_15;

import com.google.gson.*;
import com.viaversion.viaversion.libs.mcstructs.core.Identifier;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.Style;
import com.viaversion.viaversion.libs.mcstructs.text.components.*;
import com.viaversion.viaversion.libs.mcstructs.text.components.nbt.BlockNbtComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.nbt.EntityNbtComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.nbt.StorageNbtComponent;

import java.lang.reflect.Type;

import static com.viaversion.viaversion.libs.mcstructs.text.utils.JsonUtils.getBoolean;
import static com.viaversion.viaversion.libs.mcstructs.text.utils.JsonUtils.getString;

public class TextDeserializer_v1_15 implements JsonDeserializer<ATextComponent> {

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
                component = new StringComponent(getString(rawComponent, "text"));
            } else if (rawComponent.has("translate")) {
                String translate = getString(rawComponent, "translate");
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
            } else if (rawComponent.has("keybind")) {
                component = new KeybindComponent(getString(rawComponent, "keybind"));
            } else if (rawComponent.has("nbt")) {
                String nbt = getString(rawComponent, "nbt");
                boolean interpret = getBoolean(rawComponent, "interpret", false);
                if (rawComponent.has("block")) component = new BlockNbtComponent(nbt, interpret, null, getString(rawComponent, "block"));
                else if (rawComponent.has("entity")) component = new EntityNbtComponent(nbt, interpret, null, getString(rawComponent, "entity"));
                else if (rawComponent.has("storage")) component = new StorageNbtComponent(nbt, interpret, null, Identifier.of(getString(rawComponent, "storage")));
                else throw new JsonParseException("Don't know how to turn " + json + " into a Component");
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
