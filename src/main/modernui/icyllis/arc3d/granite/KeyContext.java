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
import icyllis.arc3d.engine.RecordingContext;

import javax.annotation.Nullable;

public class KeyContext {

    @RawPtr
    private final RecordingContext mRC;
    private final ImageInfo mTargetInfo;
    // color components using non-premultiplied alpha
    private float mR; // 0..1
    private float mG; // 0..1
    private float mB; // 0..1
    private float mA; // 0..1

    public KeyContext(@RawPtr RecordingContext rc,
                      ImageInfo targetInfo) {
        mRC = rc;
        mTargetInfo = targetInfo;
    }

    public void reset(PaintParams paintParams) {
        var dstCS = mTargetInfo.colorSpace();
        if (dstCS != null && !dstCS.isSrgb()) {
            float[] col = ColorSpace.connect(
                    ColorSpace.get(ColorSpace.Named.SRGB), dstCS
            ).transform(paintParams.r(), paintParams.g(), paintParams.b());
            mR = col[0];
            mG = col[1];
            mB = col[2];
        } else {
            mR = paintParams.r();
            mG = paintParams.g();
            mB = paintParams.b();
        }
        mA = paintParams.a();
    }

    /**
     * Raw ptr to context, null when pre-compiling shaders.
     */
    @RawPtr
    @Nullable
    public RecordingContext getRC() {
        return mRC;
    }

    /**
     * Returns the value of the red component, in destination space.
     */
    public float r() {
        return mR;
    }

    /**
     * Returns the value of the green component, in destination space.
     */
    public float g() {
        return mG;
    }

    /**
     * Returns the value of the blue component, in destination space.
     */
    public float b() {
        return mB;
    }

    /**
     * Returns the value of the alpha component, in destination space.
     */
    public float a() {
        return mA;
    }

    public ImageInfo targetInfo() {
        return mTargetInfo;
    }
}
