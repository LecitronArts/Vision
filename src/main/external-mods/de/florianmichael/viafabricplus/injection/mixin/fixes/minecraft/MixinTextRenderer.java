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

package de.florianmichael.viafabricplus.injection.mixin.fixes.minecraft;

import de.florianmichael.viafabricplus.protocoltranslator.ProtocolTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Font.class)
public abstract class MixinTextRenderer {

    @Shadow
    public abstract List<FormattedCharSequence> split(FormattedText text, int width);

    @Shadow
    public abstract int drawInBatch(FormattedCharSequence text, float x, float y, int color, boolean shadow, Matrix4f matrix, MultiBufferSource vertexConsumers, Font.DisplayMode layerType, int backgroundColor, int light);

    @Shadow
    public abstract String bidirectionalShaping(String text);

    @Shadow
    @Final
    public int lineHeight;

    @Shadow
    protected abstract int drawInternal(FormattedCharSequence text, float x, float y, int color, boolean shadow, Matrix4f matrix, MultiBufferSource vertexConsumers, Font.DisplayMode layerType, int backgroundColor, int light);

    @Shadow
    public abstract int width(FormattedCharSequence text);

    @Inject(method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I", at = @At("HEAD"), cancellable = true)
    private void allowNewLines_String(String text, float x, float y, int color, boolean shadow, Matrix4f matrix, MultiBufferSource vertexConsumers, Font.DisplayMode layerType, int backgroundColor, int light, boolean rightToLeft, CallbackInfoReturnable<Integer> cir) {
        if (ProtocolTranslator.getTargetVersion() == BedrockProtocolVersion.bedrockLatest) {
            final List<FormattedCharSequence> lines = split(FormattedText.of(rightToLeft ? this.bidirectionalShaping(text) : text), Integer.MAX_VALUE);
            if (lines.size() > 1) {
                int offsetX = 0;
                for (int i = 0; i < lines.size(); i++) {
                    offsetX = this.drawInternal(lines.get(i), x, y - (lines.size() * (lineHeight + 2)) + (i * (lineHeight + 2)), color, shadow, new Matrix4f(matrix), vertexConsumers, layerType, backgroundColor, light);
                }
                cir.setReturnValue(offsetX);
            }
        }
    }

    @Inject(method = "drawInBatch(Lnet/minecraft/network/chat/Component;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I", at = @At("HEAD"), cancellable = true)
    private void allowNewLines_Text(Component text, float x, float y, int color, boolean shadow, Matrix4f matrix, MultiBufferSource vertexConsumers, Font.DisplayMode layerType, int backgroundColor, int light, CallbackInfoReturnable<Integer> cir) {
        if (ProtocolTranslator.getTargetVersion() == BedrockProtocolVersion.bedrockLatest) {
            final List<FormattedCharSequence> lines = split(text, Integer.MAX_VALUE);
            if (lines.size() > 1) {
                int offsetX = 0;
                for (int i = 0; i < lines.size(); i++) {
                    offsetX = this.drawInBatch(lines.get(i), x, y - (lines.size() * (lineHeight + 2)) + (i * (lineHeight + 2)), color, shadow, new Matrix4f(matrix), vertexConsumers, layerType, backgroundColor, light);
                }
                cir.setReturnValue(offsetX);
            }
        }
    }

    @Inject(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), cancellable = true)
    private void allowNewLines_getWidth(FormattedText text, CallbackInfoReturnable<Integer> cir) {
        if (Minecraft.getInstance().level != null && ProtocolTranslator.getTargetVersion() == BedrockProtocolVersion.bedrockLatest) {
            int i = 0;
            for (FormattedCharSequence wrapLine : this.split(text, Integer.MAX_VALUE)) {
                if (width(wrapLine) >= i) i = width(wrapLine);
            }
            cir.setReturnValue(Mth.ceil(i));
        }
    }

}
