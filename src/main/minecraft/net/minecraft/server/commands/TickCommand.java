package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.util.TimeUtil;

public class TickCommand {
   private static final float MAX_TICKRATE = 10000.0F;
   private static final String DEFAULT_TICKRATE = String.valueOf((int)20);

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(Commands.literal("tick").requires((p_313254_) -> {
         return p_313254_.hasPermission(3);
      }).then(Commands.literal("query").executes((p_310022_) -> {
         return tickQuery(p_310022_.getSource());
      })).then(Commands.literal("rate").then(Commands.argument("rate", FloatArgumentType.floatArg(1.0F, 10000.0F)).suggests((p_311314_, p_311289_) -> {
         return SharedSuggestionProvider.suggest(new String[]{DEFAULT_TICKRATE}, p_311289_);
      }).executes((p_312329_) -> {
         return setTickingRate(p_312329_.getSource(), FloatArgumentType.getFloat(p_312329_, "rate"));
      }))).then(Commands.literal("step").executes((p_311036_) -> {
         return step(p_311036_.getSource(), 1);
      }).then(Commands.literal("stop").executes((p_309944_) -> {
         return stopStepping(p_309944_.getSource());
      })).then(Commands.argument("time", TimeArgument.time(1)).suggests((p_313203_, p_312907_) -> {
         return SharedSuggestionProvider.suggest(new String[]{"1t", "1s"}, p_312907_);
      }).executes((p_312113_) -> {
         return step(p_312113_.getSource(), IntegerArgumentType.getInteger(p_312113_, "time"));
      }))).then(Commands.literal("sprint").then(Commands.literal("stop").executes((p_311524_) -> {
         return stopSprinting(p_311524_.getSource());
      })).then(Commands.argument("time", TimeArgument.time(1)).suggests((p_311140_, p_312761_) -> {
         return SharedSuggestionProvider.suggest(new String[]{"60s", "1d", "3d"}, p_312761_);
      }).executes((p_311082_) -> {
         return sprint(p_311082_.getSource(), IntegerArgumentType.getInteger(p_311082_, "time"));
      }))).then(Commands.literal("unfreeze").executes((p_309501_) -> {
         return setFreeze(p_309501_.getSource(), false);
      })).then(Commands.literal("freeze").executes((p_312020_) -> {
         return setFreeze(p_312020_.getSource(), true);
      })));
   }

   private static String nanosToMilisString(long pNanos) {
      return String.format("%.1f", (float)pNanos / (float)TimeUtil.NANOSECONDS_PER_MILLISECOND);
   }

   private static int setTickingRate(CommandSourceStack pSource, float pTickRate) {
      ServerTickRateManager servertickratemanager = pSource.getServer().tickRateManager();
      servertickratemanager.setTickRate(pTickRate);
      String s = String.format("%.1f", pTickRate);
      pSource.sendSuccess(() -> {
         return Component.translatable("commands.tick.rate.success", s);
      }, true);
      return (int)pTickRate;
   }

   private static int tickQuery(CommandSourceStack pSource) {
      ServerTickRateManager servertickratemanager = pSource.getServer().tickRateManager();
      String s = nanosToMilisString(pSource.getServer().getAverageTickTimeNanos());
      float f = servertickratemanager.tickrate();
      String s1 = String.format("%.1f", f);
      if (servertickratemanager.isSprinting()) {
         pSource.sendSuccess(() -> {
            return Component.translatable("commands.tick.status.sprinting");
         }, false);
         pSource.sendSuccess(() -> {
            return Component.translatable("commands.tick.query.rate.sprinting", s1, s);
         }, false);
      } else {
         if (servertickratemanager.isFrozen()) {
            pSource.sendSuccess(() -> {
               return Component.translatable("commands.tick.status.frozen");
            }, false);
         } else if (servertickratemanager.nanosecondsPerTick() < pSource.getServer().getAverageTickTimeNanos()) {
            pSource.sendSuccess(() -> {
               return Component.translatable("commands.tick.status.lagging");
            }, false);
         } else {
            pSource.sendSuccess(() -> {
               return Component.translatable("commands.tick.status.running");
            }, false);
         }

         String s2 = nanosToMilisString(servertickratemanager.nanosecondsPerTick());
         pSource.sendSuccess(() -> {
            return Component.translatable("commands.tick.query.rate.running", s1, s, s2);
         }, false);
      }

      long[] along = Arrays.copyOf(pSource.getServer().getTickTimesNanos(), pSource.getServer().getTickTimesNanos().length);
      Arrays.sort(along);
      String s3 = nanosToMilisString(along[along.length / 2]);
      String s4 = nanosToMilisString(along[(int)((double)along.length * 0.95D)]);
      String s5 = nanosToMilisString(along[(int)((double)along.length * 0.99D)]);
      pSource.sendSuccess(() -> {
         return Component.translatable("commands.tick.query.percentiles", s3, s4, s5, along.length);
      }, false);
      return (int)f;
   }

   private static int sprint(CommandSourceStack pSource, int pSprintTime) {
      boolean flag = pSource.getServer().tickRateManager().requestGameToSprint(pSprintTime);
      if (flag) {
         pSource.sendSuccess(() -> {
            return Component.translatable("commands.tick.sprint.stop.success");
         }, true);
      }

      pSource.sendSuccess(() -> {
         return Component.translatable("commands.tick.status.sprinting");
      }, true);
      return 1;
   }

   private static int setFreeze(CommandSourceStack pSource, boolean pFrozen) {
      ServerTickRateManager servertickratemanager = pSource.getServer().tickRateManager();
      if (pFrozen) {
         if (servertickratemanager.isSprinting()) {
            servertickratemanager.stopSprinting();
         }

         if (servertickratemanager.isSteppingForward()) {
            servertickratemanager.stopStepping();
         }
      }

      servertickratemanager.setFrozen(pFrozen);
      if (pFrozen) {
         pSource.sendSuccess(() -> {
            return Component.translatable("commands.tick.status.frozen");
         }, true);
      } else {
         pSource.sendSuccess(() -> {
            return Component.translatable("commands.tick.status.running");
         }, true);
      }

      return pFrozen ? 1 : 0;
   }

   private static int step(CommandSourceStack pSource, int pTicks) {
      ServerTickRateManager servertickratemanager = pSource.getServer().tickRateManager();
      boolean flag = servertickratemanager.stepGameIfPaused(pTicks);
      if (flag) {
         pSource.sendSuccess(() -> {
            return Component.translatable("commands.tick.step.success", pTicks);
         }, true);
      } else {
         pSource.sendFailure(Component.translatable("commands.tick.step.fail"));
      }

      return 1;
   }

   private static int stopStepping(CommandSourceStack pSource) {
      ServerTickRateManager servertickratemanager = pSource.getServer().tickRateManager();
      boolean flag = servertickratemanager.stopStepping();
      if (flag) {
         pSource.sendSuccess(() -> {
            return Component.translatable("commands.tick.step.stop.success");
         }, true);
         return 1;
      } else {
         pSource.sendFailure(Component.translatable("commands.tick.step.stop.fail"));
         return 0;
      }
   }

   private static int stopSprinting(CommandSourceStack pSource) {
      ServerTickRateManager servertickratemanager = pSource.getServer().tickRateManager();
      boolean flag = servertickratemanager.stopSprinting();
      if (flag) {
         pSource.sendSuccess(() -> {
            return Component.translatable("commands.tick.sprint.stop.success");
         }, true);
         return 1;
      } else {
         pSource.sendFailure(Component.translatable("commands.tick.sprint.stop.fail"));
         return 0;
      }
   }
}