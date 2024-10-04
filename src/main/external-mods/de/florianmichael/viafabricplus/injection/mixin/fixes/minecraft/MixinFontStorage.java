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

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import de.florianmichael.viafabricplus.fixes.data.RenderableGlyphDiff;
import de.florianmichael.viafabricplus.fixes.versioned.visual.BuiltinEmptyGlyph1_12_2;
import de.florianmichael.viafabricplus.settings.impl.VisualSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(FontSet.class)
public abstract class MixinFontStorage {

    /*@Shadow
    private BakedGlyph missingGlyph;

    @Shadow
    protected abstract BakedGlyph stitch(SheetGlyphInfo c);

    @Shadow
    @Final
    private ResourceLocation name;

    @Unique
    private BakedGlyph viaFabricPlus$blankGlyphRenderer1_12_2;

    @Inject(method = "reload", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/font/glyphs/SpecialGlyphs;bake(Ljava/util/function/Function;)Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;", ordinal = 0))
    private void bakeBlankGlyph1_12_2(List<GlyphProvider> fonts, CallbackInfo ci) {
        this.viaFabricPlus$blankGlyphRenderer1_12_2 = BuiltinEmptyGlyph1_12_2.INSTANCE.bake(this::stitch);
    }

    @Inject(method = "computeGlyphInfo", at = @At("RETURN"), cancellable = true)
    private void filterGlyphs1(int codePoint, CallbackInfoReturnable<FontSet.GlyphInfoFilter> cir, @Local GlyphProvider font) {
        if (this.viaFabricPlus$shouldBeInvisible(codePoint)) {
            cir.setReturnValue(this.viaFabricPlus$getBlankGlyphPair());
        }
    }

    @Inject(method = "computeBakedGlyph", at = @At("RETURN"), cancellable = true)
    private void filterGlyphs2(int codePoint, CallbackInfoReturnable<BakedGlyph> cir, @Local GlyphProvider font) {
        if (this.viaFabricPlus$shouldBeInvisible(codePoint)) {
            cir.setReturnValue(this.viaFabricPlus$getBlankGlyphRenderer());
        }
    }

    @Inject(method = "computeGlyphInfo", at = @At("RETURN"), cancellable = true)
    private void fixBlankGlyph1_12_2(int codePoint, CallbackInfoReturnable<FontSet.GlyphInfoFilter> cir) {
        if (VisualSettings.global().changeFontRendererBehaviour.isEnabled()) {
            final FontSet.GlyphInfoFilter glyphPair = cir.getReturnValue();
            final GlyphInfo glyph1 = glyphPair.glyphInfo();
            final GlyphInfo glyph2 = glyphPair.glyphInfoNotFishy();
            cir.setReturnValue(new FontSet.GlyphInfoFilter(glyph1 == SpecialGlyphs.MISSING ? BuiltinEmptyGlyph1_12_2.INSTANCE : glyph1, glyph2 == SpecialGlyphs.MISSING ? BuiltinEmptyGlyph1_12_2.INSTANCE : glyph2));
        }
    }

    @Redirect(method = "computeBakedGlyph", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/font/FontSet;missingGlyph:Lnet/minecraft/client/gui/font/glyphs/BakedGlyph;"))
    private BakedGlyph fixBlankGlyphRenderer1_12_2(FontSet instance) {
        return this.viaFabricPlus$getBlankGlyphRenderer();
    }

    @Unique
    private boolean viaFabricPlus$shouldBeInvisible(final int codePoint) {
        return (this.name.equals(Minecraft.DEFAULT_FONT) || this.name.equals(Minecraft.UNIFORM_FONT)) && !RenderableGlyphDiff.isGlyphRenderable(codePoint);
    }

    @Unique
    private FontSet.GlyphInfoFilter viaFabricPlus$getBlankGlyphPair() {
        if (VisualSettings.global().changeFontRendererBehaviour.isEnabled()) {
            return new FontSet.GlyphInfoFilter(BuiltinEmptyGlyph1_12_2.INSTANCE, BuiltinEmptyGlyph1_12_2.INSTANCE);
        } else {
            return FontSet.GlyphInfoFilter.MISSING;
        }
    }

    @Unique
    private BakedGlyph viaFabricPlus$getBlankGlyphRenderer() {
        if (VisualSettings.global().changeFontRendererBehaviour.isEnabled()) {
            return this.viaFabricPlus$blankGlyphRenderer1_12_2;
        } else {
            return this.missingGlyph;
        }
    }

     */

}
