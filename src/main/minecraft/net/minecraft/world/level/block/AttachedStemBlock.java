package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AttachedStemBlock extends BushBlock {
   public static final MapCodec<AttachedStemBlock> CODEC = RecordCodecBuilder.mapCodec((p_310408_) -> {
      return p_310408_.group(ResourceKey.codec(Registries.BLOCK).fieldOf("fruit").forGetter((p_309932_) -> {
         return p_309932_.fruit;
      }), ResourceKey.codec(Registries.BLOCK).fieldOf("stem").forGetter((p_312475_) -> {
         return p_312475_.stem;
      }), ResourceKey.codec(Registries.ITEM).fieldOf("seed").forGetter((p_312517_) -> {
         return p_312517_.seed;
      }), propertiesCodec()).apply(p_310408_, AttachedStemBlock::new);
   });
   public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
   protected static final float AABB_OFFSET = 2.0F;
   private static final Map<Direction, VoxelShape> AABBS = Maps.newEnumMap(ImmutableMap.of(Direction.SOUTH, Block.box(6.0D, 0.0D, 6.0D, 10.0D, 10.0D, 16.0D), Direction.WEST, Block.box(0.0D, 0.0D, 6.0D, 10.0D, 10.0D, 10.0D), Direction.NORTH, Block.box(6.0D, 0.0D, 0.0D, 10.0D, 10.0D, 10.0D), Direction.EAST, Block.box(6.0D, 0.0D, 6.0D, 16.0D, 10.0D, 10.0D)));
   private final ResourceKey<Block> fruit;
   private final ResourceKey<Block> stem;
   private final ResourceKey<Item> seed;

   public MapCodec<AttachedStemBlock> codec() {
      return CODEC;
   }

   protected AttachedStemBlock(ResourceKey<Block> p_309773_, ResourceKey<Block> p_312687_, ResourceKey<Item> p_310792_, BlockBehaviour.Properties p_152062_) {
      super(p_152062_);
      this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
      this.stem = p_309773_;
      this.fruit = p_312687_;
      this.seed = p_310792_;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AABBS.get(pState.getValue(FACING));
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      if (!pFacingState.is(this.fruit) && pFacing == pState.getValue(FACING)) {
         Optional<Block> optional = pLevel.registryAccess().registryOrThrow(Registries.BLOCK).getOptional(this.stem);
         if (optional.isPresent()) {
            return optional.get().defaultBlockState().trySetValue(StemBlock.AGE, Integer.valueOf(7));
         }
      }

      return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return pState.is(Blocks.FARMLAND);
   }

   public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
      return new ItemStack(DataFixUtils.orElse(pLevel.registryAccess().registryOrThrow(Registries.ITEM).getOptional(this.seed), this));
   }

   public BlockState rotate(BlockState pState, Rotation pRot) {
      return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
   }

   public BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(FACING);
   }
}