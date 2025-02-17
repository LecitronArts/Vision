package net.minecraft.server.commands;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.ExecutionCommandSource;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;

public class FunctionCommand {
   private static final DynamicCommandExceptionType ERROR_ARGUMENT_NOT_COMPOUND = new DynamicCommandExceptionType((p_308741_) -> {
      return Component.translatableEscape("commands.function.error.argument_not_compound", p_308741_);
   });
   static final DynamicCommandExceptionType ERROR_NO_FUNCTIONS = new DynamicCommandExceptionType((p_308742_) -> {
      return Component.translatableEscape("commands.function.scheduled.no_functions", p_308742_);
   });
   @VisibleForTesting
   public static final Dynamic2CommandExceptionType ERROR_FUNCTION_INSTANTATION_FAILURE = new Dynamic2CommandExceptionType((p_308724_, p_308725_) -> {
      return Component.translatableEscape("commands.function.instantiationFailure", p_308724_, p_308725_);
   });
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_FUNCTION = (p_137719_, p_137720_) -> {
      ServerFunctionManager serverfunctionmanager = p_137719_.getSource().getServer().getFunctions();
      SharedSuggestionProvider.suggestResource(serverfunctionmanager.getTagNames(), p_137720_, "#");
      return SharedSuggestionProvider.suggestResource(serverfunctionmanager.getFunctionNames(), p_137720_);
   };
   static final FunctionCommand.Callbacks<CommandSourceStack> FULL_CONTEXT_CALLBACKS = new FunctionCommand.Callbacks<CommandSourceStack>() {
      public void signalResult(CommandSourceStack p_311645_, ResourceLocation p_312021_, int p_313021_) {
         p_311645_.sendSuccess(() -> {
            return Component.translatable("commands.function.result", Component.translationArg(p_312021_), p_313021_);
         }, true);
      }
   };

   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("with");

      for(DataCommands.DataProvider datacommands$dataprovider : DataCommands.SOURCE_PROVIDERS) {
         datacommands$dataprovider.wrap(literalargumentbuilder, (p_308740_) -> {
            return p_308740_.executes(new FunctionCommand.FunctionCustomExecutor() {
               protected CompoundTag arguments(CommandContext<CommandSourceStack> p_309658_) throws CommandSyntaxException {
                  return datacommands$dataprovider.access(p_309658_).getData();
               }
            }).then(Commands.argument("path", NbtPathArgument.nbtPath()).executes(new FunctionCommand.FunctionCustomExecutor() {
               protected CompoundTag arguments(CommandContext<CommandSourceStack> p_310697_) throws CommandSyntaxException {
                  return FunctionCommand.getArgumentTag(NbtPathArgument.getPath(p_310697_, "path"), datacommands$dataprovider.access(p_310697_));
               }
            }));
         });
      }

