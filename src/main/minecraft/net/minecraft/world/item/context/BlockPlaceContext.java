package net.minecraft.world.item.context;

import javax.annotation.Nullable;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.fixes.data.Material1_19_4;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BlockPlaceContext extends UseOnContext {
   private final BlockPos relativePos;
   protected boolean replaceClicked = true;

   public BlockPlaceContext(Player pPlayer, InteractionHand pHand, ItemStack pItemStack, BlockHitResult pHitResult) {
      this(pPlayer.level(), pPlayer, pHand, pItemStack, pHitResult);
   }

   public BlockPlaceContext(UseOnContext pContext) {
      this(pContext.getLevel(), pContext.getPlayer(), pContext.getHand(), pContext.getItemInHand(), pContext.getHitResult());
   }

   protected BlockPlaceContext(Level pLevel, @Nullable Player pPlayer, InteractionHand pHand, ItemStack pItemStack, BlockHitResult pHitResult) {
      super(pLevel, pPlayer, pHand, pItemStack, pHitResult);
      this.relativePos = pHitResult.getBlockPos().relative(pHitResult.getDirection());
      this.replaceClicked = pLevel.getBlockState(pHitResult.getBlockPos()).canBeReplaced(this);
   }

   public static BlockPlaceContext at(BlockPlaceContext pContext, BlockPos pPos, Direction pDirection) {
      return new BlockPlaceContext(pContext.getLevel(), pContext.getPlayer(), pContext.getHand(), pContext.getItemInHand(), new BlockHitResult(new Vec3((double)pPos.getX() + 0.5D + (double)pDirection.getStepX() * 0.5D, (double)pPos.getY() + 0.5D + (double)pDirection.getStepY() * 0.5D, (double)pPos.getZ() + 0.5D + (double)pDirection.getStepZ() * 0.5D), pDirection, pPos, false));
   }

   public BlockPos getClickedPos() {
      return this.replaceClicked ? super.getClickedPos() : this.relativePos;
   }

   public boolean canPlace() {
      boolean returnValue = this.replaceClicked || this.getLevel().getBlockState(this.getClickedPos()).canBeReplaced(this);
      if (!returnValue && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         return (Material1_19_4.getMaterial(this.getLevel().getBlockState(this.getClickedPos())).equals(Material1_19_4.DECORATION) && Block.byItem(this.getItemInHand().getItem()).equals(Blocks.ANVIL));
      }
      return returnValue;
   }

   public boolean replacingClickedOnBlock() {
      return this.replaceClicked;
   }

   public Direction getNearestLookingDirection() {
      final BlockPlaceContext self = (BlockPlaceContext) (Object) this;
      final Player player = self.getPlayer();

      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
         final BlockPos placementPos = self.getClickedPos();
         final double blockPosCenterFactor = ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_10) ? 0.5 : 0;

         if (Math.abs(player.getX() - (placementPos.getX() + blockPosCenterFactor)) < 2 && Math.abs(player.getZ() - (placementPos.getZ() + blockPosCenterFactor)) < 2) {
            final double eyeY = player.getY() + player.getEyeHeight(player.getPose());

            if (eyeY - placementPos.getY() > 2) {
               return (Direction.DOWN);
            }

            if (placementPos.getY() - eyeY > 0) {
               return (Direction.UP);
            }
         }

        return (player.getDirection());
      }
      return Direction.orderedByNearest(this.getPlayer())[0];
   }

   public Direction getNearestLookingVerticalDirection() {
      return Direction.getFacingAxis(this.getPlayer(), Direction.Axis.Y);
   }

   public Direction[] getNearestLookingDirections() {
      Direction[] adirection = Direction.orderedByNearest(this.getPlayer());
      if (this.replaceClicked) {
         return adirection;
      } else {
         Direction direction = this.getClickedFace();

         int i;
         for(i = 0; i < adirection.length && adirection[i] != direction.getOpposite(); ++i) {
         }

         if (i > 0) {
            System.arraycopy(adirection, 0, adirection, 1, i);
            adirection[0] = direction.getOpposite();
         }

         return adirection;
      }
   }
}