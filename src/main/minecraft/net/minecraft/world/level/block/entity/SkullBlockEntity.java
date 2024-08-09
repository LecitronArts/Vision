package net.minecraft.world.level.block.entity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Services;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;

public class SkullBlockEntity extends BlockEntity {
   public static final String TAG_SKULL_OWNER = "SkullOwner";
   public static final String TAG_NOTE_BLOCK_SOUND = "note_block_sound";
   @Nullable
   private static Executor mainThreadExecutor;
   @Nullable
   private static LoadingCache<String, CompletableFuture<Optional<GameProfile>>> profileCache;
   private static final Executor CHECKED_MAIN_THREAD_EXECUTOR = (p_296964_) -> {
      Executor executor = mainThreadExecutor;
      if (executor != null) {
         executor.execute(p_296964_);
      }

   };
   @Nullable
   private GameProfile owner;
   @Nullable
   private ResourceLocation noteBlockSound;
   private int animationTickCount;
   private boolean isAnimating;

   public SkullBlockEntity(BlockPos pPos, BlockState pBlockState) {
      super(BlockEntityType.SKULL, pPos, pBlockState);
   }

   public static void setup(final Services pServices, Executor pMainThreadExecutor) {
      mainThreadExecutor = pMainThreadExecutor;
      final BooleanSupplier booleansupplier = () -> {
         return profileCache == null;
      };
      profileCache = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(10L)).maximumSize(256L).build(new CacheLoader<String, CompletableFuture<Optional<GameProfile>>>() {
         public CompletableFuture<Optional<GameProfile>> load(String p_312380_) {
            return booleansupplier.getAsBoolean() ? CompletableFuture.completedFuture(Optional.empty()) : SkullBlockEntity.loadProfile(p_312380_, pServices, booleansupplier);
         }
      });
   }

   public static void clear() {
      mainThreadExecutor = null;
      profileCache = null;
   }

   static CompletableFuture<Optional<GameProfile>> loadProfile(String pProfileName, Services pServices, BooleanSupplier pHasCache) {
      return pServices.profileCache().getAsync(pProfileName).thenApplyAsync((p_309306_) -> {
         if (p_309306_.isPresent() && !pHasCache.getAsBoolean()) {
            UUID uuid = p_309306_.get().getId();
            ProfileResult profileresult = pServices.sessionService().fetchProfile(uuid, true);
            return profileresult != null ? Optional.ofNullable(profileresult.profile()) : p_309306_;
         } else {
            return Optional.empty();
         }
      }, Util.backgroundExecutor());
   }

   protected void saveAdditional(CompoundTag pTag) {
      super.saveAdditional(pTag);
      if (this.owner != null) {
         CompoundTag compoundtag = new CompoundTag();
         NbtUtils.writeGameProfile(compoundtag, this.owner);
         pTag.put("SkullOwner", compoundtag);
      }

      if (this.noteBlockSound != null) {
         pTag.putString("note_block_sound", this.noteBlockSound.toString());
      }

   }

   public void load(CompoundTag pTag) {
      super.load(pTag);
      if (pTag.contains("SkullOwner", 10)) {
         this.setOwner(NbtUtils.readGameProfile(pTag.getCompound("SkullOwner")));
      } else if (pTag.contains("ExtraType", 8)) {
         String s = pTag.getString("ExtraType");
         if (!StringUtil.isNullOrEmpty(s)) {
            this.setOwner(new GameProfile(Util.NIL_UUID, s));
         }
      }

      if (pTag.contains("note_block_sound", 8)) {
         this.noteBlockSound = ResourceLocation.tryParse(pTag.getString("note_block_sound"));
      }

   }

   public static void animation(Level pLevel, BlockPos pPos, BlockState pState, SkullBlockEntity pBlockEntity) {
      if (pState.hasProperty(SkullBlock.POWERED) && pState.getValue(SkullBlock.POWERED)) {
         pBlockEntity.isAnimating = true;
         ++pBlockEntity.animationTickCount;
      } else {
         pBlockEntity.isAnimating = false;
      }

   }

   public float getAnimation(float pPartialTick) {
      return this.isAnimating ? (float)this.animationTickCount + pPartialTick : (float)this.animationTickCount;
   }

   @Nullable
   public GameProfile getOwnerProfile() {
      return this.owner;
   }

   @Nullable
   public ResourceLocation getNoteBlockSound() {
      return this.noteBlockSound;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   public void setOwner(@Nullable GameProfile pOwner) {
      synchronized(this) {
         this.owner = pOwner;
      }

      this.updateOwnerProfile();
   }

   private void updateOwnerProfile() {
      if (this.owner != null && !Util.isBlank(this.owner.getName()) && !hasTextures(this.owner)) {
         fetchGameProfile(this.owner.getName()).thenAcceptAsync((p_296965_) -> {
            this.owner = p_296965_.orElse(this.owner);
            this.setChanged();
         }, CHECKED_MAIN_THREAD_EXECUTOR);
      } else {
         this.setChanged();
      }
   }

   @Nullable
   public static GameProfile getOrResolveGameProfile(CompoundTag pTag) {
      if (pTag.contains("SkullOwner", 10)) {
         return NbtUtils.readGameProfile(pTag.getCompound("SkullOwner"));
      } else {
         if (pTag.contains("SkullOwner", 8)) {
            String s = pTag.getString("SkullOwner");
            if (!Util.isBlank(s)) {
               pTag.remove("SkullOwner");
               resolveGameProfile(pTag, s);
            }
         }

         return null;
      }
   }

   public static void resolveGameProfile(CompoundTag pTag) {
      String s = pTag.getString("SkullOwner");
      if (!Util.isBlank(s)) {
         resolveGameProfile(pTag, s);
      }

   }

   private static void resolveGameProfile(CompoundTag pTag, String pProfileName) {
      fetchGameProfile(pProfileName).thenAccept((p_296969_) -> {
         pTag.put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), p_296969_.orElse(new GameProfile(Util.NIL_UUID, pProfileName))));
      });
   }

   private static CompletableFuture<Optional<GameProfile>> fetchGameProfile(String pProfileName) {
      LoadingCache<String, CompletableFuture<Optional<GameProfile>>> loadingcache = profileCache;
      return loadingcache != null && Player.isValidUsername(pProfileName) ? loadingcache.getUnchecked(pProfileName) : CompletableFuture.completedFuture(Optional.empty());
   }

   private static boolean hasTextures(GameProfile pProfile) {
      return pProfile.getProperties().containsKey("textures");
   }
}