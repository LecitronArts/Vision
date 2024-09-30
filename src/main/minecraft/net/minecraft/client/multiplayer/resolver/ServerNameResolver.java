package net.minecraft.client.multiplayer.resolver;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;

@OnlyIn(Dist.CLIENT)
public class ServerNameResolver {
   public static final ServerNameResolver DEFAULT = new ServerNameResolver(ServerAddressResolver.SYSTEM, ServerRedirectHandler.createDnsSrvRedirectHandler(), AddressCheck.createFromService());
   private final ServerAddressResolver resolver;
   public final ServerRedirectHandler redirectHandler;
   private final AddressCheck addressCheck;

   @VisibleForTesting
   ServerNameResolver(ServerAddressResolver pResolver, ServerRedirectHandler pRedirectHandler, AddressCheck pAddressCheck) {
      this.resolver = pResolver;
      this.redirectHandler = pRedirectHandler;
      this.addressCheck = pAddressCheck;
   }

   public Optional<ResolvedServerAddress> resolveAddress(ServerAddress pServerAddress) {
      if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_16_4) /*|| ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)*/) {
         return (this.resolver.resolve(pServerAddress));
      } else {
         Optional<ResolvedServerAddress> optional = this.resolver.resolve(pServerAddress);
         if ((!optional.isPresent() || this.addressCheck.isAllowed(optional.get())) && this.addressCheck.isAllowed(pServerAddress)) {
            Optional<ServerAddress> optional1 = this.redirectHandler.lookupRedirect(pServerAddress);
            if (optional1.isPresent()) {
               optional = this.resolver.resolve(optional1.get()).filter(this.addressCheck::isAllowed);
            }

            return optional;
         } else {
            return Optional.empty();
         }
      }
   }
}