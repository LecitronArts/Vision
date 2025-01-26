package net.minecraft.client.gui.components;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import com.ibm.icu.text.BreakIterator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import icyllis.modernui.core.UndoManager;
import icyllis.modernui.core.UndoOwner;
import icyllis.modernui.mc.EditBoxEditAction;
import icyllis.modernui.mc.IModernEditBox;
import icyllis.modernui.mc.MuiModApi;
import icyllis.modernui.mc.text.TextLayout;
import icyllis.modernui.mc.text.TextLayoutEngine;
import icyllis.modernui.mc.text.VanillaTextWrapper;
import icyllis.modernui.text.method.WordIterator;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Unique;

@OnlyIn(Dist.CLIENT)
public class EditBox extends AbstractWidget implements Renderable, IModernEditBox {
   private static final WidgetSprites SPRITES = new WidgetSprites(new ResourceLocation("widget/text_field"), new ResourceLocation("widget/text_field_highlighted"));
   public static final int BACKWARDS = -1;
   public static final int FORWARDS = 1;
   private static final int CURSOR_INSERT_WIDTH = 1;
   private static final int CURSOR_INSERT_COLOR = -3092272;
   private static final String CURSOR_APPEND_CHARACTER = "_";
   public static final int DEFAULT_TEXT_COLOR = 14737632;
   private static final int CURSOR_BLINK_INTERVAL_MS = 300;
   private final Font font;
   private String value = "";
   private int maxLength = 32;
   private boolean bordered = true;
   private boolean canLoseFocus = true;
   private boolean isEditable = true;
   private int displayPos;
   private int cursorPos;
   private int highlightPos;
   private int textColor = 14737632;
   private int textColorUneditable = 7368816;
   private boolean viaFabricPlus$forbiddenCharactersUnlocked = false;
   @Nullable
   private String suggestion;
   @Nullable
   private Consumer<String> responder;
   private Predicate<String> filter = Objects::nonNull;
   private BiFunction<String, Integer, FormattedCharSequence> formatter = (p_94147_, p_94148_) -> {
      return FormattedCharSequence.forward(p_94147_, Style.EMPTY);
   };
   @Nullable
   private Component hint;
   private long focusedTime = Util.getMillis();

   private WordIterator modernUI_MC$wordIterator;
   private long modernUI_MC$lastInsertTextNanos;
   private final UndoManager modernUI_MC$undoManager = new UndoManager();

   public EditBox(Font pFont, int pWidth, int pHeight, Component pMessage) {
      this(pFont, 0, 0, pWidth, pHeight, pMessage);
   }

   public EditBox(Font pFont, int pX, int pY, int pWidth, int pHeight, Component pMessage) {
      this(pFont, pX, pY, pWidth, pHeight, (EditBox)null, pMessage);
   }

   public EditBox(Font pFont, int pX, int pY, int pWidth, int pHeight, @Nullable EditBox pEditBox, Component pMessage) {
      super(pX, pY, pWidth, pHeight, pMessage);
      this.font = pFont;
      if (pEditBox != null) {
         formatter = (s, i) -> new VanillaTextWrapper(s);
         this.setValue(pEditBox.getValue());
      }
      formatter = (s, i) -> new VanillaTextWrapper(s);
   }

   public void setResponder(Consumer<String> pResponder) {
      this.responder = pResponder;
   }

   public void setFormatter(BiFunction<String, Integer, FormattedCharSequence> pTextFormatter) {
      this.formatter = pTextFormatter;
   }

   protected MutableComponent createNarrationMessage() {
      Component component = this.getMessage();
      return Component.translatable("gui.narrate.editBox", component, this.value);
   }

   public void setValue(String pText) {
      if (this.filter.test(pText)) {
         if (pText.length() > this.maxLength) {
            if (modernUI_MC$undoManager.isInUndo()) {
               return;
            }
            if (value.isEmpty() && pText.isEmpty()) {
               return;
            }
            // we see this operation as Replace
            EditBoxEditAction edit = new EditBoxEditAction(
                    modernUI_MC$undoOwner(),
                    cursorPos,
                    /*oldText*/ value,
                    0,
                    /*newText*/ pText
            );
            modernUI_MC$addEdit(edit, false);
            this.value = pText.substring(0, this.maxLength);
         } else {
            if (modernUI_MC$undoManager.isInUndo()) {
               return;
            }
            if (value.isEmpty() && pText.isEmpty()) {
               return;
            }
            // we see this operation as Replace
            EditBoxEditAction edit = new EditBoxEditAction(
                    modernUI_MC$undoOwner(),
                    cursorPos,
                    /*oldText*/ value,
                    0,
                    /*newText*/ pText
            );
            modernUI_MC$addEdit(edit, false);
            this.value = pText;
         }

         this.moveCursorToEnd(false);
         this.setHighlightPos(this.cursorPos);
         this.onValueChange(pText);
      }
   }

