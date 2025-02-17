package net.minecraft.world.entity.monster;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;

public class Evoker extends SpellcasterIllager {
   @Nullable
   private Sheep wololoTarget;

   public Evoker(EntityType<? extends Evoker> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
      this.xpReward = 10;
   }

   protected void registerGoals() {
      super.registerGoals();
      this.goalSelector.addGoal(0, new FloatGoal(this));
      this.goalSelector.addGoal(1, new Evoker.EvokerCastingSpellGoal());
      this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 8.0F, 0.6D, 1.0D));
      this.goalSelector.addGoal(4, new Evoker.EvokerSummonSpellGoal());
      this.goalSelector.addGoal(5, new Evoker.EvokerAttackSpellGoal());
      this.goalSelector.addGoal(6, new Evoker.EvokerWololoSpellGoal());
      this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6D));
      this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F));
      this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
      this.targetSelector.addGoal(1, (new HurtByTargetGoal(this, Raider.class)).setAlertOthers());
      this.targetSelector.addGoal(2, (new NearestAttackableTargetGoal<>(this, Player.class, true)).setUnseenMemoryTicks(300));
      this.targetSelector.addGoal(3, (new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false)).setUnseenMemoryTicks(300));
      this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, false));
   }

   public static AttributeSupplier.Builder createAttributes() {
      return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.5D).add(Attributes.FOLLOW_RANGE, 12.0D).add(Attributes.MAX_HEALTH, 24.0D);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
   }

   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
   }

   public SoundEvent getCelebrateSound() {
      return SoundEvents.EVOKER_CELEBRATE;
   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
   }

   public boolean isAlliedTo(Entity pEntity) {
      if (pEntity == null) {
         return false;
      } else if (pEntity == this) {
         return true;
      } else if (super.isAlliedTo(pEntity)) {
         return true;
      } else if (pEntity instanceof Vex) {
         return this.isAlliedTo(((Vex)pEntity).getOwner());
      } else if (pEntity instanceof LivingEntity && ((LivingEntity)pEntity).getMobType() == MobType.ILLAGER) {
         return this.getTeam() == null && pEntity.getTeam() == null;
      } else {
         return false;
      }
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.EVOKER_AMBIENT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.EVOKER_DEATH;
   }

   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.EVOKER_HURT;
   }

   void setWololoTarget(@Nullable Sheep pWololoTarget) {
      this.wololoTarget = pWololoTarget;
   }

   @Nullable
   Sheep getWololoTarget() {
      return this.wololoTarget;
   }

   protected SoundEvent getCastingSoundEvent() {
      return SoundEvents.EVOKER_CAST_SPELL;
   }

   public void applyRaidBuffs(int pWave, boolean pUnusedFalse) {
   }

   class EvokerAttackSpellGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
      protected int getCastingTime() {
         return 40;
      }

      protected int getCastingInterval() {
         return 100;
      }

      protected void performSpellCasting() {
         LivingEntity livingentity = Evoker.this.getTarget();
         double d0 = Math.min(livingentity.getY(), Evoker.this.getY());
         double d1 = Math.max(livingentity.getY(), Evoker.this.getY()) + 1.0D;
         float f = (float)Mth.atan2(livingentity.getZ() - Evoker.this.getZ(), livingentity.getX() - Evoker.this.getX());
         if (Evoker.this.distanceToSqr(livingentity) < 9.0D) {
            for(int i = 0; i < 5; ++i) {
               float f1 = f + (float)i * (float)Math.PI * 0.4F;
               this.createSpellEntity(Evoker.this.getX() + (double)Mth.cos(f1) * 1.5D, Evoker.this.getZ() + (double)Mth.sin(f1) * 1.5D, d0, d1, f1, 0);
            }

            for(int k = 0; k < 8; ++k) {
               float f2 = f + (float)k * (float)Math.PI * 2.0F / 8.0F + 1.2566371F;
               this.createSpellEntity(Evoker.this.getX() + (double)Mth.cos(f2) * 2.5D, Evoker.this.getZ() + (double)Mth.sin(f2) * 2.5D, d0, d1, f2, 3);
            }
         } else {
            for(int l = 0; l < 16; ++l) {
               double d2 = 1.25D * (double)(l + 1);
               int j = 1 * l;
               this.createSpellEntity(Evoker.this.getX() + (double)Mth.cos(f) * d2, Evoker.this.getZ() + (double)Mth.sin(f) * d2, d0, d1, f, j);
            }
         }

      }

      private void createSpellEntity(double pX, double pZ, double pMinY, double pMaxY, float pYRot, int pWarmupDelay) {
         BlockPos blockpos = BlockPos.containing(pX, pMaxY, pZ);
         boolean flag = false;
         double d0 = 0.0D;

         do {
            BlockPos blockpos1 = blockpos.below();
            BlockState blockstate = Evoker.this.level().getBlockState(blockpos1);
            if (blockstate.isFaceSturdy(Evoker.this.level(), blockpos1, Direction.UP)) {
               if (!Evoker.this.level().isEmptyBlock(blockpos)) {
                  BlockState blockstate1 = Evoker.this.level().getBlockState(blockpos);
                  VoxelShape voxelshape = blockstate1.getCollisionShape(Evoker.this.level(), blockpos);
                  if (!voxelshape.isEmpty()) {
                     d0 = voxelshape.max(Direction.Axis.Y);
                  }
               }

               flag = true;
               break;
            }

            blockpos = blockpos.below();
         } while(blockpos.getY() >= Mth.floor(pMinY) - 1);

         if (flag) {
            Evoker.this.level().addFreshEntity(new EvokerFangs(Evoker.this.level(), pX, (double)blockpos.getY() + d0, pZ, pYRot, pWarmupDelay, Evoker.this));
            Evoker.this.level().gameEvent(GameEvent.ENTITY_PLACE, new Vec3(pX, (double)blockpos.getY() + d0, pZ), GameEvent.Context.of(Evoker.this));
         }

      }

      protected SoundEvent getSpellPrepareSound() {
         return SoundEvents.EVOKER_PREPARE_ATTACK;
      }

      protected SpellcasterIllager.IllagerSpell getSpell() {
         return SpellcasterIllager.IllagerSpell.FANGS;
      }
   }

   class EvokerCastingSpellGoal extends SpellcasterIllager.SpellcasterCastingSpellGoal {
      public void tick() {
         if (Evoker.this.getTarget() != null) {
            Evoker.this.getLookControl().setLookAt(Evoker.this.getTarget(), (float)Evoker.this.getMaxHeadYRot(), (float)Evoker.this.getMaxHeadXRot());
         } else if (Evoker.this.getWololoTarget() != null) {
            Evoker.this.getLookControl().setLookAt(Evoker.this.getWololoTarget(), (float)Evoker.this.getMaxHeadYRot(), (float)Evoker.this.getMaxHeadXRot());
         }

      }
   }

   class EvokerSummonSpellGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
      private final TargetingConditions vexCountTargeting = TargetingConditions.forNonCombat().range(16.0D).ignoreLineOfSight().ignoreInvisibilityTesting();

      public boolean canUse() {
         if (!super.canUse()) {
            return false;
         } else {
            int i = Evoker.this.level().getNearbyEntities(Vex.class, this.vexCountTargeting, Evoker.this, Evoker.this.getBoundingBox().inflate(16.0D)).size();
            return Evoker.this.random.nextInt(8) + 1 > i;
         }
      }

      protected int getCastingTime() {
         return 100;
      }

      protected int getCastingInterval() {
         return 340;
      }

      protected void performSpellCasting() {
         ServerLevel serverlevel = (ServerLevel)Evoker.this.level();
         PlayerTeam playerteam = Evoker.this.getTeam();

         for(int i = 0; i < 3; ++i) {
            BlockPos blockpos = Evoker.this.blockPosition().offset(-2 + Evoker.this.random.nextInt(5), 1, -2 + Evoker.this.random.nextInt(5));
            Vex vex = EntityType.VEX.create(Evoker.this.level());
            if (vex != null) {
               vex.moveTo(blockpos, 0.0F, 0.0F);
               vex.finalizeSpawn(serverlevel, Evoker.this.level().getCurrentDifficultyAt(blockpos), MobSpawnType.MOB_SUMMONED, (SpawnGroupData)null, (CompoundTag)null);
               vex.setOwner(Evoker.this);
               vex.setBoundOrigin(blockpos);
               vex.setLimitedLife(20 * (30 + Evoker.this.random.nextInt(90)));
               if (playerteam != null) {
                  serverlevel.getScoreboard().addPlayerToTeam(vex.getScoreboardName(), playerteam);
               }

               serverlevel.addFreshEntityWithPassengers(vex);
               serverlevel.gameEvent(GameEvent.ENTITY_PLACE, blockpos, GameEvent.Context.of(Evoker.this));
            }
         }

      }

      protected SoundEvent getSpellPrepareSound() {
         return SoundEvents.EVOKER_PREPARE_SUMMON;
      }

      protected SpellcasterIllager.IllagerSpell getSpell() {
         return SpellcasterIllager.IllagerSpell.SUMMON_VEX;
      }
   }

   public class EvokerWololoSpellGoal extends SpellcasterIllager.SpellcasterUseSpellGoal {
      private final TargetingConditions wololoTargeting = TargetingConditions.forNonCombat().range(16.0D).selector((p_32710_) -> {
         return ((Sheep)p_32710_).getColor() == DyeColor.BLUE;
      });

      public boolean canUse() {
         if (Evoker.this.getTarget() != null) {
            return false;
         } else if (Evoker.this.isCastingSpell()) {
            return false;
         } else if (Evoker.this.tickCount < this.nextAttackTickCount) {
            return false;
         } else if (!Evoker.this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
            return false;
         } else {
            List<Sheep> list = Evoker.this.level().getNearbyEntities(Sheep.class, this.wololoTargeting, Evoker.this, Evoker.this.getBoundingBox().inflate(16.0D, 4.0D, 16.0D));
            if (list.isEmpty()) {
               return false;
            } else {
               Evoker.this.setWololoTarget(list.get(Evoker.this.random.nextInt(list.size())));
               return true;
            }
         }
      }

      public boolean canContinueToUse() {
         return Evoker.this.getWololoTarget() != null && this.attackWarmupDelay > 0;
      }

      public void stop() {
         super.stop();
         Evoker.this.setWololoTarget((Sheep)null);
      }

      protected void performSpellCasting() {
         Sheep sheep = Evoker.this.getWololoTarget();
         if (sheep != null && sheep.isAlive()) {
            sheep.setColor(DyeColor.RED);
         }

      }

      protected int getCastWarmupTime() {
         return 40;
      }

      protected int getCastingTime() {
         return 60;
      }

      protected int getCastingInterval() {
         return 140;
      }

      protected SoundEvent getSpellPrepareSound() {
         return SoundEvents.EVOKER_PREPARE_WOLOLO;
      }

      protected SpellcasterIllager.IllagerSpell getSpell() {
         return SpellcasterIllager.IllagerSpell.WOLOLO;
      }
   }
}