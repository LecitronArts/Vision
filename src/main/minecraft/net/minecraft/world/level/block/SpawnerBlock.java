package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnerBlock extends BaseEntityBlock {
   public static final MapCodec<SpawnerBlock> CODEC = simpleCodec(SpawnerBlock::new);

   public MapCodec<SpawnerBlock> codec() {
      return CODEC;
   }

   protected SpawnerBlock(BlockBehaviour.Properties p_56781_) {
      super(p_56781_);
   }

   public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
      return new SpawnerBlockEntity(pPos, pState);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
      return createTickerHelper(pBlockEntityType, BlockEntityType.MOB_SPAWNER, pLevel.isClientSide ? SpawnerBlockEntity::clientTick : SpawnerBlockEntity::serverTick);
   }

   public void spawnAfterBreak(BlockState pState, ServerLevel pLevel, BlockPos pPos, ItemStack pStack, boolean pDropExperience) {
      super.spawnAfterBreak(pState, pLevel, pPos, pStack, pDropExperience);
      if (pDropExperience) {
         int i = 15 + pLevel.random.nextInt(15) + pLevel.random.nextInt(15);
         this.popExperience(pLevel, pPos, i);
      }

   }

   public RenderShape getRenderShape(BlockState pState) {
      return RenderShape.MODEL;
   }

   public void appendHoverText(ItemStack pStack, @Nullable BlockGetter pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
      super.appendHoverText(pStack, pLevel, pTooltip, pFlag);
      Spawner.appendHoverText(pStack, pTooltip, "SpawnData");
   }
}