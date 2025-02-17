package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ObjectiveArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class TriggerCommand {
   private static final SimpleCommandExceptionType ERROR_NOT_PRIMED = new SimpleCommandExceptionType(Component.translatable("commands.trigger.failed.unprimed"));
   private static final SimpleCommandExceptionType ERROR_INVALID_OBJECTIVE = new SimpleCommandExceptionType(Component.translatable("commands.trigger.failed.invalid"));

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(Commands.literal("trigger").then(Commands.argument("objective", ObjectiveArgument.objective()).suggests((p_139146_, p_139147_) -> {
         return suggestObjectives(p_139146_.getSource(), p_139147_);
      }).executes((p_308912_) -> {
         return simpleTrigger(p_308912_.getSource(), p_308912_.getSource().getPlayerOrException(), ObjectiveArgument.getObjective(p_308912_, "objective"));
      }).then(Commands.literal("add").then(Commands.argument("value", IntegerArgumentType.integer()).executes((p_308911_) -> {
         return addValue(p_308911_.getSource(), p_308911_.getSource().getPlayerOrException(), ObjectiveArgument.getObjective(p_308911_, "objective"), IntegerArgumentType.getInteger(p_308911_, "value"));
      }))).then(Commands.literal("set").then(Commands.argument("value", IntegerArgumentType.integer()).executes((p_308913_) -> {
         return setValue(p_308913_.getSource(), p_308913_.getSource().getPlayerOrException(), ObjectiveArgument.getObjective(p_308913_, "objective"), IntegerArgumentType.getInteger(p_308913_, "value"));
      })))));
   }

   public static CompletableFuture<Suggestions> suggestObjectives(CommandSourceStack pSource, SuggestionsBuilder pBuilder) {
      ScoreHolder scoreholder = pSource.getEntity();
      List<String> list = Lists.newArrayList();
      if (scoreholder != null) {
         Scoreboard scoreboard = pSource.getServer().getScoreboard();

         for(Objective objective : scoreboard.getObjectives()) {
            if (objective.getCriteria() == ObjectiveCriteria.TRIGGER) {
               ReadOnlyScoreInfo readonlyscoreinfo = scoreboard.getPlayerScoreInfo(scoreholder, objective);
               if (readonlyscoreinfo != null && !readonlyscoreinfo.isLocked()) {
                  list.add(objective.getName());
               }
            }
         }
      }

      return SharedSuggestionProvider.suggest(list, pBuilder);
   }

   private static int addValue(CommandSourceStack pSource, ServerPlayer pPlayer, Objective pObjective, int pValue) throws CommandSyntaxException {
      ScoreAccess scoreaccess = getScore(pSource.getServer().getScoreboard(), pPlayer, pObjective);
      int i = scoreaccess.add(pValue);
      pSource.sendSuccess(() -> {
         return Component.translatable("commands.trigger.add.success", pObjective.getFormattedDisplayName(), pValue);
      }, true);
      return i;
   }

   private static int setValue(CommandSourceStack pSource, ServerPlayer pPlayer, Objective pObjective, int pValue) throws CommandSyntaxException {
      ScoreAccess scoreaccess = getScore(pSource.getServer().getScoreboard(), pPlayer, pObjective);
      scoreaccess.set(pValue);
      pSource.sendSuccess(() -> {
         return Component.translatable("commands.trigger.set.success", pObjective.getFormattedDisplayName(), pValue);
      }, true);
      return pValue;
   }

   private static int simpleTrigger(CommandSourceStack pSource, ServerPlayer pPlayer, Objective pObjective) throws CommandSyntaxException {
      ScoreAccess scoreaccess = getScore(pSource.getServer().getScoreboard(), pPlayer, pObjective);
      int i = scoreaccess.add(1);
      pSource.sendSuccess(() -> {
         return Component.translatable("commands.trigger.simple.success", pObjective.getFormattedDisplayName());
      }, true);
      return i;
   }

   private static ScoreAccess getScore(Scoreboard pScoreboard, ScoreHolder pScoreHolder, Objective pObjective) throws CommandSyntaxException {
      if (pObjective.getCriteria() != ObjectiveCriteria.TRIGGER) {
         throw ERROR_INVALID_OBJECTIVE.create();
      } else {
         ReadOnlyScoreInfo readonlyscoreinfo = pScoreboard.getPlayerScoreInfo(pScoreHolder, pObjective);
         if (readonlyscoreinfo != null && !readonlyscoreinfo.isLocked()) {
            ScoreAccess scoreaccess = pScoreboard.getOrCreatePlayerScore(pScoreHolder, pObjective);
            scoreaccess.lock();
            return scoreaccess;
         } else {
            throw ERROR_NOT_PRIMED.create();
         }
      }
   }
}