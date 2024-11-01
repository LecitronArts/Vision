/*
 * Modern UI.
 * Copyright (C) 2024 BloCamLimb. All rights reserved.
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

package icyllis.modernui.graphics;

import icyllis.arc3d.core.*;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.graphics.text.Font;
import org.jetbrains.annotations.ApiStatus;

import java.nio.*;
import java.util.Objects;

/**
 * Canvas powered by Arc3D Canvas.
 *
 * @hidden
 */
@ApiStatus.Internal
public class ArcCanvas extends Canvas {

    @RawPtr
    private icyllis.arc3d.core.Canvas mCanvas;

    public ArcCanvas(@RawPtr icyllis.arc3d.core.Canvas canvas) {
        mCanvas = canvas;
    }

    @RawPtr
    public icyllis.arc3d.core.Canvas getCanvas() {
        return mCanvas;
    }

    @Override
    public int save() {
        return mCanvas.save();
    }

    @Override
    public int saveLayer(float left, float top, float right, float bottom, int alpha) {
        //TODO not supported yet
        return save();
    }

    @Override
    public void restore() {
        mCanvas.restore();
    }

    @Override
    public int getSaveCount() {
        return mCanvas.getSaveCount();
    }

    @Override
    public void restoreToCount(int saveCount) {
        mCanvas.restoreToCount(saveCount);
    }

    @Override
    public void translate(float dx, float dy) {
        mCanvas.translate(dx, dy);
    }

    @Override
    public void translate(float dx, float dy, float dz) {
        mCanvas.translate(dx, dy, dz);
    }

    @Override
    public void scale(float sx, float sy) {
        mCanvas.scale(sx, sy);
    }

    @Override
    public void scale(float sx, float sy, float sz) {
        mCanvas.scale(sx, sy, sz);
    }

    @Override
    public void scale(float sx, float sy, float px, float py) {
        mCanvas.scale(sx, sy, px, py);
    }

    @Override
    public void rotate(float degrees) {
        mCanvas.rotate(degrees);
    }

    @Override
    public void rotate(float degrees, float px, float py) {
        mCanvas.rotate(degrees, px, py);
    }

    @Override
    public void shear(float sx, float sy) {
        mCanvas.shear(sx, sy);
    }

    @Override
    public void shear(float sx, float sy, float px, float py) {
        mCanvas.shear(sx, sy, px, py);
    }

    @Override
    public void concat(@NonNull Matrix matrix) {
        mCanvas.concat(matrix);
    }

    @Override
    public void concat(@NonNull Matrix4 matrix) {
        mCanvas.concat(matrix);
    }

    @NonNull
    @Override
    public Matrix4 getMatrix() {
        Matrix4 mat = new Matrix4();
        mCanvas.getLocalToDevice(mat);
        return mat;
    }

    @Override
    public boolean clipRect(float left, float top, float right, float bottom) {
        mCanvas.clipRect(left, top, right, bottom, ClipOp.CLIP_OP_INTERSECT);
        return !mCanvas.isClipEmpty();
    }

    @Override
    public boolean clipOutRect(float left, float top, float right, float bottom) {
        mCanvas.clipRect(left, top, right, bottom, ClipOp.CLIP_OP_DIFFERENCE);
        return !mCanvas.isClipEmpty();
    }

    @Override
    public boolean quickReject(float left, float top, float right, float bottom) {
        return mCanvas.quickReject(left, top, right, bottom);
    }

    @Override
    public boolean getLocalClipBounds(@NonNull RectF bounds) {
        var devClipBounds = new Rect2i();
        if (!mCanvas.getDeviceClipBounds(devClipBounds)) {
            bounds.setEmpty();
            return false;
        }
        var inverse = new Matrix();
        mCanvas.getLocalToDevice(inverse);
        if (!inverse.invert()) {
            bounds.setEmpty();
            return false;
        }
        bounds.set(devClipBounds.left(), devClipBounds.top(),
                devClipBounds.right(), devClipBounds.bottom());
        inverse.mapRect(bounds);
        return !bounds.isEmpty();
    }

    @Override
    public void drawColor(int color, @NonNull BlendMode mode) {
        mCanvas.drawColor(color, mode.getNativeBlendMode());
    }

    @Override
    public void drawColor(float r, float g, float b, float a, @NonNull BlendMode mode) {
        mCanvas.drawColor(r, g, b, a, mode.getNativeBlendMode());
    }

    @Override
    public void drawPaint(@NonNull Paint paint) {
        mCanvas.drawPaint(paint.getNativePaint());
    }

