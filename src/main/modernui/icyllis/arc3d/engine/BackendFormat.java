/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2022-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.engine;

import icyllis.arc3d.core.*;
import icyllis.arc3d.core.ImageInfo;
import org.lwjgl.system.NativeType;

import javax.annotation.concurrent.Immutable;

import static icyllis.arc3d.engine.Engine.BackendApi;

/**
 * Describes the backend texture format, immutable.
 */
@Immutable
@Deprecated()
public abstract class BackendFormat {

    /**
     * @see BackendApi#kOpenGL
     * @see BackendApi#kVulkan
     * @see BackendApi#kMock
     */
    public abstract int getBackend();

    /**
     * If the backend API is OpenGL this gets the format as a GLenum.
     */
    @NativeType("GLenum")
    public int getGLFormat() {
        throw new IllegalStateException();
    }

    /**
     * If the backend API is Vulkan this gets the format as a VkFormat.
     */
    @NativeType("VkFormat")
    public int getVkFormat() {
        throw new IllegalStateException();
    }

    /**
     * Gets the channels present in the format as a bitfield of ColorChannelFlag values.
     *
     * @see Color#COLOR_CHANNEL_FLAGS_RGBA
     */
    public abstract int getChannelFlags();

    public abstract boolean isSRGB();

    /**
     * Hints that a texture comes from external resources, and our engine cannot create such a texture
     * with this format. It will be read-only (can be read/sampled from but cannot be written/rendered to).
     * <p>
     * This is reserved for future use and currently always returns false.
     */
    public final boolean isExternal() {
        return false;
    }

    /**
     * @see ColorInfo#COMPRESSION_NONE
     */
    @ColorInfo.CompressionType
    public abstract int getCompressionType();

    public final boolean isCompressed() {
        return getCompressionType() != ColorInfo.COMPRESSION_NONE;
    }

    /**
     * @return if compressed, bytes per block, otherwise bytes per pixel
     */
    public abstract int getBytesPerBlock();

    public abstract int getDepthBits();

    public abstract int getStencilBits();

    /**
     * @return a key that is unique in the backend
     */
    public abstract int getFormatKey();
}
