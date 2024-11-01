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

package icyllis.arc3d.core;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Typeface class specifies the typeface (font face) and intrinsic style of a font.
 * This is used in the paint, along with optionally algorithmic settings like
 * textSize, textScaleX, textShearX, kFakeBoldText_Mask, to specify
 * how text appears when drawn (and measured).
 * <p>
 * Typeface objects are immutable, and so they can be shared between threads.
 */
public abstract class Typeface {

    private final UniqueID mUniqueID;

    public Typeface() {
        mUniqueID = new UniqueID();
    }

    public final UniqueID getUniqueID() {
        return mUniqueID;
    }

    @Nonnull
    public final ScalerContext createScalerContext(StrikeDesc desc) {
        return Objects.requireNonNull(onCreateScalerContext(desc));
    }

    @Nonnull
    protected abstract ScalerContext onCreateScalerContext(StrikeDesc desc);

    protected abstract void onFilterStrikeDesc(StrikeDesc desc);

    @Override
    public final int hashCode() {
        return mUniqueID.hashCode();
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof Typeface t) {
            return mUniqueID.equals(t.mUniqueID);
        }
        return false;
    }
}
