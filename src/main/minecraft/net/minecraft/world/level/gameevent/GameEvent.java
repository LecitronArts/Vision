package net.minecraft.world.level.gameevent;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class GameEvent {
   public static final GameEvent BLOCK_ACTIVATE = register("block_activate");
   public static final GameEvent BLOCK_ATTACH = register("block_attach");
   public static final GameEvent BLOCK_CHANGE = register("block_change");
   public static final GameEvent BLOCK_CLOSE = register("block_close");
   public static final GameEvent BLOCK_DEACTIVATE = register("block_deactivate");
   public static final GameEvent BLOCK_DESTROY = register("block_destroy");
   public static final GameEvent BLOCK_DETACH = register("block_detach");
   public static final GameEvent BLOCK_OPEN = register("block_open");
   public static final GameEvent BLOCK_PLACE = register("block_place");
   public static final GameEvent CONTAINER_CLOSE = register("container_close");
   public static final GameEvent CONTAINER_OPEN = register("container_open");
   public static final GameEvent DRINK = register("drink");
   public static final GameEvent EAT = register("eat");
   public static final GameEvent ELYTRA_GLIDE = register("elytra_glide");
   public static final GameEvent ENTITY_DAMAGE = register("entity_damage");
   public static final GameEvent ENTITY_DIE = register("entity_die");
   public static final GameEvent ENTITY_DISMOUNT = register("entity_dismount");
   public static final GameEvent ENTITY_INTERACT = register("entity_interact");
   public static final GameEvent ENTITY_MOUNT = register("entity_mount");
   public static final GameEvent ENTITY_PLACE = register("entity_place");
   public static final GameEvent ENTITY_ACTION = register("entity_action");
   public static final GameEvent EQUIP = register("equip");
   public static final GameEvent EXPLODE = register("explode");
   public static final GameEvent FLAP = register("flap");
   public static final GameEvent FLUID_PICKUP = register("fluid_pickup");
   public static final GameEvent FLUID_PLACE = register("fluid_place");
   public static final GameEvent HIT_GROUND = register("hit_ground");
   public static final GameEvent INSTRUMENT_PLAY = register("instrument_play");
   public static final GameEvent ITEM_INTERACT_FINISH = register("item_interact_finish");
   public static final GameEvent ITEM_INTERACT_START = register("item_interact_start");
   public static final GameEvent JUKEBOX_PLAY = register("jukebox_play", 10);
   public static final GameEvent JUKEBOX_STOP_PLAY = register("jukebox_stop_play", 10);
   public static final GameEvent LIGHTNING_STRIKE = register("lightning_strike");
   public static final GameEvent NOTE_BLOCK_PLAY = register("note_block_play");
   public static final GameEvent PRIME_FUSE = register("prime_fuse");
   public static final GameEvent PROJECTILE_LAND = register("projectile_land");
   public static final GameEvent PROJECTILE_SHOOT = register("projectile_shoot");
   public static final GameEvent SCULK_SENSOR_TENDRILS_CLICKING = register("sculk_sensor_tendrils_clicking");
   public static final GameEvent SHEAR = register("shear");
   public static final GameEvent SHRIEK = register("shriek", 32);
   public static final GameEvent SPLASH = register("splash");
   public static final GameEvent STEP = register("step");
   public static final GameEvent SWIM = register("swim");
   public static final GameEvent TELEPORT = register("teleport");
   public static final GameEvent UNEQUIP = register("unequip");
   public static final GameEvent RESONATE_1 = register("resonate_1");
   public static final GameEvent RESONATE_2 = register("resonate_2");
   public static final GameEvent RESONATE_3 = register("resonate_3");
   public static final GameEvent RESONATE_4 = register("resonate_4");
   public static final GameEvent RESONATE_5 = register("resonate_5");
   public static final GameEvent RESONATE_6 = register("resonate_6");
   public static final GameEvent RESONATE_7 = register("resonate_7");
   public static final GameEvent RESONATE_8 = register("resonate_8");
   public static final GameEvent RESONATE_9 = register("resonate_9");
   public static final GameEvent RESONATE_10 = register("resonate_10");
   public static final GameEvent RESONATE_11 = register("resonate_11");
   public static final GameEvent RESONATE_12 = register("resonate_12");
   public static final GameEvent RESONATE_13 = register("resonate_13");
   public static final GameEvent RESONATE_14 = register("resonate_14");
   public static final GameEvent RESONATE_15 = register("resonate_15");
   public static final int DEFAULT_NOTIFICATION_RADIUS = 16;
   private final int notificationRadius;
   private final Holder.Reference<GameEvent> builtInRegistryHolder = BuiltInRegistries.GAME_EVENT.createIntrusiveHolder(this);

   public GameEvent(int pNotificationRadius) {
      this.notificationRadius = pNotificationRadius;
   }

   public int getNotificationRadius() {
      return this.notificationRadius;
   }

   private static GameEvent register(String pName) {
      return register(pName, 16);
   }

   private static GameEvent register(String pName, int pNotificationRadius) {
      return Registry.register(BuiltInRegistries.GAME_EVENT, pName, new GameEvent(pNotificationRadius));
   }

   public String toString() {
      return "Game Event{ " + this.builtInRegistryHolder().key().location() + " , " + this.notificationRadius + "}";
   }

   /** @deprecated */
   @Deprecated
   public Holder.Reference<GameEvent> builtInRegistryHolder() {
      return this.builtInRegistryHolder;
   }

   public boolean is(TagKey<GameEvent> pEventTag) {
      return this.builtInRegistryHolder.is(pEventTag);
   }

   public static record Context(@Nullable Entity sourceEntity, @Nullable BlockState affectedState) {
      public static GameEvent.Context of(@Nullable Entity pSourceEntity) {
         return new GameEvent.Context(pSourceEntity, (BlockState)null);
      }

      public static GameEvent.Context of(@Nullable BlockState pAffectedState) {
         return new GameEvent.Context((Entity)null, pAffectedState);
      }

      public static GameEvent.Context of(@Nullable Entity pSourceEntity, @Nullable BlockState pAffectedState) {
         return new GameEvent.Context(pSourceEntity, pAffectedState);
      }
   }

   public static final class ListenerInfo implements Comparable<GameEvent.ListenerInfo> {
      private final GameEvent gameEvent;
      private final Vec3 source;
      private final GameEvent.Context context;
      private final GameEventListener recipient;
      private final double distanceToRecipient;

      public ListenerInfo(GameEvent pGameEvent, Vec3 pSource, GameEvent.Context pContext, GameEventListener pRecipient, Vec3 pRecipientPosition) {
         this.gameEvent = pGameEvent;
         this.source = pSource;
         this.context = pContext;
         this.recipient = pRecipient;
         this.distanceToRecipient = pSource.distanceToSqr(pRecipientPosition);
      }

      public int compareTo(GameEvent.ListenerInfo pOther) {
         return Double.compare(this.distanceToRecipient, pOther.distanceToRecipient);
      }

      public GameEvent gameEvent() {
         return this.gameEvent;
      }

      public Vec3 source() {
         return this.source;
      }

      public GameEvent.Context context() {
         return this.context;
      }

      public GameEventListener recipient() {
         return this.recipient;
      }
   }
}