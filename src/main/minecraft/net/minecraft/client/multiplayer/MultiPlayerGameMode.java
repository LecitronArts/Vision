package net.minecraft.client.multiplayer;

import baritone.utils.accessor.IPlayerControllerMP;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.Protocol1_17To1_16_4;
import de.florianmichael.viafabricplus.fixes.versioned.ActionResultException1_12_2;
import de.florianmichael.viafabricplus.fixes.versioned.ClientPlayerInteractionManager1_18_2;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.protocoltranslator.impl.provider.viaversion.ViaFabricPlusHandItemProvider;
import de.florianmichael.viafabricplus.protocoltranslator.translator.ItemTranslator;
import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@OnlyIn(Dist.CLIENT)
public class MultiPlayerGameMode implements IPlayerControllerMP {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   private final ClientPacketListener connection;
   private BlockPos destroyBlockPos = new BlockPos(-1, -1, -1);
   private ItemStack destroyingItem = ItemStack.EMPTY;
   private float destroyProgress;
   private float destroyTicks;
   private int destroyDelay;
   private boolean isDestroying;
   private GameType localPlayerMode = GameType.DEFAULT_MODE;
   private ItemStack viaFabricPlus$oldCursorStack;

   private List<ItemStack> viaFabricPlus$oldItems;

   private final ClientPlayerInteractionManager1_18_2 viaFabricPlus$1_18_2InteractionManager = new ClientPlayerInteractionManager1_18_2();

   @Nullable
   private GameType previousLocalPlayerMode;
   private int carriedIndex;
   @Override
   public void setIsHittingBlock(boolean isHittingBlock){
      this.isDestroying = isHittingBlock;
   }

   @Override
   public BlockPos getCurrentBlock() {
      return destroyBlockPos;
   };

   public MultiPlayerGameMode(Minecraft pMinecraft, ClientPacketListener pConnection) {
      this.minecraft = pMinecraft;
      this.connection = pConnection;
   }

   public void adjustPlayer(Player pPlayer) {
      this.localPlayerMode.updatePlayerAbilities(pPlayer.getAbilities());
   }

   public void setLocalMode(GameType pLocalPlayerMode, @Nullable GameType pPreviousLocalPlayerMode) {
      this.localPlayerMode = pLocalPlayerMode;
      this.previousLocalPlayerMode = pPreviousLocalPlayerMode;
      this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
   }

   public void setLocalMode(GameType pType) {
      if (pType != this.localPlayerMode) {
         this.previousLocalPlayerMode = this.localPlayerMode;
      }

      this.localPlayerMode = pType;
      this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
   }

   public boolean canHurtPlayer() {
      return this.localPlayerMode.isSurvival();
   }

