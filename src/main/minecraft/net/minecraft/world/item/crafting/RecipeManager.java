package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class RecipeManager extends SimpleJsonResourceReloadListener {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
   private static final Logger LOGGER = LogUtils.getLogger();
   private Map<RecipeType<?>, Map<ResourceLocation, RecipeHolder<?>>> recipes = ImmutableMap.of();
   private Map<ResourceLocation, RecipeHolder<?>> byName = ImmutableMap.of();
   private boolean hasErrors;

   public RecipeManager() {
      super(GSON, "recipes");
   }

   protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      this.hasErrors = false;
      Map<RecipeType<?>, ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>>> map = Maps.newHashMap();
      ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> builder = ImmutableMap.builder();

      for(Map.Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
         ResourceLocation resourcelocation = entry.getKey();

         try {
            RecipeHolder<?> recipeholder = fromJson(resourcelocation, GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
            map.computeIfAbsent(recipeholder.value().getType(), (p_44075_) -> {
               return ImmutableMap.builder();
            }).put(resourcelocation, recipeholder);
            builder.put(resourcelocation, recipeholder);
         } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
            LOGGER.error("Parsing error loading recipe {}", resourcelocation, jsonparseexception);
         }
      }

      this.recipes = map.entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, (p_44033_) -> {
         return p_44033_.getValue().build();
      }));
      this.byName = builder.build();
      LOGGER.info("Loaded {} recipes", (int)map.size());
   }

   public boolean hadErrorsLoading() {
      return this.hasErrors;
   }

   public <C extends Container, T extends Recipe<C>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> pRecipeType, C pInventory, Level pLevel) {
      return this.byType(pRecipeType).values().stream().filter((p_296918_) -> {
         return p_296918_.value().matches(pInventory, pLevel);
      }).findFirst();
   }

   public <C extends Container, T extends Recipe<C>> Optional<Pair<ResourceLocation, RecipeHolder<T>>> getRecipeFor(RecipeType<T> pRecipeType, C pInventory, Level pLevel, @Nullable ResourceLocation pLastRecipe) {
      Map<ResourceLocation, RecipeHolder<T>> map = this.byType(pRecipeType);
      if (pLastRecipe != null) {
         RecipeHolder<T> recipeholder = map.get(pLastRecipe);
         if (recipeholder != null && recipeholder.value().matches(pInventory, pLevel)) {
            return Optional.of(Pair.of(pLastRecipe, recipeholder));
         }
      }

      return map.entrySet().stream().filter((p_296906_) -> {
         return p_296906_.getValue().value().matches(pInventory, pLevel);
      }).findFirst().map((p_296909_) -> {
         return Pair.of(p_296909_.getKey(), p_296909_.getValue());
      });
   }

   public <C extends Container, T extends Recipe<C>> List<RecipeHolder<T>> getAllRecipesFor(RecipeType<T> pRecipeType) {
      return List.copyOf(this.byType(pRecipeType).values());
   }

   public <C extends Container, T extends Recipe<C>> List<RecipeHolder<T>> getRecipesFor(RecipeType<T> pRecipeType, C pInventory, Level pLevel) {
      return this.byType(pRecipeType).values().stream().filter((p_296912_) -> {
         return p_296912_.value().matches(pInventory, pLevel);
      }).sorted(Comparator.comparing((p_296908_) -> {
         return p_296908_.value().getResultItem(pLevel.registryAccess()).getDescriptionId();
      })).collect(Collectors.toList());
   }

   private <C extends Container, T extends Recipe<C>> Map<ResourceLocation, RecipeHolder<T>> byType(RecipeType<T> pRecipeType) {
      return (Map<ResourceLocation, RecipeHolder<T>>)(Map)this.recipes.getOrDefault(pRecipeType, Collections.emptyMap());
   }

   public <C extends Container, T extends Recipe<C>> NonNullList<ItemStack> getRemainingItemsFor(RecipeType<T> pRecipeType, C pInventory, Level pLevel) {
      Optional<RecipeHolder<T>> optional = this.getRecipeFor(pRecipeType, pInventory, pLevel);
      if (optional.isPresent()) {
         return optional.get().value().getRemainingItems(pInventory);
      } else {
         NonNullList<ItemStack> nonnulllist = NonNullList.withSize(pInventory.getContainerSize(), ItemStack.EMPTY);

         for(int i = 0; i < nonnulllist.size(); ++i) {
            nonnulllist.set(i, pInventory.getItem(i));
         }

         return nonnulllist;
      }
   }

   public Optional<RecipeHolder<?>> byKey(ResourceLocation pRecipeId) {
      return Optional.ofNullable(this.byName.get(pRecipeId));
   }

   public Collection<RecipeHolder<?>> getRecipes() {
      return this.recipes.values().stream().flatMap((p_220270_) -> {
         return p_220270_.values().stream();
      }).collect(Collectors.toSet());
   }

   public Stream<ResourceLocation> getRecipeIds() {
      return this.recipes.values().stream().flatMap((p_220258_) -> {
         return p_220258_.keySet().stream();
      });
   }

   protected static RecipeHolder<?> fromJson(ResourceLocation pRecipeId, JsonObject pJson) {
      Recipe<?> recipe = Util.getOrThrow(Recipe.CODEC.parse(JsonOps.INSTANCE, pJson), JsonParseException::new);
      return new RecipeHolder<>(pRecipeId, recipe);
   }

   public void replaceRecipes(Iterable<RecipeHolder<?>> pRecipes) {
      this.hasErrors = false;
      Map<RecipeType<?>, Map<ResourceLocation, RecipeHolder<?>>> map = Maps.newHashMap();
      ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> builder = ImmutableMap.builder();
      pRecipes.forEach((p_296915_) -> {
         Map<ResourceLocation, RecipeHolder<?>> map1 = map.computeIfAbsent(p_296915_.value().getType(), (p_220272_) -> {
            return Maps.newHashMap();
         });
         ResourceLocation resourcelocation = p_296915_.id();
         RecipeHolder<?> recipeholder = map1.put(resourcelocation, p_296915_);
         builder.put(resourcelocation, p_296915_);
         if (recipeholder != null) {
            throw new IllegalStateException("Duplicate recipe ignored with ID " + resourcelocation);
         }
      });
      this.recipes = ImmutableMap.copyOf(map);
      this.byName = builder.build();
   }

   public static <C extends Container, T extends Recipe<C>> RecipeManager.CachedCheck<C, T> createCheck(final RecipeType<T> pRecipeType) {
      return new RecipeManager.CachedCheck<C, T>() {
         @Nullable
         private ResourceLocation lastRecipe;

         public Optional<RecipeHolder<T>> getRecipeFor(C p_220278_, Level p_220279_) {
            RecipeManager recipemanager = p_220279_.getRecipeManager();
            Optional<Pair<ResourceLocation, RecipeHolder<T>>> optional = recipemanager.getRecipeFor(pRecipeType, p_220278_, p_220279_, this.lastRecipe);
            if (optional.isPresent()) {
               Pair<ResourceLocation, RecipeHolder<T>> pair = optional.get();
               this.lastRecipe = pair.getFirst();
               return Optional.of(pair.getSecond());
            } else {
               return Optional.empty();
            }
         }
      };
   }

   public interface CachedCheck<C extends Container, T extends Recipe<C>> {
      Optional<RecipeHolder<T>> getRecipeFor(C pContainer, Level pLevel);
   }
}