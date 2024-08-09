package net.minecraft.world.level.storage.loot.functions;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class SetNameFunction extends LootItemConditionalFunction {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<SetNameFunction> CODEC = RecordCodecBuilder.create((p_309353_) -> {
      return commonFields(p_309353_).and(p_309353_.group(ExtraCodecs.strictOptionalField(ComponentSerialization.CODEC, "name").forGetter((p_297155_) -> {
         return p_297155_.name;
      }), ExtraCodecs.strictOptionalField(LootContext.EntityTarget.CODEC, "entity").forGetter((p_297165_) -> {
         return p_297165_.resolutionContext;
      }))).apply(p_309353_, SetNameFunction::new);
   });
   private final Optional<Component> name;
   private final Optional<LootContext.EntityTarget> resolutionContext;

   private SetNameFunction(List<LootItemCondition> p_298434_, Optional<Component> p_299902_, Optional<LootContext.EntityTarget> p_300668_) {
      super(p_298434_);
      this.name = p_299902_;
      this.resolutionContext = p_300668_;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.SET_NAME;
   }

   public Set<LootContextParam<?>> getReferencedContextParams() {
      return (Set<LootContextParam<?>>)(Set)this.resolutionContext.map((p_297154_) -> {
         return Set.of(p_297154_.getParam());
      }).orElse(Set.of());
   }

   public static UnaryOperator<Component> createResolver(LootContext pLootContext, @Nullable LootContext.EntityTarget pResolutionContext) {
      if (pResolutionContext != null) {
         Entity entity = pLootContext.getParamOrNull(pResolutionContext.getParam());
         if (entity != null) {
            CommandSourceStack commandsourcestack = entity.createCommandSourceStack().withPermission(2);
            return (p_81147_) -> {
               try {
                  return ComponentUtils.updateForEntity(commandsourcestack, p_81147_, entity, 0);
               } catch (CommandSyntaxException commandsyntaxexception) {
                  LOGGER.warn("Failed to resolve text component", (Throwable)commandsyntaxexception);
                  return p_81147_;
               }
            };
         }
      }

      return (p_81152_) -> {
         return p_81152_;
      };
   }

   public ItemStack run(ItemStack pStack, LootContext pContext) {
      this.name.ifPresent((p_297161_) -> {
         pStack.setHoverName(createResolver(pContext, this.resolutionContext.orElse((LootContext.EntityTarget)null)).apply(p_297161_));
      });
      return pStack;
   }

   public static LootItemConditionalFunction.Builder<?> setName(Component pName) {
      return simpleBuilder((p_297158_) -> {
         return new SetNameFunction(p_297158_, Optional.of(pName), Optional.empty());
      });
   }

   public static LootItemConditionalFunction.Builder<?> setName(Component pName, LootContext.EntityTarget pResolutionContext) {
      return simpleBuilder((p_297164_) -> {
         return new SetNameFunction(p_297164_, Optional.of(pName), Optional.of(pResolutionContext));
      });
   }
}