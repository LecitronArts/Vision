package net.minecraft.commands.execution;

import javax.annotation.Nullable;
import net.minecraft.commands.ExecutionCommandSource;

public interface ExecutionControl<T> {
   void queueNext(EntryAction<T> pEntry);

   void tracer(@Nullable TraceCallbacks pTracer);

   @Nullable
   TraceCallbacks tracer();

   Frame currentFrame();

   static <T extends ExecutionCommandSource<T>> ExecutionControl<T> create(final ExecutionContext<T> pExecutionContext, final Frame pFrame) {
      return new ExecutionControl<T>() {
         public void queueNext(EntryAction<T> p_311389_) {
            pExecutionContext.queueNext(new CommandQueueEntry<>(pFrame, p_311389_));
         }

         public void tracer(@Nullable TraceCallbacks p_313185_) {
            pExecutionContext.tracer(p_313185_);
         }

         @Nullable
         public TraceCallbacks tracer() {
            return pExecutionContext.tracer();
         }

         public Frame currentFrame() {
            return pFrame;
         }
      };
   }
}