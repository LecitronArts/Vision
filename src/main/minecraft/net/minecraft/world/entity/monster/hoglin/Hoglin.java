package net.minecraft.world.entity.monster.hoglin;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class Hoglin extends Animal implements Enemy, HoglinBase {
   private static final EntityDataAccessor<Boolean> DATA_IMMUNE_TO_ZOMBIFICATION = SynchedEntityData.defineId(Hoglin.class, EntityDataSerializers.BOOLEAN);
   private static final float PROBABILITY_OF_SPAWNING_AS_BABY = 0.2F;
   private static final int MAX_HEALTH = 40;
   private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.3F;
   private static final int ATTACK_KNOCKBACK = 1;
   private static final float KNOCKBACK_RESISTANCE = 0.6F;
   private static final int ATTACK_DAMAGE = 6;
   private static final float BABY_ATTACK_DAMAGE = 0.5F;
   private static final int CONVERSION_TIME = 300;
   private int attackAnimationRemainingTicks;
   private int timeInOverworld;
   private boolean cannotBeHunted;
   protected static final ImmutableList<? extends SensorType<? extends Sensor<? super Hoglin>>> SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ADULT, SensorType.HOGLIN_SPECIFIC_SENSOR);
   protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(MemoryModuleType.BREED_TARGET, MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, MemoryModuleType.LOOK_TARGET, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, MemoryModuleType.AVOID_TARGET, MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS, MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.NEAREST_REPELLENT, MemoryModuleType.PACIFIED, MemoryModuleType.IS_PANICKING);

   public Hoglin(EntityType<? extends Hoglin> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.xpReward = 5;
   }

   public boolean canBeLeashed(Player pPlayer) {
      return !this.isLeashed();
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 40.0D).add(Attributes.MOVEMENT_SPEED, (double)0.3F).add(Attributes.KNOCKBACK_RESISTANCE, (double)0.6F).add(Attributes.ATTACK_KNOCKBACK, 1.0D).add(Attributes.ATTACK_DAMAGE, 6.0D);
   }

   public boolean doHurtTarget(Entity pEntity) {
      if (!(pEntity instanceof LivingEntity)) {
         return false;
      } else {
         this.attackAnimationRemainingTicks = 10;
         this.level().broadcastEntityEvent(this, (byte)4);
         this.playSound(SoundEvents.HOGLIN_ATTACK, 1.0F, this.getVoicePitch());
         HoglinAi.onHitTarget(this, (LivingEntity)pEntity);
         return HoglinBase.hurtAndThrowTarget(this, (LivingEntity)pEntity);
      }
   }

   protected void blockedByShield(LivingEntity pEntity) {
      if (this.isAdult()) {
         HoglinBase.throwTarget(this, pEntity);
      }

   }

   public boolean hurt(DamageSource pSource, float pAmount) {
      boolean flag = super.hurt(pSource, pAmount);
      if (this.level().isClientSide) {
         return false;
      } else {
         if (flag && pSource.getEntity() instanceof LivingEntity) {
            HoglinAi.wasHurtBy(this, (LivingEntity)pSource.getEntity());
         }

         return flag;
      }
   }

   protected Brain.Provider<Hoglin> brainProvider() {
      return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
   }

   protected Brain<?> makeBrain(Dynamic<?> pDynamic) {
      return HoglinAi.makeBrain(this.brainProvider().makeBrain(pDynamic));
   }

   public Brain<Hoglin> getBrain() {
      return (Brain<Hoglin>)super.getBrain();
   }

   protected void customServerAiStep() {
      this.level().getProfiler().push("hoglinBrain");
      this.getBrain().tick((ServerLevel)this.level(), this);
      this.level().getProfiler().pop();
      HoglinAi.updateActivity(this);
      if (this.isConverting()) {
         ++this.timeInOverworld;
         if (this.timeInOverworld > 300) {
            this.playSoundEvent(SoundEvents.HOGLIN_CONVERTED_TO_ZOMBIFIED);
            this.finishConversion((ServerLevel)this.level());
         }
      } else {
         this.timeInOverworld = 0;
      }

   }

   public void aiStep() {
      if (this.attackAnimationRemainingTicks > 0) {
         --this.attackAnimationRemainingTicks;
      }

      super.aiStep();
   }

   protected void ageBoundaryReached() {
      if (this.isBaby()) {
         this.xpReward = 3;
         this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(0.5D);
      } else {
         this.xpReward = 5;
         this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6.0D);
      }

   }

   public static boolean checkHoglinSpawnRules(EntityType<Hoglin> pHoglin, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom) {
      return !pLevel.getBlockState(pPos.below()).is(Blocks.NETHER_WART_BLOCK);
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
      if (pLevel.getRandom().nextFloat() < 0.2F) {
         this.setBaby(true);
      }

      return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
   }

   public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
      return !this.isPersistenceRequired();
   }

   public float getWalkTargetValue(BlockPos pPos, LevelReader pLevel) {
      if (HoglinAi.isPosNearNearestRepellent(this, pPos)) {
         return -1.0F;
      } else {
         return pLevel.getBlockState(pPos.below()).is(Blocks.CRIMSON_NYLIUM) ? 10.0F : 0.0F;
      }
   }

   protected Vector3f getPassengerAttachmentPoint(Entity pEntity, EntityDimensions pDimensions, float pScale) {
      return new Vector3f(0.0F, pDimensions.height + 0.09375F * pScale, 0.0F);
   }

   public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      InteractionResult interactionresult = super.mobInteract(pPlayer, pHand);
      if (interactionresult.consumesAction()) {
         this.setPersistenceRequired();
      }

      return interactionresult;
   }

   public void handleEntityEvent(byte pId) {
      if (pId == 4) {
         this.attackAnimationRemainingTicks = 10;
         this.playSound(SoundEvents.HOGLIN_ATTACK, 1.0F, this.getVoicePitch());
      } else {
         super.handleEntityEvent(pId);
      }

   }

   public int getAttackAnimationRemainingTicks() {
      return this.attackAnimationRemainingTicks;
   }

   public boolean shouldDropExperience() {
      return true;
   }

   public int getExperienceReward() {
      return this.xpReward;
   }

   private void finishConversion(ServerLevel pServerLevel) {
      Zoglin zoglin = this.convertTo(EntityType.ZOGLIN, true);
      if (zoglin != null) {
         zoglin.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
      }

   }

   public boolean isFood(ItemStack pStack) {
      return pStack.is(Items.CRIMSON_FUNGUS);
   }

   public boolean isAdult() {
      return !this.isBaby();
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_IMMUNE_TO_ZOMBIFICATION, false);
   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      if (this.isImmuneToZombification()) {
         pCompound.putBoolean("IsImmuneToZombification", true);
      }

      pCompound.putInt("TimeInOverworld", this.timeInOverworld);
      if (this.cannotBeHunted) {
         pCompound.putBoolean("CannotBeHunted", true);
      }

   }

   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.setImmuneToZombification(pCompound.getBoolean("IsImmuneToZombification"));
      this.timeInOverworld = pCompound.getInt("TimeInOverworld");
      this.setCannotBeHunted(pCompound.getBoolean("CannotBeHunted"));
   }

   public void setImmuneToZombification(boolean pImmuneToZombification) {
      this.getEntityData().set(DATA_IMMUNE_TO_ZOMBIFICATION, pImmuneToZombification);
   }

   private boolean isImmuneToZombification() {
      return this.getEntityData().get(DATA_IMMUNE_TO_ZOMBIFICATION);
   }

   public boolean isConverting() {
      return !this.level().dimensionType().piglinSafe() && !this.isImmuneToZombification() && !this.isNoAi();
   }

   private void setCannotBeHunted(boolean pCannotBeHunted) {
      this.cannotBeHunted = pCannotBeHunted;
   }

   public boolean canBeHunted() {
      return this.isAdult() && !this.cannotBeHunted;
   }

   @Nullable
   public AgeableMob getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
      Hoglin hoglin = EntityType.HOGLIN.create(pLevel);
      if (hoglin != null) {
         hoglin.setPersistenceRequired();
      }

      return hoglin;
   }

   public boolean canFallInLove() {
      return !HoglinAi.isPacified(this) && super.canFallInLove();
   }

   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   protected SoundEvent getAmbientSound() {
      return this.level().isClientSide ? null : HoglinAi.getSoundForCurrentActivity(this).orElse((SoundEvent)null);
   }

   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.HOGLIN_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.HOGLIN_DEATH;
   }

   protected SoundEvent getSwimSound() {
      return SoundEvents.HOSTILE_SWIM;
   }

   protected SoundEvent getSwimSplashSound() {
      return SoundEvents.HOSTILE_SPLASH;
   }

   protected void playStepSound(BlockPos pPos, BlockState pBlock) {
      this.playSound(SoundEvents.HOGLIN_STEP, 0.15F, 1.0F);
   }

   protected void playSoundEvent(SoundEvent pSoundEvent) {
      this.playSound(pSoundEvent, this.getSoundVolume(), this.getVoicePitch());
   }

   protected void sendDebugPackets() {
      super.sendDebugPackets();
      DebugPackets.sendEntityBrain(this);
   }
}