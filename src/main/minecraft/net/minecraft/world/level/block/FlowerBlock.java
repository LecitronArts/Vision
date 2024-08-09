package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlowerBlock extends BushBlock implements SuspiciousEffectHolder {
   protected static final MapCodec<List<SuspiciousEffectHolder.EffectEntry>> EFFECTS_FIELD = SuspiciousEffectHolder.EffectEntry.LIST_CODEC.fieldOf("suspicious_stew_effects");
   public static final MapCodec<FlowerBlock> CODEC = RecordCodecBuilder.mapCodec((p_312173_) -> {
      return p_312173_.group(EFFECTS_FIELD.forGetter(FlowerBlock::getSuspiciousEffects), propertiesCodec()).apply(p_312173_, FlowerBlock::new);
   });
   protected static final float AABB_OFFSET = 3.0F;
   protected static final VoxelShape SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 10.0D, 11.0D);
   private final List<SuspiciousEffectHolder.EffectEntry> suspiciousStewEffects;

   public MapCodec<? extends FlowerBlock> codec() {
      return CODEC;
   }

   public FlowerBlock(MobEffect pSuspiciousStewEffect, int pEffectDuration, BlockBehaviour.Properties pProperties) {
      this(makeEffectList(pSuspiciousStewEffect, pEffectDuration), pProperties);
   }

   public FlowerBlock(List<SuspiciousEffectHolder.EffectEntry> p_310931_, BlockBehaviour.Properties p_309749_) {
      super(p_309749_);
      this.suspiciousStewEffects = p_310931_;
   }

   protected static List<SuspiciousEffectHolder.EffectEntry> makeEffectList(MobEffect pEffect, int pDuration) {
      int i;
      if (pEffect.isInstantenous()) {
         i = pDuration;
      } else {
         i = pDuration * 20;
      }

      return List.of(new SuspiciousEffectHolder.EffectEntry(pEffect, i));
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      Vec3 vec3 = pState.getOffset(pLevel, pPos);
      return SHAPE.move(vec3.x, vec3.y, vec3.z);
   }

   public List<SuspiciousEffectHolder.EffectEntry> getSuspiciousEffects() {
      return this.suspiciousStewEffects;
   }
}