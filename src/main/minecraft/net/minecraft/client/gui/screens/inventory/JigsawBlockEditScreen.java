package net.minecraft.client.gui.screens.inventory;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class JigsawBlockEditScreen extends Screen {
   private static final Component JOINT_LABEL = Component.translatable("jigsaw_block.joint_label");
   private static final Component POOL_LABEL = Component.translatable("jigsaw_block.pool");
   private static final Component NAME_LABEL = Component.translatable("jigsaw_block.name");
   private static final Component TARGET_LABEL = Component.translatable("jigsaw_block.target");
   private static final Component FINAL_STATE_LABEL = Component.translatable("jigsaw_block.final_state");
   private static final Component PLACEMENT_PRIORITY_LABEL = Component.translatable("jigsaw_block.placement_priority");
   private static final Component PLACEMENT_PRIORITY_TOOLTIP = Component.translatable("jigsaw_block.placement_priority.tooltip");
   private static final Component SELECTION_PRIORITY_LABEL = Component.translatable("jigsaw_block.selection_priority");
   private static final Component SELECTION_PRIORITY_TOOLTIP = Component.translatable("jigsaw_block.selection_priority.tooltip");
   private final JigsawBlockEntity jigsawEntity;
   private EditBox nameEdit;
   private EditBox targetEdit;
   private EditBox poolEdit;
   private EditBox finalStateEdit;
   private EditBox selectionPriorityEdit;
   private EditBox placementPriorityEdit;
   int levels;
   private boolean keepJigsaws = true;
   private CycleButton<JigsawBlockEntity.JointType> jointButton;
   private Button doneButton;
   private Button generateButton;
   private JigsawBlockEntity.JointType joint;

   public JigsawBlockEditScreen(JigsawBlockEntity pJigsawEntity) {
      super(GameNarrator.NO_TITLE);
      this.jigsawEntity = pJigsawEntity;
   }

   private void onDone() {
      this.sendToServer();
      this.minecraft.setScreen((Screen)null);
   }

   private void onCancel() {
      this.minecraft.setScreen((Screen)null);
   }

   private void sendToServer() {
      this.minecraft.getConnection().send(new ServerboundSetJigsawBlockPacket(this.jigsawEntity.getBlockPos(), new ResourceLocation(this.nameEdit.getValue()), new ResourceLocation(this.targetEdit.getValue()), new ResourceLocation(this.poolEdit.getValue()), this.finalStateEdit.getValue(), this.joint, this.parseAsInt(this.selectionPriorityEdit.getValue()), this.parseAsInt(this.placementPriorityEdit.getValue())));
   }

   private int parseAsInt(String pString) {
      try {
         return Integer.parseInt(pString);
      } catch (NumberFormatException numberformatexception) {
         return 0;
      }
   }

   private void sendGenerate() {
      this.minecraft.getConnection().send(new ServerboundJigsawGeneratePacket(this.jigsawEntity.getBlockPos(), this.levels, this.keepJigsaws));
   }

   public void onClose() {
      this.onCancel();
   }

   protected void init() {
      this.poolEdit = new EditBox(this.font, this.width / 2 - 153, 20, 300, 20, POOL_LABEL);
      this.poolEdit.setMaxLength(128);
      this.poolEdit.setValue(this.jigsawEntity.getPool().location().toString());
      this.poolEdit.setResponder((p_98986_) -> {
         this.updateValidity();
      });
      this.addWidget(this.poolEdit);
      this.nameEdit = new EditBox(this.font, this.width / 2 - 153, 55, 300, 20, NAME_LABEL);
      this.nameEdit.setMaxLength(128);
      this.nameEdit.setValue(this.jigsawEntity.getName().toString());
      this.nameEdit.setResponder((p_98981_) -> {
         this.updateValidity();
      });
      this.addWidget(this.nameEdit);
      this.targetEdit = new EditBox(this.font, this.width / 2 - 153, 90, 300, 20, TARGET_LABEL);
      this.targetEdit.setMaxLength(128);
      this.targetEdit.setValue(this.jigsawEntity.getTarget().toString());
      this.targetEdit.setResponder((p_98977_) -> {
         this.updateValidity();
      });
      this.addWidget(this.targetEdit);
      this.finalStateEdit = new EditBox(this.font, this.width / 2 - 153, 125, 300, 20, FINAL_STATE_LABEL);
      this.finalStateEdit.setMaxLength(256);
      this.finalStateEdit.setValue(this.jigsawEntity.getFinalState());
      this.addWidget(this.finalStateEdit);
      this.selectionPriorityEdit = new EditBox(this.font, this.width / 2 - 153, 160, 98, 20, SELECTION_PRIORITY_LABEL);
      this.selectionPriorityEdit.setMaxLength(3);
      this.selectionPriorityEdit.setValue(Integer.toString(this.jigsawEntity.getSelectionPriority()));
      this.selectionPriorityEdit.setTooltip(Tooltip.create(SELECTION_PRIORITY_TOOLTIP));
      this.addWidget(this.selectionPriorityEdit);
      this.placementPriorityEdit = new EditBox(this.font, this.width / 2 - 50, 160, 98, 20, PLACEMENT_PRIORITY_LABEL);
      this.placementPriorityEdit.setMaxLength(3);
      this.placementPriorityEdit.setValue(Integer.toString(this.jigsawEntity.getPlacementPriority()));
      this.placementPriorityEdit.setTooltip(Tooltip.create(PLACEMENT_PRIORITY_TOOLTIP));
      this.addWidget(this.placementPriorityEdit);
      this.joint = this.jigsawEntity.getJoint();
      this.jointButton = this.addRenderableWidget(CycleButton.builder(JigsawBlockEntity.JointType::getTranslatedName).withValues(JigsawBlockEntity.JointType.values()).withInitialValue(this.joint).displayOnlyValue().create(this.width / 2 + 54, 160, 100, 20, JOINT_LABEL, (p_169765_, p_169766_) -> {
         this.joint = p_169766_;
      }));
      boolean flag = JigsawBlock.getFrontFacing(this.jigsawEntity.getBlockState()).getAxis().isVertical();
      this.jointButton.active = flag;
      this.jointButton.visible = flag;
      this.addRenderableWidget(new AbstractSliderButton(this.width / 2 - 154, 185, 100, 20, CommonComponents.EMPTY, 0.0D) {
         {
            this.updateMessage();
         }

         protected void updateMessage() {
            this.setMessage(Component.translatable("jigsaw_block.levels", JigsawBlockEditScreen.this.levels));
         }

         protected void applyValue() {
            JigsawBlockEditScreen.this.levels = Mth.floor(Mth.clampedLerp(0.0D, 20.0D, this.value));
         }
      });
      this.addRenderableWidget(CycleButton.onOffBuilder(this.keepJigsaws).create(this.width / 2 - 50, 185, 100, 20, Component.translatable("jigsaw_block.keep_jigsaws"), (p_169768_, p_169769_) -> {
         this.keepJigsaws = p_169769_;
      }));
      this.generateButton = this.addRenderableWidget(Button.builder(Component.translatable("jigsaw_block.generate"), (p_98979_) -> {
         this.onDone();
         this.sendGenerate();
      }).bounds(this.width / 2 + 54, 185, 100, 20).build());
      this.doneButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (p_98973_) -> {
         this.onDone();
      }).bounds(this.width / 2 - 4 - 150, 210, 150, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (p_98964_) -> {
         this.onCancel();
      }).bounds(this.width / 2 + 4, 210, 150, 20).build());
      this.setInitialFocus(this.poolEdit);
      this.updateValidity();

      if (VisualSettings.global().removeNewerFeaturesFromJigsawScreen.isEnabled()) {
         if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
            nameEdit.active = false;
            jointButton.active = false;
            int index = children().indexOf(jointButton);
            ((AbstractWidget) children().get(index + 1)).active = false; // levels slider
            ((AbstractWidget) children().get(index + 2)).active = false; // keep jigsaws toggle
            ((AbstractWidget) children().get(index + 3)).active = false; // generate button
         }

         selectionPriorityEdit.active = false;
         placementPriorityEdit.active = false;
      }
   }

   private void updateValidity() {
      boolean flag = ResourceLocation.isValidResourceLocation(this.nameEdit.getValue()) && ResourceLocation.isValidResourceLocation(this.targetEdit.getValue()) && ResourceLocation.isValidResourceLocation(this.poolEdit.getValue());
      this.doneButton.active = flag;
      this.generateButton.active = flag;
   }

   public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
      String s = this.nameEdit.getValue();
      String s1 = this.targetEdit.getValue();
      String s2 = this.poolEdit.getValue();
      String s3 = this.finalStateEdit.getValue();
      String s4 = this.selectionPriorityEdit.getValue();
      String s5 = this.placementPriorityEdit.getValue();
      int i = this.levels;
      JigsawBlockEntity.JointType jigsawblockentity$jointtype = this.joint;
      this.init(pMinecraft, pWidth, pHeight);
      this.nameEdit.setValue(s);
      this.targetEdit.setValue(s1);
      this.poolEdit.setValue(s2);
      this.finalStateEdit.setValue(s3);
      this.levels = i;
      this.joint = jigsawblockentity$jointtype;
      this.jointButton.setValue(jigsawblockentity$jointtype);
      this.selectionPriorityEdit.setValue(s4);
      this.placementPriorityEdit.setValue(s5);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
         return true;
      } else if (!this.doneButton.active || pKeyCode != 257 && pKeyCode != 335) {
         return false;
      } else {
         this.onDone();
         return true;
      }
   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      if (VisualSettings.global().removeNewerFeaturesFromJigsawScreen.isEnabled() && ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
         nameEdit.setValue(targetEdit.getValue());
      }
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawString(this.font, POOL_LABEL, this.width / 2 - 153, 10, 10526880);
      this.poolEdit.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawString(this.font, NAME_LABEL, this.width / 2 - 153, 45, 10526880);
      this.nameEdit.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawString(this.font, TARGET_LABEL, this.width / 2 - 153, 80, 10526880);
      this.targetEdit.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawString(this.font, FINAL_STATE_LABEL, this.width / 2 - 153, 115, 10526880);
      this.finalStateEdit.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawString(this.font, SELECTION_PRIORITY_LABEL, this.width / 2 - 153, 150, 10526880);
      this.placementPriorityEdit.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawString(this.font, PLACEMENT_PRIORITY_LABEL, this.width / 2 - 50, 150, 10526880);
      this.selectionPriorityEdit.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      if (JigsawBlock.getFrontFacing(this.jigsawEntity.getBlockState()).getAxis().isVertical()) {
         pGuiGraphics.drawString(this.font, JOINT_LABEL, this.width / 2 + 53, 150, 10526880);
      }

   }
}