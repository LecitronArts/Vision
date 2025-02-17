package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.optifine.SmartAnimations;
import net.optifine.render.VertexBuilderWrapper;

public class SpriteCoordinateExpander extends VertexBuilderWrapper implements VertexConsumer {
   private final VertexConsumer delegate;
   private final TextureAtlasSprite sprite;

   public SpriteCoordinateExpander(VertexConsumer pDelegate, TextureAtlasSprite pSprite) {
      super(pDelegate);
      if (SmartAnimations.isActive()) {
         SmartAnimations.spriteRendered(pSprite);
      }

      this.delegate = pDelegate;
      this.sprite = pSprite;
   }

   public VertexConsumer vertex(double pX, double pY, double pZ) {
      this.delegate.vertex(pX, pY, pZ);
      return this;
   }

   public VertexConsumer color(int pRed, int pGreen, int pBlue, int pAlpha) {
      this.delegate.color(pRed, pGreen, pBlue, pAlpha);
      return this;
   }

   public VertexConsumer uv(float pU, float pV) {
      this.delegate.uv(this.sprite.getU(pU), this.sprite.getV(pV));
      return this;
   }

   public VertexConsumer overlayCoords(int pU, int pV) {
      this.delegate.overlayCoords(pU, pV);
      return this;
   }

   public VertexConsumer uv2(int pU, int pV) {
      this.delegate.uv2(pU, pV);
      return this;
   }

   public VertexConsumer normal(float pX, float pY, float pZ) {
      this.delegate.normal(pX, pY, pZ);
      return this;
   }

   public void endVertex() {
      this.delegate.endVertex();
   }

   public void defaultColor(int pDefaultR, int pDefaultG, int pDefaultB, int pDefaultA) {
      this.delegate.defaultColor(pDefaultR, pDefaultG, pDefaultB, pDefaultA);
   }

   public void unsetDefaultColor() {
      this.delegate.unsetDefaultColor();
   }

   public void vertex(float pX, float pY, float pZ, float pRed, float pGreen, float pBlue, float pAlpha, float pTexU, float pTexV, int pOverlayUV, int pLightmapUV, float pNormalX, float pNormalY, float pNormalZ) {
      this.delegate.vertex(pX, pY, pZ, pRed, pGreen, pBlue, pAlpha, this.sprite.getU(pTexU), this.sprite.getV(pTexV), pOverlayUV, pLightmapUV, pNormalX, pNormalY, pNormalZ);
   }

   public boolean canAddVertexFast() {
      return this.delegate.canAddVertexFast();
   }

   public void addVertexFast(float x, float y, float z, int color, float texU, float texV, int overlayUV, int lightmapUV, int normals) {
      float f = this.sprite.getU(texU);
      float f1 = this.sprite.getV(texV);
      this.delegate.addVertexFast(x, y, z, color, f, f1, overlayUV, lightmapUV, normals);
   }
}