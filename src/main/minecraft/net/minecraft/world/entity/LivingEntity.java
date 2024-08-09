package net.minecraft.world.entity;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.event.events.RotationMoveEvent;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Unique;

public abstract class LivingEntity extends Entity implements Attackable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String TAG_ACTIVE_EFFECTS = "active_effects";
   private static final UUID SPEED_MODIFIER_SOUL_SPEED_UUID = UUID.fromString("87f46a96-686f-4796-b035-22e16ee9e038");
   private static final UUID SPEED_MODIFIER_POWDER_SNOW_UUID = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4ce");
   private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D"), "Sprinting speed boost", (double)0.3F, AttributeModifier.Operation.MULTIPLY_TOTAL);
   public static final int HAND_SLOTS = 2;
   public static final int ARMOR_SLOTS = 4;
   public static final int EQUIPMENT_SLOT_OFFSET = 98;
   public static final int ARMOR_SLOT_OFFSET = 100;
   public static final int SWING_DURATION = 6;
   public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
   private static final int DAMAGE_SOURCE_TIMEOUT = 40;
   public static final double MIN_MOVEMENT_DISTANCE = 0.003D;
   public static final double DEFAULT_BASE_GRAVITY = 0.08D;
   public static final int DEATH_DURATION = 20;
   private static final int WAIT_TICKS_BEFORE_ITEM_USE_EFFECTS = 7;
   private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
   private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
   public static final int USE_ITEM_INTERVAL = 4;
   private static final float BASE_JUMP_POWER = 0.42F;
   private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0D;
   protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
   protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
   protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
   protected static final EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_EFFECT_COLOR_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_STINGER_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Optional<BlockPos>> SLEEPING_POS_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
   protected static final float DEFAULT_EYE_HEIGHT = 1.74F;
   protected static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2F, 0.2F);
   public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5F;
   private final AttributeMap attributes;
   private final CombatTracker combatTracker = new CombatTracker(this);
   private final Map<MobEffect, MobEffectInstance> activeEffects = Maps.newHashMap();
   private final NonNullList<ItemStack> lastHandItemStacks = NonNullList.withSize(2, ItemStack.EMPTY);
   private final NonNullList<ItemStack> lastArmorItemStacks = NonNullList.withSize(4, ItemStack.EMPTY);
   public boolean swinging;
   private boolean discardFriction = false;
   public InteractionHand swingingArm;
   public int swingTime;
   public int removeArrowTime;
   public int removeStingerTime;
   public int hurtTime;
   public int hurtDuration;
   public int deathTime;
   public float oAttackAnim;
   public float attackAnim;
   protected int attackStrengthTicker;
   public final WalkAnimationState walkAnimation = new WalkAnimationState();
   public final int invulnerableDuration = 20;
   public final float timeOffs;
   public final float rotA;
   public float yBodyRot;
   public float yBodyRotO;
   public float yHeadRot;
   public float yHeadRotO;
   @Nullable
   protected Player lastHurtByPlayer;
   protected int lastHurtByPlayerTime;
   protected boolean dead;
   protected int noActionTime;
   protected float oRun;
   protected float run;
   protected float animStep;
   protected float animStepO;
   protected float rotOffs;
   protected int deathScore;
   protected float lastHurt;
   protected boolean jumping;
   public float xxa;
   public float yya;
   public float zza;
   protected int lerpSteps;
   protected double lerpX;
   protected double lerpY;
   protected double lerpZ;
   protected double lerpYRot;
   protected double lerpXRot;
   protected double lerpYHeadRot;
   protected int lerpHeadSteps;
   private boolean effectsDirty = true;
   @Nullable
   private LivingEntity lastHurtByMob;
   private int lastHurtByMobTimestamp;
   private LivingEntity lastHurtMob;
   private int lastHurtMobTimestamp;
   private float speed;
   private int noJumpDelay;
   private float absorptionAmount;
   protected ItemStack useItem = ItemStack.EMPTY;
   protected int useItemRemaining;
   protected int fallFlyTicks;
   private BlockPos lastPos;
   private Optional<BlockPos> lastClimbablePos = Optional.empty();
   @Nullable
   private DamageSource lastDamageSource;
   private long lastDamageStamp;
   protected int autoSpinAttackTicks;
   private float swimAmount;
   private float swimAmountO;
   protected Brain<?> brain;
   private boolean skipDropExperience;

   private RotationMoveEvent jumpRotationEvent;

   private RotationMoveEvent elytraRotationEvent;

   protected LivingEntity(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.attributes = new AttributeMap(DefaultAttributes.getSupplier(pEntityType));
      this.setHealth(this.getMaxHealth());
      this.blocksBuilding = true;
      this.rotA = (float)((Math.random() + 1.0D) * (double)0.01F);
      this.reapplyPosition();
      this.timeOffs = (float)Math.random() * 12398.0F;
      this.setYRot((float)(Math.random() * (double)((float)Math.PI * 2F)));
      this.yHeadRot = this.getYRot();
      this.setMaxUpStep(0.6F);
      NbtOps nbtops = NbtOps.INSTANCE;
      this.brain = this.makeBrain(new Dynamic<>(nbtops, nbtops.createMap(ImmutableMap.of(nbtops.createString("memories"), nbtops.emptyMap()))));
   }

   public Brain<?> getBrain() {
      return this.brain;
   }

   protected Brain.Provider<?> brainProvider() {
      return Brain.provider(ImmutableList.of(), ImmutableList.of());
   }

   protected Brain<?> makeBrain(Dynamic<?> pDynamic) {
      return this.brainProvider().makeBrain(pDynamic);
   }

   public void kill() {
      this.hurt(this.damageSources().genericKill(), Float.MAX_VALUE);
   }

   public boolean canAttackType(EntityType<?> pEntityType) {
      return true;
   }

   protected void defineSynchedData() {
      this.entityData.define(DATA_LIVING_ENTITY_FLAGS, (byte)0);
      this.entityData.define(DATA_EFFECT_COLOR_ID, 0);
      this.entityData.define(DATA_EFFECT_AMBIENCE_ID, false);
      this.entityData.define(DATA_ARROW_COUNT_ID, 0);
      this.entityData.define(DATA_STINGER_COUNT_ID, 0);
      this.entityData.define(DATA_HEALTH_ID, 1.0F);
      this.entityData.define(SLEEPING_POS_ID, Optional.empty());
   }

   public static AttributeSupplier.Builder createLivingAttributes() {
      return AttributeSupplier.builder().add(Attributes.MAX_HEALTH).add(Attributes.KNOCKBACK_RESISTANCE).add(Attributes.MOVEMENT_SPEED).add(Attributes.ARMOR).add(Attributes.ARMOR_TOUGHNESS).add(Attributes.MAX_ABSORPTION);
   }

   protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
      if (!this.isInWater()) {
         this.updateInWaterStateAndDoWaterCurrentPushing();
      }

      if (!this.level().isClientSide && pOnGround && this.fallDistance > 0.0F) {
         this.removeSoulSpeed();
         this.tryAddSoulSpeed();
      }

      if (!this.level().isClientSide && this.fallDistance > 3.0F && pOnGround && !pState.isAir()) {
         double d0 = this.getX();
         double d1 = this.getY();
         double d2 = this.getZ();
         BlockPos blockpos = this.blockPosition();
         if (pPos.getX() != blockpos.getX() || pPos.getZ() != blockpos.getZ()) {
            double d3 = d0 - (double)pPos.getX() - 0.5D;
            double d5 = d2 - (double)pPos.getZ() - 0.5D;
            double d6 = Math.max(Math.abs(d3), Math.abs(d5));
            d0 = (double)pPos.getX() + 0.5D + d3 / d6 * 0.5D;
            d2 = (double)pPos.getZ() + 0.5D + d5 / d6 * 0.5D;
         }

         float f = (float)Mth.ceil(this.fallDistance - 3.0F);
         double d4 = Math.min((double)(0.2F + f / 15.0F), 2.5D);
         int i = (int)(150.0D * d4);
         ((ServerLevel)this.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, pState), d0, d1, d2, i, 0.0D, 0.0D, 0.0D, (double)0.15F);
      }

      super.checkFallDamage(pY, pOnGround, pState, pPos);
      if (pOnGround) {
         this.lastClimbablePos = Optional.empty();
      }

   }

   public final boolean canBreatheUnderwater() {
      return this.getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
   }

   public float getSwimAmount(float pPartialTicks) {
      return Mth.lerp(pPartialTicks, this.swimAmountO, this.swimAmount);
   }

   public void baseTick() {
      this.oAttackAnim = this.attackAnim;
      if (this.firstTick) {
         this.getSleepingPos().ifPresent(this::setPosToBed);
      }

      if (this.canSpawnSoulSpeedParticle()) {
         this.spawnSoulSpeedParticle();
      }

      super.baseTick();
      this.level().getProfiler().push("livingEntityBaseTick");
      if (this.fireImmune() || this.level().isClientSide) {
         this.clearFire();
      }

      if (this.isAlive()) {
         boolean flag = this instanceof Player;
         if (!this.level().isClientSide) {
            if (this.isInWall()) {
               this.hurt(this.damageSources().inWall(), 1.0F);
            } else if (flag && !this.level().getWorldBorder().isWithinBounds(this.getBoundingBox())) {
               double d0 = this.level().getWorldBorder().getDistanceToBorder(this) + this.level().getWorldBorder().getDamageSafeZone();
               if (d0 < 0.0D) {
                  double d1 = this.level().getWorldBorder().getDamagePerBlock();
                  if (d1 > 0.0D) {
                     this.hurt(this.damageSources().outOfBorder(), (float)Math.max(1, Mth.floor(-d0 * d1)));
                  }
               }
            }
         }

         if (this.isEyeInFluid(FluidTags.WATER) && !this.level().getBlockState(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
            boolean flag1 = !this.canBreatheUnderwater() && !MobEffectUtil.hasWaterBreathing(this) && (!flag || !((Player)this).getAbilities().invulnerable);
            if (flag1) {
               this.setAirSupply(this.decreaseAirSupply(this.getAirSupply()));
               if (this.getAirSupply() == -20) {
                  this.setAirSupply(0);
                  Vec3 vec3 = this.getDeltaMovement();

                  for(int i = 0; i < 8; ++i) {
                     double d2 = this.random.nextDouble() - this.random.nextDouble();
                     double d3 = this.random.nextDouble() - this.random.nextDouble();
                     double d4 = this.random.nextDouble() - this.random.nextDouble();
                     this.level().addParticle(ParticleTypes.BUBBLE, this.getX() + d2, this.getY() + d3, this.getZ() + d4, vec3.x, vec3.y, vec3.z);
                  }

                  this.hurt(this.damageSources().drown(), 2.0F);
               }
            }

            if (!this.level().isClientSide && this.isPassenger() && this.getVehicle() != null && this.getVehicle().dismountsUnderwater()) {
               this.stopRiding();
            }
         } else if (this.getAirSupply() < this.getMaxAirSupply()) {
            this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
         }

         if (!this.level().isClientSide) {
            BlockPos blockpos = this.blockPosition();
            if (!Objects.equal(this.lastPos, blockpos)) {
               this.lastPos = blockpos;
               this.onChangedBlock(blockpos);
            }
         }
      }

      if (this.isAlive() && (this.isInWaterRainOrBubble() || this.isInPowderSnow)) {
         this.extinguishFire();
      }

      if (this.hurtTime > 0) {
         --this.hurtTime;
      }

      if (this.invulnerableTime > 0 && !(this instanceof ServerPlayer)) {
         --this.invulnerableTime;
      }

      if (this.isDeadOrDying() && this.level().shouldTickDeath(this)) {
         this.tickDeath();
      }

      if (this.lastHurtByPlayerTime > 0) {
         --this.lastHurtByPlayerTime;
      } else {
         this.lastHurtByPlayer = null;
      }

      if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
         this.lastHurtMob = null;
      }

      if (this.lastHurtByMob != null) {
         if (!this.lastHurtByMob.isAlive()) {
            this.setLastHurtByMob((LivingEntity)null);
         } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
            this.setLastHurtByMob((LivingEntity)null);
         }
      }

      this.tickEffects();
      this.animStepO = this.animStep;
      this.yBodyRotO = this.yBodyRot;
      this.yHeadRotO = this.yHeadRot;
      this.yRotO = this.getYRot();
      this.xRotO = this.getXRot();
      this.level().getProfiler().pop();
   }

   public boolean canSpawnSoulSpeedParticle() {
      return this.tickCount % 5 == 0 && this.getDeltaMovement().x != 0.0D && this.getDeltaMovement().z != 0.0D && !this.isSpectator() && EnchantmentHelper.hasSoulSpeed(this) && this.onSoulSpeedBlock();
   }

   protected void spawnSoulSpeedParticle() {
      Vec3 vec3 = this.getDeltaMovement();
      this.level().addParticle(ParticleTypes.SOUL, this.getX() + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth(), this.getY() + 0.1D, this.getZ() + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth(), vec3.x * -0.2D, 0.1D, vec3.z * -0.2D);
      float f = this.random.nextFloat() * 0.4F + this.random.nextFloat() > 0.9F ? 0.6F : 0.0F;
      this.playSound(SoundEvents.SOUL_ESCAPE, f, 0.6F + this.random.nextFloat() * 0.4F);
   }

   protected boolean onSoulSpeedBlock() {
      return this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).is(BlockTags.SOUL_SPEED_BLOCKS);
   }

   protected float getBlockSpeedFactor() {
      return this.onSoulSpeedBlock() && EnchantmentHelper.getEnchantmentLevel(Enchantments.SOUL_SPEED, this) > 0 ? 1.0F : super.getBlockSpeedFactor();
   }

   protected boolean shouldRemoveSoulSpeed(BlockState pState) {
      return !pState.isAir() || this.isFallFlying();
   }

   protected void removeSoulSpeed() {
      AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attributeinstance != null) {
         if (attributeinstance.getModifier(SPEED_MODIFIER_SOUL_SPEED_UUID) != null) {
            attributeinstance.removeModifier(SPEED_MODIFIER_SOUL_SPEED_UUID);
         }

      }
   }

   protected void tryAddSoulSpeed() {
      if (!this.getBlockStateOnLegacy().isAir()) {
         int i = EnchantmentHelper.getEnchantmentLevel(Enchantments.SOUL_SPEED, this);
         if (i > 0 && this.onSoulSpeedBlock()) {
            AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attributeinstance == null) {
               return;
            }

            attributeinstance.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_SOUL_SPEED_UUID, "Soul speed boost", (double)(0.03F * (1.0F + (float)i * 0.35F)), AttributeModifier.Operation.ADDITION));
            if (this.getRandom().nextFloat() < 0.04F) {
               ItemStack itemstack = this.getItemBySlot(EquipmentSlot.FEET);
               itemstack.hurtAndBreak(1, this, (p_21301_) -> {
                  p_21301_.broadcastBreakEvent(EquipmentSlot.FEET);
               });
            }
         }
      }

   }

   protected void removeFrost() {
      AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
      if (attributeinstance != null) {
         if (attributeinstance.getModifier(SPEED_MODIFIER_POWDER_SNOW_UUID) != null) {
            attributeinstance.removeModifier(SPEED_MODIFIER_POWDER_SNOW_UUID);
         }

      }
   }

   protected void tryAddFrost() {
      if (!this.getBlockStateOnLegacy().isAir()) {
         int i = this.getTicksFrozen();
         if (i > 0) {
            AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attributeinstance == null) {
               return;
            }

            float f = -0.05F * this.getPercentFrozen();
            attributeinstance.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_UUID, "Powder snow slow", (double)f, AttributeModifier.Operation.ADDITION));
         }
      }

   }

   protected void onChangedBlock(BlockPos pPos) {
      int i = EnchantmentHelper.getEnchantmentLevel(Enchantments.FROST_WALKER, this);
      if (i > 0) {
         FrostWalkerEnchantment.onEntityMoved(this, this.level(), pPos, i);
      }

      if (this.shouldRemoveSoulSpeed(this.getBlockStateOnLegacy())) {
         this.removeSoulSpeed();
      }

      this.tryAddSoulSpeed();
   }

   public boolean isBaby() {
      return false;
   }

   public float getScale() {
      return this.isBaby() ? 0.5F : 1.0F;
   }

   protected boolean isAffectedByFluids() {
      return true;
   }

   protected void tickDeath() {
      ++this.deathTime;
      if (this.deathTime >= 20 && !this.level().isClientSide() && !this.isRemoved()) {
         this.level().broadcastEntityEvent(this, (byte)60);
         this.remove(Entity.RemovalReason.KILLED);
      }

   }

   public boolean shouldDropExperience() {
      return !this.isBaby();
   }

   protected boolean shouldDropLoot() {
      return !this.isBaby();
   }

   protected int decreaseAirSupply(int pCurrentAir) {
      int i = EnchantmentHelper.getRespiration(this);
      return i > 0 && this.random.nextInt(i + 1) > 0 ? pCurrentAir : pCurrentAir - 1;
   }

   protected int increaseAirSupply(int pCurrentAir) {
      return Math.min(pCurrentAir + 4, this.getMaxAirSupply());
   }

   public int getExperienceReward() {
      return 0;
   }

   protected boolean isAlwaysExperienceDropper() {
      return false;
   }

   public RandomSource getRandom() {
      return this.random;
   }

   @Nullable
   public LivingEntity getLastHurtByMob() {
      return this.lastHurtByMob;
   }

   public LivingEntity getLastAttacker() {
      return this.getLastHurtByMob();
   }

   public int getLastHurtByMobTimestamp() {
      return this.lastHurtByMobTimestamp;
   }

   public void setLastHurtByPlayer(@Nullable Player pPlayer) {
      this.lastHurtByPlayer = pPlayer;
      this.lastHurtByPlayerTime = this.tickCount;
   }

   public void setLastHurtByMob(@Nullable LivingEntity pLivingEntity) {
      this.lastHurtByMob = pLivingEntity;
      this.lastHurtByMobTimestamp = this.tickCount;
   }

   @Nullable
   public LivingEntity getLastHurtMob() {
      return this.lastHurtMob;
   }

   public int getLastHurtMobTimestamp() {
      return this.lastHurtMobTimestamp;
   }

   public void setLastHurtMob(Entity pEntity) {
      if (pEntity instanceof LivingEntity) {
         this.lastHurtMob = (LivingEntity)pEntity;
      } else {
         this.lastHurtMob = null;
      }

      this.lastHurtMobTimestamp = this.tickCount;
   }

   public int getNoActionTime() {
      return this.noActionTime;
   }

   public void setNoActionTime(int pIdleTime) {
      this.noActionTime = pIdleTime;
   }

   public boolean shouldDiscardFriction() {
      return this.discardFriction;
   }

   public void setDiscardFriction(boolean pDiscardFriction) {
      this.discardFriction = pDiscardFriction;
   }

   protected boolean doesEmitEquipEvent(EquipmentSlot pSlot) {
      return true;
   }

   public void onEquipItem(EquipmentSlot pSlot, ItemStack pOldItem, ItemStack pNewItem) {
      boolean flag = pNewItem.isEmpty() && pOldItem.isEmpty();
      if (!flag && !ItemStack.isSameItemSameTags(pOldItem, pNewItem) && !this.firstTick) {
         Equipable equipable = Equipable.get(pNewItem);
         if (!this.level().isClientSide() && !this.isSpectator()) {
            if (!this.isSilent() && equipable != null && equipable.getEquipmentSlot() == pSlot) {
               this.level().playSound((Player)null, this.getX(), this.getY(), this.getZ(), equipable.getEquipSound(), this.getSoundSource(), 1.0F, 1.0F);
            }

            if (this.doesEmitEquipEvent(pSlot)) {
               this.gameEvent(equipable != null ? GameEvent.EQUIP : GameEvent.UNEQUIP);
            }
         }

      }
   }

   public void remove(Entity.RemovalReason pReason) {
      super.remove(pReason);
      this.brain.clearMemories();
   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
      pCompound.putFloat("Health", this.getHealth());
      pCompound.putShort("HurtTime", (short)this.hurtTime);
      pCompound.putInt("HurtByTimestamp", this.lastHurtByMobTimestamp);
      pCompound.putShort("DeathTime", (short)this.deathTime);
      pCompound.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
      pCompound.put("Attributes", this.getAttributes().save());
      if (!this.activeEffects.isEmpty()) {
         ListTag listtag = new ListTag();

         for(MobEffectInstance mobeffectinstance : this.activeEffects.values()) {
            listtag.add(mobeffectinstance.save(new CompoundTag()));
         }

         pCompound.put("active_effects", listtag);
      }

      pCompound.putBoolean("FallFlying", this.isFallFlying());
      this.getSleepingPos().ifPresent((p_21099_) -> {
         pCompound.putInt("SleepingX", p_21099_.getX());
         pCompound.putInt("SleepingY", p_21099_.getY());
         pCompound.putInt("SleepingZ", p_21099_.getZ());
      });
      DataResult<Tag> dataresult = this.brain.serializeStart(NbtOps.INSTANCE);
      dataresult.resultOrPartial(LOGGER::error).ifPresent((p_21102_) -> {
         pCompound.put("Brain", p_21102_);
      });
   }

   public void readAdditionalSaveData(CompoundTag pCompound) {
      this.internalSetAbsorptionAmount(pCompound.getFloat("AbsorptionAmount"));
      if (pCompound.contains("Attributes", 9) && this.level() != null && !this.level().isClientSide) {
         this.getAttributes().load(pCompound.getList("Attributes", 10));
      }

      if (pCompound.contains("active_effects", 9)) {
         ListTag listtag = pCompound.getList("active_effects", 10);

         for(int i = 0; i < listtag.size(); ++i) {
            CompoundTag compoundtag = listtag.getCompound(i);
            MobEffectInstance mobeffectinstance = MobEffectInstance.load(compoundtag);
            if (mobeffectinstance != null) {
               this.activeEffects.put(mobeffectinstance.getEffect(), mobeffectinstance);
            }
         }
      }

      if (pCompound.contains("Health", 99)) {
         this.setHealth(pCompound.getFloat("Health"));
      }

      this.hurtTime = pCompound.getShort("HurtTime");
      this.deathTime = pCompound.getShort("DeathTime");
      this.lastHurtByMobTimestamp = pCompound.getInt("HurtByTimestamp");
      if (pCompound.contains("Team", 8)) {
         String s = pCompound.getString("Team");
         PlayerTeam playerteam = this.level().getScoreboard().getPlayerTeam(s);
         boolean flag = playerteam != null && this.level().getScoreboard().addPlayerToTeam(this.getStringUUID(), playerteam);
         if (!flag) {
            LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", (Object)s);
         }
      }

      if (pCompound.getBoolean("FallFlying")) {
         this.setSharedFlag(7, true);
      }

      if (pCompound.contains("SleepingX", 99) && pCompound.contains("SleepingY", 99) && pCompound.contains("SleepingZ", 99)) {
         BlockPos blockpos = new BlockPos(pCompound.getInt("SleepingX"), pCompound.getInt("SleepingY"), pCompound.getInt("SleepingZ"));
         this.setSleepingPos(blockpos);
         this.entityData.set(DATA_POSE, Pose.SLEEPING);
         if (!this.firstTick) {
            this.setPosToBed(blockpos);
         }
      }

      if (pCompound.contains("Brain", 10)) {
         this.brain = this.makeBrain(new Dynamic<>(NbtOps.INSTANCE, pCompound.get("Brain")));
      }

   }

   protected void tickEffects() {
      Iterator<MobEffect> iterator = this.activeEffects.keySet().iterator();

      try {
         while(iterator.hasNext()) {
            MobEffect mobeffect = iterator.next();
            MobEffectInstance mobeffectinstance = this.activeEffects.get(mobeffect);
            if (!mobeffectinstance.tick(this, () -> {
               this.onEffectUpdated(mobeffectinstance, true, (Entity)null);
            })) {
               if (!this.level().isClientSide) {
                  iterator.remove();
                  this.onEffectRemoved(mobeffectinstance);
               }
            } else if (mobeffectinstance.getDuration() % 600 == 0) {
               this.onEffectUpdated(mobeffectinstance, false, (Entity)null);
            }
         }
      } catch (ConcurrentModificationException concurrentmodificationexception) {
      }

      if (this.effectsDirty) {
         if (!this.level().isClientSide) {
            this.updateInvisibilityStatus();
            this.updateGlowingStatus();
         }

         this.effectsDirty = false;
      }

      int i = this.entityData.get(DATA_EFFECT_COLOR_ID);
      boolean flag1 = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
      if (i > 0) {
         boolean flag;
         if (this.isInvisible()) {
            flag = this.random.nextInt(15) == 0;
         } else {
            flag = this.random.nextBoolean();
         }

         if (flag1) {
            flag &= this.random.nextInt(5) == 0;
         }

         if (flag && i > 0) {
            double d0 = (double)(i >> 16 & 255) / 255.0D;
            double d1 = (double)(i >> 8 & 255) / 255.0D;
            double d2 = (double)(i >> 0 & 255) / 255.0D;
            this.level().addParticle(flag1 ? ParticleTypes.AMBIENT_ENTITY_EFFECT : ParticleTypes.ENTITY_EFFECT, this.getRandomX(0.5D), this.getRandomY(), this.getRandomZ(0.5D), d0, d1, d2);
         }
      }

   }

   protected void updateInvisibilityStatus() {
      if (this.activeEffects.isEmpty()) {
         this.removeEffectParticles();
         this.setInvisible(false);
      } else {
         Collection<MobEffectInstance> collection = this.activeEffects.values();
         this.entityData.set(DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(collection));
         this.entityData.set(DATA_EFFECT_COLOR_ID, PotionUtils.getColor(collection));
         this.setInvisible(this.hasEffect(MobEffects.INVISIBILITY));
      }

   }

   private void updateGlowingStatus() {
      boolean flag = this.isCurrentlyGlowing();
      if (this.getSharedFlag(6) != flag) {
         this.setSharedFlag(6, flag);
      }

   }

   public double getVisibilityPercent(@Nullable Entity pLookingEntity) {
      double d0 = 1.0D;
      if (this.isDiscrete()) {
         d0 *= 0.8D;
      }

      if (this.isInvisible()) {
         float f = this.getArmorCoverPercentage();
         if (f < 0.1F) {
            f = 0.1F;
         }

         d0 *= 0.7D * (double)f;
      }

      if (pLookingEntity != null) {
         ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
         EntityType<?> entitytype = pLookingEntity.getType();
         if (entitytype == EntityType.SKELETON && itemstack.is(Items.SKELETON_SKULL) || entitytype == EntityType.ZOMBIE && itemstack.is(Items.ZOMBIE_HEAD) || entitytype == EntityType.PIGLIN && itemstack.is(Items.PIGLIN_HEAD) || entitytype == EntityType.PIGLIN_BRUTE && itemstack.is(Items.PIGLIN_HEAD) || entitytype == EntityType.CREEPER && itemstack.is(Items.CREEPER_HEAD)) {
            d0 *= 0.5D;
         }
      }

      return d0;
   }

   public boolean canAttack(LivingEntity pTarget) {
      return pTarget instanceof Player && this.level().getDifficulty() == Difficulty.PEACEFUL ? false : pTarget.canBeSeenAsEnemy();
   }

   public boolean canAttack(LivingEntity pLivingentity, TargetingConditions pCondition) {
      return pCondition.test(this, pLivingentity);
   }

   public boolean canBeSeenAsEnemy() {
      return !this.isInvulnerable() && this.canBeSeenByAnyone();
   }

   public boolean canBeSeenByAnyone() {
      return !this.isSpectator() && this.isAlive();
   }

   public static boolean areAllEffectsAmbient(Collection<MobEffectInstance> pPotionEffects) {
      for(MobEffectInstance mobeffectinstance : pPotionEffects) {
         if (mobeffectinstance.isVisible() && !mobeffectinstance.isAmbient()) {
            return false;
         }
      }

      return true;
   }

   protected void removeEffectParticles() {
      this.entityData.set(DATA_EFFECT_AMBIENCE_ID, false);
      this.entityData.set(DATA_EFFECT_COLOR_ID, 0);
   }

   public boolean removeAllEffects() {
      if (this.level().isClientSide) {
         return false;
      } else {
         Iterator<MobEffectInstance> iterator = this.activeEffects.values().iterator();

         boolean flag;
         for(flag = false; iterator.hasNext(); flag = true) {
            this.onEffectRemoved(iterator.next());
            iterator.remove();
         }

         return flag;
      }
   }

   public Collection<MobEffectInstance> getActiveEffects() {
      return this.activeEffects.values();
   }

   public Map<MobEffect, MobEffectInstance> getActiveEffectsMap() {
      return this.activeEffects;
   }

   public boolean hasEffect(MobEffect pEffect) {
      return this.activeEffects.containsKey(pEffect);
   }

   @Nullable
   public MobEffectInstance getEffect(MobEffect pEffect) {
      return this.activeEffects.get(pEffect);
   }

   public final boolean addEffect(MobEffectInstance pEffectInstance) {
      return this.addEffect(pEffectInstance, (Entity)null);
   }

   public boolean addEffect(MobEffectInstance pEffectInstance, @Nullable Entity pEntity) {
      if (!this.canBeAffected(pEffectInstance)) {
         return false;
      } else {
         MobEffectInstance mobeffectinstance = this.activeEffects.get(pEffectInstance.getEffect());
         boolean flag = false;
         if (mobeffectinstance == null) {
            this.activeEffects.put(pEffectInstance.getEffect(), pEffectInstance);
            this.onEffectAdded(pEffectInstance, pEntity);
            flag = true;
         } else if (mobeffectinstance.update(pEffectInstance)) {
            this.onEffectUpdated(mobeffectinstance, true, pEntity);
            flag = true;
         }

         pEffectInstance.onEffectStarted(this);
         return flag;
      }
   }

   public boolean canBeAffected(MobEffectInstance pEffectInstance) {
      if (this.getMobType() == MobType.UNDEAD) {
         MobEffect mobeffect = pEffectInstance.getEffect();
         if (mobeffect == MobEffects.REGENERATION || mobeffect == MobEffects.POISON) {
            return false;
         }
      }

      return true;
   }

   public void forceAddEffect(MobEffectInstance pInstance, @Nullable Entity pEntity) {
      if (this.canBeAffected(pInstance)) {
         MobEffectInstance mobeffectinstance = this.activeEffects.put(pInstance.getEffect(), pInstance);
         if (mobeffectinstance == null) {
            this.onEffectAdded(pInstance, pEntity);
         } else {
            this.onEffectUpdated(pInstance, true, pEntity);
         }

      }
   }

   public boolean isInvertedHealAndHarm() {
      return this.getMobType() == MobType.UNDEAD;
   }

   @Nullable
   public MobEffectInstance removeEffectNoUpdate(@Nullable MobEffect pEffect) {
      return this.activeEffects.remove(pEffect);
   }

   public boolean removeEffect(MobEffect pEffect) {
      MobEffectInstance mobeffectinstance = this.removeEffectNoUpdate(pEffect);
      if (mobeffectinstance != null) {
         this.onEffectRemoved(mobeffectinstance);
         return true;
      } else {
         return false;
      }
   }

   protected void onEffectAdded(MobEffectInstance pEffectInstance, @Nullable Entity pEntity) {
      this.effectsDirty = true;
      if (!this.level().isClientSide) {
         pEffectInstance.getEffect().addAttributeModifiers(this.getAttributes(), pEffectInstance.getAmplifier());
         this.sendEffectToPassengers(pEffectInstance);
      }

   }

   public void sendEffectToPassengers(MobEffectInstance pEffectInstance) {
      for(Entity entity : this.getPassengers()) {
         if (entity instanceof ServerPlayer serverplayer) {
            serverplayer.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), pEffectInstance));
         }
      }

   }

   protected void onEffectUpdated(MobEffectInstance pEffectInstance, boolean pForced, @Nullable Entity pEntity) {
      this.effectsDirty = true;
      if (pForced && !this.level().isClientSide) {
         MobEffect mobeffect = pEffectInstance.getEffect();
         mobeffect.removeAttributeModifiers(this.getAttributes());
         mobeffect.addAttributeModifiers(this.getAttributes(), pEffectInstance.getAmplifier());
         this.refreshDirtyAttributes();
      }

      if (!this.level().isClientSide) {
         this.sendEffectToPassengers(pEffectInstance);
      }

   }

   protected void onEffectRemoved(MobEffectInstance pEffectInstance) {
      this.effectsDirty = true;
      if (!this.level().isClientSide) {
         pEffectInstance.getEffect().removeAttributeModifiers(this.getAttributes());
         this.refreshDirtyAttributes();

         for(Entity entity : this.getPassengers()) {
            if (entity instanceof ServerPlayer) {
               ServerPlayer serverplayer = (ServerPlayer)entity;
               serverplayer.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), pEffectInstance.getEffect()));
            }
         }
      }

   }

   private void refreshDirtyAttributes() {
      for(AttributeInstance attributeinstance : this.getAttributes().getDirtyAttributes()) {
         this.onAttributeUpdated(attributeinstance.getAttribute());
      }

   }

   private void onAttributeUpdated(Attribute pAttribute) {
      if (pAttribute == Attributes.MAX_HEALTH) {
         float f = this.getMaxHealth();
         if (this.getHealth() > f) {
            this.setHealth(f);
         }
      } else if (pAttribute == Attributes.MAX_ABSORPTION) {
         float f1 = this.getMaxAbsorption();
         if (this.getAbsorptionAmount() > f1) {
            this.setAbsorptionAmount(f1);
         }
      }

   }

   public void heal(float pHealAmount) {
      float f = this.getHealth();
      if (f > 0.0F) {
         this.setHealth(f + pHealAmount);
      }

   }

   public float getHealth() {
      return this.entityData.get(DATA_HEALTH_ID);
   }

   public void setHealth(float pHealth) {
      this.entityData.set(DATA_HEALTH_ID, Mth.clamp(pHealth, 0.0F, this.getMaxHealth()));
   }

   public boolean isDeadOrDying() {
      return this.getHealth() <= 0.0F;
   }

   public boolean hurt(DamageSource pSource, float pAmount) {
      if (this.isInvulnerableTo(pSource)) {
         return false;
      } else if (this.level().isClientSide) {
         return false;
      } else if (this.isDeadOrDying()) {
         return false;
      } else if (pSource.is(DamageTypeTags.IS_FIRE) && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
         return false;
      } else {
         if (this.isSleeping() && !this.level().isClientSide) {
            this.stopSleeping();
         }

         this.noActionTime = 0;
         float f = pAmount;
         boolean flag = false;
         float f1 = 0.0F;
         if (pAmount > 0.0F && this.isDamageSourceBlocked(pSource)) {
            this.hurtCurrentlyUsedShield(pAmount);
            f1 = pAmount;
            pAmount = 0.0F;
            if (!pSource.is(DamageTypeTags.IS_PROJECTILE)) {
               Entity entity = pSource.getDirectEntity();
               if (entity instanceof LivingEntity) {
                  LivingEntity livingentity = (LivingEntity)entity;
                  this.blockUsingShield(livingentity);
               }
            }

            flag = true;
         }

         if (pSource.is(DamageTypeTags.IS_FREEZING) && this.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
            pAmount *= 5.0F;
         }

         this.walkAnimation.setSpeed(1.5F);
         boolean flag1 = true;
         if ((float)this.invulnerableTime > 10.0F && !pSource.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            if (pAmount <= this.lastHurt) {
               return false;
            }

            this.actuallyHurt(pSource, pAmount - this.lastHurt);
            this.lastHurt = pAmount;
            flag1 = false;
         } else {
            this.lastHurt = pAmount;
            this.invulnerableTime = 20;
            this.actuallyHurt(pSource, pAmount);
            this.hurtDuration = 10;
            this.hurtTime = this.hurtDuration;
         }

         if (pSource.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            this.hurtHelmet(pSource, pAmount);
            pAmount *= 0.75F;
         }

         Entity entity1 = pSource.getEntity();
         if (entity1 != null) {
            if (entity1 instanceof LivingEntity) {
               LivingEntity livingentity1 = (LivingEntity)entity1;
               if (!pSource.is(DamageTypeTags.NO_ANGER)) {
                  this.setLastHurtByMob(livingentity1);
               }
            }

            if (entity1 instanceof Player) {
               Player player1 = (Player)entity1;
               this.lastHurtByPlayerTime = 100;
               this.lastHurtByPlayer = player1;
            } else if (entity1 instanceof Wolf) {
               Wolf wolf = (Wolf)entity1;
               if (wolf.isTame()) {
                  this.lastHurtByPlayerTime = 100;
                  LivingEntity livingentity2 = wolf.getOwner();
                  if (livingentity2 instanceof Player) {
                     Player player = (Player)livingentity2;
                     this.lastHurtByPlayer = player;
                  } else {
                     this.lastHurtByPlayer = null;
                  }
               }
            }
         }

         if (flag1) {
            if (flag) {
               this.level().broadcastEntityEvent(this, (byte)29);
            } else {
               this.level().broadcastDamageEvent(this, pSource);
            }

            if (!pSource.is(DamageTypeTags.NO_IMPACT) && (!flag || pAmount > 0.0F)) {
               this.markHurt();
            }

            if (entity1 != null && !pSource.is(DamageTypeTags.NO_KNOCKBACK)) {
               double d0 = entity1.getX() - this.getX();

               double d1;
               for(d1 = entity1.getZ() - this.getZ(); d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
                  d0 = (Math.random() - Math.random()) * 0.01D;
               }

               this.knockback((double)0.4F, d0, d1);
               if (!flag) {
                  this.indicateDamage(d0, d1);
               }
            }
         }

         if (this.isDeadOrDying()) {
            if (!this.checkTotemDeathProtection(pSource)) {
               SoundEvent soundevent = this.getDeathSound();
               if (flag1 && soundevent != null) {
                  this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
               }

               this.die(pSource);
            }
         } else if (flag1) {
            this.playHurtSound(pSource);
         }

         boolean flag2 = !flag || pAmount > 0.0F;
         if (flag2) {
            this.lastDamageSource = pSource;
            this.lastDamageStamp = this.level().getGameTime();
         }

         if (this instanceof ServerPlayer) {
            CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((ServerPlayer)this, pSource, f, pAmount, flag);
            if (f1 > 0.0F && f1 < 3.4028235E37F) {
               ((ServerPlayer)this).awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(f1 * 10.0F));
            }
         }

         if (entity1 instanceof ServerPlayer) {
            CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((ServerPlayer)entity1, this, pSource, f, pAmount, flag);
         }

         return flag2;
      }
   }

   protected void blockUsingShield(LivingEntity pAttacker) {
      pAttacker.blockedByShield(this);
   }

   protected void blockedByShield(LivingEntity pDefender) {
      pDefender.knockback(0.5D, pDefender.getX() - this.getX(), pDefender.getZ() - this.getZ());
   }

   private boolean checkTotemDeathProtection(DamageSource pDamageSource) {
      if (pDamageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
         return false;
      } else {
         ItemStack itemstack = null;

         for(InteractionHand interactionhand : InteractionHand.values()) {
            ItemStack itemstack1 = this.getItemInHand(interactionhand);
            if (itemstack1.is(Items.TOTEM_OF_UNDYING)) {
               itemstack = itemstack1.copy();
               itemstack1.shrink(1);
               break;
            }
         }

         if (itemstack != null) {
            if (this instanceof ServerPlayer) {
               ServerPlayer serverplayer = (ServerPlayer)this;
               serverplayer.awardStat(Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
               CriteriaTriggers.USED_TOTEM.trigger(serverplayer, itemstack);
               this.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            }

            this.setHealth(1.0F);
            this.removeAllEffects();
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
            this.level().broadcastEntityEvent(this, (byte)35);
         }

         return itemstack != null;
      }
   }

   @Nullable
   public DamageSource getLastDamageSource() {
      if (this.level().getGameTime() - this.lastDamageStamp > 40L) {
         this.lastDamageSource = null;
      }

      return this.lastDamageSource;
   }

   protected void playHurtSound(DamageSource pSource) {
      SoundEvent soundevent = this.getHurtSound(pSource);
      if (soundevent != null) {
         this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
      }

   }

   public boolean isDamageSourceBlocked(DamageSource pDamageSource) {
      Entity entity = pDamageSource.getDirectEntity();
      boolean flag = false;
      if (entity instanceof AbstractArrow abstractarrow) {
         if (abstractarrow.getPierceLevel() > 0) {
            flag = true;
         }
      }

      if (!pDamageSource.is(DamageTypeTags.BYPASSES_SHIELD) && this.isBlocking() && !flag) {
         Vec3 vec32 = pDamageSource.getSourcePosition();
         if (vec32 != null) {
            Vec3 vec3 = this.calculateViewVector(0.0F, this.getYHeadRot());
            Vec3 vec31 = vec32.vectorTo(this.position());
            vec31 = (new Vec3(vec31.x, 0.0D, vec31.z)).normalize();
            return vec31.dot(vec3) < 0.0D;
         }
      }

      return false;
   }

   private void breakItem(ItemStack pStack) {
      if (!pStack.isEmpty()) {
         if (!this.isSilent()) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_BREAK, this.getSoundSource(), 0.8F, 0.8F + this.level().random.nextFloat() * 0.4F, false);
         }

         this.spawnItemParticles(pStack, 5);
      }

   }

   public void die(DamageSource pDamageSource) {
      if (!this.isRemoved() && !this.dead) {
         Entity entity = pDamageSource.getEntity();
         LivingEntity livingentity = this.getKillCredit();
         if (this.deathScore >= 0 && livingentity != null) {
            livingentity.awardKillScore(this, this.deathScore, pDamageSource);
         }

         if (this.isSleeping()) {
            this.stopSleeping();
         }

         if (!this.level().isClientSide && this.hasCustomName()) {
            LOGGER.info("Named entity {} died: {}", this, this.getCombatTracker().getDeathMessage().getString());
         }

         this.dead = true;
         this.getCombatTracker().recheckStatus();
         Level level = this.level();
         if (level instanceof ServerLevel) {
            ServerLevel serverlevel = (ServerLevel)level;
            if (entity == null || entity.killedEntity(serverlevel, this)) {
               this.gameEvent(GameEvent.ENTITY_DIE);
               this.dropAllDeathLoot(pDamageSource);
               this.createWitherRose(livingentity);
            }

            this.level().broadcastEntityEvent(this, (byte)3);
         }

         this.setPose(Pose.DYING);
      }
   }

   protected void createWitherRose(@Nullable LivingEntity pEntitySource) {
      if (!this.level().isClientSide) {
         boolean flag = false;
         if (pEntitySource instanceof WitherBoss) {
            if (this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
               BlockPos blockpos = this.blockPosition();
               BlockState blockstate = Blocks.WITHER_ROSE.defaultBlockState();
               if (this.level().getBlockState(blockpos).isAir() && blockstate.canSurvive(this.level(), blockpos)) {
                  this.level().setBlock(blockpos, blockstate, 3);
                  flag = true;
               }
            }

            if (!flag) {
               ItemEntity itementity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), new ItemStack(Items.WITHER_ROSE));
               this.level().addFreshEntity(itementity);
            }
         }

      }
   }

   protected void dropAllDeathLoot(DamageSource pDamageSource) {
      Entity entity = pDamageSource.getEntity();
      int i;
      if (entity instanceof Player) {
         i = EnchantmentHelper.getMobLooting((LivingEntity)entity);
      } else {
         i = 0;
      }

      boolean flag = this.lastHurtByPlayerTime > 0;
      if (this.shouldDropLoot() && this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
         this.dropFromLootTable(pDamageSource, flag);
         this.dropCustomDeathLoot(pDamageSource, i, flag);
      }

      this.dropEquipment();
      this.dropExperience();
   }

   protected void dropEquipment() {
   }

   protected void dropExperience() {
      if (this.level() instanceof ServerLevel && !this.wasExperienceConsumed() && (this.isAlwaysExperienceDropper() || this.lastHurtByPlayerTime > 0 && this.shouldDropExperience() && this.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))) {
         ExperienceOrb.award((ServerLevel)this.level(), this.position(), this.getExperienceReward());
      }

   }

   protected void dropCustomDeathLoot(DamageSource pDamageSource, int pLooting, boolean pHitByPlayer) {
   }

   public ResourceLocation getLootTable() {
      return this.getType().getDefaultLootTable();
   }

   public long getLootTableSeed() {
      return 0L;
   }

   protected void dropFromLootTable(DamageSource pDamageSource, boolean pHitByPlayer) {
      ResourceLocation resourcelocation = this.getLootTable();
      LootTable loottable = this.level().getServer().getLootData().getLootTable(resourcelocation);
      LootParams.Builder lootparams$builder = (new LootParams.Builder((ServerLevel)this.level())).withParameter(LootContextParams.THIS_ENTITY, this).withParameter(LootContextParams.ORIGIN, this.position()).withParameter(LootContextParams.DAMAGE_SOURCE, pDamageSource).withOptionalParameter(LootContextParams.KILLER_ENTITY, pDamageSource.getEntity()).withOptionalParameter(LootContextParams.DIRECT_KILLER_ENTITY, pDamageSource.getDirectEntity());
      if (pHitByPlayer && this.lastHurtByPlayer != null) {
         lootparams$builder = lootparams$builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, this.lastHurtByPlayer).withLuck(this.lastHurtByPlayer.getLuck());
      }

      LootParams lootparams = lootparams$builder.create(LootContextParamSets.ENTITY);
      loottable.getRandomItems(lootparams, this.getLootTableSeed(), this::spawnAtLocation);
   }

   public void knockback(double pStrength, double pX, double pZ) {
      pStrength *= 1.0D - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      if (!(pStrength <= 0.0D)) {
         this.hasImpulse = true;
         Vec3 vec3 = this.getDeltaMovement();
         Vec3 vec31 = (new Vec3(pX, 0.0D, pZ)).normalize().scale(pStrength);
         this.setDeltaMovement(vec3.x / 2.0D - vec31.x, this.onGround() ? Math.min(0.4D, vec3.y / 2.0D + pStrength) : vec3.y, vec3.z / 2.0D - vec31.z);
      }
   }

   public void indicateDamage(double pXDistance, double pZDistance) {
   }

   @Nullable
   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.GENERIC_HURT;
   }

   @Nullable
   protected SoundEvent getDeathSound() {
      return SoundEvents.GENERIC_DEATH;
   }

   private SoundEvent getFallDamageSound(int pHeight) {
      return pHeight > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
   }

   public void skipDropExperience() {
      this.skipDropExperience = true;
   }

   public boolean wasExperienceConsumed() {
      return this.skipDropExperience;
   }

   public float getHurtDir() {
      return 0.0F;
   }

   protected AABB getHitbox() {
      AABB aabb = this.getBoundingBox();
      Entity entity = this.getVehicle();
      if (entity != null) {
         Vec3 vec3 = entity.getPassengerRidingPosition(this);
         return aabb.setMinY(Math.max(vec3.y, aabb.minY));
      } else {
         return aabb;
      }
   }

   public LivingEntity.Fallsounds getFallSounds() {
      return new LivingEntity.Fallsounds(SoundEvents.GENERIC_SMALL_FALL, SoundEvents.GENERIC_BIG_FALL);
   }

   protected SoundEvent getDrinkingSound(ItemStack pStack) {
      return pStack.getDrinkingSound();
   }

   public SoundEvent getEatingSound(ItemStack pStack) {
      return pStack.getEatingSound();
   }

   public Optional<BlockPos> getLastClimbablePos() {
      return this.lastClimbablePos;
   }

   public boolean onClimbable() {
      if (this.isSpectator()) {
         return false;
      } else {
         BlockPos blockpos = this.blockPosition();
         BlockState blockstate = this.getFeetBlockState();
         if (blockstate.is(BlockTags.CLIMBABLE)) {
            this.lastClimbablePos = Optional.of(blockpos);
            return true;
         } else if (blockstate.getBlock() instanceof TrapDoorBlock && this.trapdoorUsableAsLadder(blockpos, blockstate)) {
            this.lastClimbablePos = Optional.of(blockpos);
            return true;
         } else {
            return false;
         }
      }
   }

   private boolean trapdoorUsableAsLadder(BlockPos pPos, BlockState pState) {
      if (pState.getValue(TrapDoorBlock.OPEN)) {
         BlockState blockstate = this.level().getBlockState(pPos.below());
         if (blockstate.is(Blocks.LADDER) && blockstate.getValue(LadderBlock.FACING) == pState.getValue(TrapDoorBlock.FACING)) {
            return true;
         }
      }

      return false;
   }

   public boolean isAlive() {
      return !this.isRemoved() && this.getHealth() > 0.0F;
   }

   public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
      boolean flag = super.causeFallDamage(pFallDistance, pMultiplier, pSource);
      int i = this.calculateFallDamage(pFallDistance, pMultiplier);
      if (i > 0) {
         this.playSound(this.getFallDamageSound(i), 1.0F, 1.0F);
         this.playBlockFallSound();
         this.hurt(pSource, (float)i);
         return true;
      } else {
         return flag;
      }
   }

   protected int calculateFallDamage(float pFallDistance, float pDamageMultiplier) {
      if (this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
         return 0;
      } else {
         MobEffectInstance mobeffectinstance = this.getEffect(MobEffects.JUMP);
         float f = mobeffectinstance == null ? 0.0F : (float)(mobeffectinstance.getAmplifier() + 1);
         return Mth.ceil((pFallDistance - 3.0F - f) * pDamageMultiplier);
      }
   }

   protected void playBlockFallSound() {
      if (!this.isSilent()) {
         int i = Mth.floor(this.getX());
         int j = Mth.floor(this.getY() - (double)0.2F);
         int k = Mth.floor(this.getZ());
         BlockState blockstate = this.level().getBlockState(new BlockPos(i, j, k));
         if (!blockstate.isAir()) {
            SoundType soundtype = blockstate.getSoundType();
            this.playSound(soundtype.getFallSound(), soundtype.getVolume() * 0.5F, soundtype.getPitch() * 0.75F);
         }

      }
   }

   public void animateHurt(float pYaw) {
      this.hurtDuration = 10;
      this.hurtTime = this.hurtDuration;
   }

   public int getArmorValue() {
      return Mth.floor(this.getAttributeValue(Attributes.ARMOR));
   }

   protected void hurtArmor(DamageSource pDamageSource, float pDamageAmount) {
   }

   protected void hurtHelmet(DamageSource pDamageSource, float pDamageAmount) {
   }

   protected void hurtCurrentlyUsedShield(float pDamageAmount) {
   }

   protected float getDamageAfterArmorAbsorb(DamageSource pDamageSource, float pDamageAmount) {
      if (!pDamageSource.is(DamageTypeTags.BYPASSES_ARMOR)) {
         this.hurtArmor(pDamageSource, pDamageAmount);
         pDamageAmount = CombatRules.getDamageAfterAbsorb(pDamageAmount, (float)this.getArmorValue(), (float)this.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
      }

      return pDamageAmount;
   }

   protected float getDamageAfterMagicAbsorb(DamageSource pDamageSource, float pDamageAmount) {
      if (pDamageSource.is(DamageTypeTags.BYPASSES_EFFECTS)) {
         return pDamageAmount;
      } else {
         if (this.hasEffect(MobEffects.DAMAGE_RESISTANCE) && !pDamageSource.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
            int i = (this.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
            int j = 25 - i;
            float f = pDamageAmount * (float)j;
            float f1 = pDamageAmount;
            pDamageAmount = Math.max(f / 25.0F, 0.0F);
            float f2 = f1 - pDamageAmount;
            if (f2 > 0.0F && f2 < 3.4028235E37F) {
               if (this instanceof ServerPlayer) {
                  ((ServerPlayer)this).awardStat(Stats.DAMAGE_RESISTED, Math.round(f2 * 10.0F));
               } else if (pDamageSource.getEntity() instanceof ServerPlayer) {
                  ((ServerPlayer)pDamageSource.getEntity()).awardStat(Stats.DAMAGE_DEALT_RESISTED, Math.round(f2 * 10.0F));
               }
            }
         }

         if (pDamageAmount <= 0.0F) {
            return 0.0F;
         } else if (pDamageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            return pDamageAmount;
         } else {
            int k = EnchantmentHelper.getDamageProtection(this.getArmorSlots(), pDamageSource);
            if (k > 0) {
               pDamageAmount = CombatRules.getDamageAfterMagicAbsorb(pDamageAmount, (float)k);
            }

            return pDamageAmount;
         }
      }
   }

   protected void actuallyHurt(DamageSource pDamageSource, float pDamageAmount) {
      if (!this.isInvulnerableTo(pDamageSource)) {
         pDamageAmount = this.getDamageAfterArmorAbsorb(pDamageSource, pDamageAmount);
         pDamageAmount = this.getDamageAfterMagicAbsorb(pDamageSource, pDamageAmount);
         float f1 = Math.max(pDamageAmount - this.getAbsorptionAmount(), 0.0F);
         this.setAbsorptionAmount(this.getAbsorptionAmount() - (pDamageAmount - f1));
         float f = pDamageAmount - f1;
         if (f > 0.0F && f < 3.4028235E37F) {
            Entity entity = pDamageSource.getEntity();
            if (entity instanceof ServerPlayer) {
               ServerPlayer serverplayer = (ServerPlayer)entity;
               serverplayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(f * 10.0F));
            }
         }

         if (f1 != 0.0F) {
            this.getCombatTracker().recordDamage(pDamageSource, f1);
            this.setHealth(this.getHealth() - f1);
            this.setAbsorptionAmount(this.getAbsorptionAmount() - f1);
            this.gameEvent(GameEvent.ENTITY_DAMAGE);
         }
      }
   }

   public CombatTracker getCombatTracker() {
      return this.combatTracker;
   }

   @Nullable
   public LivingEntity getKillCredit() {
      if (this.lastHurtByPlayer != null) {
         return this.lastHurtByPlayer;
      } else {
         return this.lastHurtByMob != null ? this.lastHurtByMob : null;
      }
   }

   public final float getMaxHealth() {
      return (float)this.getAttributeValue(Attributes.MAX_HEALTH);
   }

   public final float getMaxAbsorption() {
      return (float)this.getAttributeValue(Attributes.MAX_ABSORPTION);
   }

   public final int getArrowCount() {
      return this.entityData.get(DATA_ARROW_COUNT_ID);
   }

   public final void setArrowCount(int pCount) {
      this.entityData.set(DATA_ARROW_COUNT_ID, pCount);
   }

   public final int getStingerCount() {
      return this.entityData.get(DATA_STINGER_COUNT_ID);
   }

   public final void setStingerCount(int pStingerCount) {
      this.entityData.set(DATA_STINGER_COUNT_ID, pStingerCount);
   }

   private int getCurrentSwingDuration() {
      if (MobEffectUtil.hasDigSpeed(this)) {
         return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(this));
      } else {
         return this.hasEffect(MobEffects.DIG_SLOWDOWN) ? 6 + (1 + this.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) * 2 : 6;
      }
   }

   public void swing(InteractionHand pHand) {
      this.swing(pHand, false);
   }

   public void swing(InteractionHand pHand, boolean pUpdateSelf) {
      if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
         this.swingTime = -1;
         this.swinging = true;
         this.swingingArm = pHand;
         if (this.level() instanceof ServerLevel) {
            ClientboundAnimatePacket clientboundanimatepacket = new ClientboundAnimatePacket(this, pHand == InteractionHand.MAIN_HAND ? 0 : 3);
            ServerChunkCache serverchunkcache = ((ServerLevel)this.level()).getChunkSource();
            if (pUpdateSelf) {
               serverchunkcache.broadcastAndSend(this, clientboundanimatepacket);
            } else {
               serverchunkcache.broadcast(this, clientboundanimatepacket);
            }
         }
      }

   }

   public void handleDamageEvent(DamageSource pDamageSource) {
      this.walkAnimation.setSpeed(1.5F);
      this.invulnerableTime = 20;
      this.hurtDuration = 10;
      this.hurtTime = this.hurtDuration;
      SoundEvent soundevent = this.getHurtSound(pDamageSource);
      if (soundevent != null) {
         this.playSound(soundevent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
      }

      this.hurt(this.damageSources().generic(), 0.0F);
      this.lastDamageSource = pDamageSource;
      this.lastDamageStamp = this.level().getGameTime();
   }

   public void handleEntityEvent(byte pId) {
      switch (pId) {
         case 3:
            SoundEvent soundevent = this.getDeathSound();
            if (soundevent != null) {
               this.playSound(soundevent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            }

            if (!(this instanceof Player)) {
               this.setHealth(0.0F);
               this.die(this.damageSources().generic());
            }
            break;
         case 29:
            this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F + this.level().random.nextFloat() * 0.4F);
            break;
         case 30:
            this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level().random.nextFloat() * 0.4F);
            break;
         case 46:
            int i = 128;

            for(int j = 0; j < 128; ++j) {
               double d0 = (double)j / 127.0D;
               float f = (this.random.nextFloat() - 0.5F) * 0.2F;
               float f1 = (this.random.nextFloat() - 0.5F) * 0.2F;
               float f2 = (this.random.nextFloat() - 0.5F) * 0.2F;
               double d1 = Mth.lerp(d0, this.xo, this.getX()) + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth() * 2.0D;
               double d2 = Mth.lerp(d0, this.yo, this.getY()) + this.random.nextDouble() * (double)this.getBbHeight();
               double d3 = Mth.lerp(d0, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5D) * (double)this.getBbWidth() * 2.0D;
               this.level().addParticle(ParticleTypes.PORTAL, d1, d2, d3, (double)f, (double)f1, (double)f2);
            }
            break;
         case 47:
            this.breakItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
            break;
         case 48:
            this.breakItem(this.getItemBySlot(EquipmentSlot.OFFHAND));
            break;
         case 49:
            this.breakItem(this.getItemBySlot(EquipmentSlot.HEAD));
            break;
         case 50:
            this.breakItem(this.getItemBySlot(EquipmentSlot.CHEST));
            break;
         case 51:
            this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
            break;
         case 52:
            this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
            break;
         case 54:
            HoneyBlock.showJumpParticles(this);
            break;
         case 55:
            this.swapHandItems();
            break;
         case 60:
            this.makePoofParticles();
            break;
         default:
            super.handleEntityEvent(pId);
      }

   }

   private void makePoofParticles() {
      for(int i = 0; i < 20; ++i) {
         double d0 = this.random.nextGaussian() * 0.02D;
         double d1 = this.random.nextGaussian() * 0.02D;
         double d2 = this.random.nextGaussian() * 0.02D;
         this.level().addParticle(ParticleTypes.POOF, this.getRandomX(1.0D), this.getRandomY(), this.getRandomZ(1.0D), d0, d1, d2);
      }

   }

   private void swapHandItems() {
      ItemStack itemstack = this.getItemBySlot(EquipmentSlot.OFFHAND);
      this.setItemSlot(EquipmentSlot.OFFHAND, this.getItemBySlot(EquipmentSlot.MAINHAND));
      this.setItemSlot(EquipmentSlot.MAINHAND, itemstack);
   }

   protected void onBelowWorld() {
      this.hurt(this.damageSources().fellOutOfWorld(), 4.0F);
   }

   protected void updateSwingTime() {
      int i = this.getCurrentSwingDuration();
      if (this.swinging) {
         ++this.swingTime;
         if (this.swingTime >= i) {
            this.swingTime = 0;
            this.swinging = false;
         }
      } else {
         this.swingTime = 0;
      }

      this.attackAnim = (float)this.swingTime / (float)i;
   }

   @Nullable
   public AttributeInstance getAttribute(Attribute pAttribute) {
      return this.getAttributes().getInstance(pAttribute);
   }

   public double getAttributeValue(Holder<Attribute> pAttribute) {
      return this.getAttributeValue(pAttribute.value());
   }

   public double getAttributeValue(Attribute pAttribute) {
      return this.getAttributes().getValue(pAttribute);
   }

   public double getAttributeBaseValue(Holder<Attribute> pAttribute) {
      return this.getAttributeBaseValue(pAttribute.value());
   }

   public double getAttributeBaseValue(Attribute pAttribute) {
      return this.getAttributes().getBaseValue(pAttribute);
   }

   public AttributeMap getAttributes() {
      return this.attributes;
   }

   public MobType getMobType() {
      return MobType.UNDEFINED;
   }

   public ItemStack getMainHandItem() {
      return this.getItemBySlot(EquipmentSlot.MAINHAND);
   }

   public ItemStack getOffhandItem() {
      return this.getItemBySlot(EquipmentSlot.OFFHAND);
   }

   public boolean isHolding(Item pItem) {
      return this.isHolding((p_147200_) -> {
         return p_147200_.is(pItem);
      });
   }

   public boolean isHolding(Predicate<ItemStack> pPredicate) {
      return pPredicate.test(this.getMainHandItem()) || pPredicate.test(this.getOffhandItem());
   }

   public ItemStack getItemInHand(InteractionHand pHand) {
      if (pHand == InteractionHand.MAIN_HAND) {
         return this.getItemBySlot(EquipmentSlot.MAINHAND);
      } else if (pHand == InteractionHand.OFF_HAND) {
         return this.getItemBySlot(EquipmentSlot.OFFHAND);
      } else {
         throw new IllegalArgumentException("Invalid hand " + pHand);
      }
   }

   public void setItemInHand(InteractionHand pHand, ItemStack pStack) {
      if (pHand == InteractionHand.MAIN_HAND) {
         this.setItemSlot(EquipmentSlot.MAINHAND, pStack);
      } else {
         if (pHand != InteractionHand.OFF_HAND) {
            throw new IllegalArgumentException("Invalid hand " + pHand);
         }

         this.setItemSlot(EquipmentSlot.OFFHAND, pStack);
      }

   }

   public boolean hasItemInSlot(EquipmentSlot pSlot) {
      return !this.getItemBySlot(pSlot).isEmpty();
   }

   public abstract Iterable<ItemStack> getArmorSlots();

   public abstract ItemStack getItemBySlot(EquipmentSlot pSlot);

   public abstract void setItemSlot(EquipmentSlot pSlot, ItemStack pStack);

   protected void verifyEquippedItem(ItemStack pStack) {
      CompoundTag compoundtag = pStack.getTag();
      if (compoundtag != null) {
         pStack.getItem().verifyTagAfterLoad(compoundtag);
      }

   }

   public float getArmorCoverPercentage() {
      Iterable<ItemStack> iterable = this.getArmorSlots();
      int i = 0;
      int j = 0;

      for(ItemStack itemstack : iterable) {
         if (!itemstack.isEmpty()) {
            ++j;
         }

         ++i;
      }

      return i > 0 ? (float)j / (float)i : 0.0F;
   }

   public void setSprinting(boolean pSprinting) {
      super.setSprinting(pSprinting);
      AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
      attributeinstance.removeModifier(SPEED_MODIFIER_SPRINTING.getId());
      if (pSprinting) {
         attributeinstance.addTransientModifier(SPEED_MODIFIER_SPRINTING);
      }

   }

   protected float getSoundVolume() {
      return 1.0F;
   }

   public float getVoicePitch() {
      return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
   }

   protected boolean isImmobile() {
      return this.isDeadOrDying();
   }

   public void push(Entity pEntity) {
      if (!this.isSleeping()) {
         super.push(pEntity);
      }

   }

   private void dismountVehicle(Entity pVehicle) {
      Vec3 vec3;
      if (this.isRemoved()) {
         vec3 = this.position();
      } else if (!pVehicle.isRemoved() && !this.level().getBlockState(pVehicle.blockPosition()).is(BlockTags.PORTALS)) {
         vec3 = pVehicle.getDismountLocationForPassenger(this);
      } else {
         double d0 = Math.max(this.getY(), pVehicle.getY());
         vec3 = new Vec3(this.getX(), d0, this.getZ());
      }

      this.dismountTo(vec3.x, vec3.y, vec3.z);
   }

   public boolean shouldShowName() {
      return this.isCustomNameVisible();
   }

   protected float getJumpPower() {
      return 0.42F * this.getBlockJumpFactor() + this.getJumpBoostPower();
   }

   public float getJumpBoostPower() {
      return this.hasEffect(MobEffects.JUMP) ? 0.1F * ((float)this.getEffect(MobEffects.JUMP).getAmplifier() + 1.0F) : 0.0F;
   }

   protected void jumpFromGround() {
      this.getBaritone().ifPresent(baritone -> {
         this.jumpRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.JUMP, (this instanceof LocalPlayer && BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this) == null) ? this.jumpRotationEvent.getYaw(): getYRot(), this.getXRot());
         baritone.getGameEventHandler().onPlayerRotationMove(this.jumpRotationEvent);
      });
      Vec3 vec3 = this.getDeltaMovement();
      this.setDeltaMovement(vec3.x, (double)this.getJumpPower(), vec3.z);
      if (this.isSprinting()) {
         float f = this.getYRot() * ((float)Math.PI / 180F);
         //float f = this.overrideYaw(this) * ((float)Math.PI / 180F);
         this.setDeltaMovement(this.getDeltaMovement().add((double)(-Mth.sin(f) * 0.2F), 0.0D, (double)(Mth.cos(f) * 0.2F)));
      }

      this.hasImpulse = true;
   }
   private Optional<IBaritone> getBaritone() {
      // noinspection ConstantConditions
      if (LocalPlayer.class.isInstance(this)) {
         return Optional.ofNullable(BaritoneAPI.getProvider().getBaritoneForPlayer((LocalPlayer) (Object) this));
      } else {
         return Optional.empty();
      }
   }

   protected void goDownInWater() {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0D, (double)-0.04F, 0.0D));
   }

   protected void jumpInLiquid(TagKey<Fluid> pFluidTag) {
      this.setDeltaMovement(this.getDeltaMovement().add(0.0D, (double)0.04F, 0.0D));
   }

   protected float getWaterSlowDown() {
      return 0.8F;
   }

   public boolean canStandOnFluid(FluidState pFluidState) {
      return false;
   }

   public void travel(Vec3 pTravelVector) {
      if (this.isControlledByLocalInstance()) {
         double d0 = 0.08D;
         boolean flag = this.getDeltaMovement().y <= 0.0D;
         if (flag && this.hasEffect(MobEffects.SLOW_FALLING)) {
            d0 = 0.01D;
         }

         FluidState fluidstate = this.level().getFluidState(this.blockPosition());
         if (this.isInWater() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidstate)) {
            double d9 = this.getY();
            float f4 = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
            float f5 = 0.02F;
            float f6 = (float)EnchantmentHelper.getDepthStrider(this);
            if (f6 > 3.0F) {
               f6 = 3.0F;
            }

            if (!this.onGround()) {
               f6 *= 0.5F;
            }

            if (f6 > 0.0F) {
               f4 += (0.54600006F - f4) * f6 / 3.0F;
               f5 += (this.getSpeed() - f5) * f6 / 3.0F;
            }

            if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
               f4 = 0.96F;
            }

            this.moveRelative(f5, pTravelVector);
            if (this.elytraRotationEvent != null) {
               this.setYRot(this.elytraRotationEvent.getOriginal().getYaw());
               this.setXRot(this.elytraRotationEvent.getOriginal().getPitch());
               this.elytraRotationEvent = null;
            }
            this.move(MoverType.SELF, this.getDeltaMovement());
            Vec3 vec36 = this.getDeltaMovement();
            if (this.horizontalCollision && this.onClimbable()) {
               vec36 = new Vec3(vec36.x, 0.2D, vec36.z);
            }

            this.setDeltaMovement(vec36.multiply((double)f4, (double)0.8F, (double)f4));
            Vec3 vec32 = this.getFluidFallingAdjustedMovement(d0, flag, this.getDeltaMovement());
            this.setDeltaMovement(vec32);
            if (this.horizontalCollision && this.isFree(vec32.x, vec32.y + (double)0.6F - this.getY() + d9, vec32.z)) {
               this.setDeltaMovement(vec32.x, (double)0.3F, vec32.z);
            }
         } else if (this.isInLava() && this.isAffectedByFluids() && !this.canStandOnFluid(fluidstate)) {
            double d8 = this.getY();
            this.moveRelative(0.02F, pTravelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.5D, (double)0.8F, 0.5D));
               Vec3 vec33 = this.getFluidFallingAdjustedMovement(d0, flag, this.getDeltaMovement());
               this.setDeltaMovement(vec33);
            } else {
               this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
            }

            if (!this.isNoGravity()) {
               this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -d0 / 4.0D, 0.0D));
            }

            Vec3 vec34 = this.getDeltaMovement();
            if (this.horizontalCollision && this.isFree(vec34.x, vec34.y + (double)0.6F - this.getY() + d8, vec34.z)) {
               this.setDeltaMovement(vec34.x, (double)0.3F, vec34.z);
            }
         } else if (this.isFallFlying()) {
            this.checkSlowFallDistance();
            Vec3 vec3 = this.getDeltaMovement();
            this.getBaritone().ifPresent(baritone -> {
               this.elytraRotationEvent = new RotationMoveEvent(RotationMoveEvent.Type.MOTION_UPDATE, this.getYRot(), this.getXRot());
               baritone.getGameEventHandler().onPlayerRotationMove(this.elytraRotationEvent);
               this.setYRot(this.elytraRotationEvent.getYaw());
               this.setXRot(this.elytraRotationEvent.getPitch());
            });
            Vec3 vec31 = this.getLookAngle();
            float f = this.getXRot() * ((float)Math.PI / 180F);
            double d1 = Math.sqrt(vec31.x * vec31.x + vec31.z * vec31.z);
            double d3 = vec3.horizontalDistance();
            double d4 = vec31.length();
            double d5 = Math.cos((double)f);
            d5 = d5 * d5 * Math.min(1.0D, d4 / 0.4D);
            vec3 = this.getDeltaMovement().add(0.0D, d0 * (-1.0D + d5 * 0.75D), 0.0D);
            if (vec3.y < 0.0D && d1 > 0.0D) {
               double d6 = vec3.y * -0.1D * d5;
               vec3 = vec3.add(vec31.x * d6 / d1, d6, vec31.z * d6 / d1);
            }

            if (f < 0.0F && d1 > 0.0D) {
               double d10 = d3 * (double)(-Mth.sin(f)) * 0.04D;
               vec3 = vec3.add(-vec31.x * d10 / d1, d10 * 3.2D, -vec31.z * d10 / d1);
            }

            if (d1 > 0.0D) {
               vec3 = vec3.add((vec31.x / d1 * d3 - vec3.x) * 0.1D, 0.0D, (vec31.z / d1 * d3 - vec3.z) * 0.1D);
            }

            this.setDeltaMovement(vec3.multiply((double)0.99F, (double)0.98F, (double)0.99F));
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (this.horizontalCollision && !this.level().isClientSide) {
               double d11 = this.getDeltaMovement().horizontalDistance();
               double d7 = d3 - d11;
               float f1 = (float)(d7 * 10.0D - 3.0D);
               if (f1 > 0.0F) {
                  this.playSound(this.getFallDamageSound((int)f1), 1.0F, 1.0F);
                  this.hurt(this.damageSources().flyIntoWall(), f1);
               }
            }

            if (this.onGround() && !this.level().isClientSide) {
               this.setSharedFlag(7, false);
            }
         } else {
            BlockPos blockpos = this.getBlockPosBelowThatAffectsMyMovement();
            float f2 = this.level().getBlockState(blockpos).getBlock().getFriction();
            float f3 = this.onGround() ? f2 * 0.91F : 0.91F;
            Vec3 vec35 = this.handleRelativeFrictionAndCalculateMovement(pTravelVector, f2);
            double d2 = vec35.y;
            if (this.hasEffect(MobEffects.LEVITATION)) {
               d2 += (0.05D * (double)(this.getEffect(MobEffects.LEVITATION).getAmplifier() + 1) - vec35.y) * 0.2D;
            } else if (this.level().isClientSide && !this.level().hasChunkAt(blockpos)) {
               if (this.getY() > (double)this.level().getMinBuildHeight()) {
                  d2 = -0.1D;
               } else {
                  d2 = 0.0D;
               }
            } else if (!this.isNoGravity()) {
               d2 -= d0;
            }

            if (this.shouldDiscardFriction()) {
               this.setDeltaMovement(vec35.x, d2, vec35.z);
            } else {
               this.setDeltaMovement(vec35.x * (double)f3, d2 * (double)0.98F, vec35.z * (double)f3);
            }
         }
      }

      this.calculateEntityAnimation(this instanceof FlyingAnimal);
   }

   private void travelRidden(Player pPlayer, Vec3 pTravelVector) {
      Vec3 vec3 = this.getRiddenInput(pPlayer, pTravelVector);
      this.tickRidden(pPlayer, vec3);
      if (this.isControlledByLocalInstance()) {
         this.setSpeed(this.getRiddenSpeed(pPlayer));
         this.travel(vec3);
      } else {
         this.calculateEntityAnimation(false);
         this.setDeltaMovement(Vec3.ZERO);
         this.tryCheckInsideBlocks();
      }

   }

   protected void tickRidden(Player pPlayer, Vec3 pTravelVector) {
   }

   protected Vec3 getRiddenInput(Player pPlayer, Vec3 pTravelVector) {
      return pTravelVector;
   }

   protected float getRiddenSpeed(Player pPlayer) {
      return this.getSpeed();
   }

   public void calculateEntityAnimation(boolean pIncludeHeight) {
      float f = (float)Mth.length(this.getX() - this.xo, pIncludeHeight ? this.getY() - this.yo : 0.0D, this.getZ() - this.zo);
      this.updateWalkAnimation(f);
   }

   protected void updateWalkAnimation(float pPartialTick) {
      float f = Math.min(pPartialTick * 4.0F, 1.0F);
      this.walkAnimation.update(f, 0.4F);
   }

   public Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 pDeltaMovement, float pFriction) {
      this.moveRelative(this.getFrictionInfluencedSpeed(pFriction), pDeltaMovement);
      this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
      this.move(MoverType.SELF, this.getDeltaMovement());
      Vec3 vec3 = this.getDeltaMovement();
      if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || this.getFeetBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
         vec3 = new Vec3(vec3.x, 0.2D, vec3.z);
      }

      return vec3;
   }

   public Vec3 getFluidFallingAdjustedMovement(double pGravity, boolean pIsFalling, Vec3 pDeltaMovement) {
      if (!this.isNoGravity() && !this.isSprinting()) {
         double d0;
         if (pIsFalling && Math.abs(pDeltaMovement.y - 0.005D) >= 0.003D && Math.abs(pDeltaMovement.y - pGravity / 16.0D) < 0.003D) {
            d0 = -0.003D;
         } else {
            d0 = pDeltaMovement.y - pGravity / 16.0D;
         }

         return new Vec3(pDeltaMovement.x, d0, pDeltaMovement.z);
      } else {
         return pDeltaMovement;
      }
   }

   private Vec3 handleOnClimbable(Vec3 pDeltaMovement) {
      if (this.onClimbable()) {
         this.resetFallDistance();
         float f = 0.15F;
         double d0 = Mth.clamp(pDeltaMovement.x, (double)-0.15F, (double)0.15F);
         double d1 = Mth.clamp(pDeltaMovement.z, (double)-0.15F, (double)0.15F);
         double d2 = Math.max(pDeltaMovement.y, (double)-0.15F);
         if (d2 < 0.0D && !this.getFeetBlockState().is(Blocks.SCAFFOLDING) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
            d2 = 0.0D;
         }

         pDeltaMovement = new Vec3(d0, d2, d1);
      }

      return pDeltaMovement;
   }

   private float getFrictionInfluencedSpeed(float pFriction) {
      return this.onGround() ? this.getSpeed() * (0.21600002F / (pFriction * pFriction * pFriction)) : this.getFlyingSpeed();
   }

   protected float getFlyingSpeed() {
      return this.getControllingPassenger() instanceof Player ? this.getSpeed() * 0.1F : 0.02F;
   }

   public float getSpeed() {
      return this.speed;
   }

   public void setSpeed(float pSpeed) {
      this.speed = pSpeed;
   }

   public boolean doHurtTarget(Entity pTarget) {
      this.setLastHurtMob(pTarget);
      return false;
   }

   public void tick() {
      super.tick();
      this.updatingUsingItem();
      this.updateSwimAmount();
      if (!this.level().isClientSide) {
         int i = this.getArrowCount();
         if (i > 0) {
            if (this.removeArrowTime <= 0) {
               this.removeArrowTime = 20 * (30 - i);
            }

            --this.removeArrowTime;
            if (this.removeArrowTime <= 0) {
               this.setArrowCount(i - 1);
            }
         }

         int j = this.getStingerCount();
         if (j > 0) {
            if (this.removeStingerTime <= 0) {
               this.removeStingerTime = 20 * (30 - j);
            }

            --this.removeStingerTime;
            if (this.removeStingerTime <= 0) {
               this.setStingerCount(j - 1);
            }
         }

         this.detectEquipmentUpdates();
         if (this.tickCount % 20 == 0) {
            this.getCombatTracker().recheckStatus();
         }

         if (this.isSleeping() && !this.checkBedExists()) {
            this.stopSleeping();
         }
      }

      if (!this.isRemoved()) {
         this.aiStep();
      }

      double d1 = this.getX() - this.xo;
      double d0 = this.getZ() - this.zo;
      float f = (float)(d1 * d1 + d0 * d0);
      float f1 = this.yBodyRot;
      float f2 = 0.0F;
      this.oRun = this.run;
      float f3 = 0.0F;
      if (f > 0.0025000002F) {
         f3 = 1.0F;
         f2 = (float)Math.sqrt((double)f) * 3.0F;
         float f4 = (float)Mth.atan2(d0, d1) * (180F / (float)Math.PI) - 90.0F;
         float f5 = Mth.abs(Mth.wrapDegrees(this.getYRot()) - f4);
         if (95.0F < f5 && f5 < 265.0F) {
            f1 = f4 - 180.0F;
         } else {
            f1 = f4;
         }
      }

      if (this.attackAnim > 0.0F) {
         f1 = this.getYRot();
      }

      if (!this.onGround()) {
         f3 = 0.0F;
      }

      this.run += (f3 - this.run) * 0.3F;
      this.level().getProfiler().push("headTurn");
      f2 = this.tickHeadTurn(f1, f2);
      this.level().getProfiler().pop();
      this.level().getProfiler().push("rangeChecks");

      while(this.getYRot() - this.yRotO < -180.0F) {
         this.yRotO -= 360.0F;
      }

      while(this.getYRot() - this.yRotO >= 180.0F) {
         this.yRotO += 360.0F;
      }

      while(this.yBodyRot - this.yBodyRotO < -180.0F) {
         this.yBodyRotO -= 360.0F;
      }

      while(this.yBodyRot - this.yBodyRotO >= 180.0F) {
         this.yBodyRotO += 360.0F;
      }

      while(this.getXRot() - this.xRotO < -180.0F) {
         this.xRotO -= 360.0F;
      }

      while(this.getXRot() - this.xRotO >= 180.0F) {
         this.xRotO += 360.0F;
      }

      while(this.yHeadRot - this.yHeadRotO < -180.0F) {
         this.yHeadRotO -= 360.0F;
      }

      while(this.yHeadRot - this.yHeadRotO >= 180.0F) {
         this.yHeadRotO += 360.0F;
      }

      this.level().getProfiler().pop();
      this.animStep += f2;
      if (this.isFallFlying()) {
         ++this.fallFlyTicks;
      } else {
         this.fallFlyTicks = 0;
      }

      if (this.isSleeping()) {
         this.setXRot(0.0F);
      }

      this.refreshDirtyAttributes();
   }

   private void detectEquipmentUpdates() {
      Map<EquipmentSlot, ItemStack> map = this.collectEquipmentChanges();
      if (map != null) {
         this.handleHandSwap(map);
         if (!map.isEmpty()) {
            this.handleEquipmentChanges(map);
         }
      }

   }

   @Nullable
   private Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
      Map<EquipmentSlot, ItemStack> map = null;

      for(EquipmentSlot equipmentslot : EquipmentSlot.values()) {
         ItemStack itemstack;
         switch (equipmentslot.getType()) {
            case HAND:
               itemstack = this.getLastHandItem(equipmentslot);
               break;
            case ARMOR:
               itemstack = this.getLastArmorItem(equipmentslot);
               break;
            default:
               continue;
         }

         ItemStack itemstack1 = this.getItemBySlot(equipmentslot);
         if (this.equipmentHasChanged(itemstack, itemstack1)) {
            if (map == null) {
               map = Maps.newEnumMap(EquipmentSlot.class);
            }

            map.put(equipmentslot, itemstack1);
            if (!itemstack.isEmpty()) {
               this.getAttributes().removeAttributeModifiers(itemstack.getAttributeModifiers(equipmentslot));
            }

            if (!itemstack1.isEmpty()) {
               this.getAttributes().addTransientAttributeModifiers(itemstack1.getAttributeModifiers(equipmentslot));
            }
         }
      }

      return map;
   }

   public boolean equipmentHasChanged(ItemStack pOldItem, ItemStack pNewItem) {
      return !ItemStack.matches(pNewItem, pOldItem);
   }

   private void handleHandSwap(Map<EquipmentSlot, ItemStack> pHands) {
      ItemStack itemstack = pHands.get(EquipmentSlot.MAINHAND);
      ItemStack itemstack1 = pHands.get(EquipmentSlot.OFFHAND);
      if (itemstack != null && itemstack1 != null && ItemStack.matches(itemstack, this.getLastHandItem(EquipmentSlot.OFFHAND)) && ItemStack.matches(itemstack1, this.getLastHandItem(EquipmentSlot.MAINHAND))) {
         ((ServerLevel)this.level()).getChunkSource().broadcast(this, new ClientboundEntityEventPacket(this, (byte)55));
         pHands.remove(EquipmentSlot.MAINHAND);
         pHands.remove(EquipmentSlot.OFFHAND);
         this.setLastHandItem(EquipmentSlot.MAINHAND, itemstack.copy());
         this.setLastHandItem(EquipmentSlot.OFFHAND, itemstack1.copy());
      }

   }

   private void handleEquipmentChanges(Map<EquipmentSlot, ItemStack> pEquipments) {
      List<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayListWithCapacity(pEquipments.size());
      pEquipments.forEach((p_147204_, p_147205_) -> {
         ItemStack itemstack = p_147205_.copy();
         list.add(Pair.of(p_147204_, itemstack));
         switch (p_147204_.getType()) {
            case HAND:
               this.setLastHandItem(p_147204_, itemstack);
               break;
            case ARMOR:
               this.setLastArmorItem(p_147204_, itemstack);
         }

      });
      ((ServerLevel)this.level()).getChunkSource().broadcast(this, new ClientboundSetEquipmentPacket(this.getId(), list));
   }

   private ItemStack getLastArmorItem(EquipmentSlot pSlot) {
      return this.lastArmorItemStacks.get(pSlot.getIndex());
   }

   private void setLastArmorItem(EquipmentSlot pSlot, ItemStack pStack) {
      this.lastArmorItemStacks.set(pSlot.getIndex(), pStack);
   }

   private ItemStack getLastHandItem(EquipmentSlot pSlot) {
      return this.lastHandItemStacks.get(pSlot.getIndex());
   }

   private void setLastHandItem(EquipmentSlot pSlot, ItemStack pStack) {
      this.lastHandItemStacks.set(pSlot.getIndex(), pStack);
   }

   protected float tickHeadTurn(float pYRot, float pAnimStep) {
      float f = Mth.wrapDegrees(pYRot - this.yBodyRot);
      this.yBodyRot += f * 0.3F;
      float f1 = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
      float f2 = this.getMaxHeadRotationRelativeToBody();
      if (Math.abs(f1) > f2) {
         this.yBodyRot += f1 - (float)Mth.sign((double)f1) * f2;
      }

      boolean flag = f1 < -90.0F || f1 >= 90.0F;
      if (flag) {
         pAnimStep *= -1.0F;
      }

      return pAnimStep;
   }

   protected float getMaxHeadRotationRelativeToBody() {
      return 50.0F;
   }

   public void aiStep() {
      if (this.noJumpDelay > 0) {
         --this.noJumpDelay;
      }

      if (this.isControlledByLocalInstance()) {
         this.lerpSteps = 0;
         this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());
      }

      if (this.lerpSteps > 0) {
         this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
         --this.lerpSteps;
      } else if (!this.isEffectiveAi()) {
         this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
      }

      if (this.lerpHeadSteps > 0) {
         this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
         --this.lerpHeadSteps;
      }

      Vec3 vec3 = this.getDeltaMovement();
      double d0 = vec3.x;
      double d1 = vec3.y;
      double d2 = vec3.z;
      if (Math.abs(vec3.x) < 0.003D) {
         d0 = 0.0D;
      }

      if (Math.abs(vec3.y) < 0.003D) {
         d1 = 0.0D;
      }

      if (Math.abs(vec3.z) < 0.003D) {
         d2 = 0.0D;
      }

      this.setDeltaMovement(d0, d1, d2);
      this.level().getProfiler().push("ai");
      if (this.isImmobile()) {
         this.jumping = false;
         this.xxa = 0.0F;
         this.zza = 0.0F;
      } else if (this.isEffectiveAi()) {
         this.level().getProfiler().push("newAi");
         this.serverAiStep();
         this.level().getProfiler().pop();
      }

      this.level().getProfiler().pop();
      this.level().getProfiler().push("jump");
      if (this.jumping && this.isAffectedByFluids()) {
         double d3;
         if (this.isInLava()) {
            d3 = this.getFluidHeight(FluidTags.LAVA);
         } else {
            d3 = this.getFluidHeight(FluidTags.WATER);
         }

         boolean flag = this.isInWater() && d3 > 0.0D;
         double d4 = this.getFluidJumpThreshold();
         if (!flag || this.onGround() && !(d3 > d4)) {
            if (!this.isInLava() || this.onGround() && !(d3 > d4)) {
               if ((this.onGround() || flag && d3 <= d4) && this.noJumpDelay == 0) {
                  this.jumpFromGround();
                  this.noJumpDelay = 10;
               }
            } else {
               this.jumpInLiquid(FluidTags.LAVA);
            }
         } else {
            this.jumpInLiquid(FluidTags.WATER);
         }
      } else {
         this.noJumpDelay = 0;
      }

      this.level().getProfiler().pop();
      this.level().getProfiler().push("travel");
      this.xxa *= 0.98F;
      this.zza *= 0.98F;
      this.updateFallFlying();
      AABB aabb = this.getBoundingBox();
      Vec3 vec31 = new Vec3((double)this.xxa, (double)this.yya, (double)this.zza);
      if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
         this.resetFallDistance();
      }

      label104: {
         LivingEntity livingentity = this.getControllingPassenger();
         if (livingentity instanceof Player player) {
            if (this.isAlive()) {
               this.travelRidden(player, vec31);
               break label104;
            }
         }

         this.travel(vec31);
      }

      this.level().getProfiler().pop();
      this.level().getProfiler().push("freezing");
      if (!this.level().isClientSide && !this.isDeadOrDying()) {
         int i = this.getTicksFrozen();
         if (this.isInPowderSnow && this.canFreeze()) {
            this.setTicksFrozen(Math.min(this.getTicksRequiredToFreeze(), i + 1));
         } else {
            this.setTicksFrozen(Math.max(0, i - 2));
         }
      }

      this.removeFrost();
      this.tryAddFrost();
      if (!this.level().isClientSide && this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
         this.hurt(this.damageSources().freeze(), 1.0F);
      }

      this.level().getProfiler().pop();
      this.level().getProfiler().push("push");
      if (this.autoSpinAttackTicks > 0) {
         --this.autoSpinAttackTicks;
         this.checkAutoSpinAttack(aabb, this.getBoundingBox());
      }

      this.pushEntities();
      this.level().getProfiler().pop();
      if (!this.level().isClientSide && this.isSensitiveToWater() && this.isInWaterRainOrBubble()) {
         this.hurt(this.damageSources().drown(), 1.0F);
      }

   }

   public boolean isSensitiveToWater() {
      return false;
   }

   private void updateFallFlying() {
      boolean flag = this.getSharedFlag(7);
      if (flag && !this.onGround() && !this.isPassenger() && !this.hasEffect(MobEffects.LEVITATION)) {
         ItemStack itemstack = this.getItemBySlot(EquipmentSlot.CHEST);
         if (itemstack.is(Items.ELYTRA) && ElytraItem.isFlyEnabled(itemstack)) {
            flag = true;
            int i = this.fallFlyTicks + 1;
            if (!this.level().isClientSide && i % 10 == 0) {
               int j = i / 10;
               if (j % 2 == 0) {
                  itemstack.hurtAndBreak(1, this, (p_147232_) -> {
                     p_147232_.broadcastBreakEvent(EquipmentSlot.CHEST);
                  });
               }

               this.gameEvent(GameEvent.ELYTRA_GLIDE);
            }
         } else {
            flag = false;
         }
      } else {
         flag = false;
      }

      if (!this.level().isClientSide) {
         this.setSharedFlag(7, flag);
      }

   }

   protected void serverAiStep() {
   }

   protected void pushEntities() {
      if (this.level().isClientSide()) {
         this.level().getEntities(EntityTypeTest.forClass(Player.class), this.getBoundingBox(), EntitySelector.pushableBy(this)).forEach(this::doPush);
      } else {
         List<Entity> list = this.level().getEntities(this, this.getBoundingBox(), EntitySelector.pushableBy(this));
         if (!list.isEmpty()) {
            int i = this.level().getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
            if (i > 0 && list.size() > i - 1 && this.random.nextInt(4) == 0) {
               int j = 0;

               for(Entity entity : list) {
                  if (!entity.isPassenger()) {
                     ++j;
                  }
               }

               if (j > i - 1) {
                  this.hurt(this.damageSources().cramming(), 6.0F);
               }
            }

            for(Entity entity1 : list) {
               this.doPush(entity1);
            }
         }

      }
   }

   protected void checkAutoSpinAttack(AABB pBoundingBoxBeforeSpin, AABB pBoundingBoxAfterSpin) {
      AABB aabb = pBoundingBoxBeforeSpin.minmax(pBoundingBoxAfterSpin);
      List<Entity> list = this.level().getEntities(this, aabb);
      if (!list.isEmpty()) {
         for(Entity entity : list) {
            if (entity instanceof LivingEntity) {
               this.doAutoAttackOnTouch((LivingEntity)entity);
               this.autoSpinAttackTicks = 0;
               this.setDeltaMovement(this.getDeltaMovement().scale(-0.2D));
               break;
            }
         }
      } else if (this.horizontalCollision) {
         this.autoSpinAttackTicks = 0;
      }

      if (!this.level().isClientSide && this.autoSpinAttackTicks <= 0) {
         this.setLivingEntityFlag(4, false);
      }

   }

   protected void doPush(Entity p_20971_) {
      p_20971_.push(this);
   }

   protected void doAutoAttackOnTouch(LivingEntity pTarget) {
   }

   public boolean isAutoSpinAttack() {
      return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
   }

   public void stopRiding() {
      Entity entity = this.getVehicle();
      super.stopRiding();
      if (entity != null && entity != this.getVehicle() && !this.level().isClientSide) {
         this.dismountVehicle(entity);
      }

   }

   public void rideTick() {
      super.rideTick();
      this.oRun = this.run;
      this.run = 0.0F;
      this.resetFallDistance();
   }

   public void lerpTo(double pX, double pY, double pZ, float pYRot, float pXRot, int pSteps) {
      this.lerpX = pX;
      this.lerpY = pY;
      this.lerpZ = pZ;
      this.lerpYRot = (double)pYRot;
      this.lerpXRot = (double)pXRot;
      this.lerpSteps = pSteps;
   }

   public double lerpTargetX() {
      return this.lerpSteps > 0 ? this.lerpX : this.getX();
   }

   public double lerpTargetY() {
      return this.lerpSteps > 0 ? this.lerpY : this.getY();
   }

   public double lerpTargetZ() {
      return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
   }

   public float lerpTargetXRot() {
      return this.lerpSteps > 0 ? (float)this.lerpXRot : this.getXRot();
   }

   public float lerpTargetYRot() {
      return this.lerpSteps > 0 ? (float)this.lerpYRot : this.getYRot();
   }

   public void lerpHeadTo(float pYaw, int pPitch) {
      this.lerpYHeadRot = (double)pYaw;
      this.lerpHeadSteps = pPitch;
   }

   public void setJumping(boolean pJumping) {
      this.jumping = pJumping;
   }

   public void onItemPickup(ItemEntity pItemEntity) {
      Entity entity = pItemEntity.getOwner();
      if (entity instanceof ServerPlayer) {
         CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY.trigger((ServerPlayer)entity, pItemEntity.getItem(), this);
      }

   }

   public void take(Entity pEntity, int pAmount) {
      if (!pEntity.isRemoved() && !this.level().isClientSide && (pEntity instanceof ItemEntity || pEntity instanceof AbstractArrow || pEntity instanceof ExperienceOrb)) {
         ((ServerLevel)this.level()).getChunkSource().broadcast(pEntity, new ClientboundTakeItemEntityPacket(pEntity.getId(), this.getId(), pAmount));
      }

   }

   public boolean hasLineOfSight(Entity pEntity) {
      if (pEntity.level() != this.level()) {
         return false;
      } else {
         Vec3 vec3 = new Vec3(this.getX(), this.getEyeY(), this.getZ());
         Vec3 vec31 = new Vec3(pEntity.getX(), pEntity.getEyeY(), pEntity.getZ());
         if (vec31.distanceTo(vec3) > 128.0D) {
            return false;
         } else {
            return this.level().clip(new ClipContext(vec3, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.MISS;
         }
      }
   }

   public float getViewYRot(float pPartialTicks) {
      return pPartialTicks == 1.0F ? this.yHeadRot : Mth.lerp(pPartialTicks, this.yHeadRotO, this.yHeadRot);
   }

   public float getAttackAnim(float pPartialTick) {
      float f = this.attackAnim - this.oAttackAnim;
      if (f < 0.0F) {
         ++f;
      }

      return this.oAttackAnim + f * pPartialTick;
   }

   public boolean isPickable() {
      return !this.isRemoved();
   }

   public boolean isPushable() {
      return this.isAlive() && !this.isSpectator() && !this.onClimbable();
   }

   public float getYHeadRot() {
      return this.yHeadRot;
   }

   public void setYHeadRot(float pRotation) {
      this.yHeadRot = pRotation;
   }

   public void setYBodyRot(float pOffset) {
      this.yBodyRot = pOffset;
   }

   protected Vec3 getRelativePortalPosition(Direction.Axis pAxis, BlockUtil.FoundRectangle pPortal) {
      return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(pAxis, pPortal));
   }

   public static Vec3 resetForwardDirectionOfRelativePortalPosition(Vec3 pRelativePortalPosition) {
      return new Vec3(pRelativePortalPosition.x, pRelativePortalPosition.y, 0.0D);
   }

   public float getAbsorptionAmount() {
      return this.absorptionAmount;
   }

   public final void setAbsorptionAmount(float pAbsorptionAmount) {
      this.internalSetAbsorptionAmount(Mth.clamp(pAbsorptionAmount, 0.0F, this.getMaxAbsorption()));
   }

   protected void internalSetAbsorptionAmount(float pAbsorptionAmount) {
      this.absorptionAmount = pAbsorptionAmount;
   }

   public void onEnterCombat() {
   }

   public void onLeaveCombat() {
   }

   protected void updateEffectVisibility() {
      this.effectsDirty = true;
   }

   public abstract HumanoidArm getMainArm();

   public boolean isUsingItem() {
      return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
   }

   public InteractionHand getUsedItemHand() {
      return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
   }

   private void updatingUsingItem() {
      if (this.isUsingItem()) {
         if (ItemStack.isSameItem(this.getItemInHand(this.getUsedItemHand()), this.useItem)) {
            this.useItem = this.getItemInHand(this.getUsedItemHand());
            this.updateUsingItem(this.useItem);
         } else {
            this.stopUsingItem();
         }
      }

   }

   protected void updateUsingItem(ItemStack pUsingItem) {
      pUsingItem.onUseTick(this.level(), this, this.getUseItemRemainingTicks());
      if (this.shouldTriggerItemUseEffects()) {
         this.triggerItemUseEffects(pUsingItem, 5);
      }

      if (--this.useItemRemaining == 0 && !this.level().isClientSide && !pUsingItem.useOnRelease()) {
         this.completeUsingItem();
      }

   }

   private boolean shouldTriggerItemUseEffects() {
      int i = this.getUseItemRemainingTicks();
      FoodProperties foodproperties = this.useItem.getItem().getFoodProperties();
      boolean flag = foodproperties != null && foodproperties.isFastFood();
      flag |= i <= this.useItem.getUseDuration() - 7;
      return flag && i % 4 == 0;
   }

   private void updateSwimAmount() {
      this.swimAmountO = this.swimAmount;
      if (this.isVisuallySwimming()) {
         this.swimAmount = Math.min(1.0F, this.swimAmount + 0.09F);
      } else {
         this.swimAmount = Math.max(0.0F, this.swimAmount - 0.09F);
      }

   }

   protected void setLivingEntityFlag(int pKey, boolean pValue) {
      int i = this.entityData.get(DATA_LIVING_ENTITY_FLAGS);
      if (pValue) {
         i |= pKey;
      } else {
         i &= ~pKey;
      }

      this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)i);
   }

   public void startUsingItem(InteractionHand pHand) {
      ItemStack itemstack = this.getItemInHand(pHand);
      if (!itemstack.isEmpty() && !this.isUsingItem()) {
         this.useItem = itemstack;
         this.useItemRemaining = itemstack.getUseDuration();
         if (!this.level().isClientSide) {
            this.setLivingEntityFlag(1, true);
            this.setLivingEntityFlag(2, pHand == InteractionHand.OFF_HAND);
            this.gameEvent(GameEvent.ITEM_INTERACT_START);
         }

      }
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
      super.onSyncedDataUpdated(pKey);
      if (SLEEPING_POS_ID.equals(pKey)) {
         if (this.level().isClientSide) {
            this.getSleepingPos().ifPresent(this::setPosToBed);
         }
      } else if (DATA_LIVING_ENTITY_FLAGS.equals(pKey) && this.level().isClientSide) {
         if (this.isUsingItem() && this.useItem.isEmpty()) {
            this.useItem = this.getItemInHand(this.getUsedItemHand());
            if (!this.useItem.isEmpty()) {
               this.useItemRemaining = this.useItem.getUseDuration();
            }
         } else if (!this.isUsingItem() && !this.useItem.isEmpty()) {
            this.useItem = ItemStack.EMPTY;
            this.useItemRemaining = 0;
         }
      }

   }

   public void lookAt(EntityAnchorArgument.Anchor pAnchor, Vec3 pTarget) {
      super.lookAt(pAnchor, pTarget);
      this.yHeadRotO = this.yHeadRot;
      this.yBodyRot = this.yHeadRot;
      this.yBodyRotO = this.yBodyRot;
   }

   protected void triggerItemUseEffects(ItemStack pStack, int pAmount) {
      if (!pStack.isEmpty() && this.isUsingItem()) {
         if (pStack.getUseAnimation() == UseAnim.DRINK) {
            this.playSound(this.getDrinkingSound(pStack), 0.5F, this.level().random.nextFloat() * 0.1F + 0.9F);
         }

         if (pStack.getUseAnimation() == UseAnim.EAT) {
            this.spawnItemParticles(pStack, pAmount);
            this.playSound(this.getEatingSound(pStack), 0.5F + 0.5F * (float)this.random.nextInt(2), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
         }

      }
   }

   private void spawnItemParticles(ItemStack pStack, int pAmount) {
      for(int i = 0; i < pAmount; ++i) {
         Vec3 vec3 = new Vec3(((double)this.random.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);
         vec3 = vec3.xRot(-this.getXRot() * ((float)Math.PI / 180F));
         vec3 = vec3.yRot(-this.getYRot() * ((float)Math.PI / 180F));
         double d0 = (double)(-this.random.nextFloat()) * 0.6D - 0.3D;
         Vec3 vec31 = new Vec3(((double)this.random.nextFloat() - 0.5D) * 0.3D, d0, 0.6D);
         vec31 = vec31.xRot(-this.getXRot() * ((float)Math.PI / 180F));
         vec31 = vec31.yRot(-this.getYRot() * ((float)Math.PI / 180F));
         vec31 = vec31.add(this.getX(), this.getEyeY(), this.getZ());
         this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, pStack), vec31.x, vec31.y, vec31.z, vec3.x, vec3.y + 0.05D, vec3.z);
      }

   }

   protected void completeUsingItem() {
      if (!this.level().isClientSide || this.isUsingItem()) {
         InteractionHand interactionhand = this.getUsedItemHand();
         if (!this.useItem.equals(this.getItemInHand(interactionhand))) {
            this.releaseUsingItem();
         } else {
            if (!this.useItem.isEmpty() && this.isUsingItem()) {
               this.triggerItemUseEffects(this.useItem, 16);
               ItemStack itemstack = this.useItem.finishUsingItem(this.level(), this);
               if (itemstack != this.useItem) {
                  this.setItemInHand(interactionhand, itemstack);
               }

               this.stopUsingItem();
            }

         }
      }
   }

   public ItemStack getUseItem() {
      return this.useItem;
   }

   public int getUseItemRemainingTicks() {
      return this.useItemRemaining;
   }

   public int getTicksUsingItem() {
      return this.isUsingItem() ? this.useItem.getUseDuration() - this.getUseItemRemainingTicks() : 0;
   }

   public void releaseUsingItem() {
      if (!this.useItem.isEmpty()) {
         this.useItem.releaseUsing(this.level(), this, this.getUseItemRemainingTicks());
         if (this.useItem.useOnRelease()) {
            this.updatingUsingItem();
         }
      }

      this.stopUsingItem();
   }

   public void stopUsingItem() {
      if (!this.level().isClientSide) {
         boolean flag = this.isUsingItem();
         this.setLivingEntityFlag(1, false);
         if (flag) {
            this.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
         }
      }

      this.useItem = ItemStack.EMPTY;
      this.useItemRemaining = 0;
   }

   public boolean isBlocking() {
      if (this.isUsingItem() && !this.useItem.isEmpty()) {
         Item item = this.useItem.getItem();
         if (item.getUseAnimation(this.useItem) != UseAnim.BLOCK) {
            return false;
         } else {
            return item.getUseDuration(this.useItem) - this.useItemRemaining >= 5;
         }
      } else {
         return false;
      }
   }

   public boolean isSuppressingSlidingDownLadder() {
      return this.isShiftKeyDown();
   }

   public boolean isFallFlying() {
      return this.getSharedFlag(7);
   }

   public boolean isVisuallySwimming() {
      return super.isVisuallySwimming() || !this.isFallFlying() && this.hasPose(Pose.FALL_FLYING);
   }

   public int getFallFlyingTicks() {
      return this.fallFlyTicks;
   }

   public boolean randomTeleport(double pX, double pY, double pZ, boolean pBroadcastTeleport) {
      double d0 = this.getX();
      double d1 = this.getY();
      double d2 = this.getZ();
      double d3 = pY;
      boolean flag = false;
      BlockPos blockpos = BlockPos.containing(pX, pY, pZ);
      Level level = this.level();
      if (level.hasChunkAt(blockpos)) {
         boolean flag1 = false;

         while(!flag1 && blockpos.getY() > level.getMinBuildHeight()) {
            BlockPos blockpos1 = blockpos.below();
            BlockState blockstate = level.getBlockState(blockpos1);
            if (blockstate.blocksMotion()) {
               flag1 = true;
            } else {
               --d3;
               blockpos = blockpos1;
            }
         }

         if (flag1) {
            this.teleportTo(pX, d3, pZ);
            if (level.noCollision(this) && !level.containsAnyLiquid(this.getBoundingBox())) {
               flag = true;
            }
         }
      }

      if (!flag) {
         this.teleportTo(d0, d1, d2);
         return false;
      } else {
         if (pBroadcastTeleport) {
            level.broadcastEntityEvent(this, (byte)46);
         }

         if (this instanceof PathfinderMob) {
            PathfinderMob pathfindermob = (PathfinderMob)this;
            pathfindermob.getNavigation().stop();
         }

         return true;
      }
   }

   public boolean isAffectedByPotions() {
      return !this.isDeadOrDying();
   }

   public boolean attackable() {
      return true;
   }

   public void setRecordPlayingNearby(BlockPos pJukebox, boolean pPartyParrot) {
   }

   public boolean canTakeItem(ItemStack pStack) {
      return false;
   }

   public EntityDimensions getDimensions(Pose pPose) {
      return pPose == Pose.SLEEPING ? SLEEPING_DIMENSIONS : super.getDimensions(pPose).scale(this.getScale());
   }

   public ImmutableList<Pose> getDismountPoses() {
      return ImmutableList.of(Pose.STANDING);
   }

   public AABB getLocalBoundsForPose(Pose pPose) {
      EntityDimensions entitydimensions = this.getDimensions(pPose);
      return new AABB((double)(-entitydimensions.width / 2.0F), 0.0D, (double)(-entitydimensions.width / 2.0F), (double)(entitydimensions.width / 2.0F), (double)entitydimensions.height, (double)(entitydimensions.width / 2.0F));
   }

   protected boolean wouldNotSuffocateAtTargetPose(Pose pPose) {
      AABB aabb = this.getDimensions(pPose).makeBoundingBox(this.position());
      return this.level().noBlockCollision(this, aabb);
   }

   public boolean canChangeDimensions() {
      return super.canChangeDimensions() && !this.isSleeping();
   }

   public Optional<BlockPos> getSleepingPos() {
      return this.entityData.get(SLEEPING_POS_ID);
   }

   public void setSleepingPos(BlockPos pPos) {
      this.entityData.set(SLEEPING_POS_ID, Optional.of(pPos));
   }

   public void clearSleepingPos() {
      this.entityData.set(SLEEPING_POS_ID, Optional.empty());
   }

   public boolean isSleeping() {
      return this.getSleepingPos().isPresent();
   }

   public void startSleeping(BlockPos pPos) {
      if (this.isPassenger()) {
         this.stopRiding();
      }

      BlockState blockstate = this.level().getBlockState(pPos);
      if (blockstate.getBlock() instanceof BedBlock) {
         this.level().setBlock(pPos, blockstate.setValue(BedBlock.OCCUPIED, Boolean.valueOf(true)), 3);
      }

      this.setPose(Pose.SLEEPING);
      this.setPosToBed(pPos);
      this.setSleepingPos(pPos);
      this.setDeltaMovement(Vec3.ZERO);
      this.hasImpulse = true;
   }

   private void setPosToBed(BlockPos p_21081_) {
      this.setPos((double)p_21081_.getX() + 0.5D, (double)p_21081_.getY() + 0.6875D, (double)p_21081_.getZ() + 0.5D);
   }

   private boolean checkBedExists() {
      return this.getSleepingPos().map((p_309015_) -> {
         return this.level().getBlockState(p_309015_).getBlock() instanceof BedBlock;
      }).orElse(false);
   }

   public void stopSleeping() {
      this.getSleepingPos().filter(this.level()::hasChunkAt).ifPresent((p_261435_) -> {
         BlockState blockstate = this.level().getBlockState(p_261435_);
         if (blockstate.getBlock() instanceof BedBlock) {
            Direction direction = blockstate.getValue(BedBlock.FACING);
            this.level().setBlock(p_261435_, blockstate.setValue(BedBlock.OCCUPIED, Boolean.valueOf(false)), 3);
            Vec3 vec31 = BedBlock.findStandUpPosition(this.getType(), this.level(), p_261435_, direction, this.getYRot()).orElseGet(() -> {
               BlockPos blockpos = p_261435_.above();
               return new Vec3((double)blockpos.getX() + 0.5D, (double)blockpos.getY() + 0.1D, (double)blockpos.getZ() + 0.5D);
            });
            Vec3 vec32 = Vec3.atBottomCenterOf(p_261435_).subtract(vec31).normalize();
            float f = (float)Mth.wrapDegrees(Mth.atan2(vec32.z, vec32.x) * (double)(180F / (float)Math.PI) - 90.0D);
            this.setPos(vec31.x, vec31.y, vec31.z);
            this.setYRot(f);
            this.setXRot(0.0F);
         }

      });
      Vec3 vec3 = this.position();
      this.setPose(Pose.STANDING);
      this.setPos(vec3.x, vec3.y, vec3.z);
      this.clearSleepingPos();
   }

   @Nullable
   public Direction getBedOrientation() {
      BlockPos blockpos = this.getSleepingPos().orElse((BlockPos)null);
      return blockpos != null ? BedBlock.getBedOrientation(this.level(), blockpos) : null;
   }

   public boolean isInWall() {
      return !this.isSleeping() && super.isInWall();
   }

   protected final float getEyeHeight(Pose pPose, EntityDimensions pSize) {
      return pPose == Pose.SLEEPING ? 0.2F : this.getStandingEyeHeight(pPose, pSize);
   }

   protected float getStandingEyeHeight(Pose pPose, EntityDimensions pDimensions) {
      return super.getEyeHeight(pPose, pDimensions);
   }

   public ItemStack getProjectile(ItemStack pWeaponStack) {
      return ItemStack.EMPTY;
   }

   public ItemStack eat(Level pLevel, ItemStack pFood) {
      if (pFood.isEdible()) {
         pLevel.playSound((Player)null, this.getX(), this.getY(), this.getZ(), this.getEatingSound(pFood), SoundSource.NEUTRAL, 1.0F, 1.0F + (pLevel.random.nextFloat() - pLevel.random.nextFloat()) * 0.4F);
         this.addEatEffect(pFood, pLevel, this);
         if (!(this instanceof Player) || !((Player)this).getAbilities().instabuild) {
            pFood.shrink(1);
         }

         this.gameEvent(GameEvent.EAT);
      }

      return pFood;
   }

   private void addEatEffect(ItemStack pFood, Level pLevel, LivingEntity pLivingEntity) {
      Item item = pFood.getItem();
      if (item.isEdible()) {
         for(Pair<MobEffectInstance, Float> pair : item.getFoodProperties().getEffects()) {
            if (!pLevel.isClientSide && pair.getFirst() != null && pLevel.random.nextFloat() < pair.getSecond()) {
               pLivingEntity.addEffect(new MobEffectInstance(pair.getFirst()));
            }
         }
      }

   }

   private static byte entityEventForEquipmentBreak(EquipmentSlot pSlot) {
      switch (pSlot) {
         case MAINHAND:
            return 47;
         case OFFHAND:
            return 48;
         case HEAD:
            return 49;
         case CHEST:
            return 50;
         case FEET:
            return 52;
         case LEGS:
            return 51;
         default:
            return 47;
      }
   }

   public void broadcastBreakEvent(EquipmentSlot pSlot) {
      this.level().broadcastEntityEvent(this, entityEventForEquipmentBreak(pSlot));
   }

   public void broadcastBreakEvent(InteractionHand pHand) {
      this.broadcastBreakEvent(pHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
   }

   public AABB getBoundingBoxForCulling() {
      if (this.getItemBySlot(EquipmentSlot.HEAD).is(Items.DRAGON_HEAD)) {
         float f = 0.5F;
         return this.getBoundingBox().inflate(0.5D, 0.5D, 0.5D);
      } else {
         return super.getBoundingBoxForCulling();
      }
   }

   public static EquipmentSlot getEquipmentSlotForItem(ItemStack pItem) {
      Equipable equipable = Equipable.get(pItem);
      return equipable != null ? equipable.getEquipmentSlot() : EquipmentSlot.MAINHAND;
   }

   private static SlotAccess createEquipmentSlotAccess(LivingEntity pEntity, EquipmentSlot pSlot) {
      return pSlot != EquipmentSlot.HEAD && pSlot != EquipmentSlot.MAINHAND && pSlot != EquipmentSlot.OFFHAND ? SlotAccess.forEquipmentSlot(pEntity, pSlot, (p_269791_) -> {
         return p_269791_.isEmpty() || Mob.getEquipmentSlotForItem(p_269791_) == pSlot;
      }) : SlotAccess.forEquipmentSlot(pEntity, pSlot);
   }

   @Nullable
   private static EquipmentSlot getEquipmentSlot(int pIndex) {
      if (pIndex == 100 + EquipmentSlot.HEAD.getIndex()) {
         return EquipmentSlot.HEAD;
      } else if (pIndex == 100 + EquipmentSlot.CHEST.getIndex()) {
         return EquipmentSlot.CHEST;
      } else if (pIndex == 100 + EquipmentSlot.LEGS.getIndex()) {
         return EquipmentSlot.LEGS;
      } else if (pIndex == 100 + EquipmentSlot.FEET.getIndex()) {
         return EquipmentSlot.FEET;
      } else if (pIndex == 98) {
         return EquipmentSlot.MAINHAND;
      } else {
         return pIndex == 99 ? EquipmentSlot.OFFHAND : null;
      }
   }

   public SlotAccess getSlot(int pSlot) {
      EquipmentSlot equipmentslot = getEquipmentSlot(pSlot);
      return equipmentslot != null ? createEquipmentSlotAccess(this, equipmentslot) : super.getSlot(pSlot);
   }

   public boolean canFreeze() {
      if (this.isSpectator()) {
         return false;
      } else {
         boolean flag = !this.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.FREEZE_IMMUNE_WEARABLES) && !this.getItemBySlot(EquipmentSlot.CHEST).is(ItemTags.FREEZE_IMMUNE_WEARABLES) && !this.getItemBySlot(EquipmentSlot.LEGS).is(ItemTags.FREEZE_IMMUNE_WEARABLES) && !this.getItemBySlot(EquipmentSlot.FEET).is(ItemTags.FREEZE_IMMUNE_WEARABLES);
         return flag && super.canFreeze();
      }
   }

   public boolean isCurrentlyGlowing() {
      return !this.level().isClientSide() && this.hasEffect(MobEffects.GLOWING) || super.isCurrentlyGlowing();
   }

   public float getVisualRotationYInDegrees() {
      return this.yBodyRot;
   }

   public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
      double d0 = pPacket.getX();
      double d1 = pPacket.getY();
      double d2 = pPacket.getZ();
      float f = pPacket.getYRot();
      float f1 = pPacket.getXRot();
      this.syncPacketPositionCodec(d0, d1, d2);
      this.yBodyRot = pPacket.getYHeadRot();
      this.yHeadRot = pPacket.getYHeadRot();
      this.yBodyRotO = this.yBodyRot;
      this.yHeadRotO = this.yHeadRot;
      this.setId(pPacket.getId());
      this.setUUID(pPacket.getUUID());
      this.absMoveTo(d0, d1, d2, f, f1);
      this.setDeltaMovement(pPacket.getXa(), pPacket.getYa(), pPacket.getZa());
   }

   public boolean canDisableShield() {
      return this.getMainHandItem().getItem() instanceof AxeItem;
   }

   public float maxUpStep() {
      float f = super.maxUpStep();
      return this.getControllingPassenger() instanceof Player ? Math.max(f, 1.0F) : f;
   }

   public Vec3 getPassengerRidingPosition(Entity pEntity) {
      return (new Vec3(this.getPassengerAttachmentPoint(pEntity, this.getDimensions(this.getPose()), this.getScale()).rotateY(-this.yBodyRot * ((float)Math.PI / 180F)))).add(this.position());
   }

   public float getMyRidingOffset(Entity pEntity) {
      return this.ridingOffset(pEntity) * this.getScale();
   }

   protected void lerpHeadRotationStep(int pLerpHeadSteps, double pLerpYHeadRot) {
      this.yHeadRot = (float)Mth.rotLerp(1.0D / (double)pLerpHeadSteps, (double)this.yHeadRot, pLerpYHeadRot);
   }

   public static record Fallsounds(SoundEvent small, SoundEvent big) {
   }
}