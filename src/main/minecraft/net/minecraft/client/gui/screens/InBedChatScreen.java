package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class InBedChatScreen extends ChatScreen {
   private Button leaveBedButton;

   public InBedChatScreen() {
      super("");
   }

   protected void init() {
      super.init();
      this.leaveBedButton = Button.builder(Component.translatable("multiplayer.stopSleeping"), (p_96074_) -> {
         this.sendWakeUp();
      }).bounds(this.width / 2 - 100, this.height - 40, 200, 20).build();
      this.addRenderableWidget(this.leaveBedButton);
   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      if (!this.minecraft.getChatStatus().isChatAllowed(this.minecraft.isLocalServer())) {
         this.leaveBedButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      } else {
         super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      }
   }

   public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
   }

   public void onClose() {
      this.sendWakeUp();
   }

   public boolean charTyped(char pCodePoint, int pModifiers) {
      return !this.minecraft.getChatStatus().isChatAllowed(this.minecraft.isLocalServer()) ? true : super.charTyped(pCodePoint, pModifiers);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.sendWakeUp();
      }

      if (!this.minecraft.getChatStatus().isChatAllowed(this.minecraft.isLocalServer())) {
         return true;
      } else if (pKeyCode != 257 && pKeyCode != 335) {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      } else {
         if (this.handleChatInput(this.input.getValue(), true)) {
            this.minecraft.setScreen((Screen)null);
            this.input.setValue("");
            this.minecraft.gui.getChat().resetChatScroll();
         }

         return true;
      }
   }

   private void sendWakeUp() {
      ClientPacketListener clientpacketlistener = this.minecraft.player.connection;
      clientpacketlistener.send(new ServerboundPlayerCommandPacket(this.minecraft.player, ServerboundPlayerCommandPacket.Action.STOP_SLEEPING));
   }

   public void onPlayerWokeUp() {
      if (this.input.getValue().isEmpty()) {
         this.minecraft.setScreen((Screen)null);
      } else {
         this.minecraft.setScreen(new ChatScreen(this.input.getValue()));
      }

   }
}