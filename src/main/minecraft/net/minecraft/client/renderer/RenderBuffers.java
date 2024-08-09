package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.SortedMap;
import net.minecraft.Util;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderBuffers {
   private final SectionBufferBuilderPack fixedBufferPack = new SectionBufferBuilderPack();
   private final SectionBufferBuilderPool sectionBufferPool;
   private final MultiBufferSource.BufferSource bufferSource;
   private final MultiBufferSource.BufferSource crumblingBufferSource;
   private final OutlineBufferSource outlineBufferSource;

   public RenderBuffers(int pBufferCount) {
      this.sectionBufferPool = SectionBufferBuilderPool.allocate(pBufferCount);
      SortedMap<RenderType, BufferBuilder> sortedmap = Util.make(new Object2ObjectLinkedOpenHashMap<>(), (p_308289_) -> {
         p_308289_.put(Sheets.solidBlockSheet(), this.fixedBufferPack.builder(RenderType.solid()));
         p_308289_.put(Sheets.cutoutBlockSheet(), this.fixedBufferPack.builder(RenderType.cutout()));
         p_308289_.put(Sheets.bannerSheet(), this.fixedBufferPack.builder(RenderType.cutoutMipped()));
         p_308289_.put(Sheets.translucentCullBlockSheet(), this.fixedBufferPack.builder(RenderType.translucent()));
         put(p_308289_, Sheets.shieldSheet());
         put(p_308289_, Sheets.bedSheet());
         put(p_308289_, Sheets.shulkerBoxSheet());
         put(p_308289_, Sheets.signSheet());
         put(p_308289_, Sheets.hangingSignSheet());
         p_308289_.put(Sheets.chestSheet(), new BufferBuilder(786432));
         put(p_308289_, RenderType.armorGlint());
         put(p_308289_, RenderType.armorEntityGlint());
         put(p_308289_, RenderType.glint());
         put(p_308289_, RenderType.glintDirect());
         put(p_308289_, RenderType.glintTranslucent());
         put(p_308289_, RenderType.entityGlint());
         put(p_308289_, RenderType.entityGlintDirect());
         put(p_308289_, RenderType.waterMask());
         ModelBakery.DESTROY_TYPES.forEach((p_173062_) -> {
            put(p_308289_, p_173062_);
         });
      });
      this.crumblingBufferSource = MultiBufferSource.immediate(new BufferBuilder(1536));
      this.bufferSource = MultiBufferSource.immediateWithBuffers(sortedmap, new BufferBuilder(786432));
      this.outlineBufferSource = new OutlineBufferSource(this.bufferSource);
   }

   private static void put(Object2ObjectLinkedOpenHashMap<RenderType, BufferBuilder> pMapBuilders, RenderType pRenderType) {
      pMapBuilders.put(pRenderType, new BufferBuilder(pRenderType.bufferSize()));
   }

   public SectionBufferBuilderPack fixedBufferPack() {
      return this.fixedBufferPack;
   }

   public SectionBufferBuilderPool sectionBufferPool() {
      return this.sectionBufferPool;
   }

   public MultiBufferSource.BufferSource bufferSource() {
      return this.bufferSource;
   }

   public MultiBufferSource.BufferSource crumblingBufferSource() {
      return this.crumblingBufferSource;
   }

   public OutlineBufferSource outlineBufferSource() {
      return this.outlineBufferSource;
   }
}