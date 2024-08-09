package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityVariantPredicate<V> {
   private final Function<Entity, Optional<V>> getter;
   private final EntitySubPredicate.Type type;

   public static <V> EntityVariantPredicate<V> create(Registry<V> pRegistry, Function<Entity, Optional<V>> pGetter) {
      return new EntityVariantPredicate<>(pRegistry.byNameCodec(), pGetter);
   }

   public static <V> EntityVariantPredicate<V> create(Codec<V> pVariantCodec, Function<Entity, Optional<V>> pGetter) {
      return new EntityVariantPredicate<>(pVariantCodec, pGetter);
   }

   private EntityVariantPredicate(Codec<V> pVariantCodec, Function<Entity, Optional<V>> pGetter) {
      this.getter = pGetter;
      MapCodec<EntityVariantPredicate.SubPredicate<V>> mapcodec = RecordCodecBuilder.mapCodec((p_296131_) -> {
         return p_296131_.group(pVariantCodec.fieldOf("variant").forGetter(EntityVariantPredicate.SubPredicate::variant)).apply(p_296131_, this::createPredicate);
      });
      this.type = new EntitySubPredicate.Type(mapcodec);
   }

   public EntitySubPredicate.Type type() {
      return this.type;
   }

   public EntityVariantPredicate.SubPredicate<V> createPredicate(V p_219097_) {
      return new EntityVariantPredicate.SubPredicate<>(this.type, this.getter, p_219097_);
   }

   public static record SubPredicate<V>(EntitySubPredicate.Type type, Function<Entity, Optional<V>> getter, V variant) implements EntitySubPredicate {
      public boolean matches(Entity pEntity, ServerLevel pLevel, @Nullable Vec3 pPosition) {
         return this.getter.apply(pEntity).filter((p_298468_) -> {
            return p_298468_.equals(this.variant);
         }).isPresent();
      }

      public EntitySubPredicate.Type type() {
         return this.type;
      }
   }
}