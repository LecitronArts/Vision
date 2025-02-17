package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SpawnerBlockEntity extends BlockEntity implements Spawner {
   private final BaseSpawner spawner = new BaseSpawner() {
      public void broadcastEvent(Level p_155767_, BlockPos p_155768_, int p_155769_) {
         p_155767_.blockEvent(p_155768_, Blocks.SPAWNER, p_155769_, 0);
      }

      public void setNextSpawnData(@Nullable Level p_155771_, BlockPos p_155772_, SpawnData p_155773_) {
         super.setNextSpawnData(p_155771_, p_155772_, p_155773_);
         if (p_155771_ != null) {
            BlockState blockstate = p_155771_.getBlockState(p_155772_);
            p_155771_.sendBlockUpdated(p_155772_, blockstate, blockstate, 4);
         }

      }
   };

   public SpawnerBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super(BlockEntityType.MOB_SPAWNER, pPos, pBlockState);
   }

   public void load(CompoundTag pTag) {
      super.load(pTag);
      this.spawner.load(this.level, this.worldPosition, pTag);
   }

   protected void saveAdditional(CompoundTag pTag) {
      super.saveAdditional(pTag);
      this.spawner.save(pTag);
   }

   public static void clientTick(Level pLevel, BlockPos pPos, BlockState pState, SpawnerBlockEntity pBlockEntity) {
      pBlockEntity.spawner.clientTick(pLevel, pPos);
   }

   public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, SpawnerBlockEntity pBlockEntity) {
      pBlockEntity.spawner.serverTick((ServerLevel)pLevel, pPos);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag() {
      CompoundTag compoundtag = this.saveWithoutMetadata();
      compoundtag.remove("SpawnPotentials");
      return compoundtag;
   }

   public boolean triggerEvent(int pId, int pType) {
      return this.spawner.onEventTriggered(this.level, pId) ? true : super.triggerEvent(pId, pType);
   }

   public boolean onlyOpCanSetNbt() {
      return true;
   }

   public void setEntityId(EntityType<?> pType, RandomSource pRandom) {
      this.spawner.setEntityId(pType, this.level, pRandom, this.worldPosition);
      this.setChanged();
   }

   public BaseSpawner getSpawner() {
      return this.spawner;
   }
}