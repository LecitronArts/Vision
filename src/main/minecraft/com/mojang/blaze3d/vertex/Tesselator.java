package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.optifine.SmartAnimations;

public class Tesselator {
   private static final int MAX_BYTES = 786432;
   private final BufferBuilder builder;
   @Nullable
   private static Tesselator instance;

   public static void init() {
      RenderSystem.assertOnGameThreadOrInit();
      if (instance != null) {
         throw new IllegalStateException("Tesselator has already been initialized");
      } else {
         instance = new Tesselator();
      }
   }

   public static Tesselator getInstance() {
      RenderSystem.assertOnGameThreadOrInit();
      if (instance == null) {
         throw new IllegalStateException("Tesselator has not been initialized");
      } else {
         return instance;
      }
   }

   public Tesselator(int pCapacity) {
      this.builder = new BufferBuilder(pCapacity);
   }

   public Tesselator() {
      this(786432);
   }

   public void end() {
      if (this.builder.animatedSprites != null) {
         SmartAnimations.spritesRendered(this.builder.animatedSprites);
      }

      BufferBuilder.RenderedBuffer bufferbuilder$renderedbuffer = this.builder.endOrDiscardIfEmpty();
      if (bufferbuilder$renderedbuffer != null) {
         BufferUploader.drawWithShader(bufferbuilder$renderedbuffer);
      }

   }

   public BufferBuilder getBuilder() {
      return this.builder;
   }
}