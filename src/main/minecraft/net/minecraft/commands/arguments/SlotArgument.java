package net.minecraft.commands.arguments;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;

public class SlotArgument implements ArgumentType<Integer> {
   private static final Collection<String> EXAMPLES = Arrays.asList("container.5", "12", "weapon");
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_SLOT = new DynamicCommandExceptionType((p_308386_) -> {
      return Component.translatableEscape("slot.unknown", p_308386_);
   });
   private static final Map<String, Integer> SLOTS = Util.make(Maps.newHashMap(), (p_111285_) -> {
      for(int i = 0; i < 54; ++i) {
         p_111285_.put("container." + i, i);
      }

      for(int j = 0; j < 9; ++j) {
         p_111285_.put("hotbar." + j, j);
      }

      for(int k = 0; k < 27; ++k) {
         p_111285_.put("inventory." + k, 9 + k);
      }

      for(int l = 0; l < 27; ++l) {
         p_111285_.put("enderchest." + l, 200 + l);
      }

      for(int i1 = 0; i1 < 8; ++i1) {
         p_111285_.put("villager." + i1, 300 + i1);
      }

      for(int j1 = 0; j1 < 15; ++j1) {
         p_111285_.put("horse." + j1, 500 + j1);
      }

      p_111285_.put("weapon", EquipmentSlot.MAINHAND.getIndex(98));
      p_111285_.put("weapon.mainhand", EquipmentSlot.MAINHAND.getIndex(98));
      p_111285_.put("weapon.offhand", EquipmentSlot.OFFHAND.getIndex(98));
      p_111285_.put("armor.head", EquipmentSlot.HEAD.getIndex(100));
      p_111285_.put("armor.chest", EquipmentSlot.CHEST.getIndex(100));
      p_111285_.put("armor.legs", EquipmentSlot.LEGS.getIndex(100));
      p_111285_.put("armor.feet", EquipmentSlot.FEET.getIndex(100));
      p_111285_.put("horse.saddle", 400);
      p_111285_.put("horse.armor", 401);
      p_111285_.put("horse.chest", 499);
   });

   public static SlotArgument slot() {
      return new SlotArgument();
   }

   public static int getSlot(CommandContext<CommandSourceStack> pContext, String pName) {
      return pContext.getArgument(pName, Integer.class);
   }

   public Integer parse(StringReader pReader) throws CommandSyntaxException {
      String s = pReader.readUnquotedString();
      if (!SLOTS.containsKey(s)) {
         throw ERROR_UNKNOWN_SLOT.create(s);
      } else {
         return SLOTS.get(s);
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      return SharedSuggestionProvider.suggest(SLOTS.keySet(), pBuilder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}