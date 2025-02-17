package net.minecraft.advancements.critereon;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public record PlayerPredicate(MinMaxBounds.Ints level, Optional<GameType> gameType, List<PlayerPredicate.StatMatcher<?>> stats, Object2BooleanMap<ResourceLocation> recipes, Map<ResourceLocation, PlayerPredicate.AdvancementPredicate> advancements, Optional<EntityPredicate> lookingAt) implements EntitySubPredicate {
   public static final int LOOKING_AT_RANGE = 100;
   public static final MapCodec<PlayerPredicate> CODEC = RecordCodecBuilder.mapCodec((p_296141_) -> {
      return p_296141_.group(ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "level", MinMaxBounds.Ints.ANY).forGetter(PlayerPredicate::level), GameType.CODEC.optionalFieldOf("gamemode").forGetter(PlayerPredicate::gameType), ExtraCodecs.strictOptionalField(PlayerPredicate.StatMatcher.CODEC.listOf(), "stats", List.of()).forGetter(PlayerPredicate::stats), ExtraCodecs.strictOptionalField(ExtraCodecs.object2BooleanMap(ResourceLocation.CODEC), "recipes", Object2BooleanMaps.emptyMap()).forGetter(PlayerPredicate::recipes), ExtraCodecs.strictOptionalField(Codec.unboundedMap(ResourceLocation.CODEC, PlayerPredicate.AdvancementPredicate.CODEC), "advancements", Map.of()).forGetter(PlayerPredicate::advancements), ExtraCodecs.strictOptionalField(EntityPredicate.CODEC, "looking_at").forGetter(PlayerPredicate::lookingAt)).apply(p_296141_, PlayerPredicate::new);
   });

   public boolean matches(Entity pEntity, ServerLevel pLevel, @Nullable Vec3 pPosition) {
      if (!(pEntity instanceof ServerPlayer serverplayer)) {
         return false;
      } else if (!this.level.matches(serverplayer.experienceLevel)) {
         return false;
      } else if (this.gameType.isPresent() && this.gameType.get() != serverplayer.gameMode.getGameModeForPlayer()) {
         return false;
      } else {
         StatsCounter statscounter = serverplayer.getStats();

         for(PlayerPredicate.StatMatcher<?> statmatcher : this.stats) {
            if (!statmatcher.matches(statscounter)) {
               return false;
            }
         }

         RecipeBook recipebook = serverplayer.getRecipeBook();

         for(Object2BooleanMap.Entry<ResourceLocation> entry : this.recipes.object2BooleanEntrySet()) {
            if (recipebook.contains(entry.getKey()) != entry.getBooleanValue()) {
               return false;
            }
         }

         if (!this.advancements.isEmpty()) {
            PlayerAdvancements playeradvancements = serverplayer.getAdvancements();
            ServerAdvancementManager serveradvancementmanager = serverplayer.getServer().getAdvancements();

            for(Map.Entry<ResourceLocation, PlayerPredicate.AdvancementPredicate> entry1 : this.advancements.entrySet()) {
               AdvancementHolder advancementholder = serveradvancementmanager.get(entry1.getKey());
               if (advancementholder == null || !entry1.getValue().test(playeradvancements.getOrStartProgress(advancementholder))) {
                  return false;
               }
            }
         }

         if (this.lookingAt.isPresent()) {
            Vec3 vec3 = serverplayer.getEyePosition();
            Vec3 vec31 = serverplayer.getViewVector(1.0F);
            Vec3 vec32 = vec3.add(vec31.x * 100.0D, vec31.y * 100.0D, vec31.z * 100.0D);
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(serverplayer.level(), serverplayer, vec3, vec32, (new AABB(vec3, vec32)).inflate(1.0D), (p_156765_) -> {
               return !p_156765_.isSpectator();
            }, 0.0F);
            if (entityhitresult == null || entityhitresult.getType() != HitResult.Type.ENTITY) {
               return false;
            }

            Entity entity = entityhitresult.getEntity();
            if (!this.lookingAt.get().matches(serverplayer, entity) || !serverplayer.hasLineOfSight(entity)) {
               return false;
            }
         }

         return true;
      }
   }

   public EntitySubPredicate.Type type() {
      return EntitySubPredicate.Types.PLAYER;
   }

   static record AdvancementCriterionsPredicate(Object2BooleanMap<String> criterions) implements PlayerPredicate.AdvancementPredicate {
      public static final Codec<PlayerPredicate.AdvancementCriterionsPredicate> CODEC = ExtraCodecs.object2BooleanMap(Codec.STRING).xmap(PlayerPredicate.AdvancementCriterionsPredicate::new, PlayerPredicate.AdvancementCriterionsPredicate::criterions);

      public boolean test(AdvancementProgress pProgress) {
         for(Object2BooleanMap.Entry<String> entry : this.criterions.object2BooleanEntrySet()) {
            CriterionProgress criterionprogress = pProgress.getCriterion(entry.getKey());
            if (criterionprogress == null || criterionprogress.isDone() != entry.getBooleanValue()) {
               return false;
            }
         }

         return true;
      }
   }

   static record AdvancementDonePredicate(boolean state) implements PlayerPredicate.AdvancementPredicate {
      public static final Codec<PlayerPredicate.AdvancementDonePredicate> CODEC = Codec.BOOL.xmap(PlayerPredicate.AdvancementDonePredicate::new, PlayerPredicate.AdvancementDonePredicate::state);

      public boolean test(AdvancementProgress pProgress) {
         return pProgress.isDone() == this.state;
      }
   }

   interface AdvancementPredicate extends Predicate<AdvancementProgress> {
      Codec<PlayerPredicate.AdvancementPredicate> CODEC = Codec.either(PlayerPredicate.AdvancementDonePredicate.CODEC, PlayerPredicate.AdvancementCriterionsPredicate.CODEC).xmap((p_300258_) -> {
         return p_300258_.map((p_301189_) -> {
            return p_301189_;
         }, (p_300077_) -> {
            return p_300077_;
         });
      }, (p_298131_) -> {
         if (p_298131_ instanceof PlayerPredicate.AdvancementDonePredicate playerpredicate$advancementdonepredicate) {
            return Either.left(playerpredicate$advancementdonepredicate);
         } else if (p_298131_ instanceof PlayerPredicate.AdvancementCriterionsPredicate playerpredicate$advancementcriterionspredicate) {
            return Either.right(playerpredicate$advancementcriterionspredicate);
         } else {
            throw new UnsupportedOperationException();
         }
      });
   }

   public static class Builder {
      private MinMaxBounds.Ints level = MinMaxBounds.Ints.ANY;
      private Optional<GameType> gameType = Optional.empty();
      private final ImmutableList.Builder<PlayerPredicate.StatMatcher<?>> stats = ImmutableList.builder();
      private final Object2BooleanMap<ResourceLocation> recipes = new Object2BooleanOpenHashMap<>();
      private final Map<ResourceLocation, PlayerPredicate.AdvancementPredicate> advancements = Maps.newHashMap();
      private Optional<EntityPredicate> lookingAt = Optional.empty();

      public static PlayerPredicate.Builder player() {
         return new PlayerPredicate.Builder();
      }

      public PlayerPredicate.Builder setLevel(MinMaxBounds.Ints pLevel) {
         this.level = pLevel;
         return this;
      }

      public <T> PlayerPredicate.Builder addStat(StatType<T> pType, Holder.Reference<T> pValue, MinMaxBounds.Ints pRange) {
         this.stats.add(new PlayerPredicate.StatMatcher<>(pType, pValue, pRange));
         return this;
      }

      public PlayerPredicate.Builder addRecipe(ResourceLocation pRecipe, boolean pUnlocked) {
         this.recipes.put(pRecipe, pUnlocked);
         return this;
      }

      public PlayerPredicate.Builder setGameType(GameType pGameType) {
         this.gameType = Optional.of(pGameType);
         return this;
      }

      public PlayerPredicate.Builder setLookingAt(EntityPredicate.Builder pLookingAt) {
         this.lookingAt = Optional.of(pLookingAt.build());
         return this;
      }

      public PlayerPredicate.Builder checkAdvancementDone(ResourceLocation pAdvancement, boolean pDone) {
         this.advancements.put(pAdvancement, new PlayerPredicate.AdvancementDonePredicate(pDone));
         return this;
      }

      public PlayerPredicate.Builder checkAdvancementCriterions(ResourceLocation pAdvancement, Map<String, Boolean> pCriterions) {
         this.advancements.put(pAdvancement, new PlayerPredicate.AdvancementCriterionsPredicate(new Object2BooleanOpenHashMap<>(pCriterions)));
         return this;
      }

      public PlayerPredicate build() {
         return new PlayerPredicate(this.level, this.gameType, this.stats.build(), this.recipes, this.advancements, this.lookingAt);
      }
   }

   static record StatMatcher<T>(StatType<T> type, Holder<T> value, MinMaxBounds.Ints range, Supplier<Stat<T>> stat) {
      public static final Codec<PlayerPredicate.StatMatcher<?>> CODEC = BuiltInRegistries.STAT_TYPE.byNameCodec().dispatch(PlayerPredicate.StatMatcher::type, PlayerPredicate.StatMatcher::createTypedCodec);

      public StatMatcher(StatType<T> pType, Holder<T> pValue, MinMaxBounds.Ints pRange) {
         this(pType, pValue, pRange, Suppliers.memoize(() -> {
            return pType.get(pValue.value());
         }));
      }

      private static <T> Codec<PlayerPredicate.StatMatcher<T>> createTypedCodec(StatType<T> p_297243_) {
         return RecordCodecBuilder.create((p_298561_) -> {
            return p_298561_.group(p_297243_.getRegistry().holderByNameCodec().fieldOf("stat").forGetter(PlayerPredicate.StatMatcher::value), ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "value", MinMaxBounds.Ints.ANY).forGetter(PlayerPredicate.StatMatcher::range)).apply(p_298561_, (p_301267_, p_297932_) -> {
               return new PlayerPredicate.StatMatcher<>(p_297243_, p_301267_, p_297932_);
            });
         });
      }

      public boolean matches(StatsCounter pStatsCounter) {
         return this.range.matches(pStatsCounter.getValue(this.stat.get()));
      }
   }
}