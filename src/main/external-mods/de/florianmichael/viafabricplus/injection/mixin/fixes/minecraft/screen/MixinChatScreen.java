/*
 * This file is part of ViaFabricPlus - https://github.com/FlorianMichael/ViaFabricPlus
 * Copyright (C) 2021-2024 FlorianMichael/EnZaXD <florian.michael07@gmail.com> and RK_01/RaphiMC
 * Copyright (C) 2023-2024 contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.florianmichael.viafabricplus.injection.mixin.fixes.minecraft.screen;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.viafabricplus.fixes.ClientsideFixes;
import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen {

    @Shadow
    protected EditBox input;

    @Shadow
    private String initial;

    @Shadow
    CommandSuggestions commandSuggestions;

    @Inject(method = "init", at = @At("RETURN"))
    private void changeChatLength(CallbackInfo ci) {
        this.input.setMaxLength(ClientsideFixes.getChatLength());
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;getMessageTagAt(DD)Lnet/minecraft/client/GuiMessageTag;"))
    private GuiMessageTag removeIndicator(ChatComponent instance, double mouseX, double mouseY) {
        if (VisualSettings.global().hideSignatureIndicator.isEnabled()) {
            return null;
        } else {
            return instance.getMessageTagAt(mouseX, mouseY);
        }
    }

    @WrapWithCondition(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/EditBox;setValue(Ljava/lang/String;)V"))
    public boolean moveSetTextDown(EditBox instance, String text) {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_12_2);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void moveSetTextDown(CallbackInfo ci) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            this.input.setValue(this.initial);
            this.commandSuggestions.updateCommandInfo();
        }
    }

    @Redirect(method = "onEdited", at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z"))
    private boolean fixCommandKey(String instance, Object other) {
        if (this.viaFabricPlus$keepTabComplete()) {
            return instance.equals(other);
        } else {
            return instance.isEmpty();
        }
    }

    @WrapWithCondition(method = "onEdited", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/CommandSuggestions;updateCommandInfo()V"))
    private boolean disableAutoTabComplete(CommandSuggestions instance) {
        return this.viaFabricPlus$keepTabComplete();
    }

    @Unique
    private boolean viaFabricPlus$keepTabComplete() {
        return ProtocolTranslator.getTargetVersion().newerThan(ProtocolVersion.v1_12_2) || !this.input.getValue().startsWith("/");
    }

}
