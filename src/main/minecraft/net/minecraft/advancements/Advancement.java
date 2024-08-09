package net.minecraft.advancements;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.CriterionValidator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootDataResolver;

public record Advancement(Optional<ResourceLocation> parent, Optional<DisplayInfo> display, AdvancementRewards rewards, Map<String, Criterion<?>> criteria, AdvancementRequirements requirements, boolean sendsTelemetryEvent, Optional<Component> name) {
   private static final Codec<Map<String, Criterion<?>>> CRITERIA_CODEC = ExtraCodecs.validate(Codec.unboundedMap(Codec.STRING, Criterion.CODEC), (p_308091_) -> {
      return p_308091_.isEmpty() ? DataResult.error(() -> {
         return "Advancement criteria cannot be empty";
      }) : DataResult.success(p_308091_);
   });
   public static final Codec<Advancement> CODEC = ExtraCodecs.validate(RecordCodecBuilder.create((p_308092_) -> {
      return p_308092_.group(ExtraCodecs.strictOptionalField(ResourceLocation.CODEC, "parent").forGetter(Advancement::parent), ExtraCodecs.strictOptionalField(DisplayInfo.CODEC, "display").forGetter(Advancement::display), ExtraCodecs.strictOptionalField(AdvancementRewards.CODEC, "rewards", AdvancementRewards.EMPTY).forGetter(Advancement::rewards), CRITERIA_CODEC.fieldOf("criteria").forGetter(Advancement::criteria), ExtraCodecs.strictOptionalField(AdvancementRequirements.CODEC, "requirements").forGetter((p_308099_) -> {
         return Optional.of(p_308099_.requirements());
      }), ExtraCodecs.strictOptionalField(Codec.BOOL, "sends_telemetry_event", false).forGetter(Advancement::sendsTelemetryEvent)).apply(p_308092_, (p_308085_, p_308086_, p_308087_, p_308088_, p_308089_, p_308090_) -> {
         AdvancementRequirements advancementrequirements = p_308089_.orElseGet(() -> {
            return AdvancementRequirements.allOf(p_308088_.keySet());
         });
         return new Advancement(p_308085_, p_308086_, p_308087_, p_308088_, advancementrequirements, p_308090_);
      });
   }), Advancement::validate);

   public Advancement(Optional<ResourceLocation> pParent, Optional<DisplayInfo> pDisplay, AdvancementRewards pRewards, Map<String, Criterion<?>> pCriteria, AdvancementRequirements pRequirements, boolean pSendsTelemetryEvent) {
      this(pParent, pDisplay, pRewards, Map.copyOf(pCriteria), pRequirements, pSendsTelemetryEvent, pDisplay.map(Advancement::decorateName));
   }

   private static DataResult<Advancement> validate(Advancement p_312373_) {
      return p_312373_.requirements().validate(p_312373_.criteria().keySet()).map((p_308094_) -> {
         return p_312373_;
      });
   }

