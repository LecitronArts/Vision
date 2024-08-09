package com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3.nbt;

import com.viaversion.viaversion.libs.mcstructs.core.Identifier;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.Style;
import com.viaversion.viaversion.libs.mcstructs.text.components.*;
import com.viaversion.viaversion.libs.mcstructs.text.components.nbt.BlockNbtComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.nbt.EntityNbtComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.nbt.StorageNbtComponent;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.ITypedSerializer;
import com.viaversion.viaversion.libs.mcstructs.text.serializer.v1_20_3.CodecUtils_v1_20_3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class NbtTextSerializer_v1_20_3 implements ITypedSerializer<Tag, ATextComponent>, CodecUtils_v1_20_3 {

    private final ITypedSerializer<Tag, Style> styleSerializer;

    public NbtTextSerializer_v1_20_3(final Function<NbtTextSerializer_v1_20_3, ITypedSerializer<Tag, Style>> styleSerializer) {
        this.styleSerializer = styleSerializer.apply(this);
    }

    @Override
    public Tag serialize(ATextComponent object) {
        CompoundTag out = new CompoundTag();
        if (object instanceof StringComponent component) {
            if (component.getSiblings().isEmpty() && component.getStyle().isEmpty()) return new StringTag(component.getText());
            else out.putString("text", component.getText());
        } else if (object instanceof TranslationComponent component) {
            out.putString("translate", component.getKey());
            if (component.getFallback() != null) out.putString("fallback", component.getFallback());
            if (component.getArgs().length > 0) {
                List<Tag> args = new ArrayList<>();
                for (Object arg : component.getArgs()) args.add(this.convert(arg));
                out.put("with", this.optimizeAndConvert(args));
            }
        } else if (object instanceof KeybindComponent component) {
            out.putString("keybind", component.getKeybind());
        } else if (object instanceof ScoreComponent component) {
            CompoundTag score = new CompoundTag();
            score.putString("name", component.getName());
            score.putString("objective", component.getObjective());
            out.put("score", score);
        } else if (object instanceof SelectorComponent component) {
            out.putString("selector", component.getSelector());
            if (component.getSeparator() != null) out.put("separator", this.serialize(component.getSeparator()));
        } else if (object instanceof NbtComponent component) {
            out.putString("nbt", component.getComponent());
            if (component.isResolve()) out.putBoolean("interpret", true);
            if (component instanceof EntityNbtComponent entityComponent) {
                out.putString("entity", entityComponent.getSelector());
            } else if (component instanceof BlockNbtComponent blockNbtComponent) {
                out.putString("block", blockNbtComponent.getPos());
            } else if (component instanceof StorageNbtComponent storageNbtComponent) {
                out.putString("storage", storageNbtComponent.getId().get());
            } else {
                throw new IllegalArgumentException("Unknown Nbt component type: " + component.getClass().getName());
            }
        } else {
            throw new IllegalArgumentException("Unknown component type: " + object.getClass().getName());
        }

        CompoundTag style = (CompoundTag) this.styleSerializer.serialize(object.getStyle());
        if (!style.isEmpty()) out.putAll(style);

        if (!object.getSiblings().isEmpty()) {
            List<Tag> siblings = new ArrayList<>();
            for (ATextComponent sibling : object.getSiblings()) siblings.add(this.serialize(sibling));
            out.put("extra", this.optimizeAndConvert(siblings));
        }

        return out;
    }

    protected Tag convert(final Object object) {
        if (object instanceof Boolean) return new ByteTag((byte) ((boolean) object ? 1 : 0));
        else if (object instanceof Byte) return new ByteTag((byte) object);
        else if (object instanceof Short) return new ShortTag((short) object);
        else if (object instanceof Integer) return new IntTag((int) object);
        else if (object instanceof Long) return new LongTag((long) object);
        else if (object instanceof Float) return new FloatTag((float) object);
        else if (object instanceof Double) return new DoubleTag((double) object);
        else if (object instanceof String) return new StringTag((String) object);
        else if (object instanceof ATextComponent) return this.serialize((ATextComponent) object);
        else throw new IllegalArgumentException("Unknown object type: " + object.getClass().getName());
    }

    protected Tag optimizeAndConvert(final List<Tag> tags) {
        Tag commonType = this.getCommonType(tags);
        if (commonType == null) {
            ListTag<CompoundTag> out = new ListTag<>();
            for (Tag tag : tags) {
                if (tag instanceof CompoundTag) {
                    out.add((CompoundTag) tag);
                } else {
                    CompoundTag marker = new CompoundTag();
                    marker.put("", tag);
                    out.add(marker);
                }
            }
            return out;
        } else if ((commonType instanceof ByteTag)) {
            byte[] bytes = new byte[tags.size()];
            for (int i = 0; i < tags.size(); i++) bytes[i] = ((ByteTag) tags.get(i)).getValue();
            return new ByteArrayTag(bytes);
        } else if ((commonType instanceof IntTag)) {
            int[] ints = new int[tags.size()];
            for (int i = 0; i < tags.size(); i++) ints[i] = ((IntTag) tags.get(i)).getValue();
            return new IntArrayTag(ints);
        } else if ((commonType instanceof LongTag)) {
            long[] longs = new long[tags.size()];
            for (int i = 0; i < tags.size(); i++) longs[i] = ((LongTag) tags.get(i)).getValue();
            return new LongArrayTag(longs);
        } else {
            ListTag<Tag> out = new ListTag<>();
            for (Tag tag : tags) out.add(tag);
            return out;
        }
    }

    protected Tag getCommonType(final List<Tag> tags) {
        if (tags.size() == 1) return tags.get(0);
        Tag type = tags.get(0);
        for (int i = 1; i < tags.size(); i++) {
            if (type.getClass() != tags.get(i).getClass()) return null;
        }
        return type;
    }

    @Override
    public ATextComponent deserialize(Tag object) {
        if ((object instanceof StringTag)) {
            return new StringComponent(((StringTag) object).getValue());
        } else if ((object instanceof ListTag)) {
            if (((ListTag) object).isEmpty()) throw new IllegalArgumentException("Empty list tag");
            ListTag<Tag> listTag = ((ListTag) object);
            ATextComponent[] components = new ATextComponent[listTag.size()];
            for (int i = 0; i < listTag.size(); i++) components[i] = this.deserialize(listTag.get(i));
            if (components.length == 1) {
                return components[0];
            } else {
                ATextComponent parent = components[0];
                for (int i = 1; i < components.length; i++) parent.append(components[i]);
                return parent;
            }
        } else if (!(object instanceof CompoundTag)) {
            throw new IllegalArgumentException("Unknown component type: " + object.getClass());
        }

        ATextComponent component = null;
        CompoundTag tag = ((CompoundTag) object);
        String type = (tag.get("type") instanceof StringTag ? ((StringTag) tag.get("type")).getValue() : null);
        if ((tag.get("text") instanceof StringTag) && (type == null || type.equals("text"))) {
            component = new StringComponent((tag.get("text") instanceof StringTag ? ((StringTag) tag.get("text")).getValue() : ""));
        } else if ((tag.get("translate") instanceof StringTag) && (type == null || type.equals("translatable"))) {
            String key = (tag.get("translate") instanceof StringTag ? ((StringTag) tag.get("translate")).getValue() : "");
            String fallback = (tag.get("fallback") instanceof StringTag ? ((StringTag) tag.get("fallback")).getValue() : null);
            if (tag.contains("with")) {
                List<Tag> with = unwrapMarkers(this.getArrayOrList(tag, "with"));
                Object[] args = new Object[with.size()];
                for (int i = 0; i < with.size(); i++) {
                    Tag arg = with.get(i);
                    if ((arg instanceof NumberTag)) args[i] = ((NumberTag) arg).getValue();
                    else if ((arg instanceof StringTag)) args[i] = ((StringTag) arg).getValue();
                    else args[i] = this.deserialize(arg);
                }
                component = new TranslationComponent(key, args).setFallback(fallback);
            } else {
                component = new TranslationComponent(key).setFallback(fallback);
            }
        } else if ((tag.get("keybind") instanceof StringTag) && (type == null || type.equals("keybind"))) {
            component = new KeybindComponent((tag.get("keybind") instanceof StringTag ? ((StringTag) tag.get("keybind")).getValue() : ""));
        } else if ((tag.get("score") instanceof CompoundTag) && (tag.get("score") instanceof CompoundTag ? ((CompoundTag) tag.get("score")) : new CompoundTag()).get("name") instanceof StringTag && (tag.get("score") instanceof CompoundTag ? ((CompoundTag) tag.get("score")) : new CompoundTag()).get("objective") instanceof StringTag && (type == null || type.equals("score"))) {
            CompoundTag score = (tag.get("score") instanceof CompoundTag ? ((CompoundTag) tag.get("score")) : new CompoundTag());
            String name = (score.get("name") instanceof StringTag ? ((StringTag) score.get("name")).getValue() : "");
            String objective = (score.get("objective") instanceof StringTag ? ((StringTag) score.get("objective")).getValue() : "");
            component = new ScoreComponent(name, objective);
        } else if ((tag.get("selector") instanceof StringTag) && (type == null || type.equals("selector"))) {
            String selector = (tag.get("selector") instanceof StringTag ? ((StringTag) tag.get("selector")).getValue() : "");
            ATextComponent separator = null;
            if (tag.contains("separator")) separator = this.deserialize(tag.get("separator"));
            component = new SelectorComponent(selector, separator);
        } else if ((tag.get("nbt") instanceof StringTag) && (type == null || type.equals("nbt"))) {
            String nbt = (tag.get("nbt") instanceof StringTag ? ((StringTag) tag.get("nbt")).getValue() : "");
            boolean interpret = (tag.get("interpret") instanceof ByteTag ? ((ByteTag) tag.get("interpret")).asBoolean() : false);
            ATextComponent separator = null;
            if (tag.contains("separator")) {
                try {
                    separator = this.deserialize(tag.get("separator"));
                } catch (Throwable ignored) {
                    //If the separator fails to parse we just ignore it
                }
            }
            String source = (tag.get("source") instanceof StringTag ? ((StringTag) tag.get("source")).getValue() : null);

            boolean typeFound = false;
            if ((tag.get("entity") instanceof StringTag) && (source == null || source.equals("entity"))) {
                component = new EntityNbtComponent(nbt, interpret, separator, (tag.get("entity") instanceof StringTag ? ((StringTag) tag.get("entity")).getValue() : ""));
                typeFound = true;
            } else if ((tag.get("block") instanceof StringTag) && (source == null || source.equals("block"))) {
                component = new BlockNbtComponent(nbt, interpret, separator, (tag.get("block") instanceof StringTag ? ((StringTag) tag.get("block")).getValue() : ""));
                typeFound = true;
            } else if ((tag.get("storage") instanceof StringTag) && (source == null || source.equals("storage"))) {
                try {
                    component = new StorageNbtComponent(nbt, interpret, separator, Identifier.of((tag.get("storage") instanceof StringTag ? ((StringTag) tag.get("storage")).getValue() : "")));
                    typeFound = true;
                } catch (Throwable ignored) {
                    //If the storage identifier fails to parse we just ignore it
                }
            }
            if (!typeFound) throw new IllegalArgumentException("Unknown Nbt component type: " + tag.getClass());
        } else {
            throw new IllegalArgumentException("Unknown component type: " + tag.getClass());
        }

        Style style = this.styleSerializer.deserialize(tag);
        if (!style.isEmpty()) component.setStyle(style);

        if (tag.contains("extra")) {
            if (!(tag.get("extra") instanceof ListTag)) throw new IllegalArgumentException("Expected list tag for 'extra' tag");
            ListTag<Tag> extraTag = (tag.get("extra") instanceof ListTag ? ((ListTag) tag.get("extra")) : new ListTag<>());
            if (extraTag.isEmpty()) throw new IllegalArgumentException("Empty extra list tag");

            ATextComponent[] extra;
            List<Tag> unwrapped = unwrapMarkers(extraTag);
            extra = new ATextComponent[unwrapped.size()];
            for (int i = 0; i < unwrapped.size(); i++) extra[i] = this.deserialize(unwrapped.get(i));
            component.append(extra);
        }

        return component;
    }

    protected ListTag<?> getArrayOrList(final CompoundTag tag, final String key) {
        if ((tag.get(key) instanceof ListTag)) return (tag.get(key) instanceof ListTag ? ((ListTag) tag.get(key)) : new ListTag<>());
        else if ((tag.get(key) instanceof ByteArrayTag)) return ((ByteArrayTag) tag.get(key)).toListTag();
        else if ((tag.get(key) instanceof IntArrayTag)) return ((IntArrayTag) tag.get(key)).toListTag();
        else if ((tag.get(key) instanceof LongArrayTag)) return ((LongArrayTag) tag.get(key)).toListTag();
        else throw new IllegalArgumentException("Expected array or list tag for '" + key + "' tag");
    }

}
