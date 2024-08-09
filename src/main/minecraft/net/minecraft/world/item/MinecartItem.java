package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class MinecartItem extends Item {
   private static final DispenseItemBehavior DISPENSE_ITEM_BEHAVIOR = new DefaultDispenseItemBehavior() {
      private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

      public ItemStack execute(BlockSource p_42949_, ItemStack p_42950_) {
         Direction direction = p_42949_.state().getValue(DispenserBlock.FACING);
         ServerLevel serverlevel = p_42949_.level();
         Vec3 vec3 = p_42949_.center();
         double d0 = vec3.x() + (double)direction.getStepX() * 1.125D;
         double d1 = Math.floor(vec3.y()) + (double)direction.getStepY();
         double d2 = vec3.z() + (double)direction.getStepZ() * 1.125D;
         BlockPos blockpos = p_42949_.pos().relative(direction);
         BlockState blockstate = serverlevel.getBlockState(blockpos);
         RailShape railshape = blockstate.getBlock() instanceof BaseRailBlock ? blockstate.getValue(((BaseRailBlock)blockstate.getBlock()).getShapeProperty()) : RailShape.NORTH_SOUTH;
         double d3;
         if (blockstate.is(BlockTags.RAILS)) {
            if (railshape.isAscending()) {
               d3 = 0.6D;
            } else {
               d3 = 0.1D;
            }
         } else {
            if (!blockstate.isAir() || !serverlevel.getBlockState(blockpos.below()).is(BlockTags.RAILS)) {
               return this.defaultDispenseItemBehavior.dispense(p_42949_, p_42950_);
            }

            BlockState blockstate1 = serverlevel.getBlockState(blockpos.below());
            RailShape railshape1 = blockstate1.getBlock() instanceof BaseRailBlock ? blockstate1.getValue(((BaseRailBlock)blockstate1.getBlock()).getShapeProperty()) : RailShape.NORTH_SOUTH;
            if (direction != Direction.DOWN && railshape1.isAscending()) {
               d3 = -0.4D;
            } else {
               d3 = -0.9D;
            }
         }

         AbstractMinecart abstractminecart = AbstractMinecart.createMinecart(serverlevel, d0, d1 + d3, d2, ((MinecartItem)p_42950_.getItem()).type, p_42950_, (Player)null);
         serverlevel.addFreshEntity(abstractminecart);
         p_42950_.shrink(1);
         return p_42950_;
      }

      protected void playSound(BlockSource p_42947_) {
         p_42947_.level().levelEvent(1000, p_42947_.pos(), 0);
      }
   };
   final AbstractMinecart.Type type;

   public MinecartItem(AbstractMinecart.Type pType, Item.Properties pProperties) {
      super(pProperties);
      this.type = pType;
      DispenserBlock.registerBehavior(this, DISPENSE_ITEM_BEHAVIOR);
   }

   public InteractionResult useOn(UseOnContext pContext) {
      Level level = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      BlockState blockstate = level.getBlockState(blockpos);
      if (!blockstate.is(BlockTags.RAILS)) {
         return InteractionResult.FAIL;
      } else {
         ItemStack itemstack = pContext.getItemInHand();
         if (level instanceof ServerLevel) {
            ServerLevel serverlevel = (ServerLevel)level;
            RailShape railshape = blockstate.getBlock() instanceof BaseRailBlock ? blockstate.getValue(((BaseRailBlock)blockstate.getBlock()).getShapeProperty()) : RailShape.NORTH_SOUTH;
            double d0 = 0.0D;
            if (railshape.isAscending()) {
               d0 = 0.5D;
            }

            AbstractMinecart abstractminecart = AbstractMinecart.createMinecart(serverlevel, (double)blockpos.getX() + 0.5D, (double)blockpos.getY() + 0.0625D + d0, (double)blockpos.getZ() + 0.5D, this.type, itemstack, pContext.getPlayer());
            serverlevel.addFreshEntity(abstractminecart);
            serverlevel.gameEvent(GameEvent.ENTITY_PLACE, blockpos, GameEvent.Context.of(pContext.getPlayer(), serverlevel.getBlockState(blockpos.below())));
         }

         itemstack.shrink(1);
         return InteractionResult.sidedSuccess(level.isClientSide);
      }
   }
}