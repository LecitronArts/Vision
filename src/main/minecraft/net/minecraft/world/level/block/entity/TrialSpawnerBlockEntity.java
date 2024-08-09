package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.trialspawner.PlayerDetector;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.slf4j.Logger;

public class TrialSpawnerBlockEntity extends BlockEntity implements Spawner, TrialSpawner.StateAccessor {
   private static final Logger LOGGER = LogUtils.getLogger();
   private TrialSpawner trialSpawner;

   public TrialSpawnerBlockEntity(BlockPos pPos, BlockState pState) {
      super(BlockEntityType.TRIAL_SPAWNER, pPos, pState);
      PlayerDetector playerdetector = PlayerDetector.PLAYERS;
      this.trialSpawner = new TrialSpawner(this, playerdetector);
   }

   public void load(CompoundTag pTag) {
      super.load(pTag);
      this.trialSpawner.codec().parse(NbtOps.INSTANCE, pTag).resultOrPartial(LOGGER::error).ifPresent((p_311010_) -> {
         this.trialSpawner = p_311010_;
      });
      if (this.level != null) {
         this.markUpdated();
      }

   }

   protected void saveAdditional(CompoundTag pTag) {
      super.saveAdditional(pTag);
      this.trialSpawner.codec().encodeStart(NbtOps.INSTANCE, this.trialSpawner).get().ifLeft((p_312114_) -> {
         pTag.merge((CompoundTag)p_312114_);
      }).ifRight((p_311966_) -> {
         LOGGER.warn("Failed to encode TrialSpawner {}", (Object)p_311966_.message());
      });
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag() {
      return this.trialSpawner.getData().getUpdateTag(this.getBlockState().getValue(TrialSpawnerBlock.STATE));
   }

   public boolean onlyOpCanSetNbt() {
      return true;
   }

   public void setEntityId(EntityType<?> pEntityType, RandomSource pRandom) {
      this.trialSpawner.getData().setEntityId(this.trialSpawner, pRandom, pEntityType);
      this.setChanged();
   }

   public TrialSpawner getTrialSpawner() {
      return this.trialSpawner;
   }

   public TrialSpawnerState getState() {
      return !this.getBlockState().hasProperty(BlockStateProperties.TRIAL_SPAWNER_STATE) ? TrialSpawnerState.INACTIVE : this.getBlockState().getValue(BlockStateProperties.TRIAL_SPAWNER_STATE);
   }

   public void setState(Level pLevel, TrialSpawnerState pState) {
      this.setChanged();
      pLevel.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(BlockStateProperties.TRIAL_SPAWNER_STATE, pState));
   }

   public void markUpdated() {
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }

   }
}