   private static Component decorateName(DisplayInfo p_300038_) {
      Component component = p_300038_.getTitle();
      ChatFormatting chatformatting = p_300038_.getType().getChatColor();
      Component component1 = ComponentUtils.mergeStyles(component.copy(), Style.EMPTY.withColor(chatformatting)).append("\n").append(p_300038_.getDescription());
      Component component2 = component.copy().withStyle((p_138316_) -> {
         return p_138316_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component1));
      });
      return ComponentUtils.wrapInSquareBrackets(component2).withStyle(chatformatting);
   }

   public static Component name(AdvancementHolder pAdvancement) {
      return pAdvancement.value().name().orElseGet(() -> {
         return Component.literal(pAdvancement.id().toString());
      });
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeOptional(this.parent, FriendlyByteBuf::writeResourceLocation);
      pBuffer.writeOptional(this.display, (p_296098_, p_296099_) -> {
         p_296099_.serializeToNetwork(p_296098_);
      });
      this.requirements.write(pBuffer);
      pBuffer.writeBoolean(this.sendsTelemetryEvent);
   }

   public static Advancement read(FriendlyByteBuf pBuffer) {
      return new Advancement(pBuffer.readOptional(FriendlyByteBuf::readResourceLocation), pBuffer.readOptional(DisplayInfo::fromNetwork), AdvancementRewards.EMPTY, Map.of(), new AdvancementRequirements(pBuffer), pBuffer.readBoolean());
   }

   public boolean isRoot() {
      return this.parent.isEmpty();
   }

   public void validate(ProblemReporter pProblemReporter, LootDataResolver pLootDataResolver) {
      this.criteria.forEach((p_308097_, p_308098_) -> {
         CriterionValidator criterionvalidator = new CriterionValidator(pProblemReporter.forChild(p_308097_), pLootDataResolver);
         p_308098_.triggerInstance().validate(criterionvalidator);
      });
   }

   public static class Builder {
      private Optional<ResourceLocation> parent = Optional.empty();
      private Optional<DisplayInfo> display = Optional.empty();
      private AdvancementRewards rewards = AdvancementRewards.EMPTY;
      private final ImmutableMap.Builder<String, Criterion<?>> criteria = ImmutableMap.builder();
      private Optional<AdvancementRequirements> requirements = Optional.empty();
      private AdvancementRequirements.Strategy requirementsStrategy = AdvancementRequirements.Strategy.AND;
      private boolean sendsTelemetryEvent;

      public static Advancement.Builder advancement() {
         return (new Advancement.Builder()).sendsTelemetryEvent();
      }

      public static Advancement.Builder recipeAdvancement() {
         return new Advancement.Builder();
      }

      public Advancement.Builder parent(AdvancementHolder pParent) {
         this.parent = Optional.of(pParent.id());
         return this;
      }

      /** @deprecated */
      @Deprecated(
         forRemoval = true
      )
      public Advancement.Builder parent(ResourceLocation pParentId) {
         this.parent = Optional.of(pParentId);
         return this;
      }

      public Advancement.Builder display(ItemStack pIcon, Component pTitle, Component pDescription, @Nullable ResourceLocation pBackground, AdvancementType pType, boolean pShowToast, boolean pAnnounceChat, boolean pHidden) {
         return this.display(new DisplayInfo(pIcon, pTitle, pDescription, Optional.ofNullable(pBackground), pType, pShowToast, pAnnounceChat, pHidden));
      }

      public Advancement.Builder display(ItemLike pIcon, Component pTitle, Component pDescription, @Nullable ResourceLocation pBackground, AdvancementType pType, boolean pShowToast, boolean pAnnounceChat, boolean pHidden) {
         return this.display(new DisplayInfo(new ItemStack(pIcon.asItem()), pTitle, pDescription, Optional.ofNullable(pBackground), pType, pShowToast, pAnnounceChat, pHidden));
      }

      public Advancement.Builder display(DisplayInfo pDisplay) {
         this.display = Optional.of(pDisplay);
         return this;
      }

      public Advancement.Builder rewards(AdvancementRewards.Builder pRewardsBuilder) {
         return this.rewards(pRewardsBuilder.build());
      }

      public Advancement.Builder rewards(AdvancementRewards pRewards) {
         this.rewards = pRewards;
         return this;
      }

      public Advancement.Builder addCriterion(String pKey, Criterion<?> pCriterion) {
         this.criteria.put(pKey, pCriterion);
         return this;
      }

      public Advancement.Builder requirements(AdvancementRequirements.Strategy pRequirementsStrategy) {
         this.requirementsStrategy = pRequirementsStrategy;
         return this;
      }

      public Advancement.Builder requirements(AdvancementRequirements pRequirements) {
         this.requirements = Optional.of(pRequirements);
         return this;
      }

      public Advancement.Builder sendsTelemetryEvent() {
         this.sendsTelemetryEvent = true;
         return this;
      }

      public AdvancementHolder build(ResourceLocation pId) {
         Map<String, Criterion<?>> map = this.criteria.buildOrThrow();
         AdvancementRequirements advancementrequirements = this.requirements.orElseGet(() -> {
            return this.requirementsStrategy.create(map.keySet());
         });
         return new AdvancementHolder(pId, new Advancement(this.parent, this.display, this.rewards, map, advancementrequirements, this.sendsTelemetryEvent));
      }

      public AdvancementHolder save(Consumer<AdvancementHolder> pOutput, String pId) {
         AdvancementHolder advancementholder = this.build(new ResourceLocation(pId));
         pOutput.accept(advancementholder);
         return advancementholder;
      }
   }
}