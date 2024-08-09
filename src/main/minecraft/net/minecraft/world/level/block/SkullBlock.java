package net.minecraft.world.level.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SkullBlock extends AbstractSkullBlock {
   public static final MapCodec<SkullBlock> CODEC = RecordCodecBuilder.mapCodec((p_311084_) -> {
      return p_311084_.group(SkullBlock.Type.CODEC.fieldOf("kind").forGetter(AbstractSkullBlock::getType), propertiesCodec()).apply(p_311084_, SkullBlock::new);
   });
   public static final int MAX = RotationSegment.getMaxSegmentIndex();
   private static final int ROTATIONS = MAX + 1;
   public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
   protected static final VoxelShape SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 8.0D, 12.0D);
   protected static final VoxelShape PIGLIN_SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 8.0D, 13.0D);

   public MapCodec<? extends SkullBlock> codec() {
      return CODEC;
   }

   protected SkullBlock(SkullBlock.Type p_56318_, BlockBehaviour.Properties p_56319_) {
      super(p_56318_, p_56319_);
      this.registerDefaultState(this.defaultBlockState().setValue(ROTATION, Integer.valueOf(0)));
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return this.getType() == SkullBlock.Types.PIGLIN ? PIGLIN_SHAPE : SHAPE;
   }

   public VoxelShape getOcclusionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return Shapes.empty();
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return super.getStateForPlacement(pContext).setValue(ROTATION, Integer.valueOf(RotationSegment.convertToSegment(pContext.getRotation())));
   }

   public BlockState rotate(BlockState pState, Rotation pRotation) {
      return pState.setValue(ROTATION, Integer.valueOf(pRotation.rotate(pState.getValue(ROTATION), ROTATIONS)));
   }

   public BlockState mirror(BlockState pState, Mirror pMirror) {
      return pState.setValue(ROTATION, Integer.valueOf(pMirror.mirror(pState.getValue(ROTATION), ROTATIONS)));
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder);
      pBuilder.add(ROTATION);
   }

   public interface Type extends StringRepresentable {
      Map<String, SkullBlock.Type> TYPES = new Object2ObjectArrayMap<>();
      Codec<SkullBlock.Type> CODEC = ExtraCodecs.stringResolverCodec(StringRepresentable::getSerializedName, TYPES::get);
   }

   public static enum Types implements SkullBlock.Type {
      SKELETON("skeleton"),
      WITHER_SKELETON("wither_skeleton"),
      PLAYER("player"),
      ZOMBIE("zombie"),
      CREEPER("creeper"),
      PIGLIN("piglin"),
      DRAGON("dragon");

      private final String name;

      private Types(String pName) {
         this.name = pName;
         TYPES.put(pName, this);
      }

      public String getSerializedName() {
         return this.name;
      }
   }
}