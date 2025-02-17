package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.Unit;
import com.mojang.realmsclient.client.FileUpload;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.client.UploadStatus;
import com.mojang.realmsclient.dto.UploadInfo;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.exception.RetryCallException;
import com.mojang.realmsclient.util.UploadTokenCache;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsUploadScreen extends RealmsScreen {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final ReentrantLock UPLOAD_LOCK = new ReentrantLock();
   private static final int BAR_WIDTH = 200;
   private static final int BAR_TOP = 80;
   private static final int BAR_BOTTOM = 95;
   private static final int BAR_BORDER = 1;
   private static final String[] DOTS = new String[]{"", ".", ". .", ". . ."};
   private static final Component VERIFYING_TEXT = Component.translatable("mco.upload.verifying");
   private final RealmsResetWorldScreen lastScreen;
   private final LevelSummary selectedLevel;
   private final long worldId;
   private final int slotId;
   private final UploadStatus uploadStatus;
   private final RateLimiter narrationRateLimiter;
   @Nullable
   private volatile Component[] errorMessage;
   private volatile Component status = Component.translatable("mco.upload.preparing");
   private volatile String progress;
   private volatile boolean cancelled;
   private volatile boolean uploadFinished;
   private volatile boolean showDots = true;
   private volatile boolean uploadStarted;
   private Button backButton;
   private Button cancelButton;
   private int tickCount;
   @Nullable
   private Long previousWrittenBytes;
   @Nullable
   private Long previousTimeSnapshot;
   private long bytesPersSecond;

   public RealmsUploadScreen(long pWorldId, int pSlotId, RealmsResetWorldScreen pLastScreen, LevelSummary pSelectedLevel) {
      super(GameNarrator.NO_TITLE);
      this.worldId = pWorldId;
      this.slotId = pSlotId;
      this.lastScreen = pLastScreen;
      this.selectedLevel = pSelectedLevel;
      this.uploadStatus = new UploadStatus();
      this.narrationRateLimiter = RateLimiter.create((double)0.1F);
   }

   public void init() {
      this.backButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, (p_90118_) -> {
         this.onBack();
      }).bounds((this.width - 200) / 2, this.height - 42, 200, 20).build());
      this.backButton.visible = false;
      this.cancelButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, (p_90104_) -> {
         this.onCancel();
      }).bounds((this.width - 200) / 2, this.height - 42, 200, 20).build());
      if (!this.uploadStarted) {
         if (this.lastScreen.slot == -1) {
            this.upload();
         } else {
            this.lastScreen.switchSlot(() -> {
               if (!this.uploadStarted) {
                  this.uploadStarted = true;
                  this.minecraft.setScreen(this);
                  this.upload();
               }

            });
         }
      }

   }

   private void onBack() {
      this.minecraft.setScreen(new RealmsConfigureWorldScreen(new RealmsMainScreen(new TitleScreen()), this.worldId));
   }

   private void onCancel() {
      this.cancelled = true;
      this.minecraft.setScreen(this.lastScreen);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         if (this.showDots) {
            this.onCancel();
         } else {
            this.onBack();
         }

         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }

   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      if (!this.uploadFinished && this.uploadStatus.bytesWritten != 0L && this.uploadStatus.bytesWritten == this.uploadStatus.totalBytes) {
         this.status = VERIFYING_TEXT;
         this.cancelButton.active = false;
      }

      pGuiGraphics.drawCenteredString(this.font, this.status, this.width / 2, 50, 16777215);
      if (this.showDots) {
         this.drawDots(pGuiGraphics);
      }

      if (this.uploadStatus.bytesWritten != 0L && !this.cancelled) {
         this.drawProgressBar(pGuiGraphics);
         this.drawUploadSpeed(pGuiGraphics);
      }

      if (this.errorMessage != null) {
         for(int i = 0; i < this.errorMessage.length; ++i) {
            pGuiGraphics.drawCenteredString(this.font, this.errorMessage[i], this.width / 2, 110 + 12 * i, 16711680);
         }
      }

   }

   private void drawDots(GuiGraphics pGuiGraphics) {
      int i = this.font.width(this.status);
      pGuiGraphics.drawString(this.font, DOTS[this.tickCount / 10 % DOTS.length], this.width / 2 + i / 2 + 5, 50, 16777215, false);
   }

   private void drawProgressBar(GuiGraphics pGuiGraphics) {
      double d0 = Math.min((double)this.uploadStatus.bytesWritten / (double)this.uploadStatus.totalBytes, 1.0D);
      this.progress = String.format(Locale.ROOT, "%.1f", d0 * 100.0D);
      int i = (this.width - 200) / 2;
      int j = i + (int)Math.round(200.0D * d0);
      pGuiGraphics.fill(i - 1, 79, j + 1, 96, -2501934);
      pGuiGraphics.fill(i, 80, j, 95, -8355712);
      pGuiGraphics.drawCenteredString(this.font, Component.translatable("mco.upload.percent", this.progress), this.width / 2, 84, 16777215);
   }

   private void drawUploadSpeed(GuiGraphics pGuiGraphics) {
      if (this.tickCount % 20 == 0) {
         if (this.previousWrittenBytes != null) {
            long i = Util.getMillis() - this.previousTimeSnapshot;
            if (i == 0L) {
               i = 1L;
            }

            this.bytesPersSecond = 1000L * (this.uploadStatus.bytesWritten - this.previousWrittenBytes) / i;
            this.drawUploadSpeed0(pGuiGraphics, this.bytesPersSecond);
         }

         this.previousWrittenBytes = this.uploadStatus.bytesWritten;
         this.previousTimeSnapshot = Util.getMillis();
      } else {
         this.drawUploadSpeed0(pGuiGraphics, this.bytesPersSecond);
      }

   }

   private void drawUploadSpeed0(GuiGraphics pGuiGraphics, long pBytesPerSecond) {
      if (pBytesPerSecond > 0L) {
         int i = this.font.width(this.progress);
         String s = "(" + Unit.humanReadable(pBytesPerSecond) + "/s)";
         pGuiGraphics.drawString(this.font, s, this.width / 2 + i / 2 + 15, 84, 16777215, false);
      }

   }

   public void tick() {
      super.tick();
      ++this.tickCount;
      if (this.status != null && this.narrationRateLimiter.tryAcquire(1)) {
         Component component = this.createProgressNarrationMessage();
         this.minecraft.getNarrator().sayNow(component);
      }

   }

   private Component createProgressNarrationMessage() {
      List<Component> list = Lists.newArrayList();
      list.add(this.status);
      if (this.progress != null) {
         list.add(Component.translatable("mco.upload.percent", this.progress));
      }

      if (this.errorMessage != null) {
         list.addAll(Arrays.asList(this.errorMessage));
      }

      return CommonComponents.joinLines(list);
   }

   private void upload() {
      this.uploadStarted = true;
      (new Thread(() -> {
         File file1 = null;
         RealmsClient realmsclient = RealmsClient.create();
         long i = this.worldId;

         try {
            if (UPLOAD_LOCK.tryLock(1L, TimeUnit.SECONDS)) {
               UploadInfo uploadinfo = null;

               for(int j = 0; j < 20; ++j) {
                  try {
                     if (this.cancelled) {
                        this.uploadCancelled();
                        return;
                     }

                     uploadinfo = realmsclient.requestUploadInfo(i, UploadTokenCache.get(i));
                     if (uploadinfo != null) {
                        break;
                     }
                  } catch (RetryCallException retrycallexception) {
                     Thread.sleep((long)(retrycallexception.delaySeconds * 1000));
                  }
               }

               if (uploadinfo == null) {
                  this.status = Component.translatable("mco.upload.close.failure");
                  return;
               }

               UploadTokenCache.put(i, uploadinfo.getToken());
               if (!uploadinfo.isWorldClosed()) {
                  this.status = Component.translatable("mco.upload.close.failure");
                  return;
               }

               if (this.cancelled) {
                  this.uploadCancelled();
                  return;
               }

               File file2 = new File(this.minecraft.gameDirectory.getAbsolutePath(), "saves");
               file1 = this.tarGzipArchive(new File(file2, this.selectedLevel.getLevelId()));
               if (this.cancelled) {
                  this.uploadCancelled();
                  return;
               }

               if (this.verify(file1)) {
                  this.status = Component.translatable("mco.upload.uploading", this.selectedLevel.getLevelName());
                  FileUpload fileupload = new FileUpload(file1, this.worldId, this.slotId, uploadinfo, this.minecraft.getUser(), SharedConstants.getCurrentVersion().getName(), this.uploadStatus);
                  fileupload.upload((p_167557_) -> {
                     if (p_167557_.statusCode >= 200 && p_167557_.statusCode < 300) {
                        this.uploadFinished = true;
                        this.status = Component.translatable("mco.upload.done");
                        this.backButton.setMessage(CommonComponents.GUI_DONE);
                        UploadTokenCache.invalidate(i);
                     } else if (p_167557_.statusCode == 400 && p_167557_.errorMessage != null) {
                        this.setErrorMessage(Component.translatable("mco.upload.failed", p_167557_.errorMessage));
                     } else {
                        this.setErrorMessage(Component.translatable("mco.upload.failed", p_167557_.statusCode));
                     }

                  });

                  while(!fileupload.isFinished()) {
                     if (this.cancelled) {
                        fileupload.cancel();
                        this.uploadCancelled();
                        return;
                     }

                     try {
                        Thread.sleep(500L);
                     } catch (InterruptedException interruptedexception) {
                        LOGGER.error("Failed to check Realms file upload status");
                     }
                  }

                  return;
               }

               long k = file1.length();
               Unit unit = Unit.getLargest(k);
               Unit unit1 = Unit.getLargest(5368709120L);
               if (Unit.humanReadable(k, unit).equals(Unit.humanReadable(5368709120L, unit1)) && unit != Unit.B) {
                  Unit unit2 = Unit.values()[unit.ordinal() - 1];
                  this.setErrorMessage(Component.translatable("mco.upload.size.failure.line1", this.selectedLevel.getLevelName()), Component.translatable("mco.upload.size.failure.line2", Unit.humanReadable(k, unit2), Unit.humanReadable(5368709120L, unit2)));
                  return;
               }

               this.setErrorMessage(Component.translatable("mco.upload.size.failure.line1", this.selectedLevel.getLevelName()), Component.translatable("mco.upload.size.failure.line2", Unit.humanReadable(k, unit), Unit.humanReadable(5368709120L, unit1)));
               return;
            }

            this.status = Component.translatable("mco.upload.close.failure");
         } catch (IOException ioexception) {
            this.setErrorMessage(Component.translatable("mco.upload.failed", ioexception.getMessage()));
            return;
         } catch (RealmsServiceException realmsserviceexception) {
            this.setErrorMessage(Component.translatable("mco.upload.failed", realmsserviceexception.realmsError.errorMessage()));
            return;
         } catch (InterruptedException interruptedexception1) {
            LOGGER.error("Could not acquire upload lock");
            return;
         } finally {
            this.uploadFinished = true;
            if (UPLOAD_LOCK.isHeldByCurrentThread()) {
               UPLOAD_LOCK.unlock();
               this.showDots = false;
               this.backButton.visible = true;
               this.cancelButton.visible = false;
               if (file1 != null) {
                  LOGGER.debug("Deleting file {}", (Object)file1.getAbsolutePath());
                  file1.delete();
               }

            }

            return;
         }

      })).start();
   }

   private void setErrorMessage(Component... pErrorMessage) {
      this.errorMessage = pErrorMessage;
   }

   private void uploadCancelled() {
      this.status = Component.translatable("mco.upload.cancelled");
      LOGGER.debug("Upload was cancelled");
   }

   private boolean verify(File pFile) {
      return pFile.length() < 5368709120L;
   }

   private File tarGzipArchive(File pFile) throws IOException {
      TarArchiveOutputStream tararchiveoutputstream = null;

      File file2;
      try {
         File file1 = File.createTempFile("realms-upload-file", ".tar.gz");
         tararchiveoutputstream = new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(file1)));
         tararchiveoutputstream.setLongFileMode(3);
         this.addFileToTarGz(tararchiveoutputstream, pFile.getAbsolutePath(), "world", true);
         tararchiveoutputstream.finish();
         file2 = file1;
      } finally {
         if (tararchiveoutputstream != null) {
            tararchiveoutputstream.close();
         }

      }

      return file2;
   }

   private void addFileToTarGz(TarArchiveOutputStream pTarArchiveOutputStream, String pPathname, String pName, boolean pRootDirectory) throws IOException {
      if (!this.cancelled) {
         File file1 = new File(pPathname);
         String s = pRootDirectory ? pName : pName + file1.getName();
         TarArchiveEntry tararchiveentry = new TarArchiveEntry(file1, s);
         pTarArchiveOutputStream.putArchiveEntry(tararchiveentry);
         if (file1.isFile()) {
            IOUtils.copy(new FileInputStream(file1), pTarArchiveOutputStream);
            pTarArchiveOutputStream.closeArchiveEntry();
         } else {
            pTarArchiveOutputStream.closeArchiveEntry();
            File[] afile = file1.listFiles();
            if (afile != null) {
               for(File file2 : afile) {
                  this.addFileToTarGz(pTarArchiveOutputStream, file2.getAbsolutePath(), s + "/", false);
               }
            }
         }

      }
   }
}