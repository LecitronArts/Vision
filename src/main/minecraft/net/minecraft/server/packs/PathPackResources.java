package net.minecraft.server.packs;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;

public class PathPackResources extends AbstractPackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Joiner PATH_JOINER = Joiner.on("/");
   public final Path root;

   public PathPackResources(String pName, Path pRoot, boolean pIsBuiltin) {
      super(pName, pIsBuiltin);
      this.root = pRoot;
   }

   @Nullable
   public IoSupplier<InputStream> getRootResource(String... pElements) {
      FileUtil.validatePath(pElements);
      Path path = FileUtil.resolvePath(this.root, List.of(pElements));
      return Files.exists(path) ? IoSupplier.create(path) : null;
   }

   public static boolean validatePath(Path pPath) {
      return true;
   }

   @Nullable
   public IoSupplier<InputStream> getResource(PackType pPackType, ResourceLocation pLocation) {
      Path path = this.root.resolve(pPackType.getDirectory()).resolve(pLocation.getNamespace());
      return getResource(pLocation, path);
   }

   public static IoSupplier<InputStream> getResource(ResourceLocation pLocation, Path pPath) {
      return FileUtil.decomposePath(pLocation.getPath()).get().map((listIn) -> {
         Path path = FileUtil.resolvePath(pPath, listIn);
         return returnFileIfExists(path);
      }, (p_246537_1_) -> {
         LOGGER.error("Invalid path {}: {}", pLocation, p_246537_1_.message());
         return null;
      });
   }

   @Nullable
   private static IoSupplier<InputStream> returnFileIfExists(Path pPath) {
      return Files.exists(pPath) && validatePath(pPath) ? IoSupplier.create(pPath) : null;
   }

   public void listResources(PackType pPackType, String pNamespace, String pPath, PackResources.ResourceOutput pResourceOutput) {
      FileUtil.decomposePath(pPath).get().ifLeft((listIn) -> {
         Path path = this.root.resolve(pPackType.getDirectory()).resolve(pNamespace);
         listPath(pNamespace, path, listIn, pResourceOutput);
      }).ifRight((p_246950_1_) -> {
         LOGGER.error("Invalid path {}: {}", pPath, p_246950_1_.message());
      });
   }

   public static void listPath(String pNamespace, Path pNamespacePath, List<String> pDecomposedPath, PackResources.ResourceOutput pResourceOutput) {
      Path path = FileUtil.resolvePath(pNamespacePath, pDecomposedPath);

      try (Stream<Path> stream = Files.find(path, Integer.MAX_VALUE, (path2In, attrsIn) -> {
            return attrsIn.isRegularFile();
         })) {
         stream.forEach((path3In) -> {
            String s = PATH_JOINER.join(pNamespacePath.relativize(path3In));
            ResourceLocation resourcelocation = ResourceLocation.tryBuild(pNamespace, s);
            if (resourcelocation == null) {
               Util.logAndPauseIfInIde(String.format(Locale.ROOT, "Invalid path in pack: %s:%s, ignoring", pNamespace, s));
            } else {
               pResourceOutput.accept(resourcelocation, IoSupplier.create(path3In));
            }

         });
      } catch (NoSuchFileException | NotDirectoryException notdirectoryexception) {
      } catch (IOException ioexception1) {
         LOGGER.error("Failed to list path {}", path, ioexception1);
      }

   }

   public Set<String> getNamespaces(PackType pType) {
      Set<String> set = Sets.newHashSet();
      Path path = this.root.resolve(pType.getDirectory());

      try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(path)) {
         for(Path path1 : directorystream) {
            String s = path1.getFileName().toString();
            if (ResourceLocation.isValidNamespace(s)) {
               set.add(s);
            } else {
               LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", s, this.root);
            }
         }
      } catch (NoSuchFileException | NotDirectoryException notdirectoryexception) {
      } catch (IOException ioexception1) {
         LOGGER.error("Failed to list path {}", path, ioexception1);
      }

      return set;
   }

   public void close() {
   }

   public static class PathResourcesSupplier implements Pack.ResourcesSupplier {
      private final Path content;
      private final boolean isBuiltin;

      public PathResourcesSupplier(Path pContent, boolean pIsBuiltin) {
         this.content = pContent;
         this.isBuiltin = pIsBuiltin;
      }

      public PackResources openPrimary(String pId) {
         return new PathPackResources(pId, this.content, this.isBuiltin);
      }

      public PackResources openFull(String pId, Pack.Info pInfo) {
         PackResources packresources = this.openPrimary(pId);
         List<String> list = pInfo.overlays();
         if (list.isEmpty()) {
            return packresources;
         } else {
            List<PackResources> list1 = new ArrayList<>(list.size());

            for(String s : list) {
               Path path = this.content.resolve(s);
               list1.add(new PathPackResources(pId, path, this.isBuiltin));
            }

            return new CompositePackResources(packresources, list1);
         }
      }
   }
}