package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public abstract class AbstractProjectileDispenseBehavior extends DefaultDispenseItemBehavior {
   public ItemStack execute(BlockSource pBlockSource, ItemStack pItem) {
      Level level = pBlockSource.level();
      Position position = DispenserBlock.getDispensePosition(pBlockSource);
      Direction direction = pBlockSource.state().getValue(DispenserBlock.FACING);
      Projectile projectile = this.getProjectile(level, position, pItem);
      projectile.shoot((double)direction.getStepX(), (double)((float)direction.getStepY() + 0.1F), (double)direction.getStepZ(), this.getPower(), this.getUncertainty());
      level.addFreshEntity(projectile);
      pItem.shrink(1);
      return pItem;
   }

   protected void playSound(BlockSource pBlockSource) {
      pBlockSource.level().levelEvent(1002, pBlockSource.pos(), 0);
   }

   protected abstract Projectile getProjectile(Level pLevel, Position pPosition, ItemStack pStack);

   protected float getUncertainty() {
      return 6.0F;
   }

   protected float getPower() {
      return 1.1F;
   }
}