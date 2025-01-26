package net.minecraft.client.gui.screens;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.screen.base.PerServerVersionScreen;
import de.florianmichael.viafabricplus.settings.impl.GeneralSettings;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EditServerScreen extends Screen {
   private static final Component NAME_LABEL = Component.translatable("addServer.enterName");
   private static final Component IP_LABEL = Component.translatable("addServer.enterIp");
   private Button addButton;
   private final BooleanConsumer callback;
   private final ServerData serverData;
   private EditBox ipEdit;
   private EditBox nameEdit;
   private final Screen lastScreen;


   // via
   private String viaFabricPlus$nameField;

   private String viaFabricPlus$addressField;


   public EditServerScreen(Screen pLastScreen, BooleanConsumer pCallback, ServerData pServerData) {
      super(Component.translatable("addServer.title"));
      this.lastScreen = pLastScreen;
      this.callback = pCallback;
      this.serverData = pServerData;
   }

   protected void init() {
      this.nameEdit = new EditBox(this.font, this.width / 2 - 100, 66, 200, 20, Component.translatable("addServer.enterName"));
      this.nameEdit.setValue(this.serverData.name);
      this.nameEdit.setResponder((p_169304_) -> {
         this.updateAddButtonStatus();
      });
      this.addWidget(this.nameEdit);
      this.ipEdit = new EditBox(this.font, this.width / 2 - 100, 106, 200, 20, Component.translatable("addServer.enterIp"));
      this.ipEdit.setMaxLength(128);
      this.ipEdit.setValue(this.serverData.ip);
      this.ipEdit.setResponder((p_169302_) -> {
         this.updateAddButtonStatus();
      });
      this.addWidget(this.ipEdit);
      this.addRenderableWidget(CycleButton.builder(ServerData.ServerPackStatus::getName).withValues(ServerData.ServerPackStatus.values()).withInitialValue(this.serverData.getResourcePackStatus()).create(this.width / 2 - 100, this.height / 4 + 72, 200, 20, Component.translatable("addServer.resourcePack"), (p_169299_, p_169300_) -> {
         this.serverData.setResourcePackStatus(p_169300_);
      }));
      this.addButton = this.addRenderableWidget(Button.builder(Component.translatable("addServer.add"), (p_96030_) -> {
         this.onAdd();
      }).bounds(this.width / 2 - 100, this.height / 4 + 96 + 18, 200, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (p_169297_) -> {
         this.callback.accept(false);
      }).bounds(this.width / 2 - 100, this.height / 4 + 120 + 18, 200, 20).build());
      this.setInitialFocus(this.nameEdit);
      this.updateAddButtonStatus();

      final int buttonPosition = GeneralSettings.global().addServerScreenButtonOrientation.getIndex();
      if (buttonPosition == 0) { // Off
         return;
      }

      final ProtocolVersion forcedVersion = ( serverData).viaFabricPlus$forcedVersion();

      // Restore input if the user cancels the version selection screen (or if the user is editing an existing server)
      if (viaFabricPlus$nameField != null && viaFabricPlus$addressField != null) {
         this.nameEdit.setValue(viaFabricPlus$nameField);
         this.ipEdit.setValue(viaFabricPlus$addressField);

         viaFabricPlus$nameField = null;
         viaFabricPlus$addressField = null;
      }

      Button.Builder buttonBuilder = Button.builder(forcedVersion == null ? Component.translatable("base.viafabricplus.set_version") : Component.literal(forcedVersion.getName()), button -> {
         // Store current input in case the user cancels the version selection
         viaFabricPlus$nameField = nameEdit.getValue();
         viaFabricPlus$addressField = ipEdit.getValue();

         minecraft.setScreen(new PerServerVersionScreen(this, version -> ( serverData).viaFabricPlus$forceVersion(version)));
      }).size(98, 20);

      // Set the button's position according to the configured orientation and add the button to the screen
      this.addRenderableWidget(GeneralSettings.withOrientation(buttonBuilder, buttonPosition, width, height).build());
   }

   public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
      String s = this.ipEdit.getValue();
      String s1 = this.nameEdit.getValue();
      this.init(pMinecraft, pWidth, pHeight);
      this.ipEdit.setValue(s);
      this.nameEdit.setValue(s1);
   }

   private void onAdd() {
      this.serverData.name = this.nameEdit.getValue();
      this.serverData.ip = this.ipEdit.getValue();
      this.callback.accept(true);
   }

   public void onClose() {
      this.minecraft.setScreen(this.lastScreen);
   }

   private void updateAddButtonStatus() {
      this.addButton.active = ServerAddress.isValidAddress(this.ipEdit.getValue()) && !this.nameEdit.getValue().isEmpty();
   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 17, 16777215);
      pGuiGraphics.drawString(this.font, NAME_LABEL, this.width / 2 - 100 + 1, 53, 10526880);
      pGuiGraphics.drawString(this.font, IP_LABEL, this.width / 2 - 100 + 1, 94, 10526880);
      this.nameEdit.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      this.ipEdit.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
   }
}