    @Override
    public void drawPoint(float x, float y, @NonNull Paint paint) {
        mCanvas.drawPoint(x, y, paint.getNativePaint());
    }

    @Override
    public void drawPoints(@NonNull float[] pts, int offset, int count, @NonNull Paint paint) {
        Objects.checkFromIndexSize(offset, count, pts.length);
        mCanvas.drawPoints(icyllis.arc3d.core.Canvas.POINT_MODE_POINTS,
                pts, offset, count >> 1, paint.getNativePaint());
    }

    @Override
    public void drawLine(float x0, float y0, float x1, float y1, @NonNull Paint paint) {
        mCanvas.drawLine(x0, y0, x1, y1, paint.getNativePaint());
    }

    @Override
    public void drawLine(float x0, float y0, float x1, float y1,
                         @NonNull Paint.Cap cap, float thickness, @NonNull Paint paint) {
        mCanvas.drawLine(x0, y0, x1, y1, cap.nativeInt, thickness, paint.getNativePaint());
    }

    @Override
    public void drawLines(@NonNull float[] pts, int offset, int count, boolean connected,
                          @NonNull Paint paint) {
        Objects.checkFromIndexSize(offset, count, pts.length);
        mCanvas.drawPoints(connected
                        ? icyllis.arc3d.core.Canvas.POINT_MODE_POLYGON
                        : icyllis.arc3d.core.Canvas.POINT_MODE_LINES,
                pts, offset, count >> 1, paint.getNativePaint());
    }

    @Override
    public void drawRect(float left, float top, float right, float bottom, @NonNull Paint paint) {
        mCanvas.drawRect(left, top, right, bottom, paint.getNativePaint());
    }

    @Override
    public void drawRectGradient(float left, float top, float right, float bottom,
                                 int colorUL, int colorUR, int colorLR, int colorLL, Paint paint) {
        //TODO bilinear gradient not supported yet
        mCanvas.drawRect(left, top, right, bottom, paint.getNativePaint());
    }

    @Override
    public void drawRoundRect(float left, float top, float right, float bottom, float radius,
                              @NonNull Paint paint) {
        mCanvas.drawRoundRect(left, top, right, bottom, radius, paint.getNativePaint());
    }

    @Override
    public void drawRoundRect(float left, float top, float right, float bottom, float radius,
                              int sides, @NonNull Paint paint) {
        //TODO per-corner radius not supported yet
        mCanvas.drawRoundRect(left, top, right, bottom, radius, paint.getNativePaint());
    }

    @Override
    public void drawRoundRectGradient(float left, float top, float right, float bottom,
                                      int colorUL, int colorUR, int colorLR, int colorLL, float radius, Paint paint) {
        //TODO per-corner radius not supported yet
        mCanvas.drawRoundRect(left, top, right, bottom, radius, paint.getNativePaint());
    }

    @Override
    public void drawCircle(float cx, float cy, float radius, @NonNull Paint paint) {
        mCanvas.drawCircle(cx, cy, radius, paint.getNativePaint());
    }

    @Override
    public void drawArc(float cx, float cy, float radius, float startAngle, float sweepAngle,
                        @NonNull Paint paint) {
        mCanvas.drawArc(cx, cy, radius, startAngle, sweepAngle, paint.getNativePaint());
    }

    @Override
    public void drawArc(float cx, float cy, float radius, float startAngle, float sweepAngle,
                        @NonNull Paint.Cap cap, float thickness, @NonNull Paint paint) {
        mCanvas.drawArc(cx, cy, radius, startAngle, sweepAngle, cap.nativeInt, thickness, paint.getNativePaint());
    }

    @Override
    public void drawPie(float cx, float cy, float radius, float startAngle, float sweepAngle,
                        @NonNull Paint paint) {
        mCanvas.drawPie(cx, cy, radius, startAngle, sweepAngle, paint.getNativePaint());
    }

    @Override
    public void drawChord(float cx, float cy, float radius, float startAngle, float sweepAngle,
                          @NonNull Paint paint) {
        mCanvas.drawChord(cx, cy, radius, startAngle, sweepAngle, paint.getNativePaint());
    }

    @Override
    public void drawBezier(float x0, float y0, float x1, float y1, float x2, float y2, Paint paint) {
        //TODO not supported yet
    }

    @Override
    public void drawImage(Image image, float left, float top, @Nullable Paint paint) {
        if (image == null) {
            return;
        }
        mCanvas.drawImage(image.getNativeImage(), left, top,
                paint != null && paint.getFilterMode() == ImageShader.FILTER_MODE_NEAREST
                        ? SamplingOptions.POINT
                        : SamplingOptions.LINEAR,
                paint != null ? paint.getNativePaint() : null);
    }

    @Override
    public void drawImage(Image image, float srcLeft, float srcTop, float srcRight, float srcBottom,
                          float dstLeft, float dstTop, float dstRight, float dstBottom, @Nullable Paint paint) {
        if (image == null) {
            return;
        }
        mCanvas.drawImageRect(image.getNativeImage(),
                new Rect2f(srcLeft, srcTop, srcRight, srcBottom),
                new Rect2f(dstLeft, dstTop, dstRight, dstBottom),
                paint != null && paint.getFilterMode() == ImageShader.FILTER_MODE_NEAREST
                        ? SamplingOptions.POINT
                        : SamplingOptions.LINEAR,
                paint != null ? paint.getNativePaint() : null,
                icyllis.arc3d.core.Canvas.SRC_RECT_CONSTRAINT_FAST);
    }

    @Override
    public void drawRoundImage(Image image, float left, float top, float radius,
                               @Nullable Paint paint) {
        drawImage(image, left, top, paint);
    }

    @Override
    public void drawGlyphs(@NonNull int[] glyphs, int glyphOffset,
                           @NonNull float[] positions, int positionOffset,
                           int glyphCount, @NonNull Font font,
                           float x, float y, @NonNull Paint paint) {
        var nativeFont = new icyllis.arc3d.core.Font();
        nativeFont.setTypeface(font.getNativeTypeface());
        if (nativeFont.getTypeface() == null) {
            return;
        }
        paint.getNativeFont(nativeFont);
        mCanvas.drawGlyphs(glyphs, glyphOffset,
                positions, positionOffset,
                glyphCount, x, y, nativeFont, paint.getNativePaint());
    }

    @Override
    public void drawTextBlob(TextBlob blob, float x, float y, @NonNull Paint paint) {
        mCanvas.drawTextBlob(blob, x, y, paint.getNativePaint());
    }

    @Override
    public void drawVertices(@NonNull VertexMode mode, int vertexCount,
                             @NonNull float[] positions, int positionOffset,
                             @Nullable float[] texCoords, int texCoordOffset,
                             @Nullable int[] colors, int colorOffset,
                             @Nullable short[] indices, int indexOffset, int indexCount,
                             @Nullable BlendMode blendMode, @NonNull Paint paint) {
        Objects.checkFromIndexSize(positionOffset, vertexCount, positions.length);
        if (texCoords != null) {
            Objects.checkFromIndexSize(texCoordOffset, vertexCount, texCoords.length);
        }
        if (colors != null) {
            Objects.checkFromIndexSize(colorOffset, vertexCount >> 1, colors.length);
        }
        if (indices != null) {
            Objects.checkFromIndexSize(indexOffset, indexCount, indices.length);
        }
        if (vertexCount < 2) {
            return;
        }
        Vertices vertices = Vertices.makeCopy(
                mode.nativeInt, vertexCount >> 1, positions, positionOffset,
                texCoords, texCoordOffset, colors, colorOffset,
                indices, indexOffset, indexCount
        );
        var nativePaint = paint.getNativePaint();
        if (blendMode == null) {
            blendMode = nativePaint.getShader() != null
                    ? BlendMode.MODULATE
                    : BlendMode.DST;
        }
        mCanvas.drawVertices(vertices, blendMode.getNativeBlendMode(), nativePaint);
    }

    @Override
    public void drawMesh(@NonNull VertexMode mode, @NonNull FloatBuffer positions,
                         @Nullable FloatBuffer texCoords, @Nullable IntBuffer colors,
                         @Nullable ShortBuffer indices, @Nullable BlendMode blendMode,
                         @NonNull Paint paint) {
        if (positions.remaining() < 2) {
            return;
        }
        Vertices vertices = Vertices.makeCopy(mode.nativeInt, positions, texCoords, colors, indices);
        var nativePaint = paint.getNativePaint();
        if (blendMode == null) {
            blendMode = nativePaint.getShader() != null
                    ? BlendMode.MODULATE
                    : BlendMode.DST;
        }
        mCanvas.drawVertices(vertices, blendMode.getNativeBlendMode(), nativePaint);
    }

    @Override
    public boolean isClipEmpty() {
        return mCanvas.isClipEmpty();
    }

    @Override
    public boolean isClipRect() {
        return mCanvas.isClipRect();
    }
}
