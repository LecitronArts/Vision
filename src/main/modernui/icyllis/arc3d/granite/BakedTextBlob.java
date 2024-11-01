/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.granite;

import icyllis.arc3d.core.*;

import javax.annotation.Nonnull;

/**
 * A BakedTextBlob contains a fully processed TextBlob, suitable for nearly immediate drawing
 * on the GPU.  These are initially created with valid positions and colors, but with invalid
 * texture coordinates.
 * <p>
 * A BakedTextBlob contains a number of SubRuns that are created in the blob's arena. Each SubRun
 * tracks its own glyph and position data.
 * <p>
 * In these classes, I'm trying to follow the convention about matrices and origins.
 * <ul>
 * <li> drawMatrix and drawOrigin - describes transformations for the current draw command.</li>
 * <li> positionMatrix - is equal to drawMatrix * [drawOrigin-as-translation-matrix]</li>
 * <li> initial Matrix - describes the combined initial matrix and origin the TextBlob was created
 *                    with.</li>
 * </ul>
 */
public final class BakedTextBlob {

    // LinkedList, LRU at tail, accessed by cache
    BakedTextBlob mPrev;
    BakedTextBlob mNext;
    // accessed by cache
    TextBlobCache.PrimaryKey mPrimaryKey;
    TextBlobCache.FeatureKey mFeatureKey;

    private final SubRunContainer mSubRuns;
    private final long mMemorySize;

    public BakedTextBlob(@Nonnull SubRunContainer subRuns) {
        mSubRuns = subRuns;
        long size = subRuns.getMemorySize();
        size += 16 + 8 + 8 + 8 + 8 + 40 + 8 + 16 + 8;
        mMemorySize = size;
    }

    @Nonnull
    public static BakedTextBlob make(@Nonnull GlyphRunList glyphRunList,
                                     @Nonnull Paint paint,
                                     @Nonnull Matrixc positionMatrix,
                                     @Nonnull StrikeCache strikeCache) {
        var container = SubRunContainer.make(
                glyphRunList, positionMatrix, paint,
                strikeCache
        );
        return new BakedTextBlob(container);
    }

    public void draw(Canvas canvas, float originX, float originY,
                     Paint paint, GraniteDevice device) {
        mSubRuns.draw(canvas, originX, originY, paint, device);
    }

    public long getMemorySize() {
        return mMemorySize;
    }
}