   public String getValue() {
      return this.value;
   }

   public String getHighlighted() {
      int i = Math.min(this.cursorPos, this.highlightPos);
      int j = Math.max(this.cursorPos, this.highlightPos);
      return this.value.substring(i, j);
   }

   public void setFilter(Predicate<String> pValidator) {
      this.filter = pValidator;
   }

   public void insertText(String pTextToWrite) {
      int i = Math.min(this.cursorPos, this.highlightPos);
      int j = Math.max(this.cursorPos, this.highlightPos);
      int k = this.maxLength - this.value.length() - (i - j);
      if (k > 0) {

         String s = SharedConstants.filterText(pTextToWrite);
         if (this.viaFabricPlus$forbiddenCharactersUnlocked) {
            s = pTextToWrite;
         }
         int l = s.length();
         if (k < l) {
            if (Character.isHighSurrogate(s.charAt(k - 1))) {
               --k;
            }

            s = s.substring(0, k);
            l = k;
         }

         String s1 = (new StringBuilder(this.value)).replace(i, j, s).toString();
         if (this.filter.test(s1)) {
            if (modernUI_MC$undoManager.isInUndo()) {
               return;
            }
            String oldText = value.substring(i, j);
            if (oldText.isEmpty() && pTextToWrite.isEmpty()) {
               return;
            }
            EditBoxEditAction edit = new EditBoxEditAction(
                    modernUI_MC$undoOwner(),
                    cursorPos,
                    oldText,
                    i,
                    /*newText*/ pTextToWrite
            );
            final long nanos = Util.getNanos();
            final boolean mergeInsert;
            // Minecraft split IME batch commit and even a single code point into code units,
            // if two charTyped() occur at the same time (<= 3ms), try to merge (concat) them.
            if (modernUI_MC$lastInsertTextNanos >= nanos - 3_000_000) {
               mergeInsert = true;
            } else {
               modernUI_MC$lastInsertTextNanos = nanos;
               mergeInsert = false;
            }
            modernUI_MC$addEdit(edit, mergeInsert);
            this.value = s1;
            this.setCursorPosition(i + l);
            this.setHighlightPos(this.cursorPos);
            this.onValueChange(this.value);
         }
      }
   }

   private void onValueChange(String pNewText) {
      if (this.responder != null) {
         this.responder.accept(pNewText);
      }

   }

   private void deleteText(int pCount) {
      if (Screen.hasControlDown()) {
         this.deleteWords(pCount);
      } else {
         this.deleteChars(pCount);
      }

   }

   public void deleteWords(int pNum) {
      if (!this.value.isEmpty()) {
         if (this.highlightPos != this.cursorPos) {
            this.insertText("");
         } else {
            this.deleteCharsToPos(this.getWordPosition(pNum));
         }
      }
   }

   public void deleteChars(int pNum) {
      this.deleteCharsToPos(this.getCursorPos(pNum));
   }

   public void deleteCharsToPos(int pNum) {
      if (!this.value.isEmpty()) {
         if (this.highlightPos != this.cursorPos) {
            this.insertText("");
         } else {
            int i = Math.min(pNum, this.cursorPos);
            int j = Math.max(pNum, this.cursorPos);
            if (i != j) {
               String s = (new StringBuilder(this.value)).delete(i, j).toString();
               if (this.filter.test(s)) {
                  if (modernUI_MC$undoManager.isInUndo()) {
                     return;
                  }
                  String oldText = value.substring(i, j);
                  if (oldText.isEmpty()) {
                     return;
                  }
                  EditBoxEditAction edit = new EditBoxEditAction(
                          modernUI_MC$undoOwner(),
                          /*cursorPos*/ cursorPos,
                          oldText,
                          i,
                          ""
                  );
                  modernUI_MC$addEdit(edit, false);
                  this.value = s;
                  this.moveCursorTo(i, false);
               }
            }
         }
      }
   }
   public void modernUI_MC$addEdit(EditBoxEditAction edit, boolean mergeInsert) {
      final UndoManager mgr = modernUI_MC$undoManager;
      mgr.beginUpdate("addEdit");
      EditBoxEditAction lastEdit = mgr.getLastOperation(
              EditBoxEditAction.class,
              edit.getOwner(),
              UndoManager.MERGE_MODE_UNIQUE
      );
      if (lastEdit == null) {
         mgr.addOperation(edit, UndoManager.MERGE_MODE_NONE);
      } else if (!mergeInsert || !lastEdit.mergeInsertWith(edit)) {
         mgr.commitState(edit.getOwner());
         mgr.addOperation(edit, UndoManager.MERGE_MODE_NONE);
      }
      mgr.endUpdate();
   }

