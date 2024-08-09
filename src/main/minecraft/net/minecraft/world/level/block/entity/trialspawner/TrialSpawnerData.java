package net.minecraft.world.level.block.entity.trialspawner;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;

public class TrialSpawnerData {
   public static final String TAG_SPAWN_DATA = "spawn_data";
   private static final String TAG_NEXT_MOB_SPAWNS_AT = "next_mob_spawns_at";
   public static MapCodec<TrialSpawnerData> MAP_CODEC = RecordCodecBuilder.mapCodec((p_313188_) -> {
      return p_313188_.group(UUIDUtil.CODEC_SET.optionalFieldOf("registered_players", Sets.newHashSet()).forGetter((p_309580_) -> {
         return p_309580_.detectedPlayers;
      }), UUIDUtil.CODEC_SET.optionalFieldOf("current_mobs", Sets.newHashSet()).forGetter((p_311034_) -> {
         return p_311034_.currentMobs;
      }), Codec.LONG.optionalFieldOf("cooldown_ends_at", Long.valueOf(0L)).forGetter((p_309685_) -> {
         return p_309685_.cooldownEndsAt;
      }), Codec.LONG.optionalFieldOf("next_mob_spawns_at", Long.valueOf(0L)).forGetter((p_310876_) -> {
         return p_310876_.nextMobSpawnsAt;
      }), Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("total_mobs_spawned", 0).forGetter((p_309745_) -> {
         return p_309745_.totalMobsSpawned;
      }), SpawnData.CODEC.optionalFieldOf("spawn_data").forGetter((p_312904_) -> {
         return p_312904_.nextSpawnData;
      }), ResourceLocation.CODEC.optionalFieldOf("ejecting_loot_table").forGetter((p_310765_) -> {
         return p_310765_.ejectingLootTable;
      })).apply(p_313188_, TrialSpawnerData::new);
   });
   protected final Set<UUID> detectedPlayers = new HashSet<>();
   protected final Set<UUID> currentMobs = new HashSet<>();
   protected long cooldownEndsAt;
   protected long nextMobSpawnsAt;
   protected int totalMobsSpawned;
   protected Optional<SpawnData> nextSpawnData;
   protected Optional<ResourceLocation> ejectingLootTable;
   protected SimpleWeightedRandomList<SpawnData> spawnPotentials;
   @Nullable
   protected Entity displayEntity;
   protected double spin;
   protected double oSpin;

   public TrialSpawnerData() {
      this(Collections.emptySet(), Collections.emptySet(), 0L, 0L, 0, Optional.empty(), Optional.empty());
   }

   public TrialSpawnerData(Set<UUID> p_312543_, Set<UUID> p_311274_, long p_312908_, long p_311373_, int p_311452_, Optional<SpawnData> p_311258_, Optional<ResourceLocation> p_312612_) {
      this.detectedPlayers.addAll(p_312543_);
      this.currentMobs.addAll(p_311274_);
      this.cooldownEndsAt = p_312908_;
      this.nextMobSpawnsAt = p_311373_;
      this.totalMobsSpawned = p_311452_;
      this.nextSpawnData = p_311258_;
      this.ejectingLootTable = p_312612_;
   }

   public void setSpawnPotentialsFromConfig(TrialSpawnerConfig pConfig) {
      SimpleWeightedRandomList<SpawnData> simpleweightedrandomlist = pConfig.spawnPotentialsDefinition();
      if (simpleweightedrandomlist.isEmpty()) {
         this.spawnPotentials = SimpleWeightedRandomList.single(this.nextSpawnData.orElseGet(SpawnData::new));
      } else {
         this.spawnPotentials = simpleweightedrandomlist;
      }

   }

   public void reset() {
      this.detectedPlayers.clear();
      this.totalMobsSpawned = 0;
      this.nextMobSpawnsAt = 0L;
      this.cooldownEndsAt = 0L;
      this.currentMobs.clear();
   }

   public boolean hasMobToSpawn() {
      boolean flag = this.nextSpawnData.isPresent() && this.nextSpawnData.get().getEntityToSpawn().contains("id", 8);
      return flag || !this.spawnPotentials.isEmpty();
   }

   public boolean hasFinishedSpawningAllMobs(TrialSpawnerConfig pConfig, int pPlayers) {
      return this.totalMobsSpawned >= pConfig.calculateTargetTotalMobs(pPlayers);
   }

