package net.minecraft.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public final class EntityTypeTags {
   public static final TagKey<EntityType<?>> SKELETONS = create("skeletons");
   public static final TagKey<EntityType<?>> ZOMBIES = create("zombies");
   public static final TagKey<EntityType<?>> RAIDERS = create("raiders");
   public static final TagKey<EntityType<?>> UNDEAD = create("undead");
   public static final TagKey<EntityType<?>> BEEHIVE_INHABITORS = create("beehive_inhabitors");
   public static final TagKey<EntityType<?>> ARROWS = create("arrows");
   public static final TagKey<EntityType<?>> IMPACT_PROJECTILES = create("impact_projectiles");
   public static final TagKey<EntityType<?>> POWDER_SNOW_WALKABLE_MOBS = create("powder_snow_walkable_mobs");
   public static final TagKey<EntityType<?>> AXOLOTL_ALWAYS_HOSTILES = create("axolotl_always_hostiles");
   public static final TagKey<EntityType<?>> AXOLOTL_HUNT_TARGETS = create("axolotl_hunt_targets");
   public static final TagKey<EntityType<?>> FREEZE_IMMUNE_ENTITY_TYPES = create("freeze_immune_entity_types");
   public static final TagKey<EntityType<?>> FREEZE_HURTS_EXTRA_TYPES = create("freeze_hurts_extra_types");
   public static final TagKey<EntityType<?>> CAN_BREATHE_UNDER_WATER = create("can_breathe_under_water");
   public static final TagKey<EntityType<?>> FROG_FOOD = create("frog_food");
   public static final TagKey<EntityType<?>> FALL_DAMAGE_IMMUNE = create("fall_damage_immune");
   public static final TagKey<EntityType<?>> DISMOUNTS_UNDERWATER = create("dismounts_underwater");
   public static final TagKey<EntityType<?>> NON_CONTROLLING_RIDER = create("non_controlling_rider");
   public static final TagKey<EntityType<?>> DEFLECTS_ARROWS = create("deflects_arrows");
   public static final TagKey<EntityType<?>> DEFLECTS_TRIDENTS = create("deflects_tridents");
   public static final TagKey<EntityType<?>> CAN_TURN_IN_BOATS = create("can_turn_in_boats");

   private EntityTypeTags() {
   }

   private static TagKey<EntityType<?>> create(String pName) {
      return TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation(pName));
   }
}