package net.minecraft.world.entity.monster.warden;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.warden.SonicBoom;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.EntityPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.joml.Vector3f;
import org.slf4j.Logger;

public class Warden extends Monster implements VibrationSystem {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int VIBRATION_COOLDOWN_TICKS = 40;
   private static final int TIME_TO_USE_MELEE_UNTIL_SONIC_BOOM = 200;
   private static final int MAX_HEALTH = 500;
   private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.3F;
   private static final float KNOCKBACK_RESISTANCE = 1.0F;
   private static final float ATTACK_KNOCKBACK = 1.5F;
   private static final int ATTACK_DAMAGE = 30;
   private static final EntityDataAccessor<Integer> CLIENT_ANGER_LEVEL = SynchedEntityData.defineId(Warden.class, EntityDataSerializers.INT);
   private static final int DARKNESS_DISPLAY_LIMIT = 200;
   private static final int DARKNESS_DURATION = 260;
   private static final int DARKNESS_RADIUS = 20;
   private static final int DARKNESS_INTERVAL = 120;
   private static final int ANGERMANAGEMENT_TICK_DELAY = 20;
   private static final int DEFAULT_ANGER = 35;
   private static final int PROJECTILE_ANGER = 10;
   private static final int ON_HURT_ANGER_BOOST = 20;
   private static final int RECENT_PROJECTILE_TICK_THRESHOLD = 100;
   private static final int TOUCH_COOLDOWN_TICKS = 20;
   private static final int DIGGING_PARTICLES_AMOUNT = 30;
   private static final float DIGGING_PARTICLES_DURATION = 4.5F;
   private static final float DIGGING_PARTICLES_OFFSET = 0.7F;
   private static final int PROJECTILE_ANGER_DISTANCE = 30;
   private int tendrilAnimation;
   private int tendrilAnimationO;
   private int heartAnimation;
   private int heartAnimationO;
   public AnimationState roarAnimationState = new AnimationState();
   public AnimationState sniffAnimationState = new AnimationState();
   public AnimationState emergeAnimationState = new AnimationState();
   public AnimationState diggingAnimationState = new AnimationState();
   public AnimationState attackAnimationState = new AnimationState();
   public AnimationState sonicBoomAnimationState = new AnimationState();
   private final DynamicGameEventListener<VibrationSystem.Listener> dynamicGameEventListener;
   private final VibrationSystem.User vibrationUser;
   private VibrationSystem.Data vibrationData;
   AngerManagement angerManagement = new AngerManagement(this::canTargetEntity, Collections.emptyList());

