package net.minecraft.world.level.block.entity.trialspawner;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public interface PlayerDetector {
   PlayerDetector PLAYERS = (p_311100_, p_311024_, p_313168_) -> {
      return p_311100_.getPlayers((p_310868_) -> {
         return p_310868_.blockPosition().closerThan(p_311024_, (double)p_313168_) && !p_310868_.isCreative() && !p_310868_.isSpectator();
      }).stream().map(Entity::getUUID).toList();
   };
   PlayerDetector SHEEP = (p_310997_, p_312463_, p_310473_) -> {
      AABB aabb = (new AABB(p_312463_)).inflate((double)p_310473_);
      return p_310997_.getEntities(EntityType.SHEEP, aabb, LivingEntity::isAlive).stream().map(Entity::getUUID).toList();
   };

   List<UUID> detect(ServerLevel pLevel, BlockPos pPos, int pMaxDistance);
}