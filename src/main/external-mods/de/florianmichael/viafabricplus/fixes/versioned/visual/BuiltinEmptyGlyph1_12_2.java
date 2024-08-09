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

package de.florianmichael.viafabricplus.fixes.versioned.visual;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import java.util.function.Function;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;

/**
 * Implementation of a blank glyph for 1.12.2 and lower since those versions don't draw a white rectangle for empty
 * glyphs but instead just skip them. See {@link de.florianmichael.viafabricplus.injection.mixin.fixes.minecraft.MixinFontStorage} for more information.
 */
public enum BuiltinEmptyGlyph1_12_2 implements GlyphInfo {
    INSTANCE;

    private static final int WIDTH = 0;
    private static final int HEIGHT = 8;

    @Override
    public float getAdvance() {
        return WIDTH;
    }

    @Override
    public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> glyphRendererGetter) {
        return glyphRendererGetter.apply(new SheetGlyphInfo() {

            @Override
            public int getPixelWidth() {
                return WIDTH;
            }

            @Override
            public int getPixelHeight() {
                return HEIGHT;
            }

            @Override
            public float getOversample() {
                return 1F;
            }

            @Override
            public void upload(int x, int y) {
            }

            @Override
            public boolean isColored() {
                return true;
            }
        });
    }

}