   public int getWordPosition(int pNumWords) {
      return this.getWordPosition(pNumWords, this.getCursorPosition());
   }

   private int getWordPosition(int pNumWords, int pPos) {
      return this.getWordPosition(pNumWords, pPos, true);
   }

   private int getWordPosition(int dir, int cursor, boolean pSkipConsecutiveSpaces) {
      if ((dir == -1 || dir == 1) && !value.startsWith("/")) {
         WordIterator wordIterator = modernUI_MC$wordIterator;
         if (wordIterator == null) {
            modernUI_MC$wordIterator = wordIterator = new WordIterator();
         }
         wordIterator.setCharSequence(value, cursor, cursor);
         int offset;
         if (dir == -1) {
            offset = wordIterator.preceding(cursor);
         } else {
            offset = wordIterator.following(cursor);
         }
         if (offset != BreakIterator.DONE) {
            return (offset);
         } else {
            return (cursor);
         }
      }
      int i = cursor;
      boolean flag = dir < 0;
      int j = Math.abs(dir);

      for(int k = 0; k < j; ++k) {
         if (!flag) {
            int l = this.value.length();
            i = this.value.indexOf(32, i);
            if (i == -1) {
               i = l;
            } else {
               while(pSkipConsecutiveSpaces && i < l && this.value.charAt(i) == ' ') {
                  ++i;
               }
            }
         } else {
            while(pSkipConsecutiveSpaces && i > 0 && this.value.charAt(i - 1) == ' ') {
               --i;
            }

            while(i > 0 && this.value.charAt(i - 1) != ' ') {
               --i;
            }
         }
      }

      return i;
   }

   public void moveCursor(int pDelta, boolean pSelect) {
      this.moveCursorTo(this.getCursorPos(pDelta), pSelect);
   }

   private int getCursorPos(int pDelta) {
      return MuiModApi.offsetByGrapheme(value, cursorPos, pDelta);
   }

   public void moveCursorTo(int pDelta, boolean pSelect) {
      this.setCursorPosition(pDelta);
      if (!pSelect) {
         this.setHighlightPos(this.cursorPos);
      }

      this.onValueChange(this.value);
   }

   public void setCursorPosition(int pPos) {
      this.cursorPos = Mth.clamp(pPos, 0, this.value.length());
      this.scrollTo(this.cursorPos);
      focusedTime = Util.getMillis();
   }

   public void moveCursorToStart(boolean pSelect) {
      this.moveCursorTo(0, pSelect);
   }

