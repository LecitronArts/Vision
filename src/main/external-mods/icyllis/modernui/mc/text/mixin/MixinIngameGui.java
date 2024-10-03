/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.mc.text.mixin;

import icyllis.modernui.mc.text.ModernTextRenderer;
import icyllis.modernui.mc.text.TextLayoutEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinIngameGui {

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    protected int screenWidth;

    @Shadow
    protected int screenHeight;

    @Shadow
    public abstract Font getFont();

    @Redirect(
            method = "renderExperienceBar",
            at = @At(value = "FIELD", target = "net/minecraft/client/player/LocalPlayer.experienceLevel:I",
                    opcode = Opcodes.GETFIELD)
    )
    private int fakeExperience(LocalPlayer player) {
        return 0;
    }

    @Inject(method = "renderExperienceBar", at = @At("TAIL"))
    private void drawExperience(GuiGraphics gr, int i, CallbackInfo ci) {
        LocalPlayer player = minecraft.player;
        if (player != null && player.experienceLevel > 0) {
            String s = Integer.toString(player.experienceLevel);
            TextLayoutEngine engine = TextLayoutEngine.getInstance();
            float w = engine.getStringSplitter().measureText(s);
            float x = (screenWidth - w) / 2;
            float y = screenHeight - 31 - 4;
            float offset = ModernTextRenderer.sOutlineOffset;
            Matrix4f pose = gr.pose().last().pose();
            // end batch for each draw to prevent transparency sorting
            MultiBufferSource.BufferSource source = gr.bufferSource();
            engine.getTextRenderer().drawText(s, x + offset, y, 0xff000000, false,
                    pose, source, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            source.endBatch();
            engine.getTextRenderer().drawText(s, x - offset, y, 0xff000000, false,
                    pose, source, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            source.endBatch();
            engine.getTextRenderer().drawText(s, x, y + offset, 0xff000000, false,
                    pose, source, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            source.endBatch();
            engine.getTextRenderer().drawText(s, x, y - offset, 0xff000000, false,
                    pose, source, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            source.endBatch();
            engine.getTextRenderer().drawText(s, x, y, 0xff80ff20, false,
                    pose, source, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            gr.flush();
        }
    }
}
