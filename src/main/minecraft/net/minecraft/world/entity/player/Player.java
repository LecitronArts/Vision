package net.minecraft.world.entity.player;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Unique;

public abstract class Player extends LivingEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int MAX_NAME_LENGTH = 16;
   public static final HumanoidArm DEFAULT_MAIN_HAND = HumanoidArm.RIGHT;
   public static final int DEFAULT_MODEL_CUSTOMIZATION = 0;
   public static final int MAX_HEALTH = 20;
   public static final int SLEEP_DURATION = 100;
   public static final int WAKE_UP_DURATION = 10;
   public static final int ENDER_SLOT_OFFSET = 200;
   public static final float CROUCH_BB_HEIGHT = 1.5F;
   public static final float SWIMMING_BB_WIDTH = 0.6F;
   public static final float SWIMMING_BB_HEIGHT = 0.6F;
   public static final float DEFAULT_EYE_HEIGHT = 1.62F;
   public static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.8F);
   private static final Map<Pose, EntityDimensions> POSES = ImmutableMap.<Pose, EntityDimensions>builder().put(Pose.STANDING, STANDING_DIMENSIONS).put(Pose.SLEEPING, SLEEPING_DIMENSIONS).put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.CROUCHING, EntityDimensions.scalable(0.6F, 1.5F)).put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F)).build();
   private static final EntityDataAccessor<Float> DATA_PLAYER_ABSORPTION_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_SCORE_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
   protected static final EntityDataAccessor<Byte> DATA_PLAYER_MODE_CUSTOMISATION = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BYTE);
   protected static final EntityDataAccessor<Byte> DATA_PLAYER_MAIN_HAND = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BYTE);
   protected static final EntityDataAccessor<CompoundTag> DATA_SHOULDER_LEFT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
   protected static final EntityDataAccessor<CompoundTag> DATA_SHOULDER_RIGHT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
   private long timeEntitySatOnShoulder;
   private final Inventory inventory = new Inventory(this);
   protected PlayerEnderChestContainer enderChestInventory = new PlayerEnderChestContainer();
   public final InventoryMenu inventoryMenu;
   public AbstractContainerMenu containerMenu;
   protected FoodData foodData = new FoodData();
   protected int jumpTriggerTime;
   public float oBob;
   public float bob;
   public int takeXpDelay;
   public double xCloakO;
   public double yCloakO;
   public double zCloakO;
   public double xCloak;
   public double yCloak;
   public double zCloak;
   private int sleepCounter;
   protected boolean wasUnderwater;
   private final Abilities abilities = new Abilities();
   public int experienceLevel;
   public int totalExperience;
   public float experienceProgress;
   protected int enchantmentSeed;
   protected final float defaultFlySpeed = 0.02F;
   private int lastLevelUpTime;
   private final GameProfile gameProfile;
   private boolean reducedDebugInfo;
   private ItemStack lastItemInMainHand = ItemStack.EMPTY;
   private final ItemCooldowns cooldowns = this.createItemCooldowns();
   private Optional<GlobalPos> lastDeathLocation = Optional.empty();
   @Nullable
   public FishingHook fishing;
   protected float hurtDir;

   private static final EntityDimensions viaFabricPlus$sneaking_dimensions_v1_13_2 = EntityDimensions.scalable(0.6F, 1.65F);

   private static final SoundEvent viaFabricPlus$oof_hurt = SoundEvent.createVariableRangeEvent(new ResourceLocation("viafabricplus", "oof.hurt"));

   public boolean viaFabricPlus$isSprinting;

   public Player(Level pLevel, BlockPos pPos, float pYRot, GameProfile pGameProfile) {
      super(EntityType.PLAYER, pLevel);
      this.setUUID(pGameProfile.getId());
      this.gameProfile = pGameProfile;
      this.inventoryMenu = new InventoryMenu(this.inventory, !pLevel.isClientSide, this);
      this.containerMenu = this.inventoryMenu;
      this.moveTo((double)pPos.getX() + 0.5D, (double)(pPos.getY() + 1), (double)pPos.getZ() + 0.5D, pYRot, 0.0F);
      this.rotOffs = 180.0F;
   }

   public boolean blockActionRestricted(Level pLevel, BlockPos pPos, GameType pGameMode) {
      if (!pGameMode.isBlockPlacingRestricted()) {
         return false;
      } else if (pGameMode == GameType.SPECTATOR) {
         return true;
      } else if (this.mayBuild()) {
         return false;
      } else {
         ItemStack itemstack = this.getMainHandItem();
         return itemstack.isEmpty() || !itemstack.hasAdventureModeBreakTagForBlock(pLevel.registryAccess().registryOrThrow(Registries.BLOCK), new BlockInWorld(pLevel, pPos, false));
      }
   }

   public static AttributeSupplier.Builder createAttributes() {
      return LivingEntity.createLivingAttributes().add(Attributes.ATTACK_DAMAGE, 1.0D).add(Attributes.MOVEMENT_SPEED, (double)0.1F).add(Attributes.ATTACK_SPEED).add(Attributes.LUCK);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_PLAYER_ABSORPTION_ID, 0.0F);
      this.entityData.define(DATA_SCORE_ID, 0);
      this.entityData.define(DATA_PLAYER_MODE_CUSTOMISATION, (byte)0);
      this.entityData.define(DATA_PLAYER_MAIN_HAND, (byte)DEFAULT_MAIN_HAND.getId());
      this.entityData.define(DATA_SHOULDER_LEFT, new CompoundTag());
      this.entityData.define(DATA_SHOULDER_RIGHT, new CompoundTag());
   }

   public void tick() {
      this.noPhysics = this.isSpectator();
      if (this.isSpectator()) {
         this.setOnGround(false);
      }

      if (this.takeXpDelay > 0) {
         --this.takeXpDelay;
      }

      if (this.isSleeping()) {
         ++this.sleepCounter;
         if (this.sleepCounter > 100) {
            this.sleepCounter = 100;
         }

         if (!this.level().isClientSide && this.level().isDay()) {
            this.stopSleepInBed(false, true);
         }
      } else if (this.sleepCounter > 0) {
         ++this.sleepCounter;
         if (this.sleepCounter >= 110) {
            this.sleepCounter = 0;
         }
      }

      this.updateIsUnderwater();
      super.tick();
      if (!this.level().isClientSide && this.containerMenu != null && !this.containerMenu.stillValid(this)) {
         this.closeContainer();
         this.containerMenu = this.inventoryMenu;
      }

      this.moveCloak();
      if (!this.level().isClientSide) {
         this.foodData.tick(this);
         this.awardStat(Stats.PLAY_TIME);
         this.awardStat(Stats.TOTAL_WORLD_TIME);
         if (this.isAlive()) {
            this.awardStat(Stats.TIME_SINCE_DEATH);
         }

         if (this.isDiscrete()) {
            this.awardStat(Stats.CROUCH_TIME);
         }

         if (!this.isSleeping()) {
            this.awardStat(Stats.TIME_SINCE_REST);
         }
      }

      int i = 29999999;
      double d0 = Mth.clamp(this.getX(), -2.9999999E7D, 2.9999999E7D);
      double d1 = Mth.clamp(this.getZ(), -2.9999999E7D, 2.9999999E7D);
      if (d0 != this.getX() || d1 != this.getZ()) {
         this.setPos(d0, this.getY(), d1);
      }

      ++this.attackStrengthTicker;
      ItemStack itemstack = this.getMainHandItem();
      if (!ItemStack.matches(this.lastItemInMainHand, itemstack)) {
         if (!ItemStack.isSameItem(this.lastItemInMainHand, itemstack)) {
            this.resetAttackStrengthTicker();
         }

         this.lastItemInMainHand = itemstack.copy();
      }

      this.turtleHelmetTick();
      this.cooldowns.tick();
      this.updatePlayerPose();
   }

   protected float getMaxHeadRotationRelativeToBody() {
      return (  ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_20_2) &&  this.isBlocking() )? 15.0F : super.getMaxHeadRotationRelativeToBody();
   }

   public boolean isSecondaryUseActive() {
      return this.isShiftKeyDown();
   }

   protected boolean wantsToStopRiding() {
      return this.isShiftKeyDown();
   }

   protected boolean isStayingOnGroundSurface() {
      return this.isShiftKeyDown();
   }

   protected boolean updateIsUnderwater() {
      this.wasUnderwater = this.isEyeInFluid(FluidTags.WATER);
      return this.wasUnderwater;
   }

   private void turtleHelmetTick() {
      ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
      if (itemstack.is(Items.TURTLE_HELMET) && !this.isEyeInFluid(FluidTags.WATER)) {
         this.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true));
      }

   }

   protected ItemCooldowns createItemCooldowns() {
      return new ItemCooldowns();
   }

   private void moveCloak() {
      this.xCloakO = this.xCloak;
      this.yCloakO = this.yCloak;
      this.zCloakO = this.zCloak;
      double d0 = this.getX() - this.xCloak;
      double d1 = this.getY() - this.yCloak;
      double d2 = this.getZ() - this.zCloak;
      double d3 = 10.0D;
      if (d0 > 10.0D) {
         this.xCloak = this.getX();
         this.xCloakO = this.xCloak;
      }

      if (d2 > 10.0D) {
         this.zCloak = this.getZ();
         this.zCloakO = this.zCloak;
      }

      if (d1 > 10.0D) {
         this.yCloak = this.getY();
         this.yCloakO = this.yCloak;
      }

      if (d0 < -10.0D) {
         this.xCloak = this.getX();
         this.xCloakO = this.xCloak;
      }

      if (d2 < -10.0D) {
         this.zCloak = this.getZ();
         this.zCloakO = this.zCloak;
      }

      if (d1 < -10.0D) {
         this.yCloak = this.getY();
         this.yCloakO = this.yCloak;
      }

      this.xCloak += d0 * 0.25D;
      this.zCloak += d2 * 0.25D;
      this.yCloak += d1 * 0.25D;
   }

   protected void updatePlayerPose() {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)) {
         final Pose pose;
         if (this.isFallFlying()) {
            pose = Pose.FALL_FLYING;
         } else if (this.isSleeping()) {
            pose = Pose.SLEEPING;
         } else if (this.isSwimming()) {
            pose = Pose.SWIMMING;
         } else if (this.isAutoSpinAttack()) {
            pose = Pose.SPIN_ATTACK;
         } else if (this.isShiftKeyDown() && !this.abilities.flying) {
            pose = Pose.CROUCHING;
         } else {
            pose = Pose.STANDING;
         }
         this.setPose(pose);
         return;
      }

      if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.SWIMMING)) {
         Pose pose;
         if (this.isFallFlying()) {
            pose = Pose.FALL_FLYING;
         } else if (this.isSleeping()) {
            pose = Pose.SLEEPING;
         } else if (this.isSwimming()) {
            pose = Pose.SWIMMING;
         } else if (this.isAutoSpinAttack()) {
            pose = Pose.SPIN_ATTACK;
         } else if (this.isShiftKeyDown() && !this.abilities.flying) {
            pose = Pose.CROUCHING;
         } else {
            pose = Pose.STANDING;
         }

         Pose pose1;
         if (!this.isSpectator() && !this.isPassenger() && !this.canPlayerFitWithinBlocksAndEntitiesWhen(pose)) {
            if (this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING)) {
               pose1 = Pose.CROUCHING;
            } else {
               pose1 = Pose.SWIMMING;
            }
         } else {
            pose1 = pose;
         }

         this.setPose(pose1);
      }
   }

   protected boolean canPlayerFitWithinBlocksAndEntitiesWhen(Pose pPose) {
      return this.level().noCollision(this, this.getDimensions(pPose).makeBoundingBox(this.position()).deflate(1.0E-7D));
   }

   public int getPortalWaitTime() {
      return Math.max(1, this.level().getGameRules().getInt(this.abilities.invulnerable ? GameRules.RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY : GameRules.RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY));
   }

   protected SoundEvent getSwimSound() {
      return SoundEvents.PLAYER_SWIM;
   }

   protected SoundEvent getSwimSplashSound() {
      return SoundEvents.PLAYER_SPLASH;
   }

   protected SoundEvent getSwimHighSpeedSplashSound() {
      return SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
   }

   public int getDimensionChangingDelay() {
      return 10;
   }

   public void playSound(SoundEvent pSound, float pVolume, float pPitch) {
      this.level().playSound(this, this.getX(), this.getY(), this.getZ(), pSound, this.getSoundSource(), pVolume, pPitch);
   }

   public void playNotifySound(SoundEvent pSound, SoundSource pSource, float pVolume, float pPitch) {
   }

   public SoundSource getSoundSource() {
      return SoundSource.PLAYERS;
   }

   protected int getFireImmuneTicks() {
      return 20;
   }

   public void handleEntityEvent(byte pId) {
      if (pId == 9) {
         this.completeUsingItem();
      } else if (pId == 23) {
         this.reducedDebugInfo = false;
      } else if (pId == 22) {
         this.reducedDebugInfo = true;
      } else if (pId == 43) {
         this.addParticlesAroundSelf(ParticleTypes.CLOUD);
      } else {
         super.handleEntityEvent(pId);
      }

   }

   private void addParticlesAroundSelf(ParticleOptions pParticleOption) {
      for(int i = 0; i < 5; ++i) {
         double d0 = this.random.nextGaussian() * 0.02D;
         double d1 = this.random.nextGaussian() * 0.02D;
         double d2 = this.random.nextGaussian() * 0.02D;
         this.level().addParticle(pParticleOption, this.getRandomX(1.0D), this.getRandomY() + 1.0D, this.getRandomZ(1.0D), d0, d1, d2);
      }

   }

   protected void closeContainer() {
      this.containerMenu = this.inventoryMenu;
   }

   protected void doCloseContainer() {
   }

   public void rideTick() {
      if (!this.level().isClientSide && this.wantsToStopRiding() && this.isPassenger()) {
         this.stopRiding();
         this.setShiftKeyDown(false);
      } else {
         super.rideTick();
         this.oBob = this.bob;
         this.bob = 0.0F;
      }
   }

   protected void serverAiStep() {
      super.serverAiStep();
      this.updateSwingTime();
      this.yHeadRot = this.getYRot();
   }

   public void aiStep() {
      if (this.jumpTriggerTime > 0) {
         --this.jumpTriggerTime;
      }

      if (this.level().getDifficulty() == Difficulty.PEACEFUL && this.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
         if (this.getHealth() < this.getMaxHealth() && this.tickCount % 20 == 0) {
            this.heal(1.0F);
         }

         if (this.foodData.needsFood() && this.tickCount % 10 == 0) {
            this.foodData.setFoodLevel(this.foodData.getFoodLevel() + 1);
         }
      }

      this.inventory.tick();
      this.oBob = this.bob;
      super.aiStep();
      viaFabricPlus$isSprinting = this.isSprinting();
      this.setSpeed((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED));
      float f;
      if (this.onGround() && !this.isDeadOrDying() && !this.isSwimming()) {
         f = Math.min(0.1F, (float)this.getDeltaMovement().horizontalDistance());
      } else {
         f = 0.0F;
      }

      this.bob += (f - this.bob) * 0.4F;
      if (this.getHealth() > 0.0F && !this.isSpectator()) {
         AABB aabb;
         if (this.isPassenger() && !this.getVehicle().isRemoved()) {
            aabb = this.getBoundingBox().minmax(this.getVehicle().getBoundingBox()).inflate(1.0D, 0.0D, 1.0D);
         } else {
            aabb = this.getBoundingBox().inflate(1.0D, 0.5D, 1.0D);
         }

         List<Entity> list = this.level().getEntities(this, aabb);
         List<Entity> list1 = Lists.newArrayList();

         for(Entity entity : list) {
            if (entity.getType() == EntityType.EXPERIENCE_ORB) {
               list1.add(entity);
            } else if (!entity.isRemoved()) {
               this.touch(entity);
            }
         }

         if (!list1.isEmpty()) {
            this.touch(Util.getRandom(list1, this.random));
         }
      }

      this.playShoulderEntityAmbientSound(this.getShoulderEntityLeft());
      this.playShoulderEntityAmbientSound(this.getShoulderEntityRight());
      if (!this.level().isClientSide && (this.fallDistance > 0.5F || this.isInWater()) || this.abilities.flying || this.isSleeping() || this.isInPowderSnow) {
         this.removeEntitiesOnShoulder();
      }

   }

   private void playShoulderEntityAmbientSound(@Nullable CompoundTag pEntityCompound) {
      if (pEntityCompound != null && (!pEntityCompound.contains("Silent") || !pEntityCompound.getBoolean("Silent")) && this.level().random.nextInt(200) == 0) {
         String s = pEntityCompound.getString("id");
         EntityType.byString(s).filter((p_36280_) -> {
            return p_36280_ == EntityType.PARROT;
         }).ifPresent((p_309219_) -> {
            if (!Parrot.imitateNearbyMobs(this.level(), this)) {
               this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), Parrot.getAmbient(this.level(), this.level().random), this.getSoundSource(), 1.0F, Parrot.getPitch(this.level().random));
            }

         });
      }

   }

   private void touch(Entity pEntity) {
      pEntity.playerTouch(this);
   }

   public int getScore() {
      return this.entityData.get(DATA_SCORE_ID);
   }

   public void setScore(int pScore) {
      this.entityData.set(DATA_SCORE_ID, pScore);
   }

   public void increaseScore(int pScore) {
      int i = this.getScore();
      this.entityData.set(DATA_SCORE_ID, i + pScore);
   }

   public void startAutoSpinAttack(int pAttackTicks) {
      this.autoSpinAttackTicks = pAttackTicks;
      if (!this.level().isClientSide) {
         this.removeEntitiesOnShoulder();
         this.setLivingEntityFlag(4, true);
      }

   }

   public void die(DamageSource pCause) {
      super.die(pCause);
      this.reapplyPosition();
      if (!this.isSpectator()) {
         this.dropAllDeathLoot(pCause);
      }

      if (pCause != null) {
         this.setDeltaMovement((double)(-Mth.cos((this.getHurtDir() + this.getYRot()) * ((float)Math.PI / 180F)) * 0.1F), (double)0.1F, (double)(-Mth.sin((this.getHurtDir() + this.getYRot()) * ((float)Math.PI / 180F)) * 0.1F));
      } else {
         this.setDeltaMovement(0.0D, 0.1D, 0.0D);
      }

      this.awardStat(Stats.DEATHS);
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
      this.clearFire();
      this.setSharedFlagOnFire(false);
      this.setLastDeathLocation(Optional.of(GlobalPos.of(this.level().dimension(), this.blockPosition())));
   }

   protected void dropEquipment() {
      super.dropEquipment();
      if (!this.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
         this.destroyVanishingCursedItems();
         this.inventory.dropAll();
      }

   }

   protected void destroyVanishingCursedItems() {
      for(int i = 0; i < this.inventory.getContainerSize(); ++i) {
         ItemStack itemstack = this.inventory.getItem(i);
         if (!itemstack.isEmpty() && EnchantmentHelper.hasVanishingCurse(itemstack)) {
            this.inventory.removeItemNoUpdate(i);
         }
      }

   }

   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      if (VisualSettings.global().replaceHurtSoundWithOOFSound.isEnabled()) {
         return (viaFabricPlus$oof_hurt);
      }
      return pDamageSource.type().effects().sound();
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.PLAYER_DEATH;
   }

   @Nullable
   public ItemEntity drop(ItemStack pItemStack, boolean pIncludeThrowerName) {
      return this.drop(pItemStack, false, pIncludeThrowerName);
   }

   @Nullable
   public ItemEntity drop(ItemStack pDroppedItem, boolean pDropAround, boolean pIncludeThrowerName) {
      if (pDroppedItem.isEmpty()) {
         return null;
      } else {
         if (this.level().isClientSide && ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_15_2)) {
            this.swing(InteractionHand.MAIN_HAND);
         }

         double d0 = this.getEyeY() - (double)0.3F;
         ItemEntity itementity = new ItemEntity(this.level(), this.getX(), d0, this.getZ(), pDroppedItem);
         itementity.setPickUpDelay(40);
         if (pIncludeThrowerName) {
            itementity.setThrower(this);
         }

         if (pDropAround) {
            float f = this.random.nextFloat() * 0.5F;
            float f1 = this.random.nextFloat() * ((float)Math.PI * 2F);
            itementity.setDeltaMovement((double)(-Mth.sin(f1) * f), (double)0.2F, (double)(Mth.cos(f1) * f));
         } else {
            float f7 = 0.3F;
            float f8 = Mth.sin(this.getXRot() * ((float)Math.PI / 180F));
            float f2 = Mth.cos(this.getXRot() * ((float)Math.PI / 180F));
            float f3 = Mth.sin(this.getYRot() * ((float)Math.PI / 180F));
            float f4 = Mth.cos(this.getYRot() * ((float)Math.PI / 180F));
            float f5 = this.random.nextFloat() * ((float)Math.PI * 2F);
            float f6 = 0.02F * this.random.nextFloat();
            itementity.setDeltaMovement((double)(-f3 * f2 * 0.3F) + Math.cos((double)f5) * (double)f6, (double)(-f8 * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F), (double)(f4 * f2 * 0.3F) + Math.sin((double)f5) * (double)f6);
         }

         return itementity;
      }
   }

   public float getDestroySpeed(BlockState pState) {
      float f = this.inventory.getDestroySpeed(pState);
      if (f > 1.0F) {
         int i = EnchantmentHelper.getBlockEfficiency(this);
         ItemStack itemstack = this.getMainHandItem();
         if (i > 0 && !itemstack.isEmpty()) {
            f += (float)(i * i + 1);
         }
      }

      final int efficiency = EnchantmentHelper.getBlockEfficiency(this);
      if (!(efficiency <= 0)) {
         final float speed = this.inventory.getDestroySpeed(pState);
         final int effLevel = efficiency * efficiency + 1;
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_4tor1_4_5) && this.hasCorrectToolForDrops(pState)) {
            f = (speed + effLevel);
         } else if (speed > 1F || ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_6tor1_4_7)) {
            if (!this.getMainHandItem().isEmpty()) {
               if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_7_6)) {
                  if (speed <= 1.0 && !this.hasCorrectToolForDrops(pState)) {
                     f = (speed + effLevel * 0.08F);
                  } else {
                     f = (speed + effLevel);
                  }
               }
            }
         }
      }

      if (MobEffectUtil.hasDigSpeed(this)) {
         f *= 1.0F + (float)(MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
      }

      boolean viaFixEffect;

      final boolean hasMiningFatigue = this.hasEffect(MobEffects.DIG_SLOWDOWN);
      if (hasMiningFatigue && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_7_6)) {
         f = (f * (1.0F - (this.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier() + 1) * 0.2F));
         if (f< 0) f = (0);
         viaFixEffect = false; // disable original code
      } else {
         viaFixEffect = hasMiningFatigue;
      }

      if (viaFixEffect) {
         float f1;
         switch (this.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) {
            case 0:
               f1 = 0.3F;
               break;
            case 1:
               f1 = 0.09F;
               break;
            case 2:
               f1 = 0.0027F;
               break;
            case 3:
            default:
               f1 = 8.1E-4F;
         }

         f *= f1;
      }

      if (this.isEyeInFluid(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(this)) {
         f /= 5.0F;
      }

      if (!this.onGround()) {
         f /= 5.0F;
      }

      return f;
   }

   public boolean hasCorrectToolForDrops(BlockState pState) {
      return !pState.requiresCorrectToolForDrops() || this.inventory.getSelected().isCorrectToolForDrops(pState);
   }

   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.setUUID(this.gameProfile.getId());
      ListTag listtag = pCompound.getList("Inventory", 10);
      this.inventory.load(listtag);
      this.inventory.selected = pCompound.getInt("SelectedItemSlot");
      this.sleepCounter = pCompound.getShort("SleepTimer");
      this.experienceProgress = pCompound.getFloat("XpP");
      this.experienceLevel = pCompound.getInt("XpLevel");
      this.totalExperience = pCompound.getInt("XpTotal");
      this.enchantmentSeed = pCompound.getInt("XpSeed");
      if (this.enchantmentSeed == 0) {
         this.enchantmentSeed = this.random.nextInt();
      }

      this.setScore(pCompound.getInt("Score"));
      this.foodData.readAdditionalSaveData(pCompound);
      this.abilities.loadSaveData(pCompound);
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double)this.abilities.getWalkingSpeed());
      if (pCompound.contains("EnderItems", 9)) {
         this.enderChestInventory.fromTag(pCompound.getList("EnderItems", 10));
      }

      if (pCompound.contains("ShoulderEntityLeft", 10)) {
         this.setShoulderEntityLeft(pCompound.getCompound("ShoulderEntityLeft"));
      }

      if (pCompound.contains("ShoulderEntityRight", 10)) {
         this.setShoulderEntityRight(pCompound.getCompound("ShoulderEntityRight"));
      }

      if (pCompound.contains("LastDeathLocation", 10)) {
         this.setLastDeathLocation(GlobalPos.CODEC.parse(NbtOps.INSTANCE, pCompound.get("LastDeathLocation")).resultOrPartial(LOGGER::error));
      }

   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      NbtUtils.addCurrentDataVersion(pCompound);
      pCompound.put("Inventory", this.inventory.save(new ListTag()));
      pCompound.putInt("SelectedItemSlot", this.inventory.selected);
      pCompound.putShort("SleepTimer", (short)this.sleepCounter);
      pCompound.putFloat("XpP", this.experienceProgress);
      pCompound.putInt("XpLevel", this.experienceLevel);
      pCompound.putInt("XpTotal", this.totalExperience);
      pCompound.putInt("XpSeed", this.enchantmentSeed);
      pCompound.putInt("Score", this.getScore());
      this.foodData.addAdditionalSaveData(pCompound);
      this.abilities.addSaveData(pCompound);
      pCompound.put("EnderItems", this.enderChestInventory.createTag());
      if (!this.getShoulderEntityLeft().isEmpty()) {
         pCompound.put("ShoulderEntityLeft", this.getShoulderEntityLeft());
      }

      if (!this.getShoulderEntityRight().isEmpty()) {
         pCompound.put("ShoulderEntityRight", this.getShoulderEntityRight());
      }

      this.getLastDeathLocation().flatMap((p_219745_) -> {
         return GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, p_219745_).resultOrPartial(LOGGER::error);
      }).ifPresent((p_219756_) -> {
         pCompound.put("LastDeathLocation", p_219756_);
      });
   }

   public boolean isInvulnerableTo(DamageSource pSource) {
      if (super.isInvulnerableTo(pSource)) {
         return true;
      } else if (pSource.is(DamageTypeTags.IS_DROWNING)) {
         return !this.level().getGameRules().getBoolean(GameRules.RULE_DROWNING_DAMAGE);
      } else if (pSource.is(DamageTypeTags.IS_FALL)) {
         return !this.level().getGameRules().getBoolean(GameRules.RULE_FALL_DAMAGE);
      } else if (pSource.is(DamageTypeTags.IS_FIRE)) {
         return !this.level().getGameRules().getBoolean(GameRules.RULE_FIRE_DAMAGE);
      } else if (pSource.is(DamageTypeTags.IS_FREEZING)) {
         return !this.level().getGameRules().getBoolean(GameRules.RULE_FREEZE_DAMAGE);
      } else {
         return false;
      }
   }

   public boolean hurt(DamageSource pSource, float pAmount) {
      if (this.isInvulnerableTo(pSource)) {
         return false;
      } else if (this.abilities.invulnerable && !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      } else {
         this.noActionTime = 0;
         if (this.isDeadOrDying()) {
            return false;
         } else {
            if (!this.level().isClientSide) {
               this.removeEntitiesOnShoulder();
            }

            if (pSource.scalesWithDifficulty()) {
               if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
                  pAmount = 0.0F;
               }

               if (this.level().getDifficulty() == Difficulty.EASY) {
                  pAmount = Math.min(pAmount / 2.0F + 1.0F, pAmount);
               }

               if (this.level().getDifficulty() == Difficulty.HARD) {
                  pAmount = pAmount * 3.0F / 2.0F;
               }
            }

            return pAmount == 0.0F ? false : super.hurt(pSource, pAmount);
         }
      }
   }

   protected void blockUsingShield(LivingEntity pEntity) {
      super.blockUsingShield(pEntity);
      if (pEntity.canDisableShield()) {
         this.disableShield(true);
      }

   }

   public boolean canBeSeenAsEnemy() {
      return !this.getAbilities().invulnerable && super.canBeSeenAsEnemy();
   }

   public boolean canHarmPlayer(Player pOther) {
      Team team = this.getTeam();
      Team team1 = pOther.getTeam();
      if (team == null) {
         return true;
      } else {
         return !team.isAlliedTo(team1) ? true : team.isAllowFriendlyFire();
      }
   }

   protected void hurtArmor(DamageSource pDamageSource, float pDamage) {
      this.inventory.hurtArmor(pDamageSource, pDamage, Inventory.ALL_ARMOR_SLOTS);
   }

   protected void hurtHelmet(DamageSource pDamageSource, float pDamageAmount) {
      this.inventory.hurtArmor(pDamageSource, pDamageAmount, Inventory.HELMET_SLOT_ONLY);
   }

   protected void hurtCurrentlyUsedShield(float pDamage) {
      if (this.useItem.is(Items.SHIELD)) {
         if (!this.level().isClientSide) {
            this.awardStat(Stats.ITEM_USED.get(this.useItem.getItem()));
         }

         if (pDamage >= 3.0F) {
            int i = 1 + Mth.floor(pDamage);
            InteractionHand interactionhand = this.getUsedItemHand();
            this.useItem.hurtAndBreak(i, this, (p_219739_) -> {
               p_219739_.broadcastBreakEvent(interactionhand);
            });
            if (this.useItem.isEmpty()) {
               if (interactionhand == InteractionHand.MAIN_HAND) {
                  this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
               } else {
                  this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
               }

               this.useItem = ItemStack.EMPTY;
               this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level().random.nextFloat() * 0.4F);
            }
         }

      }
   }

   protected void actuallyHurt(DamageSource pDamageSrc, float pDamageAmount) {
      if (!this.isInvulnerableTo(pDamageSrc)) {
         pDamageAmount = this.getDamageAfterArmorAbsorb(pDamageSrc, pDamageAmount);
         pDamageAmount = this.getDamageAfterMagicAbsorb(pDamageSrc, pDamageAmount);
         float f1 = Math.max(pDamageAmount - this.getAbsorptionAmount(), 0.0F);
         this.setAbsorptionAmount(this.getAbsorptionAmount() - (pDamageAmount - f1));
         float f = pDamageAmount - f1;
         if (f > 0.0F && f < 3.4028235E37F) {
            this.awardStat(Stats.DAMAGE_ABSORBED, Math.round(f * 10.0F));
         }

         if (f1 != 0.0F) {
            this.causeFoodExhaustion(pDamageSrc.getFoodExhaustion());
            this.getCombatTracker().recordDamage(pDamageSrc, f1);
            this.setHealth(this.getHealth() - f1);
            if (f1 < 3.4028235E37F) {
               this.awardStat(Stats.DAMAGE_TAKEN, Math.round(f1 * 10.0F));
            }

            this.gameEvent(GameEvent.ENTITY_DAMAGE);
         }
      }
   }

   protected boolean onSoulSpeedBlock() {
      return !this.abilities.flying && super.onSoulSpeedBlock();
   }

   public boolean isTextFilteringEnabled() {
      return false;
   }

   public void openTextEdit(SignBlockEntity pSignEntity, boolean pIsFrontText) {
   }

   public void openMinecartCommandBlock(BaseCommandBlock pCommandEntity) {
   }

   public void openCommandBlock(CommandBlockEntity pCommandBlockEntity) {
   }

   public void openStructureBlock(StructureBlockEntity pStructureEntity) {
   }

   public void openJigsawBlock(JigsawBlockEntity pJigsawBlockEntity) {
   }

   public void openHorseInventory(AbstractHorse pHorse, Container pInventory) {
   }

   public OptionalInt openMenu(@Nullable MenuProvider pMenu) {
      return OptionalInt.empty();
   }

   public void sendMerchantOffers(int pContainerId, MerchantOffers pOffers, int pVillagerLevel, int pVillagerXp, boolean pShowProgress, boolean pCanRestock) {
   }

   public void openItemGui(ItemStack pStack, InteractionHand pHand) {
   }

   public InteractionResult interactOn(Entity pEntityToInteractOn, InteractionHand pHand) {
      if (this.isSpectator()) {
         if (pEntityToInteractOn instanceof MenuProvider) {
            this.openMenu((MenuProvider)pEntityToInteractOn);
         }

         return InteractionResult.PASS;
      } else {
         ItemStack itemstack = this.getItemInHand(pHand);
         ItemStack itemstack1 = itemstack.copy();
         InteractionResult interactionresult = pEntityToInteractOn.interact(this, pHand);
         if (interactionresult.consumesAction()) {
            if (this.abilities.instabuild && itemstack == this.getItemInHand(pHand) && itemstack.getCount() < itemstack1.getCount()) {
               itemstack.setCount(itemstack1.getCount());
            }

            return interactionresult;
         } else {
            if (!itemstack.isEmpty() && pEntityToInteractOn instanceof LivingEntity) {
               if (this.abilities.instabuild) {
                  itemstack = itemstack1;
               }

               InteractionResult interactionresult1 = itemstack.interactLivingEntity(this, (LivingEntity)pEntityToInteractOn, pHand);
               if (interactionresult1.consumesAction()) {
                  this.level().gameEvent(GameEvent.ENTITY_INTERACT, pEntityToInteractOn.position(), GameEvent.Context.of(this));
                  if (itemstack.isEmpty() && !this.abilities.instabuild) {
                     this.setItemInHand(pHand, ItemStack.EMPTY);
                  }

                  return interactionresult1;
               }
            }

            return InteractionResult.PASS;
         }
      }
   }

   protected float ridingOffset(Entity pEntity) {
      return -0.6F;
   }

   public void removeVehicle() {
      super.removeVehicle();
      this.boardingCooldown = 0;
   }

   protected boolean isImmobile() {
      return super.isImmobile() || this.isSleeping();
   }

   public boolean isAffectedByFluids() {
      return !this.abilities.flying;
   }

   protected Vec3 maybeBackOffFromEdge(Vec3 pVec, MoverType pMover) {
      if (!this.abilities.flying && pVec.y <= 0.0D && (pMover == MoverType.SELF || pMover == MoverType.PLAYER) && this.isStayingOnGroundSurface() && this.isAboveGround()) {
         double d0 = pVec.x;
         double d1 = pVec.z;
         double d2 = 0.05D;

         double viaFix ;
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_10)) {
            viaFix = 1.0F;
         } else {
            viaFix = this.maxUpStep();
         }

         while(d0 != 0.0D && this.level().noCollision(this, this.getBoundingBox().move(d0, (double)(-viaFix), 0.0D))) {
            if (d0 < 0.05D && d0 >= -0.05D) {
               d0 = 0.0D;
            } else if (d0 > 0.0D) {
               d0 -= 0.05D;
            } else {
               d0 += 0.05D;
            }
         }

         while(d1 != 0.0D && this.level().noCollision(this, this.getBoundingBox().move(0.0D, (double)(-viaFix), d1))) {
            if (d1 < 0.05D && d1 >= -0.05D) {
               d1 = 0.0D;
            } else if (d1 > 0.0D) {
               d1 -= 0.05D;
            } else {
               d1 += 0.05D;
            }
         }

         while(d0 != 0.0D && d1 != 0.0D && this.level().noCollision(this, this.getBoundingBox().move(d0, (double)(-viaFix), d1))) {
            if (d0 < 0.05D && d0 >= -0.05D) {
               d0 = 0.0D;
            } else if (d0 > 0.0D) {
               d0 -= 0.05D;
            } else {
               d0 += 0.05D;
            }

            if (d1 < 0.05D && d1 >= -0.05D) {
               d1 = 0.0D;
            } else if (d1 > 0.0D) {
               d1 -= 0.05D;
            } else {
               d1 += 0.05D;
            }
         }

         pVec = new Vec3(d0, pVec.y, d1);
      }

      return pVec;
   }

   private boolean isAboveGround() {
      return this.onGround() || this.fallDistance < this.maxUpStep() && !this.level().noCollision(this, this.getBoundingBox().move(0.0D, (double)(this.fallDistance - this.maxUpStep()), 0.0D));
   }

   public void attack(Entity pTarget) {
      if (pTarget.isAttackable()) {
         if (!pTarget.skipAttackInteraction(this)) {
            float f = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float f1;
            if (pTarget instanceof LivingEntity) {
               f1 = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity)pTarget).getMobType());
            } else {
               f1 = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), MobType.UNDEFINED);
            }

            float f2 = this.getAttackStrengthScale(0.5F);
            f *= 0.2F + f2 * f2 * 0.8F;
            f1 *= f2;
            this.resetAttackStrengthTicker();
            if (f > 0.0F || f1 > 0.0F) {
               boolean flag = f2 > 0.9F;
               boolean flag1 = false;
               int i = 0;
               i += EnchantmentHelper.getKnockbackBonus(this);
               if (this.isSprinting() && flag) {
                  this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundSource(), 1.0F, 1.0F);
                  ++i;
                  flag1 = true;
               }

               boolean flag2 = flag && this.fallDistance > 0.0F && !this.onGround() && !this.onClimbable() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && pTarget instanceof LivingEntity;
               flag2 = flag2 && !this.isSprinting();
               if (flag2) {
                  f *= 1.5F;
               }

               f += f1;
               boolean flag3 = false;
               double d0 = (double)(this.walkDist - this.walkDistO);
               if (flag && !flag2 && !flag1 && this.onGround() && d0 < (double)this.getSpeed()) {
                  ItemStack itemstack = this.getItemInHand(InteractionHand.MAIN_HAND);
                  if (itemstack.getItem() instanceof SwordItem) {
                     flag3 = true;
                  }
               }

               float f4 = 0.0F;
               boolean flag4 = false;
               int j = EnchantmentHelper.getFireAspect(this);
               if (pTarget instanceof LivingEntity) {
                  f4 = ((LivingEntity)pTarget).getHealth();
                  if (j > 0 && !pTarget.isOnFire()) {
                     flag4 = true;
                     pTarget.setSecondsOnFire(1);
                  }
               }

               Vec3 vec3 = pTarget.getDeltaMovement();
               boolean flag5 = pTarget.hurt(this.damageSources().playerAttack(this), f);
               if (flag5) {
                  if (i > 0) {
                     if (pTarget instanceof LivingEntity) {
                        ((LivingEntity)pTarget).knockback((double)((float)i * 0.5F), (double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-Mth.cos(this.getYRot() * ((float)Math.PI / 180F))));
                     } else {
                        pTarget.push((double)(-Mth.sin(this.getYRot() * ((float)Math.PI / 180F)) * (float)i * 0.5F), 0.1D, (double)(Mth.cos(this.getYRot() * ((float)Math.PI / 180F)) * (float)i * 0.5F));
                     }

                     this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
                     this.setSprinting(false);
                  }

                  if (flag3) {
                     float f3 = 1.0F + EnchantmentHelper.getSweepingDamageRatio(this) * f;

                     for(LivingEntity livingentity : this.level().getEntitiesOfClass(LivingEntity.class, pTarget.getBoundingBox().inflate(1.0D, 0.25D, 1.0D))) {
                        if (livingentity != this && livingentity != pTarget && !this.isAlliedTo(livingentity) && (!(livingentity instanceof ArmorStand) || !((ArmorStand)livingentity).isMarker()) && this.distanceToSqr(livingentity) < 9.0D) {
                           livingentity.knockback((double)0.4F, (double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-Mth.cos(this.getYRot() * ((float)Math.PI / 180F))));
                           livingentity.hurt(this.damageSources().playerAttack(this), f3);
                        }
                     }

                     this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0F, 1.0F);
                     this.sweepAttack();
                  }

                  if (pTarget instanceof ServerPlayer && pTarget.hurtMarked) {
                     ((ServerPlayer)pTarget).connection.send(new ClientboundSetEntityMotionPacket(pTarget));
                     pTarget.hurtMarked = false;
                     pTarget.setDeltaMovement(vec3);
                  }

                  if (flag2) {
                     this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.0F, 1.0F);
                     this.crit(pTarget);
                  }

                  if (!flag2 && !flag3) {
                     if (flag) {
                        this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.0F, 1.0F);
                     } else {
                        this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundSource(), 1.0F, 1.0F);
                     }
                  }

                  if (f1 > 0.0F) {
                     this.magicCrit(pTarget);
                  }

                  this.setLastHurtMob(pTarget);
                  if (pTarget instanceof LivingEntity) {
                     EnchantmentHelper.doPostHurtEffects((LivingEntity)pTarget, this);
                  }

                  EnchantmentHelper.doPostDamageEffects(this, pTarget);
                  ItemStack itemstack1 = this.getMainHandItem();
                  Entity entity = pTarget;
                  if (pTarget instanceof EnderDragonPart) {
                     entity = ((EnderDragonPart)pTarget).parentMob;
                  }

                  if (!this.level().isClientSide && !itemstack1.isEmpty() && entity instanceof LivingEntity) {
                     itemstack1.hurtEnemy((LivingEntity)entity, this);
                     if (itemstack1.isEmpty()) {
                        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                     }
                  }

                  if (pTarget instanceof LivingEntity) {
                     float f5 = f4 - ((LivingEntity)pTarget).getHealth();
                     this.awardStat(Stats.DAMAGE_DEALT, Math.round(f5 * 10.0F));
                     if (j > 0) {
                        pTarget.setSecondsOnFire(j * 4);
                     }

                     if (this.level() instanceof ServerLevel && f5 > 2.0F) {
                        int k = (int)((double)f5 * 0.5D);
                        ((ServerLevel)this.level()).sendParticles(ParticleTypes.DAMAGE_INDICATOR, pTarget.getX(), pTarget.getY(0.5D), pTarget.getZ(), k, 0.1D, 0.0D, 0.1D, 0.2D);
                     }
                  }

                  this.causeFoodExhaustion(0.1F);
               } else {
                  this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource(), 1.0F, 1.0F);
                  if (flag4) {
                     pTarget.clearFire();
                  }
               }
            }

         }
      }
   }

   protected void doAutoAttackOnTouch(LivingEntity pTarget) {
      this.attack(pTarget);
   }

   public void disableShield(boolean pBecauseOfAxe) {
      float f = 0.25F + (float)EnchantmentHelper.getBlockEfficiency(this) * 0.05F;
      if (pBecauseOfAxe) {
         f += 0.75F;
      }

      if (this.random.nextFloat() < f) {
         this.getCooldowns().addCooldown(Items.SHIELD, 100);
         this.stopUsingItem();
         this.level().broadcastEntityEvent(this, (byte)30);
      }

   }

   public void crit(Entity pEntityHit) {
   }

   public void magicCrit(Entity pEntityHit) {
   }

   public void sweepAttack() {
      double d0 = (double)(-Mth.sin(this.getYRot() * ((float)Math.PI / 180F)));
      double d1 = (double)Mth.cos(this.getYRot() * ((float)Math.PI / 180F));
      if (this.level() instanceof ServerLevel) {
         ((ServerLevel)this.level()).sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + d0, this.getY(0.5D), this.getZ() + d1, 0, d0, 0.0D, d1, 0.0D);
      }

   }

   public void respawn() {
   }

   public void remove(Entity.RemovalReason pReason) {
      super.remove(pReason);
      this.inventoryMenu.removed(this);
      if (this.containerMenu != null && this.hasContainerOpen()) {
         this.doCloseContainer();
      }

   }

   public boolean isLocalPlayer() {
      return false;
   }

   public GameProfile getGameProfile() {
      return this.gameProfile;
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public Abilities getAbilities() {
      return this.abilities;
   }

   public void updateTutorialInventoryAction(ItemStack pCarried, ItemStack pClicked, ClickAction pAction) {
   }

   public boolean hasContainerOpen() {
      return this.containerMenu != this.inventoryMenu;
   }

   public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos pBedPos) {
      this.startSleeping(pBedPos);
      this.sleepCounter = 0;
      return Either.right(Unit.INSTANCE);
   }

   public void stopSleepInBed(boolean pWakeImmediately, boolean pUpdateLevelForSleepingPlayers) {
      super.stopSleeping();
      if (this.level() instanceof ServerLevel && pUpdateLevelForSleepingPlayers) {
         ((ServerLevel)this.level()).updateSleepingPlayerList();
      }

      this.sleepCounter = pWakeImmediately ? 0 : 100;
   }

   public void stopSleeping() {
      this.stopSleepInBed(true, true);
   }

   public static Optional<Vec3> findRespawnPositionAndUseSpawnBlock(ServerLevel pServerLevel, BlockPos pSpawnBlockPos, float pPlayerOrientation, boolean pIsRespawnForced, boolean pRespawnAfterWinningTheGame) {
      BlockState blockstate = pServerLevel.getBlockState(pSpawnBlockPos);
      Block block = blockstate.getBlock();
      if (block instanceof RespawnAnchorBlock && (pIsRespawnForced || blockstate.getValue(RespawnAnchorBlock.CHARGE) > 0) && RespawnAnchorBlock.canSetSpawn(pServerLevel)) {
         Optional<Vec3> optional = RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, pServerLevel, pSpawnBlockPos);
         if (!pIsRespawnForced && !pRespawnAfterWinningTheGame && optional.isPresent()) {
            pServerLevel.setBlock(pSpawnBlockPos, blockstate.setValue(RespawnAnchorBlock.CHARGE, Integer.valueOf(blockstate.getValue(RespawnAnchorBlock.CHARGE) - 1)), 3);
         }

         return optional;
      } else if (block instanceof BedBlock && BedBlock.canSetSpawn(pServerLevel)) {
         return BedBlock.findStandUpPosition(EntityType.PLAYER, pServerLevel, pSpawnBlockPos, blockstate.getValue(BedBlock.FACING), pPlayerOrientation);
      } else if (!pIsRespawnForced) {
         return Optional.empty();
      } else {
         boolean flag = block.isPossibleToRespawnInThis(blockstate);
         BlockState blockstate1 = pServerLevel.getBlockState(pSpawnBlockPos.above());
         boolean flag1 = blockstate1.getBlock().isPossibleToRespawnInThis(blockstate1);
         return flag && flag1 ? Optional.of(new Vec3((double)pSpawnBlockPos.getX() + 0.5D, (double)pSpawnBlockPos.getY() + 0.1D, (double)pSpawnBlockPos.getZ() + 0.5D)) : Optional.empty();
      }
   }

   public boolean isSleepingLongEnough() {
      return this.isSleeping() && this.sleepCounter >= 100;
   }

   public int getSleepTimer() {
      return this.sleepCounter;
   }

   public void displayClientMessage(Component pChatComponent, boolean pActionBar) {
   }

   public void awardStat(ResourceLocation pStatKey) {
      this.awardStat(Stats.CUSTOM.get(pStatKey));
   }

   public void awardStat(ResourceLocation pStat, int pIncrement) {
      this.awardStat(Stats.CUSTOM.get(pStat), pIncrement);
   }

   public void awardStat(Stat<?> pStat) {
      this.awardStat(pStat, 1);
   }

   public void awardStat(Stat<?> pStat, int pIncrement) {
   }

   public void resetStat(Stat<?> pStat) {
   }

   public int awardRecipes(Collection<RecipeHolder<?>> pRecipes) {
      return 0;
   }

   public void triggerRecipeCrafted(RecipeHolder<?> pRecipe, List<ItemStack> pItems) {
   }

   public void awardRecipesByKey(List<ResourceLocation> pRecipes) {
   }

   public int resetRecipes(Collection<RecipeHolder<?>> pRecipes) {
      return 0;
   }

   public void jumpFromGround() {
      super.jumpFromGround();
      this.awardStat(Stats.JUMP);
      if (this.isSprinting()) {
         this.causeFoodExhaustion(0.2F);
      } else {
         this.causeFoodExhaustion(0.05F);
      }

   }

   public void travel(Vec3 pTravelVector) {
      if (this.isSwimming() && !this.isPassenger()) {
         double d0 = this.getLookAngle().y;
         double d1 = d0 < -0.2D ? 0.085D : 0.06D;
         if (d0 <= 0.0D || this.jumping || !this.level().getBlockState(BlockPos.containing(this.getX(), this.getY() + 1.0D - 0.1D, this.getZ())).getFluidState().isEmpty()) {
            Vec3 vec3 = this.getDeltaMovement();
            this.setDeltaMovement(vec3.add(0.0D, (d0 - vec3.y) * d1, 0.0D));
         }
      }

      if (this.abilities.flying && !this.isPassenger()) {
         double d2 = this.getDeltaMovement().y;
         super.travel(pTravelVector);
         Vec3 vec31 = this.getDeltaMovement();
         this.setDeltaMovement(vec31.x, d2 * 0.6D, vec31.z);
         this.resetFallDistance();
         this.setSharedFlag(7, false);
      } else {
         super.travel(pTravelVector);
      }

   }

   public void updateSwimming() {
      if (this.abilities.flying) {
         this.setSwimming(false);
      } else {
         super.updateSwimming();
      }

   }

   protected boolean freeAt(BlockPos pPos) {
      return !this.level().getBlockState(pPos).isSuffocating(this.level(), pPos);
   }

   public float getSpeed() {
      return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
   }

   public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
      if (this.abilities.mayfly) {
         return false;
      } else {
         if (pFallDistance >= 2.0F) {
            this.awardStat(Stats.FALL_ONE_CM, (int)Math.round((double)pFallDistance * 100.0D));
         }

         return super.causeFallDamage(pFallDistance, pMultiplier, pSource);
      }
   }

   public boolean tryToStartFallFlying() {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14_4)) {
         if (!this.onGround() && this.getDeltaMovement().y < 0D && !this.isFallFlying()) {
            final ItemStack itemStack = this.getItemBySlot(EquipmentSlot.CHEST);
            if (itemStack.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(itemStack)) {
               return (true);
            }
         }
         return (false);
      }
      if (!this.onGround() && !this.isFallFlying() && !this.isInWater() && !(this.hasEffect(MobEffects.LEVITATION) && ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_15_2))) {
         ItemStack itemstack = this.getItemBySlot(EquipmentSlot.CHEST);
         if (itemstack.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(itemstack)) {
            this.startFallFlying();
            return true;
         }
      }

      return false;
   }

   public void startFallFlying() {
      this.setSharedFlag(7, true);
   }

   public void stopFallFlying() {
      this.setSharedFlag(7, true);
      this.setSharedFlag(7, false);
   }

   protected void doWaterSplashEffect() {
      if (!this.isSpectator()) {
         super.doWaterSplashEffect();
      }

   }

   protected void playStepSound(BlockPos pPos, BlockState pState) {
      if (this.isInWater()) {
         this.waterSwimSound();
         this.playMuffledStepSound(pState);
      } else {
         BlockPos blockpos = this.getPrimaryStepSoundBlockPos(pPos);
         if (!pPos.equals(blockpos)) {
            BlockState blockstate = this.level().getBlockState(blockpos);
            if (blockstate.is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
               this.playCombinationStepSounds(blockstate, pState);
            } else {
               super.playStepSound(blockpos, blockstate);
            }
         } else {
            super.playStepSound(pPos, pState);
         }
      }

   }

   public LivingEntity.Fallsounds getFallSounds() {
      return new LivingEntity.Fallsounds(SoundEvents.PLAYER_SMALL_FALL, SoundEvents.PLAYER_BIG_FALL);
   }

   public boolean killedEntity(ServerLevel pLevel, LivingEntity pEntity) {
      this.awardStat(Stats.ENTITY_KILLED.get(pEntity.getType()));
      return true;
   }

   public void makeStuckInBlock(BlockState pState, Vec3 pMotionMultiplier) {
      if (!this.abilities.flying) {
         super.makeStuckInBlock(pState, pMotionMultiplier);
      }

   }

   public void giveExperiencePoints(int pXpPoints) {
      this.increaseScore(pXpPoints);
      this.experienceProgress += (float)pXpPoints / (float)this.getXpNeededForNextLevel();
      this.totalExperience = Mth.clamp(this.totalExperience + pXpPoints, 0, Integer.MAX_VALUE);

      while(this.experienceProgress < 0.0F) {
         float f = this.experienceProgress * (float)this.getXpNeededForNextLevel();
         if (this.experienceLevel > 0) {
            this.giveExperienceLevels(-1);
            this.experienceProgress = 1.0F + f / (float)this.getXpNeededForNextLevel();
         } else {
            this.giveExperienceLevels(-1);
            this.experienceProgress = 0.0F;
         }
      }

      while(this.experienceProgress >= 1.0F) {
         this.experienceProgress = (this.experienceProgress - 1.0F) * (float)this.getXpNeededForNextLevel();
         this.giveExperienceLevels(1);
         this.experienceProgress /= (float)this.getXpNeededForNextLevel();
      }

   }

   public int getEnchantmentSeed() {
      return this.enchantmentSeed;
   }

   public void onEnchantmentPerformed(ItemStack pEnchantedItem, int pLevelCost) {
      this.experienceLevel -= pLevelCost;
      if (this.experienceLevel < 0) {
         this.experienceLevel = 0;
         this.experienceProgress = 0.0F;
         this.totalExperience = 0;
      }

      this.enchantmentSeed = this.random.nextInt();
   }

   public void giveExperienceLevels(int pLevels) {
      this.experienceLevel += pLevels;
      if (this.experienceLevel < 0) {
         this.experienceLevel = 0;
         this.experienceProgress = 0.0F;
         this.totalExperience = 0;
      }

      if (pLevels > 0 && this.experienceLevel % 5 == 0 && (float)this.lastLevelUpTime < (float)this.tickCount - 100.0F) {
         float f = this.experienceLevel > 30 ? 1.0F : (float)this.experienceLevel / 30.0F;
         this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), f * 0.75F, 1.0F);
         this.lastLevelUpTime = this.tickCount;
      }

   }

   public int getXpNeededForNextLevel() {
      if (this.experienceLevel >= 30) {
         return 112 + (this.experienceLevel - 30) * 9;
      } else {
         return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
      }
   }

   public void causeFoodExhaustion(float pExhaustion) {
      if (!this.abilities.invulnerable) {
         if (!this.level().isClientSide) {
            this.foodData.addExhaustion(pExhaustion);
         }

      }
   }

   public Optional<WardenSpawnTracker> getWardenSpawnTracker() {
      return Optional.empty();
   }

   public FoodData getFoodData() {
      return this.foodData;
   }

   public boolean canEat(boolean pCanAlwaysEat) {
      return this.abilities.invulnerable || pCanAlwaysEat || this.foodData.needsFood();
   }

   public boolean isHurt() {
      return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
   }

   public boolean mayBuild() {
      return this.abilities.mayBuild;
   }

   public boolean mayUseItemAt(BlockPos pPos, Direction pFacing, ItemStack pStack) {
      if (this.abilities.mayBuild) {
         return true;
      } else {
         BlockPos blockpos = pPos.relative(pFacing.getOpposite());
         BlockInWorld blockinworld = new BlockInWorld(this.level(), blockpos, false);
         return pStack.hasAdventureModePlaceTagForBlock(this.level().registryAccess().registryOrThrow(Registries.BLOCK), blockinworld);
      }
   }

   public int getExperienceReward() {
      if (!this.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && !this.isSpectator()) {
         int i = this.experienceLevel * 7;
         return i > 100 ? 100 : i;
      } else {
         return 0;
      }
   }

   protected boolean isAlwaysExperienceDropper() {
      return true;
   }

   public boolean shouldShowName() {
      return true;
   }

   protected Entity.MovementEmission getMovementEmission() {
      return this.abilities.flying || this.onGround() && this.isDiscrete() ? Entity.MovementEmission.NONE : Entity.MovementEmission.ALL;
   }

   public void onUpdateAbilities() {
   }

   public Component getName() {
      return Component.literal(this.gameProfile.getName());
   }

   public PlayerEnderChestContainer getEnderChestInventory() {
      return this.enderChestInventory;
   }

   public ItemStack getItemBySlot(EquipmentSlot pSlot1) {
      if (pSlot1 == EquipmentSlot.MAINHAND) {
         return this.inventory.getSelected();
      } else if (pSlot1 == EquipmentSlot.OFFHAND) {
         return this.inventory.offhand.get(0);
      } else {
         return pSlot1.getType() == EquipmentSlot.Type.ARMOR ? this.inventory.armor.get(pSlot1.getIndex()) : ItemStack.EMPTY;
      }
   }

   protected boolean doesEmitEquipEvent(EquipmentSlot pSlot) {
      return pSlot.getType() == EquipmentSlot.Type.ARMOR;
   }

   public void setItemSlot(EquipmentSlot pSlot, ItemStack pStack) {
      this.verifyEquippedItem(pStack);
      if (pSlot == EquipmentSlot.MAINHAND) {
         this.onEquipItem(pSlot, this.inventory.items.set(this.inventory.selected, pStack), pStack);
      } else if (pSlot == EquipmentSlot.OFFHAND) {
         this.onEquipItem(pSlot, this.inventory.offhand.set(0, pStack), pStack);
      } else if (pSlot.getType() == EquipmentSlot.Type.ARMOR) {
         this.onEquipItem(pSlot, this.inventory.armor.set(pSlot.getIndex(), pStack), pStack);
      }

   }

   public boolean addItem(ItemStack pStack) {
      return this.inventory.add(pStack);
   }

   public Iterable<ItemStack> getHandSlots() {
      return Lists.newArrayList(this.getMainHandItem(), this.getOffhandItem());
   }

   public Iterable<ItemStack> getArmorSlots() {
      return this.inventory.armor;
   }

   public boolean setEntityOnShoulder(CompoundTag pEntityCompound) {
      if (!this.isPassenger() && this.onGround() && !this.isInWater() && !this.isInPowderSnow) {
         if (this.getShoulderEntityLeft().isEmpty()) {
            this.setShoulderEntityLeft(pEntityCompound);
            this.timeEntitySatOnShoulder = this.level().getGameTime();
            return true;
         } else if (this.getShoulderEntityRight().isEmpty()) {
            this.setShoulderEntityRight(pEntityCompound);
            this.timeEntitySatOnShoulder = this.level().getGameTime();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   protected void removeEntitiesOnShoulder() {
      if (this.timeEntitySatOnShoulder + 20L < this.level().getGameTime()) {
         this.respawnEntityOnShoulder(this.getShoulderEntityLeft());
         this.setShoulderEntityLeft(new CompoundTag());
         this.respawnEntityOnShoulder(this.getShoulderEntityRight());
         this.setShoulderEntityRight(new CompoundTag());
      }

   }

   private void respawnEntityOnShoulder(CompoundTag pEntityCompound) {
      if (!this.level().isClientSide && !pEntityCompound.isEmpty()) {
         EntityType.create(pEntityCompound, this.level()).ifPresent((p_309218_) -> {
            if (p_309218_ instanceof TamableAnimal) {
               ((TamableAnimal)p_309218_).setOwnerUUID(this.uuid);
            }

            p_309218_.setPos(this.getX(), this.getY() + (double)0.7F, this.getZ());
            ((ServerLevel)this.level()).addWithUUID(p_309218_);
         });
      }

   }

   public abstract boolean isSpectator();

   public boolean canBeHitByProjectile() {
      return !this.isSpectator() && super.canBeHitByProjectile();
   }

   public boolean isSwimming() {
      return !this.abilities.flying && !this.isSpectator() && super.isSwimming();
   }

   public abstract boolean isCreative();

   public boolean isPushedByFluid() {
      return !this.abilities.flying;
   }

   public Scoreboard getScoreboard() {
      return this.level().getScoreboard();
   }

   public Component getDisplayName() {
      MutableComponent mutablecomponent = PlayerTeam.formatNameForTeam(this.getTeam(), this.getName());
      return this.decorateDisplayNameComponent(mutablecomponent);
   }

   private MutableComponent decorateDisplayNameComponent(MutableComponent pDisplayName) {
      String s = this.getGameProfile().getName();
      return pDisplayName.withStyle((p_309216_) -> {
         return p_309216_.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + s + " ")).withHoverEvent(this.createHoverEvent()).withInsertion(s);
      });
   }

   public String getScoreboardName() {
      return this.getGameProfile().getName();
   }

   public float getStandingEyeHeight(Pose pPose, EntityDimensions pSize) {

      switch (pPose) {
         case SWIMMING:
         case FALL_FLYING:
         case SPIN_ATTACK:
            return 0.4F;
         case CROUCHING:
            if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)) {
               return 1.54F;
            } else {
               return  1.27f;
            }
         default:
            return 1.62F;
      }
   }

   protected void internalSetAbsorptionAmount(float pAbsorptionAmount) {
      this.getEntityData().set(DATA_PLAYER_ABSORPTION_ID, pAbsorptionAmount);
   }

   public float getAbsorptionAmount() {
      return this.getEntityData().get(DATA_PLAYER_ABSORPTION_ID);
   }

   public boolean isModelPartShown(PlayerModelPart pPart) {
      return (this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION) & pPart.getMask()) == pPart.getMask();
   }

   public SlotAccess getSlot(int pSlot) {
      if (pSlot >= 0 && pSlot < this.inventory.items.size()) {
         return SlotAccess.forContainer(this.inventory, pSlot);
      } else {
         int i = pSlot - 200;
         return i >= 0 && i < this.enderChestInventory.getContainerSize() ? SlotAccess.forContainer(this.enderChestInventory, i) : super.getSlot(pSlot);
      }
   }

   public boolean isReducedDebugInfo() {
      return this.reducedDebugInfo;
   }

   public void setReducedDebugInfo(boolean pReducedDebugInfo) {
      this.reducedDebugInfo = pReducedDebugInfo;
   }

   public void setRemainingFireTicks(int pTicks) {
      super.setRemainingFireTicks(this.abilities.invulnerable ? Math.min(pTicks, 1) : pTicks);
   }

   public HumanoidArm getMainArm() {
      return this.entityData.get(DATA_PLAYER_MAIN_HAND) == 0 ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
   }

   public void setMainArm(HumanoidArm pHand) {
      this.entityData.set(DATA_PLAYER_MAIN_HAND, (byte)(pHand == HumanoidArm.LEFT ? 0 : 1));
   }

   public CompoundTag getShoulderEntityLeft() {
      return this.entityData.get(DATA_SHOULDER_LEFT);
   }

   protected void setShoulderEntityLeft(CompoundTag pEntityCompound) {
      this.entityData.set(DATA_SHOULDER_LEFT, pEntityCompound);
   }

   public CompoundTag getShoulderEntityRight() {
      return this.entityData.get(DATA_SHOULDER_RIGHT);
   }

   protected void setShoulderEntityRight(CompoundTag pEntityCompound) {
      this.entityData.set(DATA_SHOULDER_RIGHT, pEntityCompound);
   }

   public float getCurrentItemAttackStrengthDelay() {
      return (float)(1.0D / this.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0D);
   }

   public float getAttackStrengthScale(float pAdjustTicks) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
         return (1F);
      }
      return Mth.clamp(((float)this.attackStrengthTicker + pAdjustTicks) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
   }

   public void resetAttackStrengthTicker() {
      this.attackStrengthTicker = 0;
   }

   public ItemCooldowns getCooldowns() {
      return this.cooldowns;
   }

   protected float getBlockSpeedFactor() {
      return !this.abilities.flying && !this.isFallFlying() ? super.getBlockSpeedFactor() : 1.0F;
   }

   public float getLuck() {
      return (float)this.getAttributeValue(Attributes.LUCK);
   }

   public boolean canUseGameMasterBlocks() {
      return this.abilities.instabuild && this.getPermissionLevel() >= 2;
   }

   public boolean canTakeItem(ItemStack pItemstack) {
      EquipmentSlot equipmentslot = Mob.getEquipmentSlotForItem(pItemstack);
      return this.getItemBySlot(equipmentslot).isEmpty();
   }

   public EntityDimensions getDimensions(Pose pPose) {
      if (pPose == Pose.CROUCHING) {
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            return (Player.STANDING_DIMENSIONS);
         } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_13_2)) {
            return (viaFabricPlus$sneaking_dimensions_v1_13_2);
         }
      }
      return POSES.getOrDefault(pPose, STANDING_DIMENSIONS);
   }

   public ImmutableList<Pose> getDismountPoses() {
      return ImmutableList.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING);
   }

   public ItemStack getProjectile(ItemStack pShootable) {
      if (!(pShootable.getItem() instanceof ProjectileWeaponItem)) {
         return ItemStack.EMPTY;
      } else {
         Predicate<ItemStack> predicate = ((ProjectileWeaponItem)pShootable.getItem()).getSupportedHeldProjectiles();
         ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
         if (!itemstack.isEmpty()) {
            return itemstack;
         } else {
            predicate = ((ProjectileWeaponItem)pShootable.getItem()).getAllSupportedProjectiles();

            for(int i = 0; i < this.inventory.getContainerSize(); ++i) {
               ItemStack itemstack1 = this.inventory.getItem(i);
               if (predicate.test(itemstack1)) {
                  return itemstack1;
               }
            }

            return this.abilities.instabuild ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
         }
      }
   }

   public ItemStack eat(Level pLevel, ItemStack pFood) {
      this.getFoodData().eat(pFood.getItem(), pFood);
      this.awardStat(Stats.ITEM_USED.get(pFood.getItem()));
      pLevel.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, pLevel.random.nextFloat() * 0.1F + 0.9F);
      if (this instanceof ServerPlayer) {
         CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)this, pFood);
      }

      return super.eat(pLevel, pFood);
   }

   protected boolean shouldRemoveSoulSpeed(BlockState pState) {
      return this.abilities.flying || super.shouldRemoveSoulSpeed(pState);
   }

   public Vec3 getRopeHoldPosition(float pPartialTicks) {
      double d0 = 0.22D * (this.getMainArm() == HumanoidArm.RIGHT ? -1.0D : 1.0D);
      float f = Mth.lerp(pPartialTicks * 0.5F, this.getXRot(), this.xRotO) * ((float)Math.PI / 180F);
      float f1 = Mth.lerp(pPartialTicks, this.yBodyRotO, this.yBodyRot) * ((float)Math.PI / 180F);
      if (!this.isFallFlying() && !this.isAutoSpinAttack()) {
         if (this.isVisuallySwimming()) {
            return this.getPosition(pPartialTicks).add((new Vec3(d0, 0.2D, -0.15D)).xRot(-f).yRot(-f1));
         } else {
            double d5 = this.getBoundingBox().getYsize() - 1.0D;
            double d6 = this.isCrouching() ? -0.2D : 0.07D;
            return this.getPosition(pPartialTicks).add((new Vec3(d0, d5, d6)).yRot(-f1));
         }
      } else {
         Vec3 vec3 = this.getViewVector(pPartialTicks);
         Vec3 vec31 = this.getDeltaMovement();
         double d1 = vec31.horizontalDistanceSqr();
         double d2 = vec3.horizontalDistanceSqr();
         float f2;
         if (d1 > 0.0D && d2 > 0.0D) {
            double d3 = (vec31.x * vec3.x + vec31.z * vec3.z) / Math.sqrt(d1 * d2);
            double d4 = vec31.x * vec3.z - vec31.z * vec3.x;
            f2 = (float)(Math.signum(d4) * Math.acos(d3));
         } else {
            f2 = 0.0F;
         }

         return this.getPosition(pPartialTicks).add((new Vec3(d0, -0.11D, 0.85D)).zRot(-f2).xRot(-f).yRot(-f1));
      }
   }

   public boolean isAlwaysTicking() {
      return true;
   }

   public boolean isScoping() {
      return this.isUsingItem() && this.getUseItem().is(Items.SPYGLASS);
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public Optional<GlobalPos> getLastDeathLocation() {
      return this.lastDeathLocation;
   }

   public void setLastDeathLocation(Optional<GlobalPos> pLastDeathLocation) {
      this.lastDeathLocation = pLastDeathLocation;
   }

   public float getHurtDir() {
      return this.hurtDir;
   }

   public void animateHurt(float pYaw) {
      super.animateHurt(pYaw);
      this.hurtDir = pYaw;
   }

   public boolean canSprint() {
      return true;
   }

   protected float getFlyingSpeed() {
      boolean viaFixSprint;
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_19_3)) {
         viaFixSprint = viaFabricPlus$isSprinting;
      } else {
         viaFixSprint = this.isSprinting();
      }
      if (this.abilities.flying && !this.isPassenger()) {
         return viaFixSprint ? this.abilities.getFlyingSpeed() * 2.0F : this.abilities.getFlyingSpeed();
      } else {
         return viaFixSprint ? 0.025999999F : 0.02F;
      }
   }

   public static boolean isValidUsername(String pName) {
      return pName.length() > 16 ? false : pName.chars().filter((p_309217_) -> {
         return p_309217_ <= 32 || p_309217_ >= 127;
      }).findAny().isEmpty();
   }

   public static float getPickRange(boolean pCreative) {
      if (ProtocolTranslator.getTargetVersion().olderThan(LegacyProtocolVersion.r1_0_0tor1_0_1) && !pCreative) {
         return (4F);
      }
      return pCreative ? 5.0F : 4.5F;
   }

   public static enum BedSleepingProblem {
      NOT_POSSIBLE_HERE,
      NOT_POSSIBLE_NOW(Component.translatable("block.minecraft.bed.no_sleep")),
      TOO_FAR_AWAY(Component.translatable("block.minecraft.bed.too_far_away")),
      OBSTRUCTED(Component.translatable("block.minecraft.bed.obstructed")),
      OTHER_PROBLEM,
      NOT_SAFE(Component.translatable("block.minecraft.bed.not_safe"));

      @Nullable
      private final Component message;

      private BedSleepingProblem() {
         this.message = null;
      }

      private BedSleepingProblem(Component pMessage) {
         this.message = pMessage;
      }

      @Nullable
      public Component getMessage() {
         return this.message;
      }
   }
}