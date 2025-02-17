package net.minecraft.server.commands;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.chase.ChaseClient;
import net.minecraft.server.chase.ChaseServer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class ChaseCommand {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String DEFAULT_CONNECT_HOST = "localhost";
   private static final String DEFAULT_BIND_ADDRESS = "0.0.0.0";
   private static final int DEFAULT_PORT = 10000;
   private static final int BROADCAST_INTERVAL_MS = 100;
   public static BiMap<String, ResourceKey<Level>> DIMENSION_NAMES = ImmutableBiMap.of("o", Level.OVERWORLD, "n", Level.NETHER, "e", Level.END);
   @Nullable
   private static ChaseServer chaseServer;
   @Nullable
   private static ChaseClient chaseClient;

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      pDispatcher.register(Commands.literal("chase").then(Commands.literal("follow").then(Commands.argument("host", StringArgumentType.string()).executes((p_196104_) -> {
         return follow(p_196104_.getSource(), StringArgumentType.getString(p_196104_, "host"), 10000);
      }).then(Commands.argument("port", IntegerArgumentType.integer(1, 65535)).executes((p_196102_) -> {
         return follow(p_196102_.getSource(), StringArgumentType.getString(p_196102_, "host"), IntegerArgumentType.getInteger(p_196102_, "port"));
      }))).executes((p_196100_) -> {
         return follow(p_196100_.getSource(), "localhost", 10000);
      })).then(Commands.literal("lead").then(Commands.argument("bind_address", StringArgumentType.string()).executes((p_196098_) -> {
         return lead(p_196098_.getSource(), StringArgumentType.getString(p_196098_, "bind_address"), 10000);
      }).then(Commands.argument("port", IntegerArgumentType.integer(1024, 65535)).executes((p_196096_) -> {
         return lead(p_196096_.getSource(), StringArgumentType.getString(p_196096_, "bind_address"), IntegerArgumentType.getInteger(p_196096_, "port"));
      }))).executes((p_196088_) -> {
         return lead(p_196088_.getSource(), "0.0.0.0", 10000);
      })).then(Commands.literal("stop").executes((p_196080_) -> {
         return stop(p_196080_.getSource());
      })));
   }

   private static int stop(CommandSourceStack pSource) {
      if (chaseClient != null) {
         chaseClient.stop();
         pSource.sendSuccess(() -> {
            return Component.literal("You have now stopped chasing");
         }, false);
         chaseClient = null;
      }

      if (chaseServer != null) {
         chaseServer.stop();
         pSource.sendSuccess(() -> {
            return Component.literal("You are no longer being chased");
         }, false);
         chaseServer = null;
      }

      return 0;
   }

   private static boolean alreadyRunning(CommandSourceStack pSource) {
      if (chaseServer != null) {
         pSource.sendFailure(Component.literal("Chase server is already running. Stop it using /chase stop"));
         return true;
      } else if (chaseClient != null) {
         pSource.sendFailure(Component.literal("You are already chasing someone. Stop it using /chase stop"));
         return true;
      } else {
         return false;
      }
   }

   private static int lead(CommandSourceStack pSource, String pBindAddress, int pPort) {
      if (alreadyRunning(pSource)) {
         return 0;
      } else {
         chaseServer = new ChaseServer(pBindAddress, pPort, pSource.getServer().getPlayerList(), 100);

         try {
            chaseServer.start();
            pSource.sendSuccess(() -> {
               return Component.literal("Chase server is now running on port " + pPort + ". Clients can follow you using /chase follow <ip> <port>");
            }, false);
         } catch (IOException ioexception) {
            LOGGER.error("Failed to start chase server", (Throwable)ioexception);
            pSource.sendFailure(Component.literal("Failed to start chase server on port " + pPort));
            chaseServer = null;
         }

         return 0;
      }
   }

   private static int follow(CommandSourceStack pSource, String pHost, int pPort) {
      if (alreadyRunning(pSource)) {
         return 0;
      } else {
         chaseClient = new ChaseClient(pHost, pPort, pSource.getServer());
         chaseClient.start();
         pSource.sendSuccess(() -> {
            return Component.literal("You are now chasing " + pHost + ":" + pPort + ". If that server does '/chase lead' then you will automatically go to the same position. Use '/chase stop' to stop chasing.");
         }, false);
         return 0;
      }
   }
}