      pDispatcher.register(Commands.literal("function").requires((p_137722_) -> {
         return p_137722_.hasPermission(2);
      }).then(Commands.argument("name", FunctionArgument.functions()).suggests(SUGGEST_FUNCTION).executes(new FunctionCommand.FunctionCustomExecutor() {
         @Nullable
         protected CompoundTag arguments(CommandContext<CommandSourceStack> p_310275_) {
            return null;
         }
      }).then(Commands.argument("arguments", CompoundTagArgument.compoundTag()).executes(new FunctionCommand.FunctionCustomExecutor() {
         protected CompoundTag arguments(CommandContext<CommandSourceStack> p_310980_) {
            return CompoundTagArgument.getCompoundTag(p_310980_, "arguments");
         }
      })).then(literalargumentbuilder)));
   }

   static CompoundTag getArgumentTag(NbtPathArgument.NbtPath pNbtPath, DataAccessor pDataAccessor) throws CommandSyntaxException {
      Tag tag = DataCommands.getSingleTag(pNbtPath, pDataAccessor);
      if (tag instanceof CompoundTag) {
         return (CompoundTag)tag;
      } else {
         throw ERROR_ARGUMENT_NOT_COMPOUND.create(tag.getType().getName());
      }
   }

   public static CommandSourceStack modifySenderForExecution(CommandSourceStack pSource) {
      return pSource.withSuppressedOutput().withMaximumPermission(2);
   }

   public static <T extends ExecutionCommandSource<T>> void queueFunctions(Collection<CommandFunction<T>> pFunctions, @Nullable CompoundTag pArguments, T pOriginalSource, T pSource, ExecutionControl<T> pExecutionControl, FunctionCommand.Callbacks<T> pCallbacks, ChainModifiers pChainModifiers) throws CommandSyntaxException {
      if (pChainModifiers.isReturn()) {
         queueFunctionsAsReturn(pFunctions, pArguments, pOriginalSource, pSource, pExecutionControl, pCallbacks);
      } else {
         queueFunctionsNoReturn(pFunctions, pArguments, pOriginalSource, pSource, pExecutionControl, pCallbacks);
      }

   }

   private static <T extends ExecutionCommandSource<T>> void instantiateAndQueueFunctions(@Nullable CompoundTag pArguments, ExecutionControl<T> pExecutionControl, CommandDispatcher<T> pDispatcher, T pSource, CommandFunction<T> pFunction, ResourceLocation pFunctionId, CommandResultCallback pResultCallback, boolean pReturnParentFrame) throws CommandSyntaxException {
      try {
         InstantiatedFunction<T> instantiatedfunction = pFunction.instantiate(pArguments, pDispatcher, pSource);
         pExecutionControl.queueNext((new CallFunction<>(instantiatedfunction, pResultCallback, pReturnParentFrame)).bind(pSource));
      } catch (FunctionInstantiationException functioninstantiationexception) {
         throw ERROR_FUNCTION_INSTANTATION_FAILURE.create(pFunctionId, functioninstantiationexception.messageComponent());
      }
   }

   private static <T extends ExecutionCommandSource<T>> CommandResultCallback decorateOutputIfNeeded(T pSource, FunctionCommand.Callbacks<T> pCallbacks, ResourceLocation pFunction, CommandResultCallback pResultCallback) {
      return pSource.isSilent() ? pResultCallback : (p_308737_, p_308738_) -> {
         pCallbacks.signalResult(pSource, pFunction, p_308738_);
         pResultCallback.onSuccess(p_308738_);
      };
   }

   private static <T extends ExecutionCommandSource<T>> void queueFunctionsAsReturn(Collection<CommandFunction<T>> pFunctions, @Nullable CompoundTag pArguments, T pOriginalSource, T pSource, ExecutionControl<T> pExectutionControl, FunctionCommand.Callbacks<T> pCallbacks) throws CommandSyntaxException {
      CommandDispatcher<T> commanddispatcher = pOriginalSource.dispatcher();
      T t = pSource.clearCallbacks();
      CommandResultCallback commandresultcallback = CommandResultCallback.chain(pOriginalSource.callback(), pExectutionControl.currentFrame().returnValueConsumer());

      for(CommandFunction<T> commandfunction : pFunctions) {
         ResourceLocation resourcelocation = commandfunction.id();
         CommandResultCallback commandresultcallback1 = decorateOutputIfNeeded(pOriginalSource, pCallbacks, resourcelocation, commandresultcallback);
         instantiateAndQueueFunctions(pArguments, pExectutionControl, commanddispatcher, t, commandfunction, resourcelocation, commandresultcallback1, true);
      }

      if (commandresultcallback != CommandResultCallback.EMPTY) {
         pExectutionControl.queueNext(FallthroughTask.instance());
      }
   }

   private static <T extends ExecutionCommandSource<T>> void queueFunctionsNoReturn(Collection<CommandFunction<T>> pFunctions, @Nullable CompoundTag pArguments, T pOriginalSource, T pSource, ExecutionControl<T> pExecutionControl, FunctionCommand.Callbacks<T> pCallbacks) throws CommandSyntaxException {
      CommandDispatcher<T> commanddispatcher = pOriginalSource.dispatcher();
      T t = pSource.clearCallbacks();
      CommandResultCallback commandresultcallback = pOriginalSource.callback();
      if (!pFunctions.isEmpty()) {
         if (pFunctions.size() == 1) {
            CommandFunction<T> commandfunction = pFunctions.iterator().next();
            ResourceLocation resourcelocation = commandfunction.id();
            CommandResultCallback commandresultcallback1 = decorateOutputIfNeeded(pOriginalSource, pCallbacks, resourcelocation, commandresultcallback);
            instantiateAndQueueFunctions(pArguments, pExecutionControl, commanddispatcher, t, commandfunction, resourcelocation, commandresultcallback1, false);
         } else if (commandresultcallback == CommandResultCallback.EMPTY) {
            for(CommandFunction<T> commandfunction1 : pFunctions) {
               ResourceLocation resourcelocation2 = commandfunction1.id();
               CommandResultCallback commandresultcallback2 = decorateOutputIfNeeded(pOriginalSource, pCallbacks, resourcelocation2, commandresultcallback);
               instantiateAndQueueFunctions(pArguments, pExecutionControl, commanddispatcher, t, commandfunction1, resourcelocation2, commandresultcallback2, false);
            }
         } else {
            class Accumulator {
               boolean anyResult;
               int sum;

               public void add(int p_310205_) {
                  this.anyResult = true;
                  this.sum += p_310205_;
               }
            }

            Accumulator functioncommand$1accumulator = new Accumulator();
            CommandResultCallback commandresultcallback4 = (p_308727_, p_308728_) -> {
               functioncommand$1accumulator.add(p_308728_);
            };

            for(CommandFunction<T> commandfunction2 : pFunctions) {
               ResourceLocation resourcelocation1 = commandfunction2.id();
               CommandResultCallback commandresultcallback3 = decorateOutputIfNeeded(pOriginalSource, pCallbacks, resourcelocation1, commandresultcallback4);
               instantiateAndQueueFunctions(pArguments, pExecutionControl, commanddispatcher, t, commandfunction2, resourcelocation1, commandresultcallback3, false);
            }

            pExecutionControl.queueNext((p_308731_, p_308732_) -> {
               if (functioncommand$1accumulator.anyResult) {
                  commandresultcallback.onSuccess(functioncommand$1accumulator.sum);
               }

            });
         }

      }
   }

   public interface Callbacks<T> {
      void signalResult(T pSource, ResourceLocation pFunction, int pCommands);
   }

   abstract static class FunctionCustomExecutor extends CustomCommandExecutor.WithErrorHandling<CommandSourceStack> implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
      @Nullable
      protected abstract CompoundTag arguments(CommandContext<CommandSourceStack> pContext) throws CommandSyntaxException;

      public void runGuarded(CommandSourceStack pSource, ContextChain<CommandSourceStack> pContextChain, ChainModifiers pChainModifiers, ExecutionControl<CommandSourceStack> pExecutionControl) throws CommandSyntaxException {
         CommandContext<CommandSourceStack> commandcontext = pContextChain.getTopContext().copyFor(pSource);
         Pair<ResourceLocation, Collection<CommandFunction<CommandSourceStack>>> pair = FunctionArgument.getFunctionCollection(commandcontext, "name");
         Collection<CommandFunction<CommandSourceStack>> collection = pair.getSecond();
         if (collection.isEmpty()) {
            throw FunctionCommand.ERROR_NO_FUNCTIONS.create(Component.translationArg(pair.getFirst()));
         } else {
            CompoundTag compoundtag = this.arguments(commandcontext);
            CommandSourceStack commandsourcestack = FunctionCommand.modifySenderForExecution(pSource);
            if (collection.size() == 1) {
               pSource.sendSuccess(() -> {
                  return Component.translatable("commands.function.scheduled.single", Component.translationArg(collection.iterator().next().id()));
               }, true);
            } else {
               pSource.sendSuccess(() -> {
                  return Component.translatable("commands.function.scheduled.multiple", ComponentUtils.formatList(collection.stream().map(CommandFunction::id).toList(), Component::translationArg));
               }, true);
            }

            FunctionCommand.queueFunctions(collection, compoundtag, pSource, commandsourcestack, pExecutionControl, FunctionCommand.FULL_CONTEXT_CALLBACKS, pChainModifiers);
         }
      }
   }
}