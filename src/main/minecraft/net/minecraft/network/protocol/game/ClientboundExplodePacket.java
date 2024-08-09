package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

public class ClientboundExplodePacket implements Packet<ClientGamePacketListener> {
   private final double x;
   private final double y;
   private final double z;
   private final float power;
   private final List<BlockPos> toBlow;
   private final float knockbackX;
   private final float knockbackY;
   private final float knockbackZ;
   private final ParticleOptions smallExplosionParticles;
   private final ParticleOptions largeExplosionParticles;
   private final Explosion.BlockInteraction blockInteraction;
   private final SoundEvent explosionSound;

   public ClientboundExplodePacket(double pX, double pY, double pZ, float pPower, List<BlockPos> pToBlow, @Nullable Vec3 pKnockback, Explosion.BlockInteraction pBlockInteraction, ParticleOptions pSmallExplosionParticles, ParticleOptions pLargeExplosionParticles, SoundEvent pExplosionSound) {
      this.x = pX;
      this.y = pY;
      this.z = pZ;
      this.power = pPower;
      this.toBlow = Lists.newArrayList(pToBlow);
      this.explosionSound = pExplosionSound;
      if (pKnockback != null) {
         this.knockbackX = (float)pKnockback.x;
         this.knockbackY = (float)pKnockback.y;
         this.knockbackZ = (float)pKnockback.z;
      } else {
         this.knockbackX = 0.0F;
         this.knockbackY = 0.0F;
         this.knockbackZ = 0.0F;
      }

      this.blockInteraction = pBlockInteraction;
      this.smallExplosionParticles = pSmallExplosionParticles;
      this.largeExplosionParticles = pLargeExplosionParticles;
   }

   public ClientboundExplodePacket(FriendlyByteBuf pBuffer) {
      this.x = pBuffer.readDouble();
      this.y = pBuffer.readDouble();
      this.z = pBuffer.readDouble();
      this.power = pBuffer.readFloat();
      int i = Mth.floor(this.x);
      int j = Mth.floor(this.y);
      int k = Mth.floor(this.z);
      this.toBlow = pBuffer.readList((p_178850_) -> {
         int l = p_178850_.readByte() + i;
         int i1 = p_178850_.readByte() + j;
         int j1 = p_178850_.readByte() + k;
         return new BlockPos(l, i1, j1);
      });
      this.knockbackX = pBuffer.readFloat();
      this.knockbackY = pBuffer.readFloat();
      this.knockbackZ = pBuffer.readFloat();
      this.blockInteraction = pBuffer.readEnum(Explosion.BlockInteraction.class);
      this.smallExplosionParticles = this.readParticle(pBuffer, pBuffer.readById(BuiltInRegistries.PARTICLE_TYPE));
      this.largeExplosionParticles = this.readParticle(pBuffer, pBuffer.readById(BuiltInRegistries.PARTICLE_TYPE));
      this.explosionSound = SoundEvent.readFromNetwork(pBuffer);
   }

   public void writeParticle(FriendlyByteBuf p_311139_, ParticleOptions p_309825_) {
      p_311139_.writeId(BuiltInRegistries.PARTICLE_TYPE, p_309825_.getType());
      p_309825_.writeToNetwork(p_311139_);
   }

   private <T extends ParticleOptions> T readParticle(FriendlyByteBuf pParticleType, ParticleType<T> p_311877_) {
      return p_311877_.getDeserializer().fromNetwork(p_311877_, pParticleType);
   }

   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeDouble(this.x);
      pBuffer.writeDouble(this.y);
      pBuffer.writeDouble(this.z);
      pBuffer.writeFloat(this.power);
      int i = Mth.floor(this.x);
      int j = Mth.floor(this.y);
      int k = Mth.floor(this.z);
      pBuffer.writeCollection(this.toBlow, (p_296399_, p_296400_) -> {
         int l = p_296400_.getX() - i;
         int i1 = p_296400_.getY() - j;
         int j1 = p_296400_.getZ() - k;
         p_296399_.writeByte(l);
         p_296399_.writeByte(i1);
         p_296399_.writeByte(j1);
      });
      pBuffer.writeFloat(this.knockbackX);
      pBuffer.writeFloat(this.knockbackY);
      pBuffer.writeFloat(this.knockbackZ);
      pBuffer.writeEnum(this.blockInteraction);
      this.writeParticle(pBuffer, this.smallExplosionParticles);
      this.writeParticle(pBuffer, this.largeExplosionParticles);
      this.explosionSound.writeToNetwork(pBuffer);
   }

   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleExplosion(this);
   }

   public float getKnockbackX() {
      return this.knockbackX;
   }

   public float getKnockbackY() {
      return this.knockbackY;
   }

   public float getKnockbackZ() {
      return this.knockbackZ;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public float getPower() {
      return this.power;
   }

   public List<BlockPos> getToBlow() {
      return this.toBlow;
   }

   public Explosion.BlockInteraction getBlockInteraction() {
      return this.blockInteraction;
   }

   public ParticleOptions getSmallExplosionParticles() {
      return this.smallExplosionParticles;
   }

   public ParticleOptions getLargeExplosionParticles() {
      return this.largeExplosionParticles;
   }

   public SoundEvent getExplosionSound() {
      return this.explosionSound;
   }
}