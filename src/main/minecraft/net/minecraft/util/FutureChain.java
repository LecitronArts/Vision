package net.minecraft.util;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import org.slf4j.Logger;

public class FutureChain implements TaskChainer, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private CompletableFuture<?> head = CompletableFuture.completedFuture((Object)null);
   private final Executor executor;
   private volatile boolean closed;

   public FutureChain(Executor pExecutor) {
      this.executor = pExecutor;
   }

   public <T> void append(CompletableFuture<T> pFuture, Consumer<T> pConsumer) {
      this.head = this.head.thenCombine(pFuture, (p_308966_, p_308967_) -> {
         return p_308967_;
      }).thenAcceptAsync((p_308969_) -> {
         if (!this.closed) {
            pConsumer.accept((T)p_308969_);
         }

      }, this.executor).exceptionally((p_242215_) -> {
         if (p_242215_ instanceof CompletionException completionexception) {
            p_242215_ = completionexception.getCause();
         }

         if (p_242215_ instanceof CancellationException cancellationexception) {
            throw cancellationexception;
         } else {
            LOGGER.error("Chain link failed, continuing to next one", p_242215_);
            return null;
         }
      });
   }

   public void close() {
      this.closed = true;
   }
}