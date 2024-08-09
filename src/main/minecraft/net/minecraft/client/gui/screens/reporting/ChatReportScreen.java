package net.minecraft.client.gui.screens.reporting;

import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.UUID;
import net.minecraft.Optionull;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.report.ChatReport;
import net.minecraft.client.multiplayer.chat.report.Report;
import net.minecraft.client.multiplayer.chat.report.ReportReason;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatReportScreen extends AbstractReportScreen<ChatReport.Builder> {
   private static final int BUTTON_WIDTH = 120;
   private static final Component TITLE = Component.translatable("gui.chatReport.title");
   private static final Component SELECT_CHAT_MESSAGE = Component.translatable("gui.chatReport.select_chat");
   private final LinearLayout layout = LinearLayout.vertical().spacing(8);
   private MultiLineEditBox commentBox;
   private Button sendButton;
   private Button selectMessagesButton;
   private Button selectReasonButton;

   private ChatReportScreen(Screen pLastScreen, ReportingContext pReportingContext, ChatReport.Builder pReportBuilder) {
      super(TITLE, pLastScreen, pReportingContext, pReportBuilder);
   }

   public ChatReportScreen(Screen pLastScreen, ReportingContext pReportingContext, UUID pReportId) {
      this(pLastScreen, pReportingContext, new ChatReport.Builder(pReportId, pReportingContext.sender().reportLimits()));
   }

   public ChatReportScreen(Screen pLastScreen, ReportingContext pReportContext, ChatReport pReport) {
      this(pLastScreen, pReportContext, new ChatReport.Builder(pReport, pReportContext.sender().reportLimits()));
   }

   protected void init() {
      this.layout.defaultCellSetting().alignHorizontallyCenter();
      this.layout.addChild(new StringWidget(this.title, this.font));
      this.selectMessagesButton = this.layout.addChild(Button.builder(SELECT_CHAT_MESSAGE, (p_296205_) -> {
         this.minecraft.setScreen(new ChatSelectionScreen(this, this.reportingContext, this.reportBuilder, (p_296204_) -> {
            this.reportBuilder = p_296204_;
            this.onReportChanged();
         }));
      }).width(280).build());
      this.selectReasonButton = Button.builder(SELECT_REASON, (p_296210_) -> {
         this.minecraft.setScreen(new ReportReasonSelectionScreen(this, this.reportBuilder.reason(), (p_296212_) -> {
            this.reportBuilder.setReason(p_296212_);
            this.onReportChanged();
         }));
      }).width(280).build();
      this.layout.addChild(CommonLayouts.labeledElement(this.font, this.selectReasonButton, OBSERVED_WHAT_LABEL));
      this.commentBox = this.createCommentBox(280, 9 * 8, (p_296206_) -> {
         this.reportBuilder.setComments(p_296206_);
         this.onReportChanged();
      });
      this.layout.addChild(CommonLayouts.labeledElement(this.font, this.commentBox, MORE_COMMENTS_LABEL, (p_296209_) -> {
         p_296209_.paddingBottom(12);
      }));
      LinearLayout linearlayout = this.layout.addChild(LinearLayout.horizontal().spacing(8));
      linearlayout.addChild(Button.builder(CommonComponents.GUI_BACK, (p_308209_) -> {
         this.onClose();
      }).width(120).build());
      this.sendButton = linearlayout.addChild(Button.builder(SEND_REPORT, (p_308208_) -> {
         this.sendReport();
      }).width(120).build());
      this.layout.visitWidgets((p_296208_) -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_296208_);
      });
      this.repositionElements();
      this.onReportChanged();
   }

   protected void repositionElements() {
      this.layout.arrangeElements();
      FrameLayout.centerInRectangle(this.layout, this.getRectangle());
   }

   private void onReportChanged() {
      IntSet intset = this.reportBuilder.reportedMessages();
      if (intset.isEmpty()) {
         this.selectMessagesButton.setMessage(SELECT_CHAT_MESSAGE);
      } else {
         this.selectMessagesButton.setMessage(Component.translatable("gui.chatReport.selected_chat", intset.size()));
      }

      ReportReason reportreason = this.reportBuilder.reason();
      if (reportreason != null) {
         this.selectReasonButton.setMessage(reportreason.title());
      } else {
         this.selectReasonButton.setMessage(SELECT_REASON);
      }

      Report.CannotBuildReason report$cannotbuildreason = this.reportBuilder.checkBuildable();
      this.sendButton.active = report$cannotbuildreason == null;
      this.sendButton.setTooltip(Optionull.map(report$cannotbuildreason, Report.CannotBuildReason::tooltip));
   }

   public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
      return super.mouseReleased(pMouseX, pMouseY, pButton) ? true : this.commentBox.mouseReleased(pMouseX, pMouseY, pButton);
   }
}