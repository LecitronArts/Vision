package net.minecraft.client.resources.language;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import icyllis.modernui.mc.text.FormattedTextWrapper;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import net.optifine.Lang;
import org.slf4j.Logger;

public class ClientLanguage extends Language {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<String, String> storage;
   private final boolean defaultRightToLeft;

   private ClientLanguage(Map<String, String> pStorage, boolean pDefaultRightToLeft) {
      this.storage = pStorage;
      this.defaultRightToLeft = pDefaultRightToLeft;
   }

   public static ClientLanguage loadFrom(ResourceManager pResourceManager, List<String> pFilenames, boolean pDefaultRightToLeft) {
      Map<String, String> map = Maps.newHashMap();

      for(String s : pFilenames) {
         String s1 = String.format(Locale.ROOT, "lang/%s.json", s);

         for(String s2 : pResourceManager.getNamespaces()) {
            try {
               ResourceLocation resourcelocation = new ResourceLocation(s2, s1);
               appendFrom(s, pResourceManager.getResourceStack(resourcelocation), map);
               Lang.loadResources(pResourceManager, s, map);
            } catch (Exception exception1) {
               LOGGER.warn("Skipped language file: {}:{} ({})", s2, s1, exception1.toString());
            }
         }
      }

      return new ClientLanguage(ImmutableMap.copyOf(map), pDefaultRightToLeft);
   }

   private static void appendFrom(String pLanguageName, List<Resource> pResources, Map<String, String> pDestinationMap) {
      for(Resource resource : pResources) {
         try (InputStream inputstream = resource.open()) {
            Language.loadFromJson(inputstream, pDestinationMap::put);
         } catch (IOException ioexception1) {
            LOGGER.warn("Failed to load translations for {} from pack {}", pLanguageName, resource.sourcePackId(), ioexception1);
         }
      }

   }

   public String getOrDefault(String pKey, String pDefaultValue) {
      return this.storage.getOrDefault(pKey, pDefaultValue);
   }

   public boolean has(String pId) {
      return this.storage.containsKey(pId);
   }

   public boolean isDefaultRightToLeft() {
      return this.defaultRightToLeft;
   }

   public FormattedCharSequence getVisualOrder(FormattedText pText) {
      return new FormattedTextWrapper(pText);
   }

   public Map<String, String> getLanguageData() {
      return this.storage;
   }
}