   public boolean destroyBlock(BlockPos pPos) {
      if (this.minecraft.player.blockActionRestricted(this.minecraft.level, pPos, this.localPlayerMode)) {

         return false;
      } else {
         Level level = this.minecraft.level;
         BlockState blockstate = level.getBlockState(pPos);
         if (!this.minecraft.player.getMainHandItem().getItem().canAttackBlock(blockstate, level, pPos, this.minecraft.player)) {
            return false;
         } else {
            Block block = blockstate.getBlock();
            if (block instanceof GameMasterBlock && !this.minecraft.player.canUseGameMasterBlocks()) {
               return false;
            } else if (blockstate.isAir()) {
               return false;
            } else {
               block.playerWillDestroy(level, pPos, blockstate, this.minecraft.player);
               FluidState fluidstate = level.getFluidState(pPos);
               boolean flag = level.setBlock(pPos, fluidstate.createLegacyBlock(), 11);
               if (flag) {
                  block.destroy(level, pPos, blockstate);
               }
               if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_3)) {
                  this.destroyBlockPos = new BlockPos(this.destroyBlockPos.getX(), -1, this.destroyBlockPos.getZ());
               }
               return flag;
            }
         }
      }

   }


   public boolean startDestroyBlock(BlockPos pLoc, Direction pFace) {
      if (this.minecraft.player.blockActionRestricted(this.minecraft.level, pLoc, this.localPlayerMode)) {
         return false;
      } else if (!this.minecraft.level.getWorldBorder().isWithinBounds(pLoc)) {
         return false;
      } else {
         if (this.localPlayerMode.isCreative()) {
            BlockState blockstate = this.minecraft.level.getBlockState(pLoc);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pLoc, blockstate, 1.0F);
            this.startPrediction(this.minecraft.level, (p_233757_) -> {
               if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
                  this.viaFabricPlus$extinguishFire(pLoc, pFace);
                  this.destroyBlock(pLoc);
               } else {
                  this.destroyBlock(pLoc);
               }
   /*            this.destroyBlock(pLoc);*/

               return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pLoc, pFace, p_233757_);
            });
            this.destroyDelay = 5;
         } else if (!this.isDestroying || !this.sameDestroyTarget(pLoc)) {
            if (this.isDestroying) {
               ServerboundPlayerActionPacket.Action action = ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK;
               BlockPos pos = this.destroyBlockPos;
               if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_14_4, ProtocolVersion.v1_18_2)) {
                  this.viaFabricPlus$1_18_2InteractionManager.trackPlayerAction(action, pos);
               }
               this.connection.send(new ServerboundPlayerActionPacket(action, pos, pFace));
            }

            BlockState blockstate1 = this.minecraft.level.getBlockState(pLoc);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pLoc, blockstate1, 0.0F);
            this.startPrediction(this.minecraft.level, (p_233728_) -> {
               boolean flag = !blockstate1.isAir();
               if (flag && this.destroyProgress == 0.0F) {
                  blockstate1.attack(this.minecraft.level, pLoc, this.minecraft.player);
               }

               if (flag && blockstate1.getDestroyProgress(this.minecraft.player, this.minecraft.player.level(), pLoc) >= 1.0F) {
                  this.destroyBlock(pLoc);
               } else {
                  this.isDestroying = true;
                  this.destroyBlockPos = pLoc;
                  this.destroyingItem = this.minecraft.player.getMainHandItem();
                  this.destroyProgress = 0.0F;
                  this.destroyTicks = 0.0F;
                  this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
               }

               return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pLoc, pFace, p_233728_);
            });
         }

         return true;
      }
   }

   public void stopDestroyBlock() {
      if (this.isDestroying) {
         BlockState blockstate = this.minecraft.level.getBlockState(this.destroyBlockPos);
         this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, this.destroyBlockPos, blockstate, -1.0F);
         ServerboundPlayerActionPacket.Action action = ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK;
         BlockPos pos = this.destroyBlockPos;
         if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_14_4, ProtocolVersion.v1_18_2)) {
            this.viaFabricPlus$1_18_2InteractionManager.trackPlayerAction(action, pos);
         }
         this.connection.send(new ServerboundPlayerActionPacket(action, pos, Direction.DOWN));
         this.isDestroying = false;
         this.destroyProgress = 0.0F;
         this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, -1);
         this.minecraft.player.resetAttackStrengthTicker();
      }

   }

   public boolean continueDestroyBlock(BlockPos pPosBlock, Direction pDirectionFacing) {
      this.ensureHasSentCarriedItem();
      if (this.destroyDelay > 0) {
         --this.destroyDelay;
         return true;
      } else if (this.localPlayerMode.isCreative() && this.minecraft.level.getWorldBorder().isWithinBounds(pPosBlock)) {
         this.destroyDelay = 5;
         BlockState blockstate1 = this.minecraft.level.getBlockState(pPosBlock);
         this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pPosBlock, blockstate1, 1.0F);
         this.startPrediction(this.minecraft.level, (p_233753_) -> {
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
                this.viaFabricPlus$extinguishFire(pPosBlock,pDirectionFacing);
               this.destroyBlock(pPosBlock);
            } else {
               this.destroyBlock(pPosBlock);
            }
            /*this.destroyBlock(pPosBlock);*/
            return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pPosBlock, pDirectionFacing, p_233753_);
         });
         return true;
      } else if (this.sameDestroyTarget(pPosBlock)) {
         BlockState blockstate = this.minecraft.level.getBlockState(pPosBlock);
         if (blockstate.isAir()) {
            this.isDestroying = false;
            return false;
         } else {
            this.destroyProgress += blockstate.getDestroyProgress(this.minecraft.player, this.minecraft.player.level(), pPosBlock);
            if (this.destroyTicks % 4.0F == 0.0F) {
               SoundType soundtype = blockstate.getSoundType();
               this.minecraft.getSoundManager().play(new SimpleSoundInstance(soundtype.getHitSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 8.0F, soundtype.getPitch() * 0.5F, SoundInstance.createUnseededRandom(), pPosBlock));
            }

            ++this.destroyTicks;
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pPosBlock, blockstate, Mth.clamp(this.destroyProgress, 0.0F, 1.0F));
            if (this.destroyProgress >= 1.0F) {
               this.isDestroying = false;
               this.startPrediction(this.minecraft.level, (p_233739_) -> {
                  this.destroyBlock(pPosBlock);
                  return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pPosBlock, pDirectionFacing, p_233739_);
               });
               this.destroyProgress = 0.0F;
               this.destroyTicks = 0.0F;
               this.destroyDelay = 5;
            }

            this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
            return true;
         }
      } else {
         return this.startDestroyBlock(pPosBlock, pDirectionFacing);
      }
   }

   private void startPrediction(ClientLevel pLevel, PredictiveAction pAction) {
      if (ProtocolTranslator.getTargetVersion().betweenInclusive(ProtocolVersion.v1_14_4, ProtocolVersion.v1_18_2) && pAction instanceof ServerboundPlayerActionPacket playerActionC2SPacket) {
         this.viaFabricPlus$1_18_2InteractionManager.trackPlayerAction(playerActionC2SPacket.getAction(), playerActionC2SPacket.getPos());
      }
      try (BlockStatePredictionHandler blockstatepredictionhandler = pLevel.getBlockStatePredictionHandler().startPredicting()) {
         int i = blockstatepredictionhandler.currentSequence();
         Packet<ServerGamePacketListener> packet = pAction.predict(i);
         this.connection.send(packet);
      }

   }

   public float getPickRange() {
      return Player.getPickRange(this.localPlayerMode.isCreative());
   }

   public void tick() {
      this.ensureHasSentCarriedItem();
      if (this.connection.getConnection().isConnected()) {
         this.connection.getConnection().tick();
      } else {
         this.connection.getConnection().handleDisconnection();
      }

   }

   private boolean sameDestroyTarget(BlockPos pPos) {
      ItemStack itemstack = this.minecraft.player.getMainHandItem();
      return pPos.equals(this.destroyBlockPos) && ItemStack.isSameItemSameTags(itemstack, this.destroyingItem);
   }
   @Override
   public void callSyncCurrentPlayItem(){
      this.ensureHasSentCarriedItem();
   }
   public void ensureHasSentCarriedItem() {
      int i = this.minecraft.player.getInventory().selected;
      if (i != this.carriedIndex) {
         this.carriedIndex = i;
         this.connection.send(new ServerboundSetCarriedItemPacket(this.carriedIndex));
      }

   }

   public InteractionResult useItemOn(LocalPlayer pPlayer, InteractionHand pHand, BlockHitResult pResult) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8) && !InteractionHand.MAIN_HAND.equals(pHand)) {
         return (InteractionResult.PASS);
      }
      this.ensureHasSentCarriedItem();
      if (!this.minecraft.level.getWorldBorder().isWithinBounds(pResult.getBlockPos())) {
         return InteractionResult.FAIL;
      } else {
         MutableObject<InteractionResult> mutableobject = new MutableObject<>();
         /*
           @author RK_01
          * @reason Block place fix
          */
         try {
            this.startPrediction(this.minecraft.level, (p_233745_) ->  {
               if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
                  ViaFabricPlusHandItemProvider.lastUsedItem =pPlayer.getItemInHand(pHand).copy();
               }
               try {
                  mutableobject.setValue(this.performUseItemOn(pPlayer, pHand, pResult));
                  return new ServerboundUseItemOnPacket(pHand, pResult, p_233745_);
               } catch (ActionResultException1_12_2 e) {
                  mutableobject.setValue(e.getActionResult());
                  throw e;
               }
            });
         } catch (ActionResultException1_12_2 ignored) {
         }

         return mutableobject.getValue();
      }
   }

   private InteractionResult performUseItemOn(LocalPlayer pPlayer, InteractionHand pHand, BlockHitResult pResult) {
      BlockPos blockpos = pResult.getBlockPos();
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      if (this.localPlayerMode == GameType.SPECTATOR) {
         return InteractionResult.SUCCESS;
      } else {
         boolean flag = !pPlayer.getMainHandItem().isEmpty() || !pPlayer.getOffhandItem().isEmpty();
         boolean flag1 = pPlayer.isSecondaryUseActive() && flag;
         if (!flag1) {
            BlockState blockstate = this.minecraft.level.getBlockState(blockpos);
            if (!this.connection.isFeatureEnabled(blockstate.getBlock().requiredFeatures())) {
               return InteractionResult.FAIL;
            }

            InteractionResult interactionresult = blockstate.use(this.minecraft.level, pPlayer, pHand, pResult);
            if (interactionresult.consumesAction()) {
               return interactionresult;
            }
         }

         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            final ItemStack itemStack = pPlayer.getItemInHand(pHand);
            BlockHitResult checkHitResult = pResult;
            if (itemStack.getItem() instanceof BlockItem) {
               final BlockState clickedBlock = this.minecraft.level.getBlockState(pResult.getBlockPos());
               if (clickedBlock.getBlock().equals(Blocks.SNOW)) {
                  if (clickedBlock.getValue(SnowLayerBlock.LAYERS) == 1) {
                     checkHitResult = pResult.withDirection(Direction.UP);
                  }
               }
               final UseOnContext itemUsageContext = new UseOnContext(pPlayer, pHand, checkHitResult);
               final BlockPlaceContext itemPlacementContext = new BlockPlaceContext(itemUsageContext);
               if (!itemPlacementContext.canPlace() || ((BlockItem) itemPlacementContext.getItemInHand().getItem()).getPlacementState(itemPlacementContext) == null) {
                  throw new ActionResultException1_12_2(InteractionResult.PASS);
               }
            }

            this.connection.send(new ServerboundUseItemOnPacket(pHand, pResult, 0));
            if (itemStack.isEmpty()) {
               throw new ActionResultException1_12_2(InteractionResult.PASS);
            }
            final UseOnContext itemUsageContext = new UseOnContext(pPlayer, pHand, checkHitResult);
            InteractionResult actionResult;
            if (this.localPlayerMode.isCreative()) {
               final int count = itemStack.getCount();
               actionResult = itemStack.useOn(itemUsageContext);
               itemStack.setCount(count);
            } else {
               actionResult = itemStack.useOn(itemUsageContext);
            }
            if (!actionResult.consumesAction()) {
               actionResult = InteractionResult.PASS; // In <= 1.12.2 FAIL is the same as PASS
            }
            throw new ActionResultException1_12_2(actionResult);
         }

         if (!itemstack.isEmpty() && !pPlayer.getCooldowns().isOnCooldown(itemstack.getItem())) {
            UseOnContext useoncontext = new UseOnContext(pPlayer, pHand, pResult);
            InteractionResult interactionresult1;
            if (this.localPlayerMode.isCreative()) {
               int i = itemstack.getCount();
               interactionresult1 = itemstack.useOn(useoncontext);
               itemstack.setCount(i);
            } else {
               interactionresult1 = itemstack.useOn(useoncontext);
            }

            return interactionresult1;
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   public InteractionResult useItem(Player pPlayer, InteractionHand pHand) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8) && !InteractionHand.MAIN_HAND.equals(pHand)) {
         return (InteractionResult.PASS);
      }
      if (this.localPlayerMode == GameType.SPECTATOR) {
         return InteractionResult.PASS;
      } else {
         this.ensureHasSentCarriedItem();
         if(ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_16_4)) this.connection.send(new ServerboundMovePlayerPacket.PosRot(pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), pPlayer.getYRot(), pPlayer.getXRot(), pPlayer.onGround()));
         MutableObject<InteractionResult> mutableobject = new MutableObject<>();
         this.startPrediction(this.minecraft.level, (p_233720_) -> {
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
               ViaFabricPlusHandItemProvider.lastUsedItem = pPlayer.getItemInHand(pHand).copy();
            }
            ServerboundUseItemPacket serverbounduseitempacket = new ServerboundUseItemPacket(pHand, p_233720_);
            ItemStack itemstack = pPlayer.getItemInHand(pHand);
            if (pPlayer.getCooldowns().isOnCooldown(itemstack.getItem())) {
               mutableobject.setValue(InteractionResult.PASS);
               return serverbounduseitempacket;
            } else {
               InteractionResultHolder<ItemStack> interactionresultholder = itemstack.use(this.minecraft.level, pPlayer, pHand);
               ItemStack itemstack1 = interactionresultholder.getObject();
               if (itemstack1 != itemstack) {
                  pPlayer.setItemInHand(pHand, itemstack1);
               }

               mutableobject.setValue(interactionresultholder.getResult());
               return serverbounduseitempacket;
            }
         });
         return mutableobject.getValue();
      }
   }

   public LocalPlayer createPlayer(ClientLevel pLevel, StatsCounter pStatsManager, ClientRecipeBook pRecipes) {
      return this.createPlayer(pLevel, pStatsManager, pRecipes, false, false);
   }

   public LocalPlayer createPlayer(ClientLevel pLevel, StatsCounter pStatsManager, ClientRecipeBook pRecipes, boolean pWasShiftKeyDown, boolean pWasSprinting) {
      return new LocalPlayer(this.minecraft, pLevel, this.connection, pStatsManager, pRecipes, pWasShiftKeyDown, pWasSprinting);
   }

   public void attack(Player pPlayer, Entity pTargetEntity) {
      this.ensureHasSentCarriedItem();
      this.connection.send(ServerboundInteractPacket.createAttackPacket(pTargetEntity, pPlayer.isShiftKeyDown()));
      if (this.localPlayerMode != GameType.SPECTATOR) {
         pPlayer.attack(pTargetEntity);
         pPlayer.resetAttackStrengthTicker();
      }

   }

   public InteractionResult interact(Player pPlayer, Entity pTarget, InteractionHand pHand) {
      this.ensureHasSentCarriedItem();
      this.connection.send(ServerboundInteractPacket.createInteractionPacket(pTarget, pPlayer.isShiftKeyDown(), pHand));
      return this.localPlayerMode == GameType.SPECTATOR ? InteractionResult.PASS : pPlayer.interactOn(pTarget, pHand);
   }

   public InteractionResult interactAt(Player pPlayer, Entity pTarget, EntityHitResult pRay, InteractionHand pHand) {
      this.ensureHasSentCarriedItem();
      Vec3 vec3 = pRay.getLocation().subtract(pTarget.getX(), pTarget.getY(), pTarget.getZ());
      this.connection.send(ServerboundInteractPacket.createInteractionPacket(pTarget, pPlayer.isShiftKeyDown(), pHand, vec3));
      return this.localPlayerMode == GameType.SPECTATOR ? InteractionResult.PASS : pTarget.interactAt(pPlayer, vec3, pHand);
   }

   public void handleInventoryMouseClick(int pContainerId, int pSlotId, int pMouseButton, ClickType pClickType, Player pPlayer) {
      AbstractContainerMenu abstractcontainermenu = pPlayer.containerMenu;
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_5tob1_5_2) && !pClickType.equals(ClickType.PICKUP)) {
         return;
      } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_6tor1_4_7) && !pClickType.equals(ClickType.PICKUP) && !pClickType.equals(ClickType.QUICK_MOVE) && !pClickType.equals(ClickType.SWAP) && !pClickType.equals(ClickType.CLONE)) {
         return;
      }
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2) && pClickType == ClickType.SWAP && pMouseButton == 40) { // Pressing 'F' in inventory
        return;
      }
      if (pContainerId != abstractcontainermenu.containerId) {
         LOGGER.warn("Ignoring click in mismatching container. Click in {}, player has {}.", pContainerId, abstractcontainermenu.containerId);
      } else {
         NonNullList<Slot> nonnulllist = abstractcontainermenu.slots;
         int i = nonnulllist.size();
         List<ItemStack> list = Lists.newArrayListWithCapacity(i);
         viaFabricPlus$oldCursorStack = minecraft.player.containerMenu.getCarried().copy();
         this.viaFabricPlus$oldItems = list;

         for (Slot slot : nonnulllist) {
            list.add(slot.getItem().copy());
         }

         abstractcontainermenu.clicked(pSlotId, pMouseButton, pClickType, pPlayer);
         Int2ObjectMap<ItemStack> int2objectmap = new Int2ObjectOpenHashMap<>();

         for (int j = 0; j < i; ++j) {
            ItemStack itemstack = list.get(j);
            ItemStack itemstack1 = nonnulllist.get(j).getItem();
            if (!ItemStack.matches(itemstack, itemstack1)) {
               int2objectmap.put(j, itemstack1.copy());
            }
         }

         boolean viaCheckShouldSend;

         Packet<?> packet = new ServerboundContainerClickPacket(pContainerId, abstractcontainermenu.getStateId(), pSlotId, pMouseButton, pClickType, abstractcontainermenu.getCarried().copy(), int2objectmap);

         try {
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4)) {
               ServerboundContainerClickPacket clickSlot = (ServerboundContainerClickPacket) packet;
               ItemStack slotItemBeforeModification;
               if (this.viaFabricPlus$shouldBeEmpty(clickSlot.getClickType(), clickSlot.getSlotNum())) {
                  slotItemBeforeModification = ItemStack.EMPTY;
               } else if (clickSlot.getSlotNum() < 0 || clickSlot.getSlotNum() >= viaFabricPlus$oldItems.size()) {
                  slotItemBeforeModification = viaFabricPlus$oldCursorStack;
               } else {
                  slotItemBeforeModification = viaFabricPlus$oldItems.get(clickSlot.getSlotNum());
               }

               final PacketWrapper clickWindowPacket = PacketWrapper.create(ServerboundPackets1_16_2.CLICK_WINDOW, (connection.getConnection()).viaFabricPlus$getUserConnection());
               clickWindowPacket.write(Type.UNSIGNED_BYTE, (short) clickSlot.getContainerId());
               clickWindowPacket.write(Type.SHORT, (short) clickSlot.getSlotNum());
               clickWindowPacket.write(Type.BYTE, (byte) clickSlot.getButtonNum());
               clickWindowPacket.write(Type.SHORT, ( minecraft.player.containerMenu).viaFabricPlus$incrementAndGetActionId());
               clickWindowPacket.write(Type.VAR_INT, clickSlot.getClickType().ordinal());
               clickWindowPacket.write(Type.ITEM1_13_2, ItemTranslator.mcToVia(slotItemBeforeModification, ProtocolVersion.v1_16_4));
               clickWindowPacket.scheduleSendToServer(Protocol1_17To1_16_4.class);

               viaFabricPlus$oldCursorStack = null;
               viaFabricPlus$oldItems = null;
               viaCheckShouldSend = false;
            } else {
               this.connection.send(packet);
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   public void handlePlaceRecipe(int pContainerId, RecipeHolder<?> pRecipe, boolean pShiftDown) {
      this.connection.send(new ServerboundPlaceRecipePacket(pContainerId, pRecipe, pShiftDown));
   }

   public void handleInventoryButtonClick(int pContainerId, int pButtonId) {
      this.connection.send(new ServerboundContainerButtonClickPacket(pContainerId, pButtonId));
   }

   public void handleCreativeModeItemAdd(ItemStack pStack, int pSlotId) {
      if (this.localPlayerMode.isCreative() && this.connection.isFeatureEnabled(pStack.getItem().requiredFeatures())) {
         this.connection.send(new ServerboundSetCreativeModeSlotPacket(pSlotId, pStack));
      }

   }

   public void handleCreativeModeItemDrop(ItemStack pStack) {
      if (this.localPlayerMode.isCreative() && !pStack.isEmpty() && this.connection.isFeatureEnabled(pStack.getItem().requiredFeatures())) {
         this.connection.send(new ServerboundSetCreativeModeSlotPacket(-1, pStack));
      }

   }

   public void releaseUsingItem(Player pPlayer) {
      this.ensureHasSentCarriedItem();
      this.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
      pPlayer.releaseUsingItem();
   }

   public boolean hasExperience() {
      if (VisualSettings.global().removeNewerHudElements.isEnabled()) {
         return false;
      }
      return this.localPlayerMode.isSurvival();
   }

   public boolean hasMissTime() {
      return !this.localPlayerMode.isCreative();
   }

   public boolean hasInfiniteItems() {
      return this.localPlayerMode.isCreative();
   }

   public boolean hasFarPickRange() {
      return this.localPlayerMode.isCreative();
   }

   public boolean isServerControlledInventory() {
      return this.minecraft.player.isPassenger() && this.minecraft.player.getVehicle() instanceof HasCustomInventoryScreen;
   }

   public boolean isAlwaysFlying() {
      return this.localPlayerMode == GameType.SPECTATOR;
   }

   @Nullable
   public GameType getPreviousPlayerMode() {
      return this.previousLocalPlayerMode;
   }

   public GameType getPlayerMode() {
      return this.localPlayerMode;
   }

   public boolean isDestroying() {
      return this.isDestroying;
   }

   public int getDestroyStage() {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_4)) {
         return ((int) (this.destroyProgress * 10.0F) - 1);
      }
      return this.destroyProgress > 0.0F ? (int)(this.destroyProgress * 10.0F) : -1;
   }

   public void handlePickItem(int pIndex) {
      this.connection.send(new ServerboundPickItemPacket(pIndex));
   }

   public void handleSlotStateChanged(int pSlotId, int pContainerId, boolean pNewState) {
      this.connection.send(new ServerboundContainerSlotStateChangedPacket(pSlotId, pContainerId, pNewState));
   }
   private boolean viaFabricPlus$extinguishFire(BlockPos blockPos, final Direction direction) {
      blockPos = blockPos.relative(direction);
      if (this.minecraft.level.getBlockState(blockPos).getBlock() == Blocks.FIRE) {
         this.minecraft.level.levelEvent(this.minecraft.player, 1009, blockPos, 0);
         this.minecraft.level.removeBlock(blockPos, false);
         return true;
      }
      return false;
   }

   private boolean viaFabricPlus$shouldBeEmpty(final ClickType type, final int slot) {
      // quick craft always uses empty stack for verification
      if (type == ClickType.QUICK_CRAFT) return true;

      // Special case: throw always uses empty stack for verification
      if (type == ClickType.THROW) return true;

      // quick move always uses empty stack for verification since 1.12
      if (type == ClickType.QUICK_MOVE && ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_11_1)) return true;

      // pickup with slot -999 (outside window) to throw items always uses empty stack for verification
      return type == ClickType.PICKUP && slot == -999;
   }

   public ClientPlayerInteractionManager1_18_2 viaFabricPlus$get1_18_2InteractionManager() {
      return this.viaFabricPlus$1_18_2InteractionManager;
   }
}