   public Warden(EntityType<? extends Monster> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.vibrationUser = new Warden.VibrationUser();
      this.vibrationData = new VibrationSystem.Data();
      this.dynamicGameEventListener = new DynamicGameEventListener<>(new VibrationSystem.Listener(this));
      this.xpReward = 5;
      this.getNavigation().setCanFloat(true);
      this.setPathfindingMalus(BlockPathTypes.UNPASSABLE_RAIL, 0.0F);
      this.setPathfindingMalus(BlockPathTypes.DAMAGE_OTHER, 8.0F);
      this.setPathfindingMalus(BlockPathTypes.POWDER_SNOW, 8.0F);
      this.setPathfindingMalus(BlockPathTypes.LAVA, 8.0F);
      this.setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
      this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
   }

   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return new ClientboundAddEntityPacket(this, this.hasPose(Pose.EMERGING) ? 1 : 0);
   }

   public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
      super.recreateFromPacket(pPacket);
      if (pPacket.getData() == 1) {
         this.setPose(Pose.EMERGING);
      }

   }

   public boolean checkSpawnObstruction(LevelReader pLevel) {
      return super.checkSpawnObstruction(pLevel) && pLevel.noCollision(this, this.getType().getDimensions().makeBoundingBox(this.position()));
   }

   public float getWalkTargetValue(BlockPos pPos, LevelReader pLevel) {
      return 0.0F;
   }

   public boolean isInvulnerableTo(DamageSource pSource) {
      return this.isDiggingOrEmerging() && !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY) ? true : super.isInvulnerableTo(pSource);
   }

   boolean isDiggingOrEmerging() {
      return this.hasPose(Pose.DIGGING) || this.hasPose(Pose.EMERGING);
   }

   protected boolean canRide(Entity pVehicle) {
      return false;
   }

   public boolean canDisableShield() {
      return true;
   }

   protected float nextStep() {
      return this.moveDist + 0.55F;
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 500.0D).add(Attributes.MOVEMENT_SPEED, (double)0.3F).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).add(Attributes.ATTACK_KNOCKBACK, 1.5D).add(Attributes.ATTACK_DAMAGE, 30.0D);
   }

   public boolean dampensVibrations() {
      return true;
   }

   protected float getSoundVolume() {
      return 4.0F;
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return !this.hasPose(Pose.ROARING) && !this.isDiggingOrEmerging() ? this.getAngerLevel().getAmbientSound() : null;
   }

   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.WARDEN_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.WARDEN_DEATH;
   }

   protected void playStepSound(BlockPos pPos, BlockState pState) {
      this.playSound(SoundEvents.WARDEN_STEP, 10.0F, 1.0F);
   }

   public boolean doHurtTarget(Entity pEntity) {
      this.level().broadcastEntityEvent(this, (byte)4);
      this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 10.0F, this.getVoicePitch());
      SonicBoom.setCooldown(this, 40);
      return super.doHurtTarget(pEntity);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(CLIENT_ANGER_LEVEL, 0);
   }

   public int getClientAngerLevel() {
      return this.entityData.get(CLIENT_ANGER_LEVEL);
   }

   private void syncClientAngerLevel() {
      this.entityData.set(CLIENT_ANGER_LEVEL, this.getActiveAnger());
   }

   public void tick() {
      Level level = this.level();
      if (level instanceof ServerLevel serverlevel) {
         VibrationSystem.Ticker.tick(serverlevel, this.vibrationData, this.vibrationUser);
         if (this.isPersistenceRequired() || this.requiresCustomPersistence()) {
            WardenAi.setDigCooldown(this);
         }
      }

      super.tick();
      if (this.level().isClientSide()) {
         if (this.tickCount % this.getHeartBeatDelay() == 0) {
            this.heartAnimation = 10;
            if (!this.isSilent()) {
               this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.WARDEN_HEARTBEAT, this.getSoundSource(), 5.0F, this.getVoicePitch(), false);
            }
         }

         this.tendrilAnimationO = this.tendrilAnimation;
         if (this.tendrilAnimation > 0) {
            --this.tendrilAnimation;
         }

         this.heartAnimationO = this.heartAnimation;
         if (this.heartAnimation > 0) {
            --this.heartAnimation;
         }

         switch (this.getPose()) {
            case EMERGING:
               this.clientDiggingParticles(this.emergeAnimationState);
               break;
            case DIGGING:
               this.clientDiggingParticles(this.diggingAnimationState);
         }
      }

   }

   protected void customServerAiStep() {
      ServerLevel serverlevel = (ServerLevel)this.level();
      serverlevel.getProfiler().push("wardenBrain");
      this.getBrain().tick(serverlevel, this);
      this.level().getProfiler().pop();
      super.customServerAiStep();
      if ((this.tickCount + this.getId()) % 120 == 0) {
         applyDarknessAround(serverlevel, this.position(), this, 20);
      }

      if (this.tickCount % 20 == 0) {
         this.angerManagement.tick(serverlevel, this::canTargetEntity);
         this.syncClientAngerLevel();
      }

      WardenAi.updateActivity(this);
   }

   public void handleEntityEvent(byte pId) {
      if (pId == 4) {
         this.roarAnimationState.stop();
         this.attackAnimationState.start(this.tickCount);
      } else if (pId == 61) {
         this.tendrilAnimation = 10;
      } else if (pId == 62) {
         this.sonicBoomAnimationState.start(this.tickCount);
      } else {
         super.handleEntityEvent(pId);
      }

   }

   private int getHeartBeatDelay() {
      float f = (float)this.getClientAngerLevel() / (float)AngerLevel.ANGRY.getMinimumAnger();
      return 40 - Mth.floor(Mth.clamp(f, 0.0F, 1.0F) * 30.0F);
   }

   public float getTendrilAnimation(float pPartialTick) {
      return Mth.lerp(pPartialTick, (float)this.tendrilAnimationO, (float)this.tendrilAnimation) / 10.0F;
   }

   public float getHeartAnimation(float pPartialTick) {
      return Mth.lerp(pPartialTick, (float)this.heartAnimationO, (float)this.heartAnimation) / 10.0F;
   }

   private void clientDiggingParticles(AnimationState pAnimationState) {
      if ((float)pAnimationState.getAccumulatedTime() < 4500.0F) {
         RandomSource randomsource = this.getRandom();
         BlockState blockstate = this.getBlockStateOn();
         if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
            for(int i = 0; i < 30; ++i) {
               double d0 = this.getX() + (double)Mth.randomBetween(randomsource, -0.7F, 0.7F);
               double d1 = this.getY();
               double d2 = this.getZ() + (double)Mth.randomBetween(randomsource, -0.7F, 0.7F);
               this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, blockstate), d0, d1, d2, 0.0D, 0.0D, 0.0D);
            }
         }
      }

   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
      if (DATA_POSE.equals(pKey)) {
         switch (this.getPose()) {
            case EMERGING:
               this.emergeAnimationState.start(this.tickCount);
               break;
            case DIGGING:
               this.diggingAnimationState.start(this.tickCount);
               break;
            case ROARING:
               this.roarAnimationState.start(this.tickCount);
               break;
            case SNIFFING:
               this.sniffAnimationState.start(this.tickCount);
         }
      }

      super.onSyncedDataUpdated(pKey);
   }

   public boolean ignoreExplosion(Explosion pExplosion) {
      return this.isDiggingOrEmerging();
   }

   protected Brain<?> makeBrain(Dynamic<?> pDynamic) {
      return WardenAi.makeBrain(this, pDynamic);
   }

   public Brain<Warden> getBrain() {
      return (Brain<Warden>)super.getBrain();
   }

   protected void sendDebugPackets() {
      super.sendDebugPackets();
      DebugPackets.sendEntityBrain(this);
   }

   public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> pListenerConsumer) {
      Level level = this.level();
      if (level instanceof ServerLevel serverlevel) {
         pListenerConsumer.accept(this.dynamicGameEventListener, serverlevel);
      }

   }

   @Contract("null->false")
   public boolean canTargetEntity(@Nullable Entity p_219386_) {
      if (p_219386_ instanceof LivingEntity livingentity) {
         if (this.level() == p_219386_.level() && EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(p_219386_) && !this.isAlliedTo(p_219386_) && livingentity.getType() != EntityType.ARMOR_STAND && livingentity.getType() != EntityType.WARDEN && !livingentity.isInvulnerable() && !livingentity.isDeadOrDying() && this.level().getWorldBorder().isWithinBounds(livingentity.getBoundingBox())) {
            return true;
         }
      }

      return false;
   }

   public static void applyDarknessAround(ServerLevel pLevel, Vec3 pPos, @Nullable Entity pSource, int pRadius) {
      MobEffectInstance mobeffectinstance = new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false);
      MobEffectUtil.addEffectToPlayersAround(pLevel, pSource, pPos, (double)pRadius, mobeffectinstance, 200);
   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      AngerManagement.codec(this::canTargetEntity).encodeStart(NbtOps.INSTANCE, this.angerManagement).resultOrPartial(LOGGER::error).ifPresent((p_219437_) -> {
         pCompound.put("anger", p_219437_);
      });
      VibrationSystem.Data.CODEC.encodeStart(NbtOps.INSTANCE, this.vibrationData).resultOrPartial(LOGGER::error).ifPresent((p_219418_) -> {
         pCompound.put("listener", p_219418_);
      });
   }

   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      if (pCompound.contains("anger")) {
         AngerManagement.codec(this::canTargetEntity).parse(new Dynamic<>(NbtOps.INSTANCE, pCompound.get("anger"))).resultOrPartial(LOGGER::error).ifPresent((p_219394_) -> {
            this.angerManagement = p_219394_;
         });
         this.syncClientAngerLevel();
      }

      if (pCompound.contains("listener", 10)) {
         VibrationSystem.Data.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, pCompound.getCompound("listener"))).resultOrPartial(LOGGER::error).ifPresent((p_281093_) -> {
            this.vibrationData = p_281093_;
         });
      }

   }

   private void playListeningSound() {
      if (!this.hasPose(Pose.ROARING)) {
         this.playSound(this.getAngerLevel().getListeningSound(), 10.0F, this.getVoicePitch());
      }

   }

   public AngerLevel getAngerLevel() {
      return AngerLevel.byAnger(this.getActiveAnger());
   }

   private int getActiveAnger() {
      return this.angerManagement.getActiveAnger(this.getTarget());
   }

   public void clearAnger(Entity pEntity) {
      this.angerManagement.clearAnger(pEntity);
   }

   public void increaseAngerAt(@Nullable Entity pEntity) {
      this.increaseAngerAt(pEntity, 35, true);
   }

   @VisibleForTesting
   public void increaseAngerAt(@Nullable Entity pEntity, int pOffset, boolean pPlayListeningSound) {
      if (!this.isNoAi() && this.canTargetEntity(pEntity)) {
         WardenAi.setDigCooldown(this);
         boolean flag = !(this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse((LivingEntity)null) instanceof Player);
         int i = this.angerManagement.increaseAnger(pEntity, pOffset);
         if (pEntity instanceof Player && flag && AngerLevel.byAnger(i).isAngry()) {
            this.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
         }

         if (pPlayListeningSound) {
            this.playListeningSound();
         }
      }

   }

   public Optional<LivingEntity> getEntityAngryAt() {
      return this.getAngerLevel().isAngry() ? this.angerManagement.getActiveEntity() : Optional.empty();
   }

   @Nullable
   public LivingEntity getTarget() {
      return this.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse((LivingEntity)null);
   }

   public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
      return false;
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
      this.getBrain().setMemoryWithExpiry(MemoryModuleType.DIG_COOLDOWN, Unit.INSTANCE, 1200L);
      if (pReason == MobSpawnType.TRIGGERED) {
         this.setPose(Pose.EMERGING);
         this.getBrain().setMemoryWithExpiry(MemoryModuleType.IS_EMERGING, Unit.INSTANCE, (long)WardenAi.EMERGE_DURATION);
         this.playSound(SoundEvents.WARDEN_AGITATED, 5.0F, 1.0F);
      }

      return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
   }

   public boolean hurt(DamageSource pSource, float pAmount) {
      boolean flag = super.hurt(pSource, pAmount);
      if (!this.level().isClientSide && !this.isNoAi() && !this.isDiggingOrEmerging()) {
         Entity entity = pSource.getEntity();
         this.increaseAngerAt(entity, AngerLevel.ANGRY.getMinimumAnger() + 20, false);
         if (this.brain.getMemory(MemoryModuleType.ATTACK_TARGET).isEmpty() && entity instanceof LivingEntity) {
            LivingEntity livingentity = (LivingEntity)entity;
            if (!pSource.isIndirect() || this.closerThan(livingentity, 5.0D)) {
               this.setAttackTarget(livingentity);
            }
         }
      }

      return flag;
   }

   public void setAttackTarget(LivingEntity pAttackTarget) {
      this.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
      this.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, pAttackTarget);
      this.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
      SonicBoom.setCooldown(this, 200);
   }

   public EntityDimensions getDimensions(Pose pPose) {
      EntityDimensions entitydimensions = super.getDimensions(pPose);
      return this.isDiggingOrEmerging() ? EntityDimensions.fixed(entitydimensions.width, 1.0F) : entitydimensions;
   }

   public boolean isPushable() {
      return !this.isDiggingOrEmerging() && super.isPushable();
   }

   protected void doPush(Entity pEntity) {
      if (!this.isNoAi() && !this.getBrain().hasMemoryValue(MemoryModuleType.TOUCH_COOLDOWN)) {
         this.getBrain().setMemoryWithExpiry(MemoryModuleType.TOUCH_COOLDOWN, Unit.INSTANCE, 20L);
         this.increaseAngerAt(pEntity);
         WardenAi.setDisturbanceLocation(this, pEntity.blockPosition());
      }

      super.doPush(pEntity);
   }

   @VisibleForTesting
   public AngerManagement getAngerManagement() {
      return this.angerManagement;
   }

   protected PathNavigation createNavigation(Level pLevel) {
      return new GroundPathNavigation(this, pLevel) {
         protected PathFinder createPathFinder(int p_219479_) {
            this.nodeEvaluator = new WalkNodeEvaluator();
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, p_219479_) {
               protected float distance(Node p_219486_, Node p_219487_) {
                  return p_219486_.distanceToXZ(p_219487_);
               }
            };
         }
      };
   }

   protected Vector3f getPassengerAttachmentPoint(Entity pEntity, EntityDimensions pDimensions, float pScale) {
      return new Vector3f(0.0F, pDimensions.height + 0.25F * pScale, 0.0F);
   }

   public VibrationSystem.Data getVibrationData() {
      return this.vibrationData;
   }

   public VibrationSystem.User getVibrationUser() {
      return this.vibrationUser;
   }

   class VibrationUser implements VibrationSystem.User {
      private static final int GAME_EVENT_LISTENER_RANGE = 16;
      private final PositionSource positionSource = new EntityPositionSource(Warden.this, Warden.this.getEyeHeight());

      public int getListenerRadius() {
         return 16;
      }

      public PositionSource getPositionSource() {
         return this.positionSource;
      }

      public TagKey<GameEvent> getListenableEvents() {
         return GameEventTags.WARDEN_CAN_LISTEN;
      }

      public boolean canTriggerAvoidVibration() {
         return true;
      }

      public boolean canReceiveVibration(ServerLevel p_282574_, BlockPos p_282323_, GameEvent p_283003_, GameEvent.Context p_282515_) {
         if (!Warden.this.isNoAi() && !Warden.this.isDeadOrDying() && !Warden.this.getBrain().hasMemoryValue(MemoryModuleType.VIBRATION_COOLDOWN) && !Warden.this.isDiggingOrEmerging() && p_282574_.getWorldBorder().isWithinBounds(p_282323_)) {
            Entity entity = p_282515_.sourceEntity();
            if (entity instanceof LivingEntity) {
               LivingEntity livingentity = (LivingEntity)entity;
               if (!Warden.this.canTargetEntity(livingentity)) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }

      public void onReceiveVibration(ServerLevel p_281325_, BlockPos p_282386_, GameEvent p_282261_, @Nullable Entity p_281438_, @Nullable Entity p_282582_, float p_283699_) {
         if (!Warden.this.isDeadOrDying()) {
            Warden.this.brain.setMemoryWithExpiry(MemoryModuleType.VIBRATION_COOLDOWN, Unit.INSTANCE, 40L);
            p_281325_.broadcastEntityEvent(Warden.this, (byte)61);
            Warden.this.playSound(SoundEvents.WARDEN_TENDRIL_CLICKS, 5.0F, Warden.this.getVoicePitch());
            BlockPos blockpos = p_282386_;
            if (p_282582_ != null) {
               if (Warden.this.closerThan(p_282582_, 30.0D)) {
                  if (Warden.this.getBrain().hasMemoryValue(MemoryModuleType.RECENT_PROJECTILE)) {
                     if (Warden.this.canTargetEntity(p_282582_)) {
                        blockpos = p_282582_.blockPosition();
                     }

                     Warden.this.increaseAngerAt(p_282582_);
                  } else {
                     Warden.this.increaseAngerAt(p_282582_, 10, true);
                  }
               }

               Warden.this.getBrain().setMemoryWithExpiry(MemoryModuleType.RECENT_PROJECTILE, Unit.INSTANCE, 100L);
            } else {
               Warden.this.increaseAngerAt(p_281438_);
            }

            if (!Warden.this.getAngerLevel().isAngry()) {
               Optional<LivingEntity> optional = Warden.this.angerManagement.getActiveEntity();
               if (p_282582_ != null || optional.isEmpty() || optional.get() == p_281438_) {
                  WardenAi.setDisturbanceLocation(Warden.this, blockpos);
               }
            }

         }
      }
   }
}