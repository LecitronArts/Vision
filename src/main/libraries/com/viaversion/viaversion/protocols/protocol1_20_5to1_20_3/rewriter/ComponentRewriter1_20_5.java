/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2024 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.rewriter;

import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.FloatTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.google.common.base.Preconditions;
import com.google.common.primitives.UnsignedBytes;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.GameProfile;
import com.viaversion.viaversion.api.minecraft.GlobalPosition;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.AdventureModePredicate;
import com.viaversion.viaversion.api.minecraft.item.data.ArmorTrim;
import com.viaversion.viaversion.api.minecraft.item.data.ArmorTrimMaterial;
import com.viaversion.viaversion.api.minecraft.item.data.ArmorTrimPattern;
import com.viaversion.viaversion.api.minecraft.item.data.AttributeModifier;
import com.viaversion.viaversion.api.minecraft.item.data.AttributeModifiers;
import com.viaversion.viaversion.api.minecraft.item.data.BannerPattern;
import com.viaversion.viaversion.api.minecraft.item.data.BannerPatternLayer;
import com.viaversion.viaversion.api.minecraft.item.data.Bee;
import com.viaversion.viaversion.api.minecraft.item.data.BlockPredicate;
import com.viaversion.viaversion.api.minecraft.item.data.BlockStateProperties;
import com.viaversion.viaversion.api.minecraft.item.data.DyedColor;
import com.viaversion.viaversion.api.minecraft.item.data.Enchantments;
import com.viaversion.viaversion.api.minecraft.item.data.FilterableComponent;
import com.viaversion.viaversion.api.minecraft.item.data.FilterableString;
import com.viaversion.viaversion.api.minecraft.item.data.FireworkExplosion;
import com.viaversion.viaversion.api.minecraft.item.data.Fireworks;
import com.viaversion.viaversion.api.minecraft.item.data.FoodEffect;
import com.viaversion.viaversion.api.minecraft.item.data.FoodProperties;
import com.viaversion.viaversion.api.minecraft.item.data.Instrument;
import com.viaversion.viaversion.api.minecraft.item.data.LodestoneTracker;
import com.viaversion.viaversion.api.minecraft.item.data.ModifierData;
import com.viaversion.viaversion.api.minecraft.item.data.PotDecorations;
import com.viaversion.viaversion.api.minecraft.item.data.PotionContents;
import com.viaversion.viaversion.api.minecraft.item.data.PotionEffect;
import com.viaversion.viaversion.api.minecraft.item.data.PotionEffectData;
import com.viaversion.viaversion.api.minecraft.item.data.StatePropertyMatcher;
import com.viaversion.viaversion.api.minecraft.item.data.SuspiciousStewEffect;
import com.viaversion.viaversion.api.minecraft.item.data.ToolProperties;
import com.viaversion.viaversion.api.minecraft.item.data.ToolRule;
import com.viaversion.viaversion.api.minecraft.item.data.Unbreakable;
import com.viaversion.viaversion.api.minecraft.item.data.WrittenBook;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPacket1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.ArmorMaterials1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Attributes1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.BannerPatterns1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.DyeColors;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Enchantments1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.EquipmentSlots1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Instruments1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.PotionEffects1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Potions1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.TrimMaterials1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.TrimPatterns1_20_3;
import com.viaversion.viaversion.rewriter.ComponentRewriter;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.Either;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.SerializerVersion;
import com.viaversion.viaversion.util.UUIDUtil;
import com.viaversion.viaversion.util.Unit;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

// 1.20.5 data component -> 1.20.5 nbt conversion
public class ComponentRewriter1_20_5 extends ComponentRewriter<ClientboundPacket1_20_3> {

    @SuppressWarnings("rawtypes")
    protected final Map<StructuredDataKey, DataConverter> rewriters = new HashMap<>();

