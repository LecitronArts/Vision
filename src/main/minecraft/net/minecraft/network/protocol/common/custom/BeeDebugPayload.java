package net.minecraft.network.protocol.common.custom;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public record BeeDebugPayload(BeeDebugPayload.BeeInfo beeInfo) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/bee");

   public BeeDebugPayload(FriendlyByteBuf pBuffer) {
      this(new BeeDebugPayload.BeeInfo(pBuffer));
   }

   public void write(FriendlyByteBuf pBuffer) {
      this.beeInfo.write(pBuffer);
   }

   public ResourceLocation id() {
      return ID;
   }

   public static record BeeInfo(UUID uuid, int id, Vec3 pos, @Nullable Path path, @Nullable BlockPos hivePos, @Nullable BlockPos flowerPos, int travelTicks, Set<String> goals, List<BlockPos> blacklistedHives) {
      public BeeInfo(FriendlyByteBuf pBuffer) {
         this(pBuffer.readUUID(), pBuffer.readInt(), pBuffer.readVec3(), pBuffer.readNullable(Path::createFromStream), pBuffer.readNullable(FriendlyByteBuf::readBlockPos), pBuffer.readNullable(FriendlyByteBuf::readBlockPos), pBuffer.readInt(), pBuffer.readCollection(HashSet::new, FriendlyByteBuf::readUtf), pBuffer.readList(FriendlyByteBuf::readBlockPos));
      }

      public void write(FriendlyByteBuf pBuffer) {
         pBuffer.writeUUID(this.uuid);
         pBuffer.writeInt(this.id);
         pBuffer.writeVec3(this.pos);
         pBuffer.writeNullable(this.path, (p_297580_, p_297572_) -> {
            p_297572_.writeToStream(p_297580_);
         });
         pBuffer.writeNullable(this.hivePos, FriendlyByteBuf::writeBlockPos);
         pBuffer.writeNullable(this.flowerPos, FriendlyByteBuf::writeBlockPos);
         pBuffer.writeInt(this.travelTicks);
         pBuffer.writeCollection(this.goals, FriendlyByteBuf::writeUtf);
         pBuffer.writeCollection(this.blacklistedHives, FriendlyByteBuf::writeBlockPos);
      }

      public boolean hasHive(BlockPos pPos) {
         return Objects.equals(pPos, this.hivePos);
      }

      public String generateName() {
         return DebugEntityNameGenerator.getEntityName(this.uuid);
      }

      public String toString() {
         return this.generateName();
      }
   }
}