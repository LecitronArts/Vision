package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.ScoreAccess;

public class OperationArgument implements ArgumentType<OperationArgument.Operation> {
   private static final Collection<String> EXAMPLES = Arrays.asList("=", ">", "<");
   private static final SimpleCommandExceptionType ERROR_INVALID_OPERATION = new SimpleCommandExceptionType(Component.translatable("arguments.operation.invalid"));
   private static final SimpleCommandExceptionType ERROR_DIVIDE_BY_ZERO = new SimpleCommandExceptionType(Component.translatable("arguments.operation.div0"));

   public static OperationArgument operation() {
      return new OperationArgument();
   }

   public static OperationArgument.Operation getOperation(CommandContext<CommandSourceStack> pContext, String pName) {
      return pContext.getArgument(pName, OperationArgument.Operation.class);
   }

   public OperationArgument.Operation parse(StringReader pReader) throws CommandSyntaxException {
      if (!pReader.canRead()) {
         throw ERROR_INVALID_OPERATION.create();
      } else {
         int i = pReader.getCursor();

         while(pReader.canRead() && pReader.peek() != ' ') {
            pReader.skip();
         }

         return getOperation(pReader.getString().substring(i, pReader.getCursor()));
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      return SharedSuggestionProvider.suggest(new String[]{"=", "+=", "-=", "*=", "/=", "%=", "<", ">", "><"}, pBuilder);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   private static OperationArgument.Operation getOperation(String pName) throws CommandSyntaxException {
      return (pName.equals("><") ? (p_308356_, p_308357_) -> {
         int i = p_308356_.get();
         p_308356_.set(p_308357_.get());
         p_308357_.set(i);
      } : getSimpleOperation(pName));
   }

   private static OperationArgument.SimpleOperation getSimpleOperation(String pName) throws CommandSyntaxException {
      OperationArgument.SimpleOperation operationargument$simpleoperation;
      switch (pName) {
         case "=":
            operationargument$simpleoperation = (p_103298_, p_103299_) -> {
               return p_103299_;
            };
            break;
         case "+=":
            operationargument$simpleoperation = Integer::sum;
            break;
         case "-=":
            operationargument$simpleoperation = (p_103292_, p_103293_) -> {
               return p_103292_ - p_103293_;
            };
            break;
         case "*=":
            operationargument$simpleoperation = (p_103289_, p_103290_) -> {
               return p_103289_ * p_103290_;
            };
            break;
         case "/=":
            operationargument$simpleoperation = (p_264713_, p_264714_) -> {
               if (p_264714_ == 0) {
                  throw ERROR_DIVIDE_BY_ZERO.create();
               } else {
                  return Mth.floorDiv(p_264713_, p_264714_);
               }
            };
            break;
         case "%=":
            operationargument$simpleoperation = (p_103271_, p_103272_) -> {
               if (p_103272_ == 0) {
                  throw ERROR_DIVIDE_BY_ZERO.create();
               } else {
                  return Mth.positiveModulo(p_103271_, p_103272_);
               }
            };
            break;
         case "<":
            operationargument$simpleoperation = Math::min;
            break;
         case ">":
            operationargument$simpleoperation = Math::max;
            break;
         default:
            throw ERROR_INVALID_OPERATION.create();
      }

      return operationargument$simpleoperation;
   }

   @FunctionalInterface
   public interface Operation {
      void apply(ScoreAccess pTargetScore, ScoreAccess pSourceScore) throws CommandSyntaxException;
   }

   @FunctionalInterface
   interface SimpleOperation extends OperationArgument.Operation {
      int apply(int pTargetScore, int pSourceScore) throws CommandSyntaxException;

      default void apply(ScoreAccess pTargetScore, ScoreAccess pSourceScore) throws CommandSyntaxException {
         pTargetScore.set(this.apply(pTargetScore.get(), pSourceScore.get()));
      }
   }
}