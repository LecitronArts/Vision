package net.minecraft.client.gui.screens;

import dev.chachy.lazylanguageloader.client.api.scroll.Scrollable;
import dev.chachy.lazylanguageloader.client.impl.state.StateManager;
import dev.chachy.lazylanguageloader.client.impl.utils.Constants;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class LanguageSelectScreen extends OptionsSubScreen {
   private static final Component WARNING_LABEL = Component.translatable("options.languageAccuracyWarning").withStyle(ChatFormatting.GRAY);
   private LanguageSelectScreen.LanguageSelectionList packSelectionList;
   final LanguageManager languageManager;
   private List<LanguageSelectScreen.LanguageSelectionList.Entry> initialComponents;

   private EditBox searchText;

   public LanguageSelectScreen(Screen pLastScreen, Options pOptions, LanguageManager pLanguageManager) {
      super(pLastScreen, pOptions, Component.translatable("options.language.title"));
      this.languageManager = pLanguageManager;
   }

   protected void init() {
      this.packSelectionList = this.addRenderableWidget(new LanguageSelectScreen.LanguageSelectionList(this.minecraft));
      this.addRenderableWidget(this.options.forceUnicodeFont().createButton(this.options, this.width / 2 - 155, this.height - 38, 150));
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (p_288243_) -> this.onDone()).bounds(this.width / 2 - 155 + 160, this.height - 38, 150, 20).build());
      initialComponents = new ArrayList<>(packSelectionList.children());

      int w = width / 5;

      searchText = new EditBox(font, width - (w + 5), 11, w, 15, Component.empty());

      searchText.setSuggestion(lazyLanguageLoader$$truncateByWidth(Constants.SUGGESTION_TEXT, searchText, Constants.TRUNCATION_MARKER));
      searchText.setResponder(this::lazyLanguageLoader$$handleText);

      addRenderableWidget(searchText);
   }


   void onDone() {
      LanguageSelectScreen.LanguageSelectionList.Entry languageselectscreen$languageselectionlist$entry = this.packSelectionList.getSelected();
      if (languageselectscreen$languageselectionlist$entry != null && !languageselectscreen$languageselectionlist$entry.code.equals(this.languageManager.getSelected())) {
         this.languageManager.setSelected(languageselectscreen$languageselectionlist$entry.code);
         this.options.languageCode = languageselectscreen$languageselectionlist$entry.code;
         StateManager.setResourceLoadViaLanguage(true);
         this.minecraft.reloadResourcePacks();
         StateManager.setResourceLoadViaLanguage(false);
         this.options.save();
      }

      this.minecraft.setScreen(this.lastScreen);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (CommonInputs.selected(pKeyCode)) {
         LanguageSelectScreen.LanguageSelectionList.Entry languageselectscreen$languageselectionlist$entry = this.packSelectionList.getSelected();
         if (languageselectscreen$languageselectionlist$entry != null) {
            languageselectscreen$languageselectionlist$entry.select();
            this.onDone();
            return true;
         }
      }

      return super.keyPressed(pKeyCode, pScanCode, pModifiers);
   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 16, 16777215);
      pGuiGraphics.drawCenteredString(this.font, WARNING_LABEL, this.width / 2, this.height - 56, -8355712);
   }

   public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderDirtBackground(pGuiGraphics);
   }
   private void lazyLanguageLoader$$handleText(String text) {
      List<LanguageSelectScreen.LanguageSelectionList.Entry> children = packSelectionList.children();

      if (text.isBlank()) {
         int initialSize = initialComponents.size();
         int currentSize = children.size();

         if (initialSize != currentSize) {
            packSelectionList.replaceEntries(initialComponents);
         }

         searchText.setSuggestion(lazyLanguageLoader$$truncateByWidth(Constants.SUGGESTION_TEXT, searchText, Constants.TRUNCATION_MARKER));
      } else {
         searchText.setSuggestion(Constants.EMPTY_TEXT);
         for (LanguageSelectScreen.LanguageSelectionList.Entry entry : initialComponents) {
            Component def = entry.getLanguageDefinition();

            if (StateManager.isMatchable(text, def)) {
               lazyLanguageLoader$$safeAdd(entry);
            } else {
               packSelectionList.removeEntry(entry);
            }
         }
      }
      lazyLanguageLoader$$fixScroll();
   }

   private void lazyLanguageLoader$$fixScroll() {
      if(packSelectionList instanceof Scrollable) {
         if (((Scrollable) packSelectionList).hasScrolled()) {
            packSelectionList.setScrollAmount(packSelectionList.getScrollAmount());
         } else {
            if (packSelectionList.getSelected() != null) {
               packSelectionList.centerScrollOn(packSelectionList.getSelected());
            }
         }
      }
   }

   private void lazyLanguageLoader$$safeAdd(LanguageSelectScreen.LanguageSelectionList.Entry entry) {
      if (!packSelectionList.children().contains(entry)) {
         packSelectionList.addEntry(entry);
      }
   }

   private String lazyLanguageLoader$$truncateByWidth(String text, AbstractWidget widget, String marker) {
      int textWidth = font.width(text);
      int widgetWidth = widget.getWidth();

      if (textWidth > widgetWidth) {
         String truncatedText = text;
         int truncatedWidth = textWidth;

         while (truncatedWidth > widgetWidth) {
            truncatedText = truncatedText.substring(0, truncatedText.length() - 1);
            truncatedWidth = font.width(truncatedText);
         }

         return lazyLanguageLoader$$addTruncationMarker(truncatedText, marker);
      } else {
         return text;
      }
   }

   private String lazyLanguageLoader$$addTruncationMarker(String text, String marker) {
      return text.length() > marker.length() ? text.substring(0, text.length() - marker.length()) : marker;
   }

   @OnlyIn(Dist.CLIENT)
   class LanguageSelectionList extends ObjectSelectionList<LanguageSelectScreen.LanguageSelectionList.Entry> {
      public LanguageSelectionList(Minecraft pMinecraft) {
         super(pMinecraft, LanguageSelectScreen.this.width, LanguageSelectScreen.this.height - 93, 32, 18);
         String s = LanguageSelectScreen.this.languageManager.getSelected();
         LanguageSelectScreen.this.languageManager.getLanguages().forEach((p_265492_, p_265377_) -> {
            LanguageSelectScreen.LanguageSelectionList.Entry languageselectscreen$languageselectionlist$entry = new LanguageSelectScreen.LanguageSelectionList.Entry(p_265492_, p_265377_);
            this.addEntry(languageselectscreen$languageselectionlist$entry);
            if (s.equals(p_265492_)) {
               this.setSelected(languageselectscreen$languageselectionlist$entry);
            }

         });
         if (this.getSelected() != null) {
            this.centerScrollOn(this.getSelected());
         }

      }

      protected int getScrollbarPosition() {
         return super.getScrollbarPosition() + 20;
      }

      public int getRowWidth() {
         return super.getRowWidth() + 50;
      }

      @OnlyIn(Dist.CLIENT)
      public class Entry extends ObjectSelectionList.Entry<LanguageSelectScreen.LanguageSelectionList.Entry> {
         final String code;
         private final Component language;
         private long lastClickTime;

         public Entry(String pCode, LanguageInfo pLanguage) {
            this.code = pCode;
            this.language = pLanguage.toComponent();
         }

         public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
            pGuiGraphics.drawCenteredString(LanguageSelectScreen.this.font, this.language, LanguageSelectionList.this.width / 2, pTop + 1, 16777215);
         }

         public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            this.select();
            if (Util.getMillis() - this.lastClickTime < 250L) {
               LanguageSelectScreen.this.onDone();
            }

            this.lastClickTime = Util.getMillis();
            return true;
         }
         public Component getLanguageDefinition(){
            return language;
         }

         void select() {
            LanguageSelectionList.this.setSelected(this);
         }

         public Component getNarration() {
            return Component.translatable("narrator.select", this.language);
         }
      }
   }
}