   public void moveCursorToEnd(boolean pSelect) {
      this.moveCursorTo(this.value.length(), pSelect);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (this.isActive() && this.isFocused()) {
         switch (pKeyCode) {
            case 259:
               if (this.isEditable) {
                  this.deleteText(-1);
               }

               return true;
            case 260:
            case 264:
            case 265:
            case 266:
            case 267:
            default:
               if (pKeyCode == GLFW.GLFW_KEY_Z || pKeyCode == GLFW.GLFW_KEY_Y) {
                  if (Screen.hasControlDown() && !Screen.hasAltDown()) {
                     if (!Screen.hasShiftDown()) {
                        UndoOwner[] owners = {modernUI_MC$undoOwner()};
                        if (pKeyCode == GLFW.GLFW_KEY_Z) {
                           // CTRL+Z
                           if (modernUI_MC$undoManager.countUndos(owners) > 0) {
                              modernUI_MC$undoManager.undo(owners, 1);
                              return true;
                           }
                        } else if (modernUI_MC$tryRedo(owners)) {
                           // CTRL+Y
                           return true;
                        }
                     } else if (pKeyCode == GLFW.GLFW_KEY_Z) {
                        UndoOwner[] owners = {modernUI_MC$undoOwner()};
                        if (modernUI_MC$tryRedo(owners)) {
                           // CTRL+SHIFT+Z
                           return true;
                        }
                     }
                  }
               }
               if (Screen.isSelectAll(pKeyCode)) {
                  this.moveCursorToEnd(false);
                  this.setHighlightPos(0);
                  return true;
               } else if (Screen.isCopy(pKeyCode)) {
                  Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                  return true;
               } else if (Screen.isPaste(pKeyCode)) {
                  if (this.isEditable()) {
                     this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                  }

                  return true;
               } else {
                  if (Screen.isCut(pKeyCode)) {
                     Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                     if (this.isEditable()) {
                        this.insertText("");
                     }

                     return true;
                  }

                  return false;
               }
            case 261:
               if (this.isEditable) {
                  this.deleteText(1);
               }

               return true;
            case 262:
               if (Screen.hasControlDown()) {
                  this.moveCursorTo(this.getWordPosition(1), Screen.hasShiftDown());
               } else {
                  this.moveCursor(1, Screen.hasShiftDown());
               }

               return true;
            case 263:
               if (Screen.hasControlDown()) {
                  this.moveCursorTo(this.getWordPosition(-1), Screen.hasShiftDown());
               } else {
                  this.moveCursor(-1, Screen.hasShiftDown());
               }

               return true;
            case 268:
               this.moveCursorToStart(Screen.hasShiftDown());
               return true;
            case 269:
               this.moveCursorToEnd(Screen.hasShiftDown());
               return true;
         }
      } else {
         return false;
      }
   }

   public boolean canConsumeInput() {
      return this.isActive() && this.isFocused() && this.isEditable();
   }

   public boolean charTyped(char pCodePoint, int pModifiers) {
      if (!this.canConsumeInput()) {
         return false;
      } else if (this.viaFabricPlus$forbiddenCharactersUnlocked ||SharedConstants.isAllowedChatCharacter(pCodePoint)) {
         if (this.isEditable) {
            this.insertText(Character.toString(pCodePoint));
         }

         return true;
      } else {
         return false;
      }
   }
   private UndoOwner modernUI_MC$undoOwner() {
      return modernUI_MC$undoManager.getOwner("EditBox", this);
   }

   private boolean modernUI_MC$tryRedo(UndoOwner[] owners) {
      if (modernUI_MC$undoManager.countRedos(owners) > 0) {
         modernUI_MC$undoManager.redo(owners, 1);
         return true;
      }
      return false;
   }

   @Override
   public UndoManager modernUI_MC$getUndoManager() {
      return modernUI_MC$undoManager;
   }

   public void onClick(double pMouseX, double pMouseY) {
      int i = Mth.floor(pMouseX) - this.getX();
      if (this.bordered) {
         i -= 4;
      }

      String s = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
      this.moveCursorTo(this.font.plainSubstrByWidth(s, i).length() + this.displayPos, Screen.hasShiftDown());
   }

   public void playDownSound(SoundManager pHandler) {
   }