   public boolean haveAllCurrentMobsDied() {
      return this.currentMobs.isEmpty();
   }

   public boolean isReadyToSpawnNextMob(ServerLevel pLevel, TrialSpawnerConfig pConfig, int pPlayers) {
      return pLevel.getGameTime() >= this.nextMobSpawnsAt && this.currentMobs.size() < pConfig.calculateTargetSimultaneousMobs(pPlayers);
   }

   public int countAdditionalPlayers(BlockPos pPos) {
      if (this.detectedPlayers.isEmpty()) {
         Util.logAndPauseIfInIde("Trial Spawner at " + pPos + " has no detected players");
      }

      return Math.max(0, this.detectedPlayers.size() - 1);
   }

   public void tryDetectPlayers(ServerLevel pLevel, BlockPos pPos, PlayerDetector pPlaterDetector, int pPlayers) {
      List<UUID> list = pPlaterDetector.detect(pLevel, pPos, pPlayers);
      boolean flag = this.detectedPlayers.addAll(list);
      if (flag) {
         this.nextMobSpawnsAt = Math.max(pLevel.getGameTime() + 40L, this.nextMobSpawnsAt);
         pLevel.levelEvent(3013, pPos, this.detectedPlayers.size());
      }

   }

   public boolean isReadyToOpenShutter(ServerLevel pLevel, TrialSpawnerConfig pConfig, float pDelay) {
      long i = this.cooldownEndsAt - (long)pConfig.targetCooldownLength();
      return (float)pLevel.getGameTime() >= (float)i + pDelay;
   }

   public boolean isReadyToEjectItems(ServerLevel pLevel, TrialSpawnerConfig pConfig, float pTimeBetweenEjections) {
      long i = this.cooldownEndsAt - (long)pConfig.targetCooldownLength();
      return (float)(pLevel.getGameTime() - i) % pTimeBetweenEjections == 0.0F;
   }

   public boolean isCooldownFinished(ServerLevel pLevel) {
      return pLevel.getGameTime() >= this.cooldownEndsAt;
   }

   public void setEntityId(TrialSpawner pSpawner, RandomSource pRandom, EntityType<?> pEntityType) {
      this.getOrCreateNextSpawnData(pSpawner, pRandom).getEntityToSpawn().putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(pEntityType).toString());
   }

   protected SpawnData getOrCreateNextSpawnData(TrialSpawner pSpawner, RandomSource pRandom) {
      if (this.nextSpawnData.isPresent()) {
         return this.nextSpawnData.get();
      } else {
         this.nextSpawnData = Optional.of(this.spawnPotentials.getRandom(pRandom).map(WeightedEntry.Wrapper::getData).orElseGet(SpawnData::new));
         pSpawner.markUpdated();
         return this.nextSpawnData.get();
      }
   }

   @Nullable
   public Entity getOrCreateDisplayEntity(TrialSpawner pSpawner, Level pLevel, TrialSpawnerState pSpawnerState) {
      if (pSpawner.canSpawnInLevel(pLevel) && pSpawnerState.hasSpinningMob()) {
         if (this.displayEntity == null) {
            CompoundTag compoundtag = this.getOrCreateNextSpawnData(pSpawner, pLevel.getRandom()).getEntityToSpawn();
            if (compoundtag.contains("id", 8)) {
               this.displayEntity = EntityType.loadEntityRecursive(compoundtag, pLevel, Function.identity());
            }
         }

         return this.displayEntity;
      } else {
         return null;
      }
   }

   public CompoundTag getUpdateTag(TrialSpawnerState pSpawnerState) {
      CompoundTag compoundtag = new CompoundTag();
      if (pSpawnerState == TrialSpawnerState.ACTIVE) {
         compoundtag.putLong("next_mob_spawns_at", this.nextMobSpawnsAt);
      }

      this.nextSpawnData.ifPresent((p_312008_) -> {
         compoundtag.put("spawn_data", SpawnData.CODEC.encodeStart(NbtOps.INSTANCE, p_312008_).result().orElseThrow(() -> {
            return new IllegalStateException("Invalid SpawnData");
         }));
      });
      return compoundtag;
   }

   public double getSpin() {
      return this.spin;
   }

   public double getOSpin() {
      return this.oSpin;
   }
}