    public ComponentRewriter1_20_5(final Protocol<ClientboundPacket1_20_3, ?, ?, ?> protocol) {
        super(protocol, ReadType.NBT);

        register(StructuredDataKey.CUSTOM_DATA, this::convertCustomData);
        register(StructuredDataKey.MAX_STACK_SIZE, this::convertMaxStackSize);
        register(StructuredDataKey.MAX_DAMAGE, this::convertMaxDamage);
        register(StructuredDataKey.DAMAGE, this::convertDamage);
        register(StructuredDataKey.UNBREAKABLE, this::convertUnbreakable);
        register(StructuredDataKey.CUSTOM_NAME, this::convertCustomName);
        register(StructuredDataKey.ITEM_NAME, this::convertItemName);
        register(StructuredDataKey.LORE, this::convertLore);
        register(StructuredDataKey.RARITY, this::convertRarity);
        register(StructuredDataKey.ENCHANTMENTS, this::convertEnchantments);
        register(StructuredDataKey.CAN_PLACE_ON, this::convertCanPlaceOn);
        register(StructuredDataKey.CAN_BREAK, this::convertCanBreak);
        register(StructuredDataKey.ATTRIBUTE_MODIFIERS, this::convertAttributeModifiers);
        register(StructuredDataKey.CUSTOM_MODEL_DATA, this::convertCustomModelData);
        register(StructuredDataKey.HIDE_ADDITIONAL_TOOLTIP, this::convertHideAdditionalTooltip);
        register(StructuredDataKey.HIDE_TOOLTIP, this::convertHideTooltip);
        register(StructuredDataKey.REPAIR_COST, this::convertRepairCost);
        register(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE, this::convertEnchantmentGlintOverride);
        register(StructuredDataKey.CREATIVE_SLOT_LOCK, null);
        register(StructuredDataKey.INTANGIBLE_PROJECTILE, this::convertIntangibleProjectile);
        register(StructuredDataKey.FOOD, this::convertFood);
        register(StructuredDataKey.FIRE_RESISTANT, this::convertFireResistant);
        register(StructuredDataKey.TOOL, this::convertTool);
        register(StructuredDataKey.STORED_ENCHANTMENTS, this::convertStoredEnchantments);
        register(StructuredDataKey.DYED_COLOR, this::convertDyedColor);
        register(StructuredDataKey.MAP_COLOR, this::convertMapColor);
        register(StructuredDataKey.MAP_ID, this::convertMapId);
        register(StructuredDataKey.MAP_DECORATIONS, this::convertMapDecorations);
        register(StructuredDataKey.MAP_POST_PROCESSING, null);
        register(StructuredDataKey.CHARGED_PROJECTILES, this::convertChargedProjectiles);
        register(StructuredDataKey.BUNDLE_CONTENTS, this::convertBundleContents);
        register(StructuredDataKey.POTION_CONTENTS, this::convertPotionContents);
        register(StructuredDataKey.SUSPICIOUS_STEW_EFFECTS, this::convertSuspiciousStewEffects);
        register(StructuredDataKey.WRITABLE_BOOK_CONTENT, this::convertWritableBookContent);
        register(StructuredDataKey.WRITTEN_BOOK_CONTENT, this::convertWrittenBookContent);
        register(StructuredDataKey.TRIM, this::convertTrim);
        register(StructuredDataKey.DEBUG_STICK_STATE, this::convertDebugStickRate);
        register(StructuredDataKey.ENTITY_DATA, this::convertEntityData);
        register(StructuredDataKey.BUCKET_ENTITY_DATA, this::convertBucketEntityData);
        register(StructuredDataKey.BLOCK_ENTITY_DATA, this::convertBlockEntityData);
        register(StructuredDataKey.INSTRUMENT, this::convertInstrument);
        register(StructuredDataKey.OMINOUS_BOTTLE_AMPLIFIER, this::convertOminousBottleAmplifier);
        register(StructuredDataKey.RECIPES, this::convertRecipes);
        register(StructuredDataKey.LODESTONE_TRACKER, this::convertLodestoneTracker);
        register(StructuredDataKey.FIREWORK_EXPLOSION, this::convertFireworkExplosion);
        register(StructuredDataKey.FIREWORKS, this::convertFireworks);
        register(StructuredDataKey.PROFILE, this::convertProfile);
        register(StructuredDataKey.NOTE_BLOCK_SOUND, this::convertNoteBlockSound);
        register(StructuredDataKey.BANNER_PATTERNS, this::convertBannerPatterns);
        register(StructuredDataKey.BASE_COLOR, this::convertBaseColor);
        register(StructuredDataKey.POT_DECORATIONS, this::convertPotDecorations);
        register(StructuredDataKey.CONTAINER, this::convertContainer);
        register(StructuredDataKey.BLOCK_STATE, this::convertBlockState);
        register(StructuredDataKey.BEES, this::convertBees);
        register(StructuredDataKey.LOCK, this::convertLock);
        register(StructuredDataKey.CONTAINER_LOOT, this::convertContainerLoot);
    }

    @Override
    protected void handleHoverEvent(final UserConnection connection, final CompoundTag hoverEventTag) {
        final StringTag actionTag = hoverEventTag.getStringTag("action");
        if (actionTag == null) {
            return;
        }

        if (actionTag.getValue().equals("show_item")) {
            final Tag valueTag = hoverEventTag.remove("value");
            if (valueTag != null) { // Convert legacy hover event to new format for rewriting (Doesn't handle all cases, but good enough)
                final CompoundTag tag = ComponentUtil.deserializeShowItem(valueTag, SerializerVersion.V1_20_3);
                final CompoundTag contentsTag = new CompoundTag();
                contentsTag.put("id", tag.getStringTag("id"));
                contentsTag.put("count", new IntTag(tag.getByte("Count")));
                if (tag.get("tag") instanceof CompoundTag) {
                    contentsTag.put("tag", new StringTag(SerializerVersion.V1_20_3.toSNBT(tag.getCompoundTag("tag"))));
                }
                hoverEventTag.put("contents", contentsTag);
            }

            final CompoundTag contentsTag = hoverEventTag.getCompoundTag("contents");
            if (contentsTag == null) {
                return;
            }

            final StringTag idTag = contentsTag.getStringTag("id");
            if (idTag == null) {
                return;
            }

            final int itemId = Protocol1_20_5To1_20_3.MAPPINGS.getFullItemMappings().id(idTag.getValue());
            if (itemId == -1) {
                return;
            }

            final StringTag tag = contentsTag.remove("tag");
            if (tag == null) {
                return;
            }

            final CompoundTag tagTag;
            try {
                tagTag = (CompoundTag) SerializerVersion.V1_20_3.toTag(tag.getValue());
            } catch (final Exception e) {
                if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                    Via.getPlatform().getLogger().log(Level.WARNING, "Error reading 1.20.3 NBT in show_item: " + contentsTag, e);
                }
                return;
            }

            final Item oldItem = new DataItem();
            oldItem.setIdentifier(itemId);
            if (tagTag != null) { // We don't need to remap data if there is none
                oldItem.setTag(tagTag);
            }

            final Item newItem = protocol.getItemRewriter().handleItemToClient(connection, oldItem);
            if (newItem == null) {
                return;
            }

            if (newItem.identifier() != 0) {
                final String itemName = Protocol1_20_5To1_20_3.MAPPINGS.getFullItemMappings().mappedIdentifier(newItem.identifier());
                if (itemName != null) {
                    contentsTag.putString("id", itemName);
                }
            } else {
                // Cannot be air
                contentsTag.putString("id", "minecraft:stone");
            }

            final Map<StructuredDataKey<?>, StructuredData<?>> data = newItem.structuredData().data();
            if (!data.isEmpty()) {
                final CompoundTag components;
                try {
                    components = toTag(data, false);
                } catch (final Exception e) {
                    if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                        Via.getPlatform().getLogger().log(Level.WARNING, "Error writing 1.20.5 components in show_item!", e);
                    }
                    return;
                }
                contentsTag.put("components", components);
            }
        } else if (actionTag.getValue().equals("show_entity")) {
            final Tag valueTag = hoverEventTag.remove("value");
            if (valueTag != null) { // Convert legacy hover event to new format for rewriting (Doesn't handle all cases, but good enough)
                final CompoundTag tag = ComponentUtil.deserializeShowItem(valueTag, SerializerVersion.V1_20_3);
                final CompoundTag contentsTag = new CompoundTag();
                contentsTag.put("type", tag.getStringTag("type"));
                contentsTag.put("id", tag.getStringTag("id"));
                contentsTag.put("name", SerializerVersion.V1_20_3.toTag(SerializerVersion.V1_20_3.toComponent(tag.getString("name"))));
                hoverEventTag.put("contents", contentsTag);
            }

            final CompoundTag contentsTag = hoverEventTag.getCompoundTag("contents");
            if (contentsTag == null) {
                return;
            }

            if (this.protocol.getMappingData().getEntityMappings().mappedId(contentsTag.getString("type")) == -1) {
                contentsTag.put("type", new StringTag("pig"));
            }
        }
    }

    protected CompoundTag toTag(final Map<StructuredDataKey<?>, StructuredData<?>> data, final boolean empty) {
        final CompoundTag tag = new CompoundTag();
        for (final Map.Entry<StructuredDataKey<?>, StructuredData<?>> entry : data.entrySet()) {
            final StructuredDataKey<?> key = entry.getKey();
            if (!rewriters.containsKey(key)) { // Should NOT happen
                Via.getPlatform().getLogger().severe("No converter for " + key.identifier() + " found!");
                continue;
            }
            final StructuredData<?> value = entry.getValue();
            if (value.isEmpty()) {
                if (empty) {
                    // Theoretically not needed here, but we'll keep it for consistency
                    tag.put("!" + key.identifier(), new CompoundTag());
                    continue;
                }
                throw new IllegalArgumentException("Empty structured data: " + key.identifier());
            }

            //noinspection unchecked
            final Tag valueTag = rewriters.get(key).convert(value.value());
            if (valueTag == null) {
                continue;
            }
            tag.put(key.identifier(), valueTag);
        }
        return tag;
    }

    // ---------------------------------------------------------------------------------------
    // Conversion methods, can be overridden in future protocols to handle new changes

    protected CompoundTag convertCustomData(final CompoundTag value) {
        return value;
    }

    protected IntTag convertMaxStackSize(final Integer value) {
        return convertIntRange(value, 1, 99);
    }

    protected IntTag convertMaxDamage(final Integer value) {
        return convertPositiveInt(value);
    }

    protected IntTag convertDamage(final Integer value) {
        return convertNonNegativeInt(value);
    }

    protected CompoundTag convertUnbreakable(final Unbreakable value) {
        final CompoundTag tag = new CompoundTag();
        if (!value.showInTooltip()) {
            tag.putBoolean("show_in_tooltip", false);
        }
        return tag;
    }

    protected StringTag convertCustomName(final Tag value) {
        return convertComponent(value);
    }

    protected StringTag convertItemName(final Tag value) {
        return convertComponent(value);
    }

    protected ListTag<StringTag> convertLore(final Tag[] value) {
        return convertComponents(value, 256);
    }

    protected StringTag convertRarity(final Integer value) {
        return convertEnumEntry(value, "common", "uncommon", "rare", "epic");
    }

    protected CompoundTag convertEnchantments(final Enchantments value) {
        final CompoundTag tag = new CompoundTag();

        final CompoundTag levels = new CompoundTag();
        for (final Int2IntMap.Entry entry : value.enchantments().int2IntEntrySet()) {
            final int level = checkIntRange(0, 255, entry.getIntValue());
            levels.putInt(Enchantments1_20_5.idToKey(entry.getIntKey()), level);
        }
        tag.put("levels", levels);
        if (!value.showInTooltip()) {
            tag.putBoolean("show_in_tooltip", false);
        }
        return tag;
    }

    protected CompoundTag convertCanPlaceOn(final AdventureModePredicate value) {
        final CompoundTag tag = new CompoundTag();
        final ListTag<CompoundTag> predicates = new ListTag<>(CompoundTag.class);
        for (final BlockPredicate predicate : value.predicates()) {
            final CompoundTag predicateTag = new CompoundTag();
            if (predicate.holderSet() != null) {
                convertHolderSet(predicateTag, "blocks", predicate.holderSet());
            }
            if (predicate.propertyMatchers() != null) {
                final CompoundTag state = new CompoundTag();
                for (final StatePropertyMatcher matcher : predicate.propertyMatchers()) {
                    final Either<String, StatePropertyMatcher.RangedMatcher> match = matcher.matcher();
                    if (match.isLeft()) {
                        state.putString(matcher.name(), match.left());
                    } else {
                        final StatePropertyMatcher.RangedMatcher range = match.right();
                        final CompoundTag rangeTag = new CompoundTag();
                        if (range.minValue() != null) {
                            rangeTag.putString("min", range.minValue());
                        }
                        if (range.maxValue() != null) {
                            rangeTag.putString("max", range.maxValue());
                        }
                        state.put(matcher.name(), rangeTag);
                    }
                }
                predicateTag.put("state", state);
            }
            if (predicate.tag() != null) {
                predicateTag.putString("nbt", serializerVersion().toSNBT(predicate.tag()));
            }

            predicates.add(predicateTag);
        }
        tag.put("predicates", predicates);
        if (!value.showInTooltip()) {
            tag.putBoolean("show_in_tooltip", false);
        }
        return tag;
    }

    protected CompoundTag convertCanBreak(final AdventureModePredicate value) {
        return convertCanPlaceOn(value);
    }

    protected CompoundTag convertAttributeModifiers(final AttributeModifiers value) {
        final CompoundTag tag = new CompoundTag();
        final ListTag<CompoundTag> modifiers = new ListTag<>(CompoundTag.class);
        for (final AttributeModifier modifier : value.modifiers()) {
            final CompoundTag modifierTag = new CompoundTag();
            final String type = Attributes1_20_5.idToKey(modifier.attribute());
            if (type == null) {
                throw new IllegalArgumentException("Unknown attribute type: " + modifier.attribute());
            }

            modifierTag.putString("type", type);
            convertModifierData(modifierTag, modifier.modifier());
            if (modifier.slotType() != 0) {
                final String slotType = EquipmentSlots1_20_5.idToKey(modifier.slotType());
                Preconditions.checkNotNull(slotType, "Unknown slot type %s", modifier.slotType());
                modifierTag.putString("slot", slotType);
            }

            modifiers.add(modifierTag);
        }

        tag.put("modifiers", modifiers);
        if (!value.showInTooltip()) {
            tag.putBoolean("show_in_tooltip", false);
        }
        return tag;
    }

    protected IntTag convertCustomModelData(final Integer value) {
        return new IntTag(value);
    }

    protected CompoundTag convertHideAdditionalTooltip(final Unit value) {
        return convertUnit();
    }

    protected CompoundTag convertHideTooltip(final Unit value) {
        return convertUnit();
    }

    protected IntTag convertRepairCost(final Integer value) {
        return convertIntRange(value, 0, Integer.MAX_VALUE);
    }

    protected ByteTag convertEnchantmentGlintOverride(final Boolean value) {
        return new ByteTag(value);
    }

    protected CompoundTag convertIntangibleProjectile(final Tag value) {
        return convertUnit();
    }

    protected CompoundTag convertFood(final FoodProperties value) {
        final CompoundTag tag = new CompoundTag();
        tag.put("nutrition", convertNonNegativeInt(value.nutrition()));
        tag.putFloat("saturation_modifier", value.saturationModifier());
        if (value.canAlwaysEat()) {
            tag.putBoolean("can_always_eat", true);
        }
        if (value.eatSeconds() != 1.6F) {
            tag.put("eat_seconds", convertPositiveFloat(value.eatSeconds()));
        }
        if (value.possibleEffects().length > 0) {
            final ListTag<CompoundTag> effects = new ListTag<>(CompoundTag.class);
            for (final FoodEffect foodEffect : value.possibleEffects()) {
                final CompoundTag effectTag = new CompoundTag();
                final CompoundTag potionEffectTag = new CompoundTag();
                convertPotionEffect(potionEffectTag, foodEffect.effect());
                effectTag.put("effect", potionEffectTag);
                if (foodEffect.probability() != 1.0F) {
                    effectTag.putFloat("probability", foodEffect.probability());
                }
            }
            tag.put("effects", effects);
        }
        return tag;
    }

    protected CompoundTag convertFireResistant(final Unit value) {
        return convertUnit();
    }

    protected CompoundTag convertTool(final ToolProperties value) {
        final CompoundTag tag = new CompoundTag();

        final ListTag<CompoundTag> rules = new ListTag<>(CompoundTag.class);
        for (final ToolRule rule : value.rules()) {
            final CompoundTag ruleTag = new CompoundTag();
            convertHolderSet(ruleTag, "blocks", rule.blocks());
            if (rule.speed() != null) {
                ruleTag.putFloat("speed", rule.speed());
            }
            if (rule.correctForDrops() != null) {
                ruleTag.putBoolean("correct_for_drops", rule.correctForDrops());
            }
            rules.add(ruleTag);
        }
        tag.put("rules", rules);
        if (value.defaultMiningSpeed() != 1.0F) {
            tag.putFloat("default_mining_speed", value.defaultMiningSpeed());
        }
        if (value.damagePerBlock() != 1) {
            tag.put("damage_per_block", convertNonNegativeInt(value.damagePerBlock()));
        }
        return tag;
    }

    protected CompoundTag convertStoredEnchantments(final Enchantments value) {
        return convertEnchantments(value);
    }

    protected CompoundTag convertDyedColor(final DyedColor value) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("rgb", value.rgb());
        if (!value.showInTooltip()) {
            tag.putBoolean("show_in_tooltip", false);
        }
        return tag;
    }

    protected IntTag convertMapColor(final Integer value) {
        return new IntTag(value);
    }

    protected IntTag convertMapId(final Integer value) {
        return new IntTag(value);
    }

    protected CompoundTag convertMapDecorations(final CompoundTag value) {
        return value; // String<->id conversion is already done by the item rewriter
    }

    protected ListTag<CompoundTag> convertChargedProjectiles(final Item[] value) {
        return convertItemArray(value);
    }

    protected ListTag<CompoundTag> convertBundleContents(final Item[] value) {
        return convertItemArray(value);
    }

    protected CompoundTag convertPotionContents(final PotionContents value) {
        final CompoundTag tag = new CompoundTag();
        if (value.potion() != null) {
            final String potion = Potions1_20_5.idToKey(value.potion());
            if (potion != null) {
                tag.putString("potion", potion);
            }
        }
        if (value.customColor() != null) {
            tag.putInt("custom_color", value.customColor());
        }
        for (final PotionEffect effect : value.customEffects()) {
            convertPotionEffect(tag, effect);
        }
        return tag;
    }

    protected ListTag<CompoundTag> convertSuspiciousStewEffects(final SuspiciousStewEffect[] value) {
        final ListTag<CompoundTag> tag = new ListTag<>(CompoundTag.class);
        for (final SuspiciousStewEffect effect : value) {
            final CompoundTag effectTag = new CompoundTag();
            final String id = PotionEffects1_20_5.idToKey(effect.mobEffect());
            if (id != null) {
                effectTag.putString("id", id);
            }
            if (effect.duration() != 160) {
                effectTag.putInt("duration", effect.duration());
            }
            tag.add(effectTag);
        }
        return tag;
    }

    protected CompoundTag convertWritableBookContent(final FilterableString[] value) {
        final CompoundTag tag = new CompoundTag();
        if (value == null) {
            return tag;
        }

        if (value.length > 100) {
            throw new IllegalArgumentException("Too many pages: " + value.length);
        }

        final ListTag<CompoundTag> pagesTag = new ListTag<>(CompoundTag.class);
        for (final FilterableString page : value) {
            final CompoundTag pageTag = new CompoundTag();
            convertFilterableString(pageTag, page, 0, 1024);
            pagesTag.add(pageTag);
        }
        tag.put("pages", pagesTag);
        return tag;
    }

    protected CompoundTag convertWrittenBookContent(final WrittenBook value) {
        final CompoundTag tag = new CompoundTag();
        convertFilterableString(tag, value.title(), 0, 32);
        tag.putString("author", value.author());
        if (value.generation() != 0) {
            tag.put("generation", convertIntRange(value.generation(), 0, 3));
        }

        final CompoundTag title = new CompoundTag();
        convertFilterableString(title, value.title(), 0, 32);
        tag.put("title", title);

        final ListTag<CompoundTag> pagesTag = new ListTag<>(CompoundTag.class);
        for (final FilterableComponent page : value.pages()) {
            final CompoundTag pageTag = new CompoundTag();
            convertFilterableComponent(pageTag, page);
            pagesTag.add(pageTag);
        }

        if (!pagesTag.isEmpty()) {
            tag.put("pages", pagesTag);
        }

        if (value.resolved()) {
            tag.putBoolean("resolved", true);
        }
        return tag;
    }

    protected CompoundTag convertTrim(final ArmorTrim value) {
        final CompoundTag tag = new CompoundTag();
        final Holder<ArmorTrimMaterial> material = value.material();
        if (material.hasId()) {
            final String trimMaterial = TrimMaterials1_20_3.idToKey(material.id());
            tag.putString("material", trimMaterial);
        } else {
            final ArmorTrimMaterial armorTrimMaterial = material.value();
            final CompoundTag materialTag = new CompoundTag();
            final String ingredient = Protocol1_20_5To1_20_3.MAPPINGS.getFullItemMappings().identifier(armorTrimMaterial.itemId());
            if (ingredient == null) {
                throw new IllegalArgumentException("Unknown item: " + armorTrimMaterial.itemId());
            }

            final CompoundTag overrideArmorMaterialsTag = new CompoundTag();
            for (final Int2ObjectMap.Entry<String> entry : armorTrimMaterial.overrideArmorMaterials().int2ObjectEntrySet()) {
                final String materialKey = ArmorMaterials1_20_5.idToKey(entry.getIntKey());
                if (materialKey != null) {
                    overrideArmorMaterialsTag.putString(materialKey, entry.getValue());
                }
            }

            materialTag.putString("asset_name", armorTrimMaterial.assetName());
            materialTag.putString("ingredient", ingredient);
            materialTag.putFloat("item_model_index", armorTrimMaterial.itemModelIndex());
            materialTag.put("override_armor_materials", overrideArmorMaterialsTag);
            materialTag.put("description", armorTrimMaterial.description());
            tag.put("material", materialTag);
        }

        final Holder<ArmorTrimPattern> pattern = value.pattern();
        if (pattern.hasId()) {
            tag.putString("pattern", TrimPatterns1_20_3.idToKey(pattern.id()));
        } else {
            final ArmorTrimPattern armorTrimPattern = pattern.value();
            final CompoundTag patternTag = new CompoundTag();
            final String templateItem = Protocol1_20_5To1_20_3.MAPPINGS.getFullItemMappings().identifier(armorTrimPattern.itemId());
            if (templateItem == null) {
                throw new IllegalArgumentException("Unknown item: " + armorTrimPattern.itemId());
            }

            patternTag.put("asset_id", convertIdentifier(armorTrimPattern.assetName()));
            patternTag.putString("template_item", templateItem);
            patternTag.put("description", armorTrimPattern.description());
            if (armorTrimPattern.decal()) {
                patternTag.putBoolean("decal", true);
            }
            tag.put("pattern", patternTag);
        }

        if (!value.showInTooltip()) {
            tag.putBoolean("show_in_tooltip", false);
        }
        return tag;
    }

    protected CompoundTag convertDebugStickRate(final CompoundTag value) {
        return value;
    }

    protected CompoundTag convertEntityData(final CompoundTag value) {
        return convertNbtWithId(value);
    }

    protected CompoundTag convertBucketEntityData(final CompoundTag value) {
        return convertNbt(value);
    }

    protected CompoundTag convertBlockEntityData(final CompoundTag value) {
        return convertNbtWithId(value);
    }

    protected Tag convertInstrument(final Holder<Instrument> value) {
        if (value.hasId()) {
            return new StringTag(Instruments1_20_3.idToKey(value.id()));
        }

        final Instrument instrument = value.value();
        final CompoundTag tag = new CompoundTag();
        final Holder<SoundEvent> sound = instrument.soundEvent();
        if (sound.hasId()) {
            tag.putString("sound_event", Protocol1_20_5To1_20_3.MAPPINGS.soundName(sound.id()));
        } else {
            final SoundEvent soundEvent = sound.value();
            final CompoundTag soundEventTag = new CompoundTag();
            soundEventTag.put("sound_id", convertIdentifier(soundEvent.identifier()));
            if (soundEvent.fixedRange() != null) {
                soundEventTag.putFloat("range", soundEvent.fixedRange());
            }
        }

        tag.put("use_duration", convertPositiveInt(instrument.useDuration()));
        tag.put("range", convertPositiveFloat(instrument.range()));
        return tag;
    }

    protected IntTag convertOminousBottleAmplifier(final Integer value) {
        return convertIntRange(value, 0, 4);
    }

    protected Tag convertRecipes(final Tag value) {
        return value; // Item rewriter takes care of validation
    }

    protected CompoundTag convertLodestoneTracker(final LodestoneTracker value) {
        final CompoundTag tag = new CompoundTag();
        if (value.pos() != null) {
            convertGlobalPos(tag, "target", value.pos());
        }
        if (!value.tracked()) {
            tag.putBoolean("tracked", false);
        }
        return tag;
    }

    protected CompoundTag convertFireworkExplosion(final FireworkExplosion value) {
        final CompoundTag tag = new CompoundTag();
        tag.put("shape", convertEnumEntry(value.shape(), "small_ball", "large_ball", "star", "creeper", "burst"));
        if (value.colors().length > 0) {
            tag.put("colors", new IntArrayTag(value.colors()));
        }
        if (value.fadeColors().length > 0) {
            tag.put("fade_colors", new IntArrayTag(value.fadeColors()));
        }
        if (value.hasTrail()) {
            tag.putBoolean("trail", true);
        }
        if (value.hasTwinkle()) {
            tag.putBoolean("twinkle", true);
        }
        return tag;
    }

    protected CompoundTag convertFireworks(final Fireworks value) {
        final CompoundTag tag = new CompoundTag();
        if (value.flightDuration() != 0) {
            tag.put("flight_duration", convertUnsignedByte((byte) value.flightDuration()));
        }
        final ListTag<CompoundTag> explosions = new ListTag<>(CompoundTag.class);
        if (value.explosions().length > 256) {
            throw new IllegalArgumentException("Too many explosions: " + value.explosions().length);
        }
        for (final FireworkExplosion explosion : value.explosions()) {
            explosions.add(convertFireworkExplosion(explosion));
        }
        tag.put("explosions", explosions);
        return tag;
    }

    protected CompoundTag convertProfile(final GameProfile value) {
        final CompoundTag tag = new CompoundTag();
        if (value.name() != null) {
            tag.putString("name", value.name());
        }
        if (value.id() != null) {
            tag.put("id", new IntArrayTag(UUIDUtil.toIntArray(value.id())));
        }
        if (value.properties().length > 0) {
            convertProperties(tag, "properties", value.properties());
        }
        return tag;
    }

    protected StringTag convertNoteBlockSound(final String value) {
        return convertIdentifier(value);
    }

    protected ListTag<CompoundTag> convertBannerPatterns(final BannerPatternLayer[] value) {
        final ListTag<CompoundTag> tag = new ListTag<>(CompoundTag.class);
        for (final BannerPatternLayer layer : value) {
            final CompoundTag layerTag = new CompoundTag();
            convertBannerPattern(layerTag, "pattern", layer.pattern());
            layerTag.put("color", convertDyeColor(layer.dyeColor()));
            tag.add(layerTag);
        }
        return tag;
    }

    protected StringTag convertBaseColor(final Integer value) {
        return convertDyeColor(value);
    }

    protected ListTag<StringTag> convertPotDecorations(final PotDecorations value) {
        final ListTag<StringTag> tag = new ListTag<>(StringTag.class);
        for (final int decoration : value.itemIds()) {
            final String item = Protocol1_20_5To1_20_3.MAPPINGS.getFullItemMappings().identifier(decoration);
            if (item == null) {
                throw new IllegalArgumentException("Unknown item: " + decoration);
            }
            tag.add(new StringTag(item));
        }
        return tag;
    }

    protected ListTag<CompoundTag> convertContainer(final Item[] value) {
        final ListTag<CompoundTag> tag = new ListTag<>(CompoundTag.class);
        final ListTag<CompoundTag> items = convertItemArray(value);
        for (int i = 0; i < items.size(); i++) {
            final CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("slot", i);
            itemTag.put("item", items.get(i));
            tag.add(itemTag);
        }
        return tag;
    }

    protected CompoundTag convertBlockState(final BlockStateProperties value) {
        final CompoundTag tag = new CompoundTag();
        for (final Map.Entry<String, String> entry : value.properties().entrySet()) {
            tag.putString(entry.getKey(), entry.getValue());
        }
        return tag;
    }

    protected ListTag<CompoundTag> convertBees(final Bee[] value) {
        final ListTag<CompoundTag> tag = new ListTag<>(CompoundTag.class);
        for (final Bee bee : value) {
            final CompoundTag beeTag = new CompoundTag();
            if (!bee.entityData().isEmpty()) {
                beeTag.put("entity_data", convertNbt(bee.entityData()));
            }
            beeTag.putInt("ticks_in_hive", bee.ticksInHive());
            beeTag.putInt("min_ticks_in_hive", bee.minTicksInHive());
        }
        return tag;
    }

    protected StringTag convertLock(final Tag value) {
        return (StringTag) value;
    }

    protected CompoundTag convertContainerLoot(final CompoundTag value) {
        return value; // Handled by the item rewriter
    }

    // ---------------------------------------------------------------------------------------

    protected void convertModifierData(final CompoundTag tag, final ModifierData data) {
        tag.put("uuid", new IntArrayTag(UUIDUtil.toIntArray(data.uuid())));
        tag.putString("name", data.name());
        tag.putDouble("amount", data.amount());
        tag.putString("operation", BlockItemPacketRewriter1_20_5.ATTRIBUTE_OPERATIONS[data.operation()]);
    }

    protected void convertPotionEffect(final CompoundTag tag, final PotionEffect effect) {
        final String id = PotionEffects1_20_5.idToKey(effect.effect());
        if (id == null) {
            throw new IllegalArgumentException("Unknown potion effect: " + effect.effect());
        }
        tag.putString("id", id);
        convertPotionEffectData(tag, effect.effectData());
    }

    protected void convertPotionEffectData(final CompoundTag tag, final PotionEffectData data) {
        if (data.amplifier() != 0) {
            tag.putInt("amplifier", data.amplifier());
        }
        if (data.duration() != 0) {
            tag.putInt("duration", data.duration());
        }
        if (data.ambient()) {
            tag.putBoolean("ambient", true);
        }
        if (!data.showParticles()) {
            tag.putBoolean("show_particles", false);
        }
        tag.putBoolean("show_icon", data.showIcon());
        if (data.hiddenEffect() != null) {
            final CompoundTag hiddenEffect = new CompoundTag();
            convertPotionEffectData(hiddenEffect, data.hiddenEffect());
            tag.put("hidden_effect", hiddenEffect);
        }
    }

    protected void convertHolderSet(final CompoundTag tag, final String name, HolderSet set) {
        if (set.hasTagKey()) {
            tag.putString(name, set.tagKey());
        } else {
            tag.put(name, new IntArrayTag(set.ids()));
        }
    }

    protected ListTag<CompoundTag> convertItemArray(final Item[] value) {
        final ListTag<CompoundTag> tag = new ListTag<>(CompoundTag.class);
        for (final Item item : value) {
            final CompoundTag itemTag = new CompoundTag();
            convertItem(itemTag, item);
            tag.add(itemTag);
        }
        return tag;
    }

    protected void convertItem(final CompoundTag tag, final Item item) {
        final String name = Protocol1_20_5To1_20_3.MAPPINGS.getFullItemMappings().identifier(item.identifier());
        if (name == null) {
            throw new IllegalArgumentException("Unknown item: " + item.identifier());
        }
        tag.putString("id", name);
        try {
            tag.put("count", convertPositiveInt(item.amount()));
        } catch (IllegalArgumentException ignored) { // Fallback value
            tag.putInt("count", 1);
        }
        final Map<StructuredDataKey<?>, StructuredData<?>> components = item.structuredData().data();
        tag.put("components", toTag(components, true));
    }

    protected void convertFilterableString(final CompoundTag tag, final FilterableString string, final int min, final int max) {
        tag.put("raw", convertString(string.raw(), min, max));
        if (string.filtered() != null) {
            tag.put("filtered", convertString(string.filtered(), min, max));
        }
    }

    protected void convertFilterableComponent(final CompoundTag tag, final FilterableComponent component) {
        tag.put("raw", convertComponent(component.raw()));
        if (component.filtered() != null) {
            tag.put("filtered", convertComponent(component.filtered()));
        }
    }

    protected void convertGlobalPos(final CompoundTag tag, final String name, final GlobalPosition position) {
        final CompoundTag posTag = new CompoundTag();
        posTag.putString("dimension", position.dimension());
        posTag.put("pos", new IntArrayTag(new int[]{position.x(), position.y(), position.z()}));
        tag.put(name, posTag);
    }

    protected void convertProperties(final CompoundTag tag, final String name, final GameProfile.Property[] properties) {
        final ListTag<CompoundTag> propertiesTag = new ListTag<>(CompoundTag.class);
        for (final GameProfile.Property property : properties) {
            final CompoundTag propertyTag = new CompoundTag();
            propertyTag.putString("name", property.name());
            propertyTag.putString("value", property.value());
            if (property.signature() != null) {
                propertyTag.putString("signature", property.signature());
            }
            propertiesTag.add(propertyTag);
        }
        tag.put(name, propertiesTag);
    }

    protected void convertBannerPattern(final CompoundTag tag, final String name, final Holder<BannerPattern> pattern) {
        if (pattern.hasId()) {
            tag.putString(name, BannerPatterns1_20_5.idToKey(pattern.id()));
            return;
        }

        final BannerPattern bannerPattern = pattern.value();
        final CompoundTag patternTag = new CompoundTag();
        patternTag.put("asset_id", convertIdentifier(bannerPattern.assetId()));
        patternTag.putString("translation_key", bannerPattern.translationKey());
        tag.put(name, patternTag);
    }

    // ---------------------------------------------------------------------------------------

    protected IntTag convertPositiveInt(final Integer value) {
        return convertIntRange(value, 1, Integer.MAX_VALUE);
    }

    protected IntTag convertNonNegativeInt(final Integer value) {
        return convertIntRange(value, 0, Integer.MAX_VALUE);
    }

    protected IntTag convertIntRange(final Integer value, final int min, final int max) {
        return new IntTag(checkIntRange(min, max, value));
    }

    protected FloatTag convertPositiveFloat(final Float value) {
        return convertFloatRange(value, 0, Float.MAX_VALUE);
    }

    protected FloatTag convertFloatRange(final Float value, final float min, final float max) {
        return new FloatTag(checkFloatRange(min, max, value));
    }

    protected StringTag convertString(final String value, final int min, final int max) {
        return new StringTag(checkStringRange(min, max, value));
    }

    protected ByteTag convertUnsignedByte(final byte value) {
        if (value > UnsignedBytes.MAX_VALUE) {
            throw new IllegalArgumentException("Value out of range: " + value);
        }
        return new ByteTag(value);
    }

    protected StringTag convertComponent(final Tag value) {
        return convertComponent(value, 0, Integer.MAX_VALUE);
    }

    protected StringTag convertComponent(final Tag value, final int min, final int max) {
        final String json = serializerVersion().toString(serializerVersion().toComponent(value));
        return new StringTag(checkStringRange(min, max, json));
    }

    protected ListTag<StringTag> convertComponents(final Tag[] value, final int maxLength) {
        checkIntRange(0, maxLength, value.length);
        final ListTag<StringTag> listTag = new ListTag<>(StringTag.class);
        for (final Tag tag : value) {
            final String json = serializerVersion().toString(serializerVersion().toComponent(tag));
            listTag.add(new StringTag(json));
        }
        return listTag;
    }

    protected StringTag convertEnumEntry(Integer value, final String... values) {
        Preconditions.checkArgument(value >= 0 && value < values.length, "Enum value out of range: " + value);
        return new StringTag(values[value]);
    }

    protected CompoundTag convertUnit() {
        return new CompoundTag();
    }

    protected CompoundTag convertNbt(final CompoundTag tag) {
        return tag;
    }

    protected CompoundTag convertNbtWithId(final CompoundTag tag) {
        if (tag.getStringTag("id") == null) {
            throw new IllegalArgumentException("Missing id tag in nbt: " + tag);
        }
        return tag;
    }

    protected StringTag convertIdentifier(final String value) {
        if (!Key.isValid(value)) {
            throw new IllegalArgumentException("Invalid identifier: " + value);
        }
        return new StringTag(value);
    }

    protected StringTag convertDyeColor(final Integer value) {
        return new StringTag(DyeColors.colorById(value));
    }

    // ---------------------------------------------------------------------------------------

    private int checkIntRange(final int min, final int max, final int value) {
        Preconditions.checkArgument(value >= min && value <= max, "Value out of range: " + value);
        return value;
    }

    private float checkFloatRange(final float min, final float max, final float value) {
        Preconditions.checkArgument(value >= min && value <= max, "Value out of range: " + value);
        return value;
    }

    private String checkStringRange(final int min, final int max, final String value) {
        final int length = value.length();
        Preconditions.checkArgument(length >= min && length <= max, "Value out of range: " + value);
        return value;
    }

    // ---------------------------------------------------------------------------------------

    private <T> void register(final StructuredDataKey<T> key, final DataConverter<T> converter) {
        rewriters.put(key, converter);
    }

    public SerializerVersion serializerVersion() {
        return SerializerVersion.V1_20_5;
    }

    @FunctionalInterface
    protected interface DataConverter<T> {

        Tag convert(T value);
    }
}