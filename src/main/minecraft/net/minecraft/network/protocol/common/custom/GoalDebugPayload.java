package net.minecraft.network.protocol.common.custom;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record GoalDebugPayload(int entityId, BlockPos pos, List<GoalDebugPayload.DebugGoal> goals) implements CustomPacketPayload {
   public static final ResourceLocation ID = new ResourceLocation("debug/goal_selector");

   public GoalDebugPayload(FriendlyByteBuf pBuffer) {
      this(pBuffer.readInt(), pBuffer.readBlockPos(), pBuffer.readList(GoalDebugPayload.DebugGoal::new));
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeInt(this.entityId);
      pBuffer.writeBlockPos(this.pos);
      pBuffer.writeCollection(this.goals, (p_298191_, p_298011_) -> {
         p_298011_.write(p_298191_);
      });
   }

   public ResourceLocation id() {
      return ID;
   }

   public static record DebugGoal(int priority, boolean isRunning, String name) {
      public DebugGoal(FriendlyByteBuf pBuffer) {
         this(pBuffer.readInt(), pBuffer.readBoolean(), pBuffer.readUtf(255));
      }

      public void write(FriendlyByteBuf pBuffer) {
         pBuffer.writeInt(this.priority);
         pBuffer.writeBoolean(this.isRunning);
         pBuffer.writeUtf(this.name);
      }
   }
}