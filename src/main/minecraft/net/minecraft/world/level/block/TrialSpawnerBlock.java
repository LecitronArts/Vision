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
import net.minecraft.world.level.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class TrialSpawnerBlock extends BaseEntityBlock {
   public static final MapCodec<TrialSpawnerBlock> CODEC = simpleCodec(TrialSpawnerBlock::new);
   public static final EnumProperty<TrialSpawnerState> STATE = BlockStateProperties.TRIAL_SPAWNER_STATE;

   public MapCodec<TrialSpawnerBlock> codec() {
      return CODEC;
   }

   public TrialSpawnerBlock(BlockBehaviour.Properties p_309401_) {
      super(p_309401_);
      this.registerDefaultState(this.stateDefinition.any().setValue(STATE, TrialSpawnerState.INACTIVE));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_312861_) {
      p_312861_.add(STATE);
   }

   public RenderShape getRenderShape(BlockState p_312094_) {
      return RenderShape.MODEL;
   }

   @Nullable
   public BlockEntity newBlockEntity(BlockPos p_310402_, BlockState p_309509_) {
      return new TrialSpawnerBlockEntity(p_310402_, p_309509_);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_312042_, BlockState p_312838_, BlockEntityType<T> p_310465_) {
      BlockEntityTicker blockentityticker;
      if (p_312042_ instanceof ServerLevel serverlevel) {
         blockentityticker = createTickerHelper(p_310465_, BlockEntityType.TRIAL_SPAWNER, (p_309744_, p_311932_, p_311385_, p_309561_) -> {
            p_309561_.getTrialSpawner().tickServer(serverlevel, p_311932_);
         });
      } else {
         blockentityticker = createTickerHelper(p_310465_, BlockEntityType.TRIAL_SPAWNER, (p_310108_, p_309418_, p_311997_, p_311263_) -> {
            p_311263_.getTrialSpawner().tickClient(p_310108_, p_309418_);
         });
      }

      return blockentityticker;
   }

   public void appendHoverText(ItemStack p_311445_, @Nullable BlockGetter p_310392_, List<Component> p_310585_, TooltipFlag p_310832_) {
      super.appendHoverText(p_311445_, p_310392_, p_310585_, p_310832_);
      Spawner.appendHoverText(p_311445_, p_310585_, "spawn_data");
   }
}