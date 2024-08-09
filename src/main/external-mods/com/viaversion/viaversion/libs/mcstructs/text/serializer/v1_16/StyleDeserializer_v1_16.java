package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_16;

import com.google.gson.*;
import com.viaversion.viaversion.libs.mcstructs.core.Identifier;
import com.viaversion.viaversion.libs.mcstructs.core.TextFormatting;
import com.viaversion.viaversion.libs.mcstructs.text.Style;
import com.viaversion.viaversion.libs.mcstructs.text.events.click.ClickEvent;
import com.viaversion.viaversion.libs.mcstructs.text.events.click.ClickEventAction;
import com.viaversion.viaversion.libs.mcstructs.text.events.hover.AHoverEvent;

import java.lang.reflect.Type;

import static com.viaversion.viaversion.libs.mcstructs.text.utils.JsonUtils.getJsonObject;
import static com.viaversion.viaversion.libs.mcstructs.text.utils.JsonUtils.getString;

public class StyleDeserializer_v1_16 implements JsonDeserializer<Style> {

    @Override
    public Style deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject()) return null;
        JsonObject rawStyle = json.getAsJsonObject();
        if (rawStyle == null) return null;
        Style style = new Style();

        if (rawStyle.has("bold")) style.setBold(rawStyle.get("bold").getAsBoolean());
        if (rawStyle.has("italic")) style.setItalic(rawStyle.get("italic").getAsBoolean());
        if (rawStyle.has("underlined")) style.setUnderlined(rawStyle.get("underlined").getAsBoolean());
        if (rawStyle.has("strikethrough")) style.setStrikethrough(rawStyle.get("strikethrough").getAsBoolean());
        if (rawStyle.has("obfuscated")) style.setObfuscated(rawStyle.get("obfuscated").getAsBoolean());
        if (rawStyle.has("color")) style.setFormatting(TextFormatting.parse(getString(rawStyle, "color")));
        if (rawStyle.has("insertion")) style.setInsertion(getString(rawStyle, "insertion", null));
        if (rawStyle.has("clickEvent")) {
            JsonObject rawClickEvent = getJsonObject(rawStyle, "clickEvent");
            String rawAction = getString(rawClickEvent, "action");

            ClickEventAction action = null;
            String value = getString(rawClickEvent, "value");
            if (rawAction != null) action = ClickEventAction.getByName(rawAction);

            if (action != null && value != null && action.isUserDefinable()) style.setClickEvent(new ClickEvent(action, value));
        }
        if (rawStyle.has("hoverEvent")) {
            JsonObject rawHoverEvent = getJsonObject(rawStyle, "hoverEvent");
            AHoverEvent hoverEvent = context.deserialize(rawHoverEvent, AHoverEvent.class);
            if (hoverEvent != null && hoverEvent.getAction().isUserDefinable()) style.setHoverEvent(hoverEvent);
        }
        if (rawStyle.has("font")) {
            String font = getString(rawStyle, "font");
            try {
                style.setFont(Identifier.of(font));
            } catch (Throwable t) {
                throw new JsonSyntaxException("Invalid font name: " + font);
            }
        }
        return style;
    }

}
