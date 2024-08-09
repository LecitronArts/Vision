package net.minecraft.world.level.storage.loot;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import org.slf4j.Logger;

public class LootDataType<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final LootDataType<LootItemCondition> PREDICATE = new LootDataType<>(LootItemConditions.CODEC, "predicates", createSimpleValidator());
   public static final LootDataType<LootItemFunction> MODIFIER = new LootDataType<>(LootItemFunctions.CODEC, "item_modifiers", createSimpleValidator());
   public static final LootDataType<LootTable> TABLE = new LootDataType<>(LootTable.CODEC, "loot_tables", createLootTableValidator());
   private final Codec<T> codec;
   private final String directory;
   private final LootDataType.Validator<T> validator;

   private LootDataType(Codec<T> pCodec, String pDirectory, LootDataType.Validator<T> pValidator) {
      this.codec = pCodec;
      this.directory = pDirectory;
      this.validator = pValidator;
   }

   public String directory() {
      return this.directory;
   }

   public void runValidation(ValidationContext pContext, LootDataId<T> pId, T pElement) {
      this.validator.run(pContext, pId, pElement);
   }

   public Optional<T> deserialize(ResourceLocation pLocation, JsonElement pJson) {
      DataResult<T> dataresult = this.codec.parse(JsonOps.INSTANCE, pJson);
      dataresult.error().ifPresent((p_297003_) -> {
         LOGGER.error("Couldn't parse element {}:{} - {}", this.directory, pLocation, p_297003_.message());
      });
      return dataresult.result();
   }

   public static Stream<LootDataType<?>> values() {
      return Stream.of(PREDICATE, MODIFIER, TABLE);
   }

   private static <T extends LootContextUser> LootDataType.Validator<T> createSimpleValidator() {
      return (p_279353_, p_279374_, p_279097_) -> {
         p_279097_.validate(p_279353_.enterElement("{" + p_279374_.type().directory + ":" + p_279374_.location() + "}", p_279374_));
      };
   }

   private static LootDataType.Validator<LootTable> createLootTableValidator() {
      return (p_279333_, p_279227_, p_279406_) -> {
         p_279406_.validate(p_279333_.setParams(p_279406_.getParamSet()).enterElement("{" + p_279227_.type().directory + ":" + p_279227_.location() + "}", p_279227_));
      };
   }

   @FunctionalInterface
   public interface Validator<T> {
      void run(ValidationContext pContext, LootDataId<T> pId, T pElement);
   }
}