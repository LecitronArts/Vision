package net.minecraft.data.structures;

import com.google.common.collect.Lists;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class SnbtToNbt implements DataProvider {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackOutput output;
   private final Iterable<Path> inputFolders;
   private final List<SnbtToNbt.Filter> filters = Lists.newArrayList();

   public SnbtToNbt(PackOutput pOutput, Iterable<Path> pInputFolders) {
      this.output = pOutput;
      this.inputFolders = pInputFolders;
   }

   public SnbtToNbt addFilter(SnbtToNbt.Filter pFilter) {
      this.filters.add(pFilter);
      return this;
   }

   private CompoundTag applyFilters(String pFileName, CompoundTag pTag) {
      CompoundTag compoundtag = pTag;

      for(SnbtToNbt.Filter snbttonbt$filter : this.filters) {
         compoundtag = snbttonbt$filter.apply(pFileName, compoundtag);
      }

      return compoundtag;
   }

   public CompletableFuture<?> run(CachedOutput pOutput) {
      Path path = this.output.getOutputFolder();
      List<CompletableFuture<?>> list = Lists.newArrayList();

      for(Path path1 : this.inputFolders) {
         list.add(CompletableFuture.supplyAsync(() -> {
            try (Stream<Path> stream = Files.walk(path1)) {
               return CompletableFuture.allOf(stream.filter((p_126464_) -> {
                  return p_126464_.toString().endsWith(".snbt");
               }).map((p_253432_) -> {
                  return CompletableFuture.runAsync(() -> {
                     SnbtToNbt.TaskResult snbttonbt$taskresult = this.readStructure(p_253432_, this.getName(path1, p_253432_));
                     this.storeStructureIfChanged(pOutput, snbttonbt$taskresult, path);
                  }, Util.backgroundExecutor());
               }).toArray((p_253433_) -> {
                  return new CompletableFuture[p_253433_];
               }));
            } catch (Exception exception) {
               throw new RuntimeException("Failed to read structure input directory, aborting", exception);
            }
         }, Util.backgroundExecutor()).thenCompose((p_253441_) -> {
            return p_253441_;
         }));
      }

      return Util.sequenceFailFast(list);
   }

   public final String getName() {
      return "SNBT -> NBT";
   }

   private String getName(Path pInputFolder, Path pFile) {
      String s = pInputFolder.relativize(pFile).toString().replaceAll("\\\\", "/");
      return s.substring(0, s.length() - ".snbt".length());
   }

   private SnbtToNbt.TaskResult readStructure(Path pFilePath, String pFileName) {
      try (BufferedReader bufferedreader = Files.newBufferedReader(pFilePath)) {
         String s = IOUtils.toString((Reader)bufferedreader);
         CompoundTag compoundtag = this.applyFilters(pFileName, NbtUtils.snbtToStructure(s));
         ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
         HashingOutputStream hashingoutputstream = new HashingOutputStream(Hashing.sha1(), bytearrayoutputstream);
         NbtIo.writeCompressed(compoundtag, hashingoutputstream);
         byte[] abyte = bytearrayoutputstream.toByteArray();
         HashCode hashcode = hashingoutputstream.hash();
         return new SnbtToNbt.TaskResult(pFileName, abyte, hashcode);
      } catch (Throwable throwable) {
         throw new SnbtToNbt.StructureConversionException(pFilePath, throwable);
      }
   }

   private void storeStructureIfChanged(CachedOutput pOutput, SnbtToNbt.TaskResult pTaskResult, Path pDirectoryPath) {
      Path path = pDirectoryPath.resolve(pTaskResult.name + ".nbt");

      try {
         pOutput.writeIfNeeded(path, pTaskResult.payload, pTaskResult.hash);
      } catch (IOException ioexception) {
         LOGGER.error("Couldn't write structure {} at {}", pTaskResult.name, path, ioexception);
      }

   }

   @FunctionalInterface
   public interface Filter {
      CompoundTag apply(String pStructureLocationPath, CompoundTag pTag);
   }

   static class StructureConversionException extends RuntimeException {
      public StructureConversionException(Path pPath, Throwable pCause) {
         super(pPath.toAbsolutePath().toString(), pCause);
      }
   }

   static record TaskResult(String name, byte[] payload, HashCode hash) {
   }
}