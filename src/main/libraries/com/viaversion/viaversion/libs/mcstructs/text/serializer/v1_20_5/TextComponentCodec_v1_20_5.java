package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_5;

import com.google.gson.JsonObject;
import com.viaversion.viaversion.libs.mcstructs.core.Identifier;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.mcstructs.snbt.SNbtSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.TextComponentCodec;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3.nbt.NbtStyleSerializer_v1_20_3;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3.nbt.NbtTextSerializer_v1_20_3;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_5.json.JsonHoverEventSerializer_v1_20_5;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_5.json.JsonStyleSerializer_v1_20_5;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_5.json.JsonTextSerializer_v1_20_5;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_5.nbt.NbtHoverEventSerializer_v1_20_5;
import com.viaversion.viaversion.libs.mcstructs.text.utils.JsonNbtConverter;

public class TextComponentCodec_v1_20_5 extends TextComponentCodec {

    public TextComponentCodec_v1_20_5() {
        super(
                () -> SNbtSerializer.V1_14,
                (codec, sNbtSerializer) -> new JsonTextSerializer_v1_20_5(textSerializer -> new JsonStyleSerializer_v1_20_5(styleSerializer -> new JsonHoverEventSerializer_v1_20_5((TextComponentCodec_v1_20_5) codec, textSerializer, sNbtSerializer))),
                (codec, sNbtSerializer) -> new NbtTextSerializer_v1_20_3(textSerializer -> new NbtStyleSerializer_v1_20_3(styleSerializer -> new NbtHoverEventSerializer_v1_20_5((TextComponentCodec_v1_20_5) codec, textSerializer, sNbtSerializer)))
        );
    }

    /**
     * Verify if the given identifier is a valid item.<br>
     * You can override this method to make the component deserialization more legit
     *
     * @param identifier The identifier to verify
     */
    public void verifyItem(final Identifier identifier) {
    }

    /**
     * Verify if the given identifier is a valid entity.<br>
     * You can override this method to make the component deserialization more legit
     *
     * @param identifier The identifier to verify
     */
    public void verifyEntity(final Identifier identifier) {
    }

    /**
     * Verify if the given tag is a valid item component.<br>
     * You can override this method to make the component deserialization more legit.<br>
     * <b>This method is only called if a text component is deserialized from Nbt! The {@link #convertItemComponents(JsonObject)} method should be used to verify json item components!</b>
     *
     * @param tag The tag to verify
     */
    public void verifyItemComponents(final Tag tag) {
    }

    /**
     * Verify and convert the given item component tag to a json object.<br>
     * You can override this method to make the component deserialization more legit
     *
     * @param tag The tag to convert
     * @return The converted json object
     */
    public JsonObject convertItemComponents(final CompoundTag tag) {
        return JsonNbtConverter.toJson(tag).getAsJsonObject();
    }

    /**
     * Verify and convert the given json object to a nbt tag.<br>
     * You can override this method to make the component deserialization more legit<br>
     * <b>This method should be used to verify json item components!</b>
     *
     * @param json The json object to convert
     * @return The converted item component tag
     */
    public CompoundTag convertItemComponents(final JsonObject json) {
        return ((CompoundTag) JsonNbtConverter.toNbt(json));
    }

}