   public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.isVisible()) {
         if (this.isBordered()) {
            ResourceLocation resourcelocation = SPRITES.get(this.isActive(), this.isFocused());
            pGuiGraphics.blitSprite(resourcelocation, this.getX(), this.getY(), this.getWidth(), this.getHeight());
         }
         final TextLayoutEngine engine = TextLayoutEngine.getInstance();

         final int color = isEditable ? textColor : textColorUneditable;

         final String viewText =
                 engine.getStringSplitter().headByWidth(value.substring(displayPos), getInnerWidth(), Style.EMPTY);
         final int viewCursorPos = cursorPos - displayPos;
         final int clampedViewHighlightPos = Mth.clamp(highlightPos - displayPos, 0, viewText.length());

         final boolean cursorInRange = viewCursorPos >= 0 && viewCursorPos <= viewText.length();
         final boolean cursorVisible =
                 isFocused() && (((Util.getMillis() - focusedTime) / 500) & 1) == 0 && cursorInRange;

         final int baseX = bordered ? getX() + 4 : getX();
         final int baseY = bordered ? getY() + (height - 8) / 2 : getY();
         float hori = baseX;

         final Matrix4f matrix = pGuiGraphics.pose().last().pose();
         final MultiBufferSource.BufferSource bufferSource = pGuiGraphics.bufferSource();

         final boolean separate;
         if (!viewText.isEmpty()) {
            String subText = cursorInRange ? viewText.substring(0, viewCursorPos) : viewText;
            FormattedCharSequence subSequence = formatter.apply(subText, displayPos);
            if (subSequence != null &&
                    !(subSequence instanceof VanillaTextWrapper)) {
               separate = true;
               hori = engine.getTextRenderer().drawText(subSequence, hori, baseY, color, true,
                       matrix, bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            } else {
               separate = false;
               hori = engine.getTextRenderer().drawText(viewText, hori, baseY, color, true,
                       matrix, bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }
         } else {
            separate = false;
         }

         final boolean cursorNotAtEnd = cursorPos < value.length() || value.length() >= getMaxLength();

         // XXX: BiDi is not supported here
         final float cursorX;
         if (cursorInRange) {
            if (!separate && !viewText.isEmpty()) {
               TextLayout layout = engine.lookupVanillaLayout(viewText,
                       Style.EMPTY, TextLayoutEngine.COMPUTE_ADVANCES);
               float curAdv = 0;
               int stripIndex = 0;
               for (int i = 0; i < viewCursorPos; i++) {
                  if (viewText.charAt(i) == ChatFormatting.PREFIX_CODE) {
                     i++;
                     continue;
                  }
                  curAdv += layout.getAdvances()[stripIndex++];
               }
               cursorX = baseX + curAdv;
            } else {
               cursorX = hori;
            }
         } else {
            cursorX = viewCursorPos > 0 ? baseX + width : baseX;
         }

         if (!viewText.isEmpty() && cursorInRange && viewCursorPos < viewText.length() && separate) {
            String subText = viewText.substring(viewCursorPos);
            FormattedCharSequence subSequence = formatter.apply(subText, cursorPos);
            if (subSequence != null &&
                    !(subSequence instanceof VanillaTextWrapper)) {
               engine.getTextRenderer().drawText(subSequence, hori, baseY, color, true,
                       matrix, bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            } else {
               engine.getTextRenderer().drawText(subText, hori, baseY, color, true,
                       matrix, bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }
         }

         if (!cursorNotAtEnd && suggestion != null) {
            engine.getTextRenderer().drawText(suggestion, cursorX, baseY, 0xFF808080, true,
                    matrix, bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
         }

         if (viewCursorPos != clampedViewHighlightPos) {
            pGuiGraphics.flush();

            TextLayout layout = engine.lookupVanillaLayout(viewText,
                    Style.EMPTY, TextLayoutEngine.COMPUTE_ADVANCES);
            float startX = baseX;
            float endX = cursorX;
            int stripIndex = 0;
            for (int i = 0; i < clampedViewHighlightPos; i++) {
               if (viewText.charAt(i) == ChatFormatting.PREFIX_CODE) {
                  i++;
                  continue;
               }
               startX += layout.getAdvances()[stripIndex++];
            }

            if (endX < startX) {
               float temp = startX;
               startX = endX;
               endX = temp;
            }
            if (startX > getX() + width) {
               startX = getX() + width;
            }
            if (endX > getX() + width) {
               endX = getX() + width;
            }

            VertexConsumer consumer = pGuiGraphics.bufferSource().getBuffer(RenderType.guiOverlay());
            consumer.vertex(matrix, startX, baseY + 10, 0)
                    .color(51, 181, 229, 56).endVertex();
            consumer.vertex(matrix, endX, baseY + 10, 0)
                    .color(51, 181, 229, 56).endVertex();
            consumer.vertex(matrix, endX, baseY - 1, 0)
                    .color(51, 181, 229, 56).endVertex();
            consumer.vertex(matrix, startX, baseY - 1, 0)
                    .color(51, 181, 229, 56).endVertex();
            pGuiGraphics.flush();
         } else if (cursorVisible) {
            if (cursorNotAtEnd) {
               pGuiGraphics.flush();

               VertexConsumer consumer = pGuiGraphics.bufferSource().getBuffer(RenderType.guiOverlay());
               consumer.vertex(matrix, cursorX - 0.5f, baseY + 10, 0)
                       .color(208, 208, 208, 255).endVertex();
               consumer.vertex(matrix, cursorX + 0.5f, baseY + 10, 0)
                       .color(208, 208, 208, 255).endVertex();
               consumer.vertex(matrix, cursorX + 0.5f, baseY - 1, 0)
                       .color(208, 208, 208, 255).endVertex();
               consumer.vertex(matrix, cursorX - 0.5f, baseY - 1, 0)
                       .color(208, 208, 208, 255).endVertex();
               pGuiGraphics.flush();
            } else {
               engine.getTextRenderer().drawText(CURSOR_APPEND_CHARACTER, cursorX, baseY, color, true,
                       matrix, bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

               pGuiGraphics.flush();
            }
         } else {
            pGuiGraphics.flush();
         }
      }
   }

   private void renderHighlight(GuiGraphics pGuiGraphics, int pMinX, int pMinY, int pMaxX, int pMaxY) {
      if (pMinX < pMaxX) {
         int i = pMinX;
         pMinX = pMaxX;
         pMaxX = i;
      }

      if (pMinY < pMaxY) {
         int j = pMinY;
         pMinY = pMaxY;
         pMaxY = j;
      }

      if (pMaxX > this.getX() + this.width) {
         pMaxX = this.getX() + this.width;
      }

      if (pMinX > this.getX() + this.width) {
         pMinX = this.getX() + this.width;
      }

      pGuiGraphics.fill(RenderType.guiTextHighlight(), pMinX, pMinY, pMaxX, pMaxY, -16776961);
   }

   public void setMaxLength(int pLength) {
      this.maxLength = pLength;
      if (this.value.length() > pLength) {
         this.value = this.value.substring(0, pLength);
         this.onValueChange(this.value);
      }

   }

   private int getMaxLength() {
      return this.maxLength;
   }

   public int getCursorPosition() {
      return this.cursorPos;
   }

   public boolean isBordered() {
      return this.bordered;
   }

   public void setBordered(boolean pEnableBackgroundDrawing) {
      this.bordered = pEnableBackgroundDrawing;
   }

   public void setTextColor(int pColor) {
      this.textColor = pColor;
   }

   public void setTextColorUneditable(int pColor) {
      this.textColorUneditable = pColor;
   }

   public void setFocused(boolean pFocused) {
      if (this.canLoseFocus || pFocused) {
         super.setFocused(pFocused);
         if (pFocused) {
            this.focusedTime = Util.getMillis();
         }

      }
   }

   private boolean isEditable() {
      return this.isEditable;
   }

   public void setEditable(boolean pEnabled) {
      this.isEditable = pEnabled;
   }

   public int getInnerWidth() {
      return this.isBordered() ? this.width - 8 : this.width;
   }

   public void setHighlightPos(int pPosition) {
      this.highlightPos = Mth.clamp(pPosition, 0, this.value.length());
      this.scrollTo(this.highlightPos);
   }

   private void scrollTo(int pPosition) {
      if (this.font != null) {
         this.displayPos = Math.min(this.displayPos, this.value.length());
         int i = this.getInnerWidth();
         String s = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), i);
         int j = s.length() + this.displayPos;
         if (pPosition == this.displayPos) {
            this.displayPos -= this.font.plainSubstrByWidth(this.value, i, true).length();
         }

         if (pPosition > j) {
            this.displayPos += pPosition - j;
         } else if (pPosition <= this.displayPos) {
            this.displayPos -= this.displayPos - pPosition;
         }

         this.displayPos = Mth.clamp(this.displayPos, 0, this.value.length());
      }
   }

   public void setCanLoseFocus(boolean pCanLoseFocus) {
      this.canLoseFocus = pCanLoseFocus;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void setVisible(boolean pIsVisible) {
      this.visible = pIsVisible;
   }

   public void setSuggestion(@Nullable String pSuggestion) {
      this.suggestion = pSuggestion;
   }

   public int getScreenX(int pCharNum) {
      return pCharNum > this.value.length() ? this.getX() : this.getX() + this.font.width(this.value.substring(0, pCharNum));
   }

   public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
      pNarrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
   }

   public void setHint(Component pHint) {
      this.hint = pHint;
   }
   public void viaFabricPlus$unlockForbiddenCharacters() {
      this.viaFabricPlus$forbiddenCharactersUnlocked = true;
   }

}