package net.minecraft.client.player;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.PlayerUpdateEvent;
import baritone.api.event.events.SprintStateEvent;
import baritone.api.event.events.type.EventState;
import baritone.behavior.LookBehavior;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.DebugSettings;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.client.gui.screens.inventory.JigsawBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.MinecartCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.AmbientSoundHandler;
import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.client.resources.sounds.BubbleColumnAmbientSoundHandler;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundHandler;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundInstances;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import net.raphimc.vialegacy.protocols.release.protocol1_6_1to1_5_2.Protocol1_6_1to1_5_2;
import net.raphimc.vialegacy.protocols.release.protocol1_6_1to1_5_2.ServerboundPackets1_5_2;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class LocalPlayer extends AbstractClientPlayer {
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final int POSITION_REMINDER_INTERVAL = 20;
   private static final int WATER_VISION_MAX_TIME = 600;
   private static final int WATER_VISION_QUICK_TIME = 100;
   private static final float WATER_VISION_QUICK_PERCENT = 0.6F;
   private static final double SUFFOCATING_COLLISION_CHECK_SCALE = 0.35D;
   private static final double MINOR_COLLISION_ANGLE_THRESHOLD_RADIAN = (double)0.13962634F;
   private static final float DEFAULT_SNEAKING_MOVEMENT_FACTOR = 0.3F;
   public final ClientPacketListener connection;
   private final StatsCounter stats;
   private final ClientRecipeBook recipeBook;
   private final List<AmbientSoundHandler> ambientSoundHandlers = Lists.newArrayList();
   private int permissionLevel = 0;
   private double xLast;
   private double yLast1;
   private double zLast;
   private float yRotLast;
   private float xRotLast;
   private boolean lastOnGround;
   private boolean crouching;
   private boolean wasShiftKeyDown;
   private boolean wasSprinting;
   private int positionReminder;
   private boolean flashOnSetHealth;
   public Input input;
   protected final Minecraft minecraft;
   protected int sprintTriggerTime;
   public float yBob;
   public float xBob;
   public float yBobO;
   public float xBobO;
   private int jumpRidingTicks;
   private float jumpRidingScale;
   public float spinningEffectIntensity;
   public float oSpinningEffectIntensity;
   private boolean startedUsingItem;
   @Nullable
   private InteractionHand usingItemHand;
   private boolean handsBusy;
   private boolean autoJumpEnabled = true;
   private int autoJumpTime;
   private boolean wasFallFlying;
   private int waterVisionTime;
   private boolean showDeathScreen = true;
   private boolean doLimitedCrafting = false;

   public LocalPlayer(Minecraft pMinecraft, ClientLevel pClientLevel, ClientPacketListener pConnection, StatsCounter pStats, ClientRecipeBook pRecipeBook, boolean pWasShiftKeyDown, boolean pWasSprinting) {
      super(pClientLevel, pConnection.getLocalGameProfile());
      this.minecraft = pMinecraft;
      this.connection = pConnection;
      this.stats = pStats;
      this.recipeBook = pRecipeBook;
      this.wasShiftKeyDown = pWasShiftKeyDown;
      this.wasSprinting = pWasSprinting;
      this.ambientSoundHandlers.add(new UnderwaterAmbientSoundHandler(this, pMinecraft.getSoundManager()));
      this.ambientSoundHandlers.add(new BubbleColumnAmbientSoundHandler(this));
      this.ambientSoundHandlers.add(new BiomeAmbientSoundsHandler(this, pMinecraft.getSoundManager(), pClientLevel.getBiomeManager()));
   }

   public boolean hurt(DamageSource pSource, float pAmount) {
      return false;
   }

   public void heal(float pHealAmount) {
   }

   public boolean startRiding(Entity pEntity, boolean pForce) {
      if (!super.startRiding(pEntity, pForce)) {

         return false;
      } else {
         if (pEntity instanceof AbstractMinecart) {
            this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, (AbstractMinecart)pEntity, true));
            this.minecraft.getSoundManager().play(new RidingMinecartSoundInstance(this, (AbstractMinecart)pEntity, false));
         }
         if ( pEntity instanceof Boat && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_18)) {
            this.yRotO = pEntity.getYRot();
            this.setYRot(pEntity.getYRot());
            this.setYHeadRot(pEntity.getYRot());
         }
         return true;
      }
   }

   public void removeVehicle() {
      super.removeVehicle();
      this.handsBusy = false;
   }

   public float getViewXRot(float pPartialTick) {
      return this.getXRot();
   }

   public float getViewYRot(float pPartialTick) {
      return this.isPassenger() ? super.getViewYRot(pPartialTick) : this.getYRot();
   }

   public void tick() {
      if (this.level().hasChunkAt(this.getBlockX(), this.getBlockZ())) {
         IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this);
         if (baritone != null) {
            baritone.getGameEventHandler().onPlayerUpdate(new PlayerUpdateEvent(EventState.PRE));
         }
         super.tick();
         if (this.isPassenger()) {
            this.connection.send(new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround()));
            this.connection.send(new ServerboundPlayerInputPacket(this.xxa, this.zza, this.input.jumping, this.input.shiftKeyDown));
            Entity entity = this.getRootVehicle();
            if (entity != this && entity.isControlledByLocalInstance()) {
               //this.connection.send(new ServerboundMoveVehiclePacket(entity));
               if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_5_2)) {
                  try {
                     final PacketWrapper playerPosition = PacketWrapper.create(ServerboundPackets1_5_2.PLAYER_POSITION_AND_ROTATION, ( this.connection.getConnection()).viaFabricPlus$getUserConnection());
                     playerPosition.write(Type.DOUBLE, this.getDeltaMovement().x); // x
                     playerPosition.write(Type.DOUBLE, -999.0D); // y
                     playerPosition.write(Type.DOUBLE, -999.0D); // stance
                     playerPosition.write(Type.DOUBLE, this.getDeltaMovement().z); // z
                     playerPosition.write(Type.FLOAT, this.getYRot()); // yaw
                     playerPosition.write(Type.FLOAT, this.getXRot()); // pitch
                     playerPosition.write(Type.BOOLEAN, this.onGround()); // onGround
                     playerPosition.scheduleSendToServer(Protocol1_6_1to1_5_2.class);
                  } catch (Exception e){
                     e.printStackTrace();
                  }
               } else {
                  this.connection.send(new ServerboundMoveVehiclePacket(entity));
               }
               if (ProtocolTranslator.getTargetVersion().newerThanOrEqualTo(ProtocolVersion.v1_19_3))
                  this.sendIsSprintingIfNeeded();
            }
         } else {
            this.sendPosition();
         }

         for(AmbientSoundHandler ambientsoundhandler : this.ambientSoundHandlers) {
            ambientsoundhandler.tick();
         }

      }
   }

   public float getCurrentMood() {
      for(AmbientSoundHandler ambientsoundhandler : this.ambientSoundHandlers) {
         if (ambientsoundhandler instanceof BiomeAmbientSoundsHandler) {
            return ((BiomeAmbientSoundsHandler)ambientsoundhandler).getMoodiness();
         }
      }

      return 0.0F;
   }

   private void sendPosition() {
      this.sendIsSprintingIfNeeded();
      boolean flag = this.isShiftKeyDown();
      if (flag != this.wasShiftKeyDown) {
         ServerboundPlayerCommandPacket.Action serverboundplayercommandpacket$action = flag ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY : ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY;
         this.connection.send(new ServerboundPlayerCommandPacket(this, serverboundplayercommandpacket$action));
         this.wasShiftKeyDown = flag;
      }

      if (this.isControlledCamera()) {
         double d4 = this.getX() - this.xLast;
         double d0 = this.getY() - this.yLast1;
         double d1 = this.getZ() - this.zLast;
         double d2 = (double)(this.getYRot() - this.yRotLast);
         double d3 = (double)(this.getXRot() - this.xRotLast);

         ++this.positionReminder;
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            this.positionReminder = positionReminder - 1; // Reverting original operation
         }

         double viaFixDouble ;
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_18)) {
            viaFixDouble = 9.0E-4D;
         } else {
            viaFixDouble = Mth.square(2.0E-4D);
         }
         boolean flag1 = Mth.lengthSquared(d4, d0, d1) > /*Mth.square(2.0E-4D)*/ viaFixDouble || this.positionReminder >= 20;
         boolean flag2 = d2 != 0.0D || d3 != 0.0D;
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            this.positionReminder++;
         }

         boolean viaFix1_8C03s; //1.8玩家每tick都有c03

         if (DebugSettings.global().sendIdlePacket.isEnabled()) {
            viaFix1_8C03s = !onGround();
         } else {
            viaFix1_8C03s = lastOnGround;
         }

         if (this.isPassenger()) {
            Vec3 vec3 = this.getDeltaMovement();
            this.connection.send(new ServerboundMovePlayerPacket.PosRot(vec3.x, -999.0D, vec3.z, this.getYRot(), this.getXRot(), this.onGround()));
            flag1 = false;
         } else if (flag1 && flag2) {
            this.connection.send(new ServerboundMovePlayerPacket.PosRot(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot(), this.onGround()));
         } else if (flag1) {
            this.connection.send(new ServerboundMovePlayerPacket.Pos(this.getX(), this.getY(), this.getZ(), this.onGround()));
         } else if (flag2) {
            this.connection.send(new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround()));
         } else if (viaFix1_8C03s != onGround()) {

            this.connection.send(new ServerboundMovePlayerPacket.StatusOnly(this.onGround()));
         }

         if (flag1) {
            this.xLast = this.getX();
            this.yLast1 = this.getY();
            this.zLast = this.getZ();
            this.positionReminder = 0;
         }

         if (flag2) {
            this.yRotLast = this.getYRot();
            this.xRotLast = this.getXRot();
         }

         this.lastOnGround = this.onGround();
         this.autoJumpEnabled = this.minecraft.options.autoJump().get();
      }

   }

   private void sendIsSprintingIfNeeded() {
      boolean flag = this.isSprinting();
      if (flag != this.wasSprinting) {
         ServerboundPlayerCommandPacket.Action serverboundplayercommandpacket$action = flag ? ServerboundPlayerCommandPacket.Action.START_SPRINTING : ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;
         this.connection.send(new ServerboundPlayerCommandPacket(this, serverboundplayercommandpacket$action));
         this.wasSprinting = flag;
      }

   }

   public boolean drop(boolean pFullStack) {
      ServerboundPlayerActionPacket.Action serverboundplayeractionpacket$action = pFullStack ? ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS : ServerboundPlayerActionPacket.Action.DROP_ITEM;
      ItemStack itemstack = this.getInventory().removeFromSelected(pFullStack);
      this.connection.send(new ServerboundPlayerActionPacket(serverboundplayeractionpacket$action, BlockPos.ZERO, Direction.DOWN));
      return !itemstack.isEmpty();
   }

   public void swing(InteractionHand pHand) {
      super.swing(pHand);
      this.connection.send(new ServerboundSwingPacket(pHand));
   }

   public void respawn() {
      this.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
      KeyMapping.resetToggleKeys();
   }

   protected void actuallyHurt(DamageSource pDamageSrc, float pDamageAmount) {
      if (!this.isInvulnerableTo(pDamageSrc)) {
         this.setHealth(this.getHealth() - pDamageAmount);
      }
   }

   public void closeContainer() {
      this.connection.send(new ServerboundContainerClosePacket(this.containerMenu.containerId));
      this.clientSideCloseContainer();
   }

   public void clientSideCloseContainer() {
      super.closeContainer();
      this.minecraft.setScreen((Screen)null);
   }

   public void hurtTo(float pHealth) {
      if (this.flashOnSetHealth) {
         float f = this.getHealth() - pHealth;
         if (f <= 0.0F) {
            this.setHealth(pHealth);
            if (f < 0.0F) {
               this.invulnerableTime = 10;
            }
         } else {
            this.lastHurt = f;
            this.invulnerableTime = 20;
            this.setHealth(pHealth);
            this.hurtDuration = 10;
            this.hurtTime = this.hurtDuration;
         }
      } else {
         this.setHealth(pHealth);
         this.flashOnSetHealth = true;
      }

   }

   public void onUpdateAbilities() {
      this.connection.send(new ServerboundPlayerAbilitiesPacket(this.getAbilities()));
   }

   public boolean isLocalPlayer() {
      return true;
   }

   public boolean isSuppressingSlidingDownLadder() {
      return !this.getAbilities().flying && super.isSuppressingSlidingDownLadder();
   }

   public boolean canSpawnSprintParticle() {
      return !this.getAbilities().flying && super.canSpawnSprintParticle();
   }

   public boolean canSpawnSoulSpeedParticle() {
      return !this.getAbilities().flying && super.canSpawnSoulSpeedParticle();
   }

   protected void sendRidingJump() {
      this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP, Mth.floor(this.getJumpRidingScale() * 100.0F)));
   }

   public void sendOpenInventory() {
      this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
   }

   public StatsCounter getStats() {
      return this.stats;
   }

   public ClientRecipeBook getRecipeBook() {
      return this.recipeBook;
   }

   public void removeRecipeHighlight(RecipeHolder<?> pRecipe) {
      if (this.recipeBook.willHighlight(pRecipe)) {
         this.recipeBook.removeHighlight(pRecipe);
         this.connection.send(new ServerboundRecipeBookSeenRecipePacket(pRecipe));
      }

   }

   protected int getPermissionLevel() {
      return this.permissionLevel;
   }

   public void setPermissionLevel(int pPermissionLevel) {
      this.permissionLevel = pPermissionLevel;
   }

   public void displayClientMessage(Component pChatComponent, boolean pActionBar) {
      this.minecraft.getChatListener().handleSystemMessage(pChatComponent, pActionBar);
   }

   private void moveTowardsClosestSpace(double pX, double pZ) {
      BlockPos blockpos = BlockPos.containing(pX, this.getY(), pZ);
      if (this.suffocatesAt(blockpos)) {
         double d0 = pX - (double)blockpos.getX();
         double d1 = pZ - (double)blockpos.getZ();
         Direction direction = null;
         double d2 = Double.MAX_VALUE;
         Direction[] adirection = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};

         for(Direction direction1 : adirection) {
            double d3 = direction1.getAxis().choose(d0, 0.0D, d1);
            double d4 = direction1.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0D - d3 : d3;
            if (d4 < d2 && !this.suffocatesAt(blockpos.relative(direction1))) {
               d2 = d4;
               direction = direction1;
            }
         }

         if (direction != null) {
            Vec3 vec3 = this.getDeltaMovement();
            if (direction.getAxis() == Direction.Axis.X) {
               this.setDeltaMovement(0.1D * (double)direction.getStepX(), vec3.y, vec3.z);
            } else {
               this.setDeltaMovement(vec3.x, vec3.y, 0.1D * (double)direction.getStepZ());
            }
         }

      }
   }

   private boolean suffocatesAt(BlockPos pPos) {
      AABB aabb = this.getBoundingBox();
      AABB aabb1 = (new AABB((double)pPos.getX(), aabb.minY, (double)pPos.getZ(), (double)pPos.getX() + 1.0D, aabb.maxY, (double)pPos.getZ() + 1.0D)).deflate(1.0E-7D);
      return this.level().collidesWithSuffocatingBlock(this, aabb1);
   }

   public void setExperienceValues(float pCurrentXP, int pMaxXP, int pLevel) {
      this.experienceProgress = pCurrentXP;
      this.totalExperience = pMaxXP;
      this.experienceLevel = pLevel;
   }

   public void sendSystemMessage(Component pComponent) {
      this.minecraft.gui.getChat().addMessage(pComponent);
   }

   public void handleEntityEvent(byte pId) {
      if (pId >= 24 && pId <= 28) {
         this.setPermissionLevel(pId - 24);
      } else {
         super.handleEntityEvent(pId);
      }

   }

   public void setShowDeathScreen(boolean pShow) {
      this.showDeathScreen = pShow;
   }

   public boolean shouldShowDeathScreen() {
      return this.showDeathScreen;
   }

   public void setDoLimitedCrafting(boolean pDoLimitedCrafting) {
      this.doLimitedCrafting = pDoLimitedCrafting;
   }

   public boolean getDoLimitedCrafting() {
      return this.doLimitedCrafting;
   }

   public void playSound(SoundEvent pSound, float pVolume, float pPitch) {
      this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), pSound, this.getSoundSource(), pVolume, pPitch, false);
   }

   public void playNotifySound(SoundEvent pSound, SoundSource pSource, float pVolume, float pPitch) {
      this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), pSound, pSource, pVolume, pPitch, false);
   }

   public boolean isEffectiveAi() {
      return true;
   }

   public void startUsingItem(InteractionHand pHand) {
      ItemStack itemstack = this.getItemInHand(pHand);
      if (!itemstack.isEmpty() && !this.isUsingItem()) {
         super.startUsingItem(pHand);
         this.startedUsingItem = true;
         this.usingItemHand = pHand;
      }
   }

   public boolean isUsingItem() {
      return this.startedUsingItem;
   }

   public void stopUsingItem() {
      super.stopUsingItem();
      this.startedUsingItem = false;
   }

   public InteractionHand getUsedItemHand() {
      return Objects.requireNonNullElse(this.usingItemHand, InteractionHand.MAIN_HAND);
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
      super.onSyncedDataUpdated(pKey);
      if (DATA_LIVING_ENTITY_FLAGS.equals(pKey)) {
         boolean flag = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
         InteractionHand interactionhand = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
         if (flag && !this.startedUsingItem) {
            this.startUsingItem(interactionhand);
         } else if (!flag && this.startedUsingItem) {
            this.stopUsingItem();
         }
      }

      if (DATA_SHARED_FLAGS_ID.equals(pKey) && this.isFallFlying() && !this.wasFallFlying) {
         this.minecraft.getSoundManager().play(new ElytraOnPlayerSoundInstance(this));
      }

   }

   @Nullable
   public PlayerRideableJumping jumpableVehicle() {
      Entity entity = this.getControlledVehicle();
      if (entity instanceof PlayerRideableJumping playerrideablejumping) {
         if (playerrideablejumping.canJump()) {
            return playerrideablejumping;
         }
      }

      return null;
   }

   public float getJumpRidingScale() {
      return this.jumpRidingScale;
   }

   public boolean isTextFilteringEnabled() {
      return this.minecraft.isTextFilteringEnabled();
   }

   public void openTextEdit(SignBlockEntity pSignEntity, boolean pIsFrontText) {
      if (pSignEntity instanceof HangingSignBlockEntity hangingsignblockentity) {
         this.minecraft.setScreen(new HangingSignEditScreen(hangingsignblockentity, pIsFrontText, this.minecraft.isTextFilteringEnabled()));
      } else {
         this.minecraft.setScreen(new SignEditScreen(pSignEntity, pIsFrontText, this.minecraft.isTextFilteringEnabled()));
      }

   }

   public void openMinecartCommandBlock(BaseCommandBlock pCommandBlock) {
      this.minecraft.setScreen(new MinecartCommandBlockEditScreen(pCommandBlock));
   }

   public void openCommandBlock(CommandBlockEntity pCommandBlock) {
      this.minecraft.setScreen(new CommandBlockEditScreen(pCommandBlock));
   }

   public void openStructureBlock(StructureBlockEntity pStructure) {
      this.minecraft.setScreen(new StructureBlockEditScreen(pStructure));
   }

   public void openJigsawBlock(JigsawBlockEntity pJigsawBlockEntity) {
      this.minecraft.setScreen(new JigsawBlockEditScreen(pJigsawBlockEntity));
   }

   public void openItemGui(ItemStack pStack, InteractionHand pHand) {
      if (pStack.is(Items.WRITABLE_BOOK)) {
         this.minecraft.setScreen(new BookEditScreen(this, pStack, pHand));
      }

   }

   public void crit(Entity pEntityHit) {
      this.minecraft.particleEngine.createTrackingEmitter(pEntityHit, ParticleTypes.CRIT);
   }

   public void magicCrit(Entity pEntityHit) {
      this.minecraft.particleEngine.createTrackingEmitter(pEntityHit, ParticleTypes.ENCHANTED_HIT);
   }

   public boolean isShiftKeyDown() {
      return this.input != null && this.input.shiftKeyDown;
   }

   public boolean isCrouching() {
      return this.crouching;
   }

   public boolean isMovingSlowly() {
      return this.isCrouching() || this.isVisuallyCrawling();
   }

   public void serverAiStep() {
      super.serverAiStep();
      if (this.isControlledCamera()) {
         this.xxa = this.input.leftImpulse;
         this.zza = this.input.forwardImpulse;
         this.jumping = this.input.jumping;
         this.yBobO = this.yBob;
         this.xBobO = this.xBob;
         this.xBob += (this.getXRot() - this.xBob) * 0.5F;
         this.yBob += (this.getYRot() - this.yBob) * 0.5F;
      }

   }

   protected boolean isControlledCamera() {
      return this.minecraft.getCameraEntity() == this;
   }

   public void resetPos() {
      this.setPose(Pose.STANDING);
      if (this.level() != null) {
         for(double d0 = this.getY(); d0 > (double)this.level().getMinBuildHeight() && d0 < (double)this.level().getMaxBuildHeight(); ++d0) {
            this.setPos(this.getX(), d0, this.getZ());
            if (this.level().noCollision(this)) {
               break;
            }
         }

         this.setDeltaMovement(Vec3.ZERO);
         this.setXRot(0.0F);
      }

      this.setHealth(this.getMaxHealth());
      this.deathTime = 0;
   }
   private boolean isAllowFlying(Abilities capabilities) {
      IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this);
      if (baritone == null) {
         return capabilities.mayfly;
      }
      return !baritone.getPathingBehavior().isPathing() && capabilities.mayfly;
   }

   private boolean tryToStartFallFlying(final LocalPlayer instance) {
      IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer(instance);
      if (baritone != null && baritone.getPathingBehavior().isPathing()) {
         return false;
      }
      return instance.tryToStartFallFlying();
   }

   private boolean isKeyDown(KeyMapping keyBinding) {
      IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this);
      if (baritone == null) {
         return keyBinding.isDown();
      }
      SprintStateEvent event = new SprintStateEvent();
      baritone.getGameEventHandler().onPlayerSprintState(event);
      if (event.getState() != null) {
         return event.getState();
      }
      if (baritone != BaritoneAPI.getProvider().getPrimaryBaritone()) {
         // hitting control shouldn't make all bots sprint
         return false;
      }
      return keyBinding.isDown();
   }

   public void aiStep() {
      if (this.sprintTriggerTime > 0) {
         --this.sprintTriggerTime;
      }

      if (!(this.minecraft.screen instanceof ReceivingLevelScreen)) {
         this.handleNetherPortalClient();
      }
/*    this.isPassenger()*/
      boolean flag = this.input.jumping;
      boolean flag1 = this.input.shiftKeyDown;
      boolean flag2 = this.hasEnoughImpulseToStartSprinting(); //////
      this.crouching = !this.getAbilities().flying && ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_13_2) && !( ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_14_1) && this.isSwimming()) && !( ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_20) && this.isPassenger()) && this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING) && (this.isShiftKeyDown() || !this.isSleeping() && !this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING));
      float f = Mth.clamp(0.3F + EnchantmentHelper.getSneakingSpeedBonus(this), 0.0F, 1.0F);
      this.input.tick(this.isMovingSlowly(), f);
      this.minecraft.getTutorial().onInput(this.input);
      if (this.isUsingItem() && !this.isPassenger()) {
         this.input.leftImpulse *= 0.2F;
         this.input.forwardImpulse *= 0.2F;
         this.sprintTriggerTime = 0;
      }

      boolean flag3 = false;
      if (this.autoJumpTime > 0) {
         --this.autoJumpTime;
         flag3 = true;
         this.input.jumping = true;
      }

      if (!this.noPhysics) {
         this.moveTowardsClosestSpace(this.getX() - (double)this.getBbWidth() * 0.35D, this.getZ() + (double)this.getBbWidth() * 0.35D);
         this.moveTowardsClosestSpace(this.getX() - (double)this.getBbWidth() * 0.35D, this.getZ() - (double)this.getBbWidth() * 0.35D);
         this.moveTowardsClosestSpace(this.getX() + (double)this.getBbWidth() * 0.35D, this.getZ() - (double)this.getBbWidth() * 0.35D);
         this.moveTowardsClosestSpace(this.getX() + (double)this.getBbWidth() * 0.35D, this.getZ() + (double)this.getBbWidth() * 0.35D);
      }

      if (flag1) {
         this.sprintTriggerTime = 0;
      }

      boolean flag4 = /*ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_14_4) &&*/ this.canStartSprinting();
      boolean flag5 = this.isPassenger() ? this.getVehicle().onGround() : this.onGround();
      boolean flag6 = !flag1 && !flag2;
      if ((flag5 || this.isUnderWater()) && flag6 && flag4) {
         if (this.sprintTriggerTime <= 0 && !this.minecraft.options.keySprint.isDown()) {
            this.sprintTriggerTime = 7;
         } else {
            this.setSprinting(true);
         }
      }

      if ((!(ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_12_2) && this.isInWater()) || this.isUnderWater()) && flag4 && isKeyDown(this.minecraft.options.keySprint)) {
         this.setSprinting(true);
      }

      if (this.isSprinting()) {
         boolean viaFixSprint ;
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_1)) {
            viaFixSprint = input.forwardImpulse >= 0.8F;
         } else {
            viaFixSprint = input.hasForwardImpulse();
         }
         boolean flag7 = !viaFixSprint|| !this.hasEnoughFoodToStartSprinting();
         boolean flag8 = flag7 || this.horizontalCollision && !this.minorHorizontalCollision || (ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_12_2) && this.isInWater()) && !this.isUnderWater();
         if (this.isSwimming()) {
            if (!this.onGround() && !this.input.shiftKeyDown && flag7 || !(ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_12_2) && this.isInWater())) {
               this.setSprinting(false);
            }
         } else if (flag8) {
            this.setSprinting(false);
         }
      }

      boolean flag9 = false;
      if (isAllowFlying(this.getAbilities())) {
         if (this.minecraft.gameMode.isAlwaysFlying()) {
            if (!this.getAbilities().flying) {
               this.getAbilities().flying = true;
               flag9 = true;
               this.onUpdateAbilities();
            }
         } else if (!flag && this.input.jumping && !flag3) {
            if (this.jumpTriggerTime == 0) {
               this.jumpTriggerTime = 7;
            } else if (!this.isSwimming()) {
               this.getAbilities().flying = !this.getAbilities().flying;
               flag9 = true;
               this.onUpdateAbilities();
               this.jumpTriggerTime = 0;
            }
         }
      }

      if (this.input.jumping && !flag9 && !flag && !this.getAbilities().flying && !( ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_14_4) && this.isPassenger()) && ! ( ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_15_1) &&  this.onClimbable() )) {
         ItemStack itemstack = this.getItemBySlot(EquipmentSlot.CHEST);
         if (itemstack.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(itemstack) && this.tryToStartFallFlying(this)) {
            this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
         }
      }

      this.wasFallFlying = this.isFallFlying();
      if ((ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_12_2) && this.isInWater()) && this.input.shiftKeyDown && this.isAffectedByFluids()) {
         this.goDownInWater();
      }

      if (this.isEyeInFluid(FluidTags.WATER)) {
         int i = this.isSpectator() ? 10 : 1;
         this.waterVisionTime = Mth.clamp(this.waterVisionTime + i, 0, 600);
      } else if (this.waterVisionTime > 0) {
         this.isEyeInFluid(FluidTags.WATER);
         this.waterVisionTime = Mth.clamp(this.waterVisionTime - 10, 0, 600);
      }

      if (this.getAbilities().flying && this.isControlledCamera()) { //////
         int j = 0;
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_4)) {
            if (this.input.shiftKeyDown) {
               this.input.leftImpulse = (float) ((double) this.input.leftImpulse / 0.3D);
               this.input.forwardImpulse = (float) ((double) this.input.forwardImpulse / 0.3D);
            }
         }
         if (this.input.shiftKeyDown) {
            --j;
         }

         if (this.input.jumping) {
            ++j;
         }

         if (j != 0) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, (double)((float)j * this.getAbilities().getFlyingSpeed() * 3.0F), 0.0D));
         }
      }

      PlayerRideableJumping playerrideablejumping = this.jumpableVehicle();
      if (playerrideablejumping != null && playerrideablejumping.getJumpCooldown() == 0) {
         if (this.jumpRidingTicks < 0) {
            ++this.jumpRidingTicks;
            if (this.jumpRidingTicks == 0) {
               this.jumpRidingScale = 0.0F;
            }
         }

         if (flag && !this.input.jumping) {
            this.jumpRidingTicks = -10;
            playerrideablejumping.onPlayerJump(Mth.floor(this.getJumpRidingScale() * 100.0F));
            this.sendRidingJump();
         } else if (!flag && this.input.jumping) {
            this.jumpRidingTicks = 0;
            this.jumpRidingScale = 0.0F;
         } else if (flag) {
            ++this.jumpRidingTicks;
            if (this.jumpRidingTicks < 10) {
               this.jumpRidingScale = (float)this.jumpRidingTicks * 0.1F;
            } else {
               this.jumpRidingScale = 0.8F + 2.0F / (float)(this.jumpRidingTicks - 9) * 0.1F;
            }
         }
      } else {
         this.jumpRidingScale = 0.0F;
      }

      super.aiStep();
      if (this.onGround() && this.getAbilities().flying && !this.minecraft.gameMode.isAlwaysFlying()) {
         this.getAbilities().flying = false;
         this.onUpdateAbilities();
      }

   }

   protected void tickDeath() {
      ++this.deathTime;
      if (this.deathTime == 20) {
         this.remove(Entity.RemovalReason.KILLED);
      }

   }

   private void handleNetherPortalClient() {
      this.oSpinningEffectIntensity = this.spinningEffectIntensity;
      float f = 0.0F;
      if (this.isInsidePortal) {
         if (this.minecraft.screen != null && !this.minecraft.screen.isPauseScreen() && !(this.minecraft.screen instanceof DeathScreen)) {
            if (this.minecraft.screen instanceof AbstractContainerScreen) {
               this.closeContainer();
            }

            this.minecraft.setScreen((Screen)null);
         }

         if (this.spinningEffectIntensity == 0.0F) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRIGGER, this.random.nextFloat() * 0.4F + 0.8F, 0.25F));
         }

         f = 0.0125F;
         this.isInsidePortal = false;
      } else if (this.hasEffect(MobEffects.CONFUSION) && !this.getEffect(MobEffects.CONFUSION).endsWithin(60)) {
         f = 0.006666667F;
      } else if (this.spinningEffectIntensity > 0.0F) {
         f = -0.05F;
      }

      this.spinningEffectIntensity = Mth.clamp(this.spinningEffectIntensity + f, 0.0F, 1.0F);
      this.processPortalCooldown();
   }

   public void rideTick() {
      IBaritone baritone = BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this);
      if (baritone != null) {
         ((LookBehavior) baritone.getLookBehavior()).pig();
      }
      super.rideTick();
      this.handsBusy = false;
      Entity entity = this.getControlledVehicle();
      if (entity instanceof Boat boat) {
         boat.setInput(this.input.left, this.input.right, this.input.up, this.input.down);
         this.handsBusy |= this.input.left || this.input.right || this.input.up || this.input.down;
      }

   }

   public boolean isHandsBusy() {
      return this.handsBusy;
   }

   @Nullable
   public MobEffectInstance removeEffectNoUpdate(@Nullable MobEffect pMobEffect) {
      if (pMobEffect == MobEffects.CONFUSION) {
         this.oSpinningEffectIntensity = 0.0F;
         this.spinningEffectIntensity = 0.0F;
      }

      return super.removeEffectNoUpdate(pMobEffect);
   }

   public void move(MoverType pType, Vec3 pPos) {
      double d0 = this.getX();
      double d1 = this.getZ();
      super.move(pType, pPos);
      this.updateAutoJump((float)(this.getX() - d0), (float)(this.getZ() - d1));
   }

   public boolean isAutoJumpEnabled() {
      return this.autoJumpEnabled;
   }

   protected void updateAutoJump(float pMovementX, float pMovementZ) {
      if (this.canAutoJump()) {
         Vec3 vec3 = this.position();
         Vec3 vec31 = vec3.add((double)pMovementX, 0.0D, (double)pMovementZ);
         Vec3 vec32 = new Vec3((double)pMovementX, 0.0D, (double)pMovementZ);
         float f = this.getSpeed();
         float f1 = (float)vec32.lengthSqr();
         if (f1 <= 0.001F) {
            Vec2 vec2 = this.input.getMoveVector();
            float f2 = f * vec2.x;
            float f3 = f * vec2.y;
            float f4 = Mth.sin(this.getYRot() * ((float)Math.PI / 180F));
            float f5 = Mth.cos(this.getYRot() * ((float)Math.PI / 180F));
            vec32 = new Vec3((double)(f2 * f5 - f3 * f4), vec32.y, (double)(f3 * f5 + f2 * f4));
            f1 = (float)vec32.lengthSqr();
            if (f1 <= 0.001F) {
               return;
            }
         }
         float f12;
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_3)) {
            f1 = Float.intBitsToFloat(1597463007 - (Float.floatToIntBits(f1) >> 1));
            f12 = f1 * (1.5F - (0.5F * f1) *f1* f1);
         } else {
            f12 =  Mth.invSqrt(f1);
         }
        //float f12 = Mth.invSqrt(f1);
         Vec3 vec312 = vec32.scale((double)f12);
         Vec3 vec313 = this.getForward();
         float f13 = (float)(vec313.x * vec312.x + vec313.z * vec312.z);
         if (!(f13 < -0.15F)) {
            CollisionContext collisioncontext = CollisionContext.of(this);
            BlockPos blockpos = BlockPos.containing(this.getX(), this.getBoundingBox().maxY, this.getZ());
            BlockState blockstate = this.level().getBlockState(blockpos);
            if (blockstate.getCollisionShape(this.level(), blockpos, collisioncontext).isEmpty()) {
               blockpos = blockpos.above();
               BlockState blockstate1 = this.level().getBlockState(blockpos);
               if (blockstate1.getCollisionShape(this.level(), blockpos, collisioncontext).isEmpty()) {
                  float f6 = 7.0F;
                  float f7 = 1.2F;
                  if (this.hasEffect(MobEffects.JUMP)) {
                     f7 += (float)(this.getEffect(MobEffects.JUMP).getAmplifier() + 1) * 0.75F;
                  }

                  float f8 = Math.max(f * 7.0F, 1.0F / f12);
                  Vec3 vec34 = vec31.add(vec312.scale((double)f8));
                  float f9 = this.getBbWidth();
                  float f10 = this.getBbHeight();
                  AABB aabb = (new AABB(vec3, vec34.add(0.0D, (double)f10, 0.0D))).inflate((double)f9, 0.0D, (double)f9);
                  Vec3 $$23 = vec3.add(0.0D, (double)0.51F, 0.0D);
                  vec34 = vec34.add(0.0D, (double)0.51F, 0.0D);
                  Vec3 vec35 = vec312.cross(new Vec3(0.0D, 1.0D, 0.0D));
                  Vec3 vec36 = vec35.scale((double)(f9 * 0.5F));
                  Vec3 vec37 = $$23.subtract(vec36);
                  Vec3 vec38 = vec34.subtract(vec36);
                  Vec3 vec39 = $$23.add(vec36);
                  Vec3 vec310 = vec34.add(vec36);
                  Iterable<VoxelShape> iterable = this.level().getCollisions(this, aabb);
                  Iterator<AABB> iterator = StreamSupport.stream(iterable.spliterator(), false).flatMap((p_234124_) -> {
                     return p_234124_.toAabbs().stream();
                  }).iterator();
                  float f11 = Float.MIN_VALUE;

                  while(iterator.hasNext()) {
                     AABB aabb1 = iterator.next();
                     if (aabb1.intersects(vec37, vec38) || aabb1.intersects(vec39, vec310)) {
                        f11 = (float)aabb1.maxY;
                        Vec3 vec311 = aabb1.getCenter();
                        BlockPos blockpos1 = BlockPos.containing(vec311);

                        for(int i = 1; (float)i < f7; ++i) {
                           BlockPos blockpos2 = blockpos1.above(i);
                           BlockState blockstate2 = this.level().getBlockState(blockpos2);
                           VoxelShape voxelshape;
                           if (!(voxelshape = blockstate2.getCollisionShape(this.level(), blockpos2, collisioncontext)).isEmpty()) {
                              f11 = (float)voxelshape.max(Direction.Axis.Y) + (float)blockpos2.getY();
                              if ((double)f11 - this.getY() > (double)f7) {
                                 return;
                              }
                           }

                           if (i > 1) {
                              blockpos = blockpos.above();
                              BlockState blockstate3 = this.level().getBlockState(blockpos);
                              if (!blockstate3.getCollisionShape(this.level(), blockpos, collisioncontext).isEmpty()) {
                                 return;
                              }
                           }
                        }
                        break;
                     }
                  }

                  if (f11 != Float.MIN_VALUE) {
                     float f14 = (float)((double)f11 - this.getY());
                     if (!(f14 <= 0.5F) && !(f14 > f7)) {
                        this.autoJumpTime = 1;
                     }
                  }
               }
            }
         }
      }
   }

   protected boolean isHorizontalCollisionMinor(Vec3 pDeltaMovement) {
      float f = this.getYRot() * ((float)Math.PI / 180F);
      double d0 = (double)Mth.sin(f);
      double d1 = (double)Mth.cos(f);
      double d2 = (double)this.xxa * d1 - (double)this.zza * d0;
      double d3 = (double)this.zza * d1 + (double)this.xxa * d0;
      double d4 = Mth.square(d2) + Mth.square(d3);
      double d5 = Mth.square(pDeltaMovement.x) + Mth.square(pDeltaMovement.z);
      if (!(d4 < (double)1.0E-5F) && !(d5 < (double)1.0E-5F)) {
         double d6 = d2 * pDeltaMovement.x + d3 * pDeltaMovement.z;
         double d7 = Math.acos(d6 / Math.sqrt(d4 * d5));
         return d7 < (double)0.13962634F;
      } else {
         return false;
      }
   }

   private boolean canAutoJump() {
      return this.isAutoJumpEnabled() && this.autoJumpTime <= 0 && this.onGround() && !this.isStayingOnGroundSurface() && !this.isPassenger() && this.isMoving() && (double)this.getBlockJumpFactor() >= 1.0D;
   }

   private boolean isMoving() {
      Vec2 vec2 = this.input.getMoveVector();
      return vec2.x != 0.0F || vec2.y != 0.0F;
   }

   private boolean canStartSprinting() {
      return !this.isSprinting() && this.hasEnoughImpulseToStartSprinting() && this.hasEnoughFoodToStartSprinting() && !this.isUsingItem() && !this.hasEffect(MobEffects.BLINDNESS) && (!( ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_19_3) && this.isPassenger()) || this.vehicleCanSprint(this.getVehicle())) && !( ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_19_3) && this.isFallFlying());
   }

   private boolean vehicleCanSprint(Entity pVehicle) {
      return pVehicle.canSprint() && pVehicle.isControlledByLocalInstance();
   }

   private boolean hasEnoughImpulseToStartSprinting() {
      double d0 = 0.8D;
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_1)) {
         return (((LocalPlayer) (Object) this).input.forwardImpulse >= 0.8);
      }
      return this.isUnderWater() ? this.input.hasForwardImpulse() : (double)this.input.forwardImpulse >= 0.8D;
   }

   private boolean hasEnoughFoodToStartSprinting() {
      return ( ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_19_1) && this.isPassenger()) || (float)this.getFoodData().getFoodLevel() > 6.0F || this.getAbilities().mayfly;
   }

   public float getWaterVision() {
      if (!this.isEyeInFluid(FluidTags.WATER)) {
         return 0.0F;
      } else {
         float f = 600.0F;
         float f1 = 100.0F;
         if ((float)this.waterVisionTime >= 600.0F) {
            return 1.0F;
         } else {
            float f2 = Mth.clamp((float)this.waterVisionTime / 100.0F, 0.0F, 1.0F);
            float f3 = (float)this.waterVisionTime < 100.0F ? 0.0F : Mth.clamp(((float)this.waterVisionTime - 100.0F) / 500.0F, 0.0F, 1.0F);
            return f2 * 0.6F + f3 * 0.39999998F;
         }
      }
   }

   public void onGameModeChanged(GameType pGameMode) {
      if (pGameMode == GameType.SPECTATOR) {
         this.setDeltaMovement(this.getDeltaMovement().with(Direction.Axis.Y, 0.0D));
      }

   }

   public boolean isUnderWater() {
      return this.wasUnderwater;
   }

   protected boolean updateIsUnderwater() {
      boolean flag = this.wasUnderwater;
      boolean flag1 = super.updateIsUnderwater();
      if (this.isSpectator()) {
         return this.wasUnderwater;
      } else {
         if (!flag && flag1) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.AMBIENT, 1.0F, 1.0F, false);
            this.minecraft.getSoundManager().play(new UnderwaterAmbientSoundInstances.UnderwaterAmbientSoundInstance(this));
         }

         if (flag && !flag1) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundSource.AMBIENT, 1.0F, 1.0F, false);
         }

         return this.wasUnderwater;
      }
   }

   public Vec3 getRopeHoldPosition(float pPartialTick) {
      if (this.minecraft.options.getCameraType().isFirstPerson()) {
         float f = Mth.lerp(pPartialTick * 0.5F, this.getYRot(), this.yRotO) * ((float)Math.PI / 180F);
         float f1 = Mth.lerp(pPartialTick * 0.5F, this.getXRot(), this.xRotO) * ((float)Math.PI / 180F);
         double d0 = this.getMainArm() == HumanoidArm.RIGHT ? -1.0D : 1.0D;
         Vec3 vec3 = new Vec3(0.39D * d0, -0.6D, 0.3D);
         return vec3.xRot(-f1).yRot(-f).add(this.getEyePosition(pPartialTick));
      } else {
         return super.getRopeHoldPosition(pPartialTick);
      }
   }

   public void updateTutorialInventoryAction(ItemStack pCarried, ItemStack pClicked, ClickAction pAction) {
      this.minecraft.getTutorial().onInventoryAction(pCarried, pClicked, pAction);
   }

   public float getVisualRotationYInDegrees() {
      return this.getYRot();
   }
}