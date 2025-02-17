package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.util.RealmsUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsPlayerScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("minecraft", "textures/gui/options_background.png");
   private static final Component QUESTION_TITLE = Component.translatable("mco.question");
   static final Component NORMAL_USER_TOOLTIP = Component.translatable("mco.configure.world.invites.normal.tooltip");
   static final Component OP_TOOLTIP = Component.translatable("mco.configure.world.invites.ops.tooltip");
   static final Component REMOVE_ENTRY_TOOLTIP = Component.translatable("mco.configure.world.invites.remove.tooltip");
   private static final int NO_ENTRY_SELECTED = -1;
   private final RealmsConfigureWorldScreen lastScreen;
   final RealmsServer serverData;
   RealmsPlayerScreen.InvitedObjectSelectionList invitedObjectSelectionList;
   int column1X;
   int columnWidth;
   private Button removeButton;
   private Button opdeopButton;
   int playerIndex = -1;
   private boolean stateChanged;

   public RealmsPlayerScreen(RealmsConfigureWorldScreen pLastScreen, RealmsServer pServerData) {
      super(Component.translatable("mco.configure.world.players.title"));
      this.lastScreen = pLastScreen;
      this.serverData = pServerData;
   }

   public void init() {
      this.column1X = this.width / 2 - 160;
      this.columnWidth = 150;
      int i = this.width / 2 + 12;
      this.invitedObjectSelectionList = this.addRenderableWidget(new RealmsPlayerScreen.InvitedObjectSelectionList());
      this.invitedObjectSelectionList.setX(this.column1X);

      for(PlayerInfo playerinfo : this.serverData.players) {
         this.invitedObjectSelectionList.addEntry(playerinfo);
      }

      this.playerIndex = -1;
      this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.buttons.invite"), (p_280732_) -> {
         this.minecraft.setScreen(new RealmsInviteScreen(this.lastScreen, this, this.serverData));
      }).bounds(i, row(1), this.columnWidth + 10, 20).build());
      this.removeButton = this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.invites.remove.tooltip"), (p_278866_) -> {
         this.uninvite(this.playerIndex);
      }).bounds(i, row(7), this.columnWidth + 10, 20).build());
      this.opdeopButton = this.addRenderableWidget(Button.builder(Component.translatable("mco.configure.world.invites.ops.tooltip"), (p_278869_) -> {
         if (this.serverData.players.get(this.playerIndex).isOperator()) {
            this.deop(this.playerIndex);
         } else {
            this.op(this.playerIndex);
         }

      }).bounds(i, row(9), this.columnWidth + 10, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (p_89122_) -> {
         this.backButtonClicked();
      }).bounds(i + this.columnWidth / 2 + 2, row(12), this.columnWidth / 2 + 10 - 2, 20).build());
      this.updateButtonStates();
   }

   void updateButtonStates() {
      this.removeButton.visible = this.shouldRemoveAndOpdeopButtonBeVisible(this.playerIndex);
      this.opdeopButton.visible = this.shouldRemoveAndOpdeopButtonBeVisible(this.playerIndex);
      this.invitedObjectSelectionList.updateButtons();
   }

   private boolean shouldRemoveAndOpdeopButtonBeVisible(int pPlayerIndex) {
      return pPlayerIndex != -1;
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.backButtonClicked();
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   private void backButtonClicked() {
      if (this.stateChanged) {
         this.minecraft.setScreen(this.lastScreen.getNewScreen());
      } else {
         this.minecraft.setScreen(this.lastScreen);
      }

   }

   void op(int pIndex) {
      RealmsClient realmsclient = RealmsClient.create();
      UUID uuid = this.serverData.players.get(pIndex).getUuid();

      try {
         this.updateOps(realmsclient.op(this.serverData.id, uuid));
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Couldn't op the user", (Throwable)realmsserviceexception);
      }

      this.updateButtonStates();
   }

   void deop(int pIndex) {
      RealmsClient realmsclient = RealmsClient.create();
      UUID uuid = this.serverData.players.get(pIndex).getUuid();

      try {
         this.updateOps(realmsclient.deop(this.serverData.id, uuid));
      } catch (RealmsServiceException realmsserviceexception) {
         LOGGER.error("Couldn't deop the user", (Throwable)realmsserviceexception);
      }

      this.updateButtonStates();
   }

   private void updateOps(Ops pOps) {
      for(PlayerInfo playerinfo : this.serverData.players) {
         playerinfo.setOperator(pOps.ops.contains(playerinfo.getName()));
      }

   }

   void uninvite(int pIndex) {
      this.updateButtonStates();
      if (pIndex >= 0 && pIndex < this.serverData.players.size()) {
         PlayerInfo playerinfo = this.serverData.players.get(pIndex);
         RealmsConfirmScreen realmsconfirmscreen = new RealmsConfirmScreen((p_278868_) -> {
            if (p_278868_) {
               RealmsClient realmsclient = RealmsClient.create();

               try {
                  realmsclient.uninvite(this.serverData.id, playerinfo.getUuid());
               } catch (RealmsServiceException realmsserviceexception) {
                  LOGGER.error("Couldn't uninvite user", (Throwable)realmsserviceexception);
               }

               this.serverData.players.remove(this.playerIndex);
               this.playerIndex = -1;
               this.updateButtonStates();
            }

            this.stateChanged = true;
            this.minecraft.setScreen(this);
         }, QUESTION_TITLE, Component.translatable("mco.configure.world.uninvite.player", playerinfo.getName()));
         this.minecraft.setScreen(realmsconfirmscreen);
      }

   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
      int i = row(12) + 20;
      pGuiGraphics.setColor(0.25F, 0.25F, 0.25F, 1.0F);
      pGuiGraphics.blit(OPTIONS_BACKGROUND, 0, i, 0.0F, 0.0F, this.width, this.height - i, 32, 32);
      pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
      String s = this.serverData.players != null ? Integer.toString(this.serverData.players.size()) : "0";
      pGuiGraphics.drawString(this.font, Component.translatable("mco.configure.world.invited.number", s), this.column1X, row(0), -1, false);
   }

   @OnlyIn(Dist.CLIENT)
   class Entry extends ObjectSelectionList.Entry<RealmsPlayerScreen.Entry> {
      private static final int X_OFFSET = 3;
      private static final int Y_PADDING = 1;
      private static final int BUTTON_WIDTH = 8;
      private static final int BUTTON_HEIGHT = 7;
      private static final WidgetSprites REMOVE_BUTTON_SPRITES = new WidgetSprites(new ResourceLocation("player_list/remove_player"), new ResourceLocation("player_list/remove_player_highlighted"));
      private static final WidgetSprites MAKE_OP_BUTTON_SPRITES = new WidgetSprites(new ResourceLocation("player_list/make_operator"), new ResourceLocation("player_list/make_operator_highlighted"));
      private static final WidgetSprites REMOVE_OP_BUTTON_SPRITES = new WidgetSprites(new ResourceLocation("player_list/remove_operator"), new ResourceLocation("player_list/remove_operator_highlighted"));
      private final PlayerInfo playerInfo;
      private final List<AbstractWidget> children = new ArrayList<>();
      private final ImageButton removeButton;
      private final ImageButton makeOpButton;
      private final ImageButton removeOpButton;

      public Entry(PlayerInfo pPlayerInfo) {
         this.playerInfo = pPlayerInfo;
         int i = RealmsPlayerScreen.this.serverData.players.indexOf(this.playerInfo);
         int j = RealmsPlayerScreen.this.invitedObjectSelectionList.getRowRight() - 16 - 9;
         int k = RealmsPlayerScreen.this.invitedObjectSelectionList.getRowTop(i) + 1;
         this.removeButton = new ImageButton(j, k, 8, 7, REMOVE_BUTTON_SPRITES, (p_279099_) -> {
            RealmsPlayerScreen.this.uninvite(i);
         }, CommonComponents.EMPTY);
         this.removeButton.setTooltip(Tooltip.create(RealmsPlayerScreen.REMOVE_ENTRY_TOOLTIP));
         this.children.add(this.removeButton);
         j += 11;
         this.makeOpButton = new ImageButton(j, k, 8, 7, MAKE_OP_BUTTON_SPRITES, (p_279435_) -> {
            RealmsPlayerScreen.this.op(i);
         }, CommonComponents.EMPTY);
         this.makeOpButton.setTooltip(Tooltip.create(RealmsPlayerScreen.NORMAL_USER_TOOLTIP));
         this.children.add(this.makeOpButton);
         this.removeOpButton = new ImageButton(j, k, 8, 7, REMOVE_OP_BUTTON_SPRITES, (p_279383_) -> {
            RealmsPlayerScreen.this.deop(i);
         }, CommonComponents.EMPTY);
         this.removeOpButton.setTooltip(Tooltip.create(RealmsPlayerScreen.OP_TOOLTIP));
         this.children.add(this.removeOpButton);
         this.updateButtons();
      }

      public void updateButtons() {
         this.makeOpButton.visible = !this.playerInfo.isOperator();
         this.removeOpButton.visible = !this.makeOpButton.visible;
      }

      public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
         if (!this.makeOpButton.mouseClicked(pMouseX, pMouseY, pButton)) {
            this.removeOpButton.mouseClicked(pMouseX, pMouseY, pButton);
         }

         this.removeButton.mouseClicked(pMouseX, pMouseY, pButton);
         return true;
      }

      public void render(GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick) {
         int i;
         if (!this.playerInfo.getAccepted()) {
            i = -6250336;
         } else if (this.playerInfo.getOnline()) {
            i = 8388479;
         } else {
            i = -1;
         }

         RealmsUtil.renderPlayerFace(pGuiGraphics, RealmsPlayerScreen.this.column1X + 2 + 2, pTop + 1, 8, this.playerInfo.getUuid());
         pGuiGraphics.drawString(RealmsPlayerScreen.this.font, this.playerInfo.getName(), RealmsPlayerScreen.this.column1X + 3 + 12, pTop + 1, i, false);
         this.children.forEach((p_280738_) -> {
            p_280738_.setY(pTop + 1);
            p_280738_.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         });
      }

      public Component getNarration() {
         return Component.translatable("narrator.select", this.playerInfo.getName());
      }
   }

   @OnlyIn(Dist.CLIENT)
   class InvitedObjectSelectionList extends RealmsObjectSelectionList<RealmsPlayerScreen.Entry> {
      public InvitedObjectSelectionList() {
         super(RealmsPlayerScreen.this.columnWidth + 10, RealmsPlayerScreen.row(12) + 20, RealmsPlayerScreen.row(1), 13);
      }

      public void updateButtons() {
         if (RealmsPlayerScreen.this.playerIndex != -1) {
            this.getEntry(RealmsPlayerScreen.this.playerIndex).updateButtons();
         }

      }

      public void addEntry(PlayerInfo pPlayerInfo) {
         this.addEntry(RealmsPlayerScreen.this.new Entry(pPlayerInfo));
      }

      public int getRowWidth() {
         return (int)((double)this.width * 1.0D);
      }

      public void selectItem(int pIndex) {
         super.selectItem(pIndex);
         this.selectInviteListItem(pIndex);
      }

      public void selectInviteListItem(int pIndex) {
         RealmsPlayerScreen.this.playerIndex = pIndex;
         RealmsPlayerScreen.this.updateButtonStates();
      }

      public void setSelected(@Nullable RealmsPlayerScreen.Entry pSelected) {
         super.setSelected(pSelected);
         RealmsPlayerScreen.this.playerIndex = this.children().indexOf(pSelected);
         RealmsPlayerScreen.this.updateButtonStates();
      }

      public int getScrollbarPosition() {
         return RealmsPlayerScreen.this.column1X + this.width;
      }

      public int getMaxPosition() {
         return this.getItemCount() * 13;
      }
   }
}