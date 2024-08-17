package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlowerPotBlock extends Block {
   public static final MapCodec<FlowerPotBlock> CODEC = RecordCodecBuilder.mapCodec((p_310555_) -> {
      return p_310555_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("potted").forGetter((p_310137_) -> {
         return p_310137_.potted;
      }), propertiesCodec()).apply(p_310555_, FlowerPotBlock::new);
   });
   private static final Map<Block, Block> POTTED_BY_CONTENT = Maps.newHashMap();
   public static final float AABB_SIZE = 3.0F;
   protected static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 6.0D, 11.0D);
   private final Block potted;

   public MapCodec<FlowerPotBlock> codec() {
      return CODEC;
   }

   public FlowerPotBlock(Block p_53528_, BlockBehaviour.Properties p_53529_) {
      super(p_53529_);
      this.potted = p_53528_;
      POTTED_BY_CONTENT.put(p_53528_, this);
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   public RenderShape getRenderShape(BlockState pState) {
      return RenderShape.MODEL;
   }

   public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      Item item = itemstack.getItem();
      BlockState blockstate = (item instanceof BlockItem ? POTTED_BY_CONTENT.getOrDefault(((BlockItem)item).getBlock(), Blocks.AIR) : Blocks.AIR).defaultBlockState();
      boolean flag = blockstate.is(Blocks.AIR);
      boolean flag1 = this.isEmpty();
      if (flag != flag1) {
         if (flag1) {
            pLevel.setBlock(pPos, blockstate, 3);
            pPlayer.awardStat(Stats.POT_FLOWER);
            if (!pPlayer.getAbilities().instabuild) {
               itemstack.shrink(1);
            }
         } else {

            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_10) && potted != Blocks.AIR) {
               return (InteractionResult.CONSUME);
            }

            ItemStack itemstack1 = new ItemStack(this.potted);
            if (itemstack.isEmpty()) {
               pPlayer.setItemInHand(pHand, itemstack1);
            } else if (!pPlayer.addItem(itemstack1)) {
               pPlayer.drop(itemstack1, false);
            }

            pLevel.setBlock(pPos, Blocks.FLOWER_POT.defaultBlockState(), 3);
         }

         pLevel.gameEvent(pPlayer, GameEvent.BLOCK_CHANGE, pPos);
         return InteractionResult.sidedSuccess(pLevel.isClientSide);
      } else {
         return InteractionResult.CONSUME;
      }
   }

   public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return this.isEmpty() ? super.getCloneItemStack(pLevel, pPos, pState) : new ItemStack(this.potted);
   }

   private boolean isEmpty() {
      return this.potted == Blocks.AIR;
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      return pFacing == Direction.DOWN && !pState.canSurvive(pLevel, pCurrentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   public Block getPotted() {
      return this.potted;
   }

   public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
      return false;
   }
}