package net.minecraft.commands.execution;

@FunctionalInterface
public interface EntryAction<T> {
   void execute(ExecutionContext<T> pContext, Frame pFrame);
}