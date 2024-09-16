package net.minecraft.world.phys.shapes;

import it.unimi.dsi.fastutil.doubles.DoubleList;

public interface IndexMerger {
   DoubleList getList();

   boolean forMergedIndexes(IndexMerger.IndexConsumer pConsumer);

   int size();

   public interface IndexConsumer {
      boolean merge(int pFirstValue, int pSecondValue, int pThirdValue);
   }
}