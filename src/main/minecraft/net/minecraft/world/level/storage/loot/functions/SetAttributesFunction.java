package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

public class SetAttributesFunction extends LootItemConditionalFunction {
   public static final Codec<SetAttributesFunction> CODEC = RecordCodecBuilder.create((p_297112_) -> {
      return commonFields(p_297112_).and(ExtraCodecs.nonEmptyList(SetAttributesFunction.Modifier.CODEC.listOf()).fieldOf("modifiers").forGetter((p_297111_) -> {
         return p_297111_.modifiers;
      })).apply(p_297112_, SetAttributesFunction::new);
   });
   private final List<SetAttributesFunction.Modifier> modifiers;

   SetAttributesFunction(List<LootItemCondition> p_80834_, List<SetAttributesFunction.Modifier> p_298826_) {
      super(p_80834_);
      this.modifiers = List.copyOf(p_298826_);
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_ATTRIBUTES;
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return this.modifiers.stream().flatMap((p_279080_) -> {
         return p_279080_.amount.getReferencedContextParams().stream();
      }).collect(ImmutableSet.toImmutableSet());
   }

   public ItemStack run(ItemStack pStack, LootContext pContext) {
      RandomSource randomsource = pContext.getRandom();

      for(SetAttributesFunction.Modifier setattributesfunction$modifier : this.modifiers) {
         UUID uuid = setattributesfunction$modifier.id.orElseGet(UUID::randomUUID);
         EquipmentSlot equipmentslot = Util.getRandom(setattributesfunction$modifier.slots, randomsource);
         pStack.addAttributeModifier(setattributesfunction$modifier.attribute.value(), new AttributeModifier(uuid, setattributesfunction$modifier.name, (double)setattributesfunction$modifier.amount.getFloat(pContext), setattributesfunction$modifier.operation), equipmentslot);
      }

      return pStack;
   }

   public static SetAttributesFunction.ModifierBuilder modifier(String pName, Holder<Attribute> pAttribute, AttributeModifier.Operation pOperation, NumberProvider pAmount) {
      return new SetAttributesFunction.ModifierBuilder(pName, pAttribute, pOperation, pAmount);
   }

   public static SetAttributesFunction.Builder setAttributes() {
      return new SetAttributesFunction.Builder();
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetAttributesFunction.Builder> {
      private final List<SetAttributesFunction.Modifier> modifiers = Lists.newArrayList();

      protected SetAttributesFunction.Builder getThis() {
         return this;
      }

      public SetAttributesFunction.Builder withModifier(SetAttributesFunction.ModifierBuilder pModifierBuilder) {
         this.modifiers.add(pModifierBuilder.build());
         return this;
      }

      public LootItemFunction build() {
         return new SetAttributesFunction(this.getConditions(), this.modifiers);
      }
   }

   static record Modifier(String name, Holder<Attribute> attribute, AttributeModifier.Operation operation, NumberProvider amount, List<EquipmentSlot> slots, Optional<UUID> id) {
      private static final Codec<List<EquipmentSlot>> SLOTS_CODEC = ExtraCodecs.nonEmptyList(Codec.either(EquipmentSlot.CODEC, EquipmentSlot.CODEC.listOf()).xmap((p_298787_) -> {
         return p_298787_.map(List::of, Function.identity());
      }, (p_301099_) -> {
         return p_301099_.size() == 1 ? Either.left(p_301099_.get(0)) : Either.right(p_301099_);
      }));
      public static final Codec<SetAttributesFunction.Modifier> CODEC = RecordCodecBuilder.create((p_299099_) -> {
         return p_299099_.group(Codec.STRING.fieldOf("name").forGetter(SetAttributesFunction.Modifier::name), BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(SetAttributesFunction.Modifier::attribute), AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(SetAttributesFunction.Modifier::operation), NumberProviders.CODEC.fieldOf("amount").forGetter(SetAttributesFunction.Modifier::amount), SLOTS_CODEC.fieldOf("slot").forGetter(SetAttributesFunction.Modifier::slots), ExtraCodecs.strictOptionalField(UUIDUtil.STRING_CODEC, "id").forGetter(SetAttributesFunction.Modifier::id)).apply(p_299099_, SetAttributesFunction.Modifier::new);
      });
   }

   public static class ModifierBuilder {
      private final String name;
      private final Holder<Attribute> attribute;
      private final AttributeModifier.Operation operation;
      private final NumberProvider amount;
      private Optional<UUID> id = Optional.empty();
      private final Set<EquipmentSlot> slots = EnumSet.noneOf(EquipmentSlot.class);

      public ModifierBuilder(String pName, Holder<Attribute> pAttribute, AttributeModifier.Operation pOperation, NumberProvider pAmount) {
         this.name = pName;
         this.attribute = pAttribute;
         this.operation = pOperation;
         this.amount = pAmount;
      }

      public SetAttributesFunction.ModifierBuilder forSlot(EquipmentSlot pSlot) {
         this.slots.add(pSlot);
         return this;
      }

      public SetAttributesFunction.ModifierBuilder withUuid(UUID pId) {
         this.id = Optional.of(pId);
         return this;
      }

      public SetAttributesFunction.Modifier build() {
         return new SetAttributesFunction.Modifier(this.name, this.attribute, this.operation, this.amount, List.copyOf(this.slots), this.id);
      }
   }
}