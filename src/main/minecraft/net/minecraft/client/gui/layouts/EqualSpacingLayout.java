package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EqualSpacingLayout extends AbstractLayout {
   private final EqualSpacingLayout.Orientation orientation;
   private final List<EqualSpacingLayout.ChildContainer> children = new ArrayList<>();
   private final LayoutSettings defaultChildLayoutSettings = LayoutSettings.defaults();

   public EqualSpacingLayout(int pWidth, int pHeight, EqualSpacingLayout.Orientation pOrientation) {
      this(0, 0, pWidth, pHeight, pOrientation);
   }

   public EqualSpacingLayout(int pX, int pY, int pWidth, int pHeight, EqualSpacingLayout.Orientation pOrientation) {
      super(pX, pY, pWidth, pHeight);
      this.orientation = pOrientation;
   }

   public void arrangeElements() {
      super.arrangeElements();
      if (!this.children.isEmpty()) {
         int i = 0;
         int j = this.orientation.getSecondaryLength(this);

         for(EqualSpacingLayout.ChildContainer equalspacinglayout$childcontainer : this.children) {
            i += this.orientation.getPrimaryLength(equalspacinglayout$childcontainer);
            j = Math.max(j, this.orientation.getSecondaryLength(equalspacinglayout$childcontainer));
         }

         int k = this.orientation.getPrimaryLength(this) - i;
         int l = this.orientation.getPrimaryPosition(this);
         Iterator<EqualSpacingLayout.ChildContainer> iterator = this.children.iterator();
         EqualSpacingLayout.ChildContainer equalspacinglayout$childcontainer1 = iterator.next();
         this.orientation.setPrimaryPosition(equalspacinglayout$childcontainer1, l);
         l += this.orientation.getPrimaryLength(equalspacinglayout$childcontainer1);
         EqualSpacingLayout.ChildContainer equalspacinglayout$childcontainer2;
         if (this.children.size() >= 2) {
            for(Divisor divisor = new Divisor(k, this.children.size() - 1); divisor.hasNext(); l += this.orientation.getPrimaryLength(equalspacinglayout$childcontainer2)) {
               l += divisor.nextInt();
               equalspacinglayout$childcontainer2 = iterator.next();
               this.orientation.setPrimaryPosition(equalspacinglayout$childcontainer2, l);
            }
         }

         int i1 = this.orientation.getSecondaryPosition(this);

         for(EqualSpacingLayout.ChildContainer equalspacinglayout$childcontainer3 : this.children) {
            this.orientation.setSecondaryPosition(equalspacinglayout$childcontainer3, i1, j);
         }

         switch (this.orientation) {
            case HORIZONTAL:
               this.height = j;
               break;
            case VERTICAL:
               this.width = j;
         }

      }
   }

   public void visitChildren(Consumer<LayoutElement> pVisitor) {
      this.children.forEach((p_298818_) -> {
         pVisitor.accept(p_298818_.child);
      });
   }

   public LayoutSettings newChildLayoutSettings() {
      return this.defaultChildLayoutSettings.copy();
   }

   public LayoutSettings defaultChildLayoutSetting() {
      return this.defaultChildLayoutSettings;
   }

   public <T extends LayoutElement> T addChild(T pChild) {
      return this.addChild(pChild, this.newChildLayoutSettings());
   }

   public <T extends LayoutElement> T addChild(T pChild, LayoutSettings pLayoutSettings) {
      this.children.add(new EqualSpacingLayout.ChildContainer(pChild, pLayoutSettings));
      return pChild;
   }

   public <T extends LayoutElement> T addChild(T pChild, Consumer<LayoutSettings> pLayoutSettingsCreator) {
      return this.addChild(pChild, Util.make(this.newChildLayoutSettings(), pLayoutSettingsCreator));
   }

   @OnlyIn(Dist.CLIENT)
   static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
      protected ChildContainer(LayoutElement p_298955_, LayoutSettings p_298136_) {
         super(p_298955_, p_298136_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Orientation {
      HORIZONTAL,
      VERTICAL;

      int getPrimaryLength(LayoutElement pElement) {
         int i;
         switch (this) {
            case HORIZONTAL:
               i = pElement.getWidth();
               break;
            case VERTICAL:
               i = pElement.getHeight();
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         return i;
      }

      int getPrimaryLength(EqualSpacingLayout.ChildContainer pContainer) {
         int i;
         switch (this) {
            case HORIZONTAL:
               i = pContainer.getWidth();
               break;
            case VERTICAL:
               i = pContainer.getHeight();
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         return i;
      }

      int getSecondaryLength(LayoutElement pElement) {
         int i;
         switch (this) {
            case HORIZONTAL:
               i = pElement.getHeight();
               break;
            case VERTICAL:
               i = pElement.getWidth();
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         return i;
      }

      int getSecondaryLength(EqualSpacingLayout.ChildContainer pContainer) {
         int i;
         switch (this) {
            case HORIZONTAL:
               i = pContainer.getHeight();
               break;
            case VERTICAL:
               i = pContainer.getWidth();
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         return i;
      }

      void setPrimaryPosition(EqualSpacingLayout.ChildContainer pContainer, int pPosition) {
         switch (this) {
            case HORIZONTAL:
               pContainer.setX(pPosition, pContainer.getWidth());
               break;
            case VERTICAL:
               pContainer.setY(pPosition, pContainer.getHeight());
         }

      }

      void setSecondaryPosition(EqualSpacingLayout.ChildContainer pContainer, int pPosition, int pLength) {
         switch (this) {
            case HORIZONTAL:
               pContainer.setY(pPosition, pLength);
               break;
            case VERTICAL:
               pContainer.setX(pPosition, pLength);
         }

      }

      int getPrimaryPosition(LayoutElement pElement) {
         int i;
         switch (this) {
            case HORIZONTAL:
               i = pElement.getX();
               break;
            case VERTICAL:
               i = pElement.getY();
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         return i;
      }

      int getSecondaryPosition(LayoutElement pElement) {
         int i;
         switch (this) {
            case HORIZONTAL:
               i = pElement.getY();
               break;
            case VERTICAL:
               i = pElement.getX();
               break;
            default:
               throw new IncompatibleClassChangeError();
         }

         return i;
      }
   }
}