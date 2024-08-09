package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GustSeedParticle extends NoRenderParticle {
   GustSeedParticle(ClientLevel p_312399_, double p_312363_, double p_309505_, double p_311805_) {
      super(p_312399_, p_312363_, p_309505_, p_311805_, 0.0D, 0.0D, 0.0D);
      this.lifetime = 7;
   }

   public void tick() {
      for(int i = 0; i < 3; ++i) {
         double d0 = this.x + (this.random.nextDouble() - this.random.nextDouble()) * 4.0D;
         double d1 = this.y + (this.random.nextDouble() - this.random.nextDouble()) * 4.0D;
         double d2 = this.z + (this.random.nextDouble() - this.random.nextDouble()) * 4.0D;
         this.level.addParticle(ParticleTypes.GUST, d0, d1, d2, (double)((float)this.age / (float)this.lifetime), 0.0D, 0.0D);
      }

      if (this.age++ == this.lifetime) {
         this.remove();
      }

   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      public Particle createParticle(SimpleParticleType p_309959_, ClientLevel p_312995_, double p_310097_, double p_313201_, double p_310511_, double p_310468_, double p_310282_, double p_311555_) {
         return new GustSeedParticle(p_312995_, p_310097_, p_313201_, p_310511_);
      }
   }
}