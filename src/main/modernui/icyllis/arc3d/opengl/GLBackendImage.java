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

package icyllis.arc3d.opengl;

import icyllis.arc3d.engine.BackendFormat;
import icyllis.arc3d.engine.BackendImage;

import javax.annotation.Nonnull;

import static icyllis.arc3d.engine.Engine.*;

/**
 * When importing external memory,
 * {@link #memoryHandle} is POSIX file descriptor or Win32 NT handle. {@link #memoryObject} is
 * OpenGL memory object. If it is an NT handle, it must be released manually by the memory exporter
 * (e.g. Vulkan).
 */
public final class GLBackendImage extends BackendImage {

    private final GLImageDesc mInfo;
    // Null for renderbuffers.
    final GLTextureMutableState mParams;

    /**
     * <code>GLuint</code> - image name
     */
    public int handle;

    /**
     * <code>GLsizei</code> - number of mip levels
     */
    public int levels = 0;

    /**
     * <code>GLuint</code> - memory
     */
    public int memoryObject;
    /**
     * <pre>{@code
     * union {
     *     int fd; // file descriptor
     *     HANDLE handle; // win32 handle
     * };
     * }</pre>
     */
    public long memoryHandle = -1;

    private final BackendFormat mBackendFormat;

    // The GLTextureInfo must have a valid mFormat, can NOT be modified anymore.
    public GLBackendImage(int width, int height, GLImageDesc desc) {
        this(width, height, desc, new GLTextureMutableState(), GLBackendFormat.make(desc.mFormat));
        assert desc.mFormat != 0;
        // Make no assumptions about client's texture's parameters.
        glTextureParametersModified();
    }

    // Internally used by GLContext and GLTexture
    GLBackendImage(int width, int height, GLImageDesc desc,
                   GLTextureMutableState params, BackendFormat backendFormat) {
        super(desc, params);
        mInfo = desc;
        mParams = params;
        mBackendFormat = backendFormat;
    }

    @Override
    public int getBackend() {
        return BackendApi.kOpenGL;
    }

    @Override
    public boolean isExternal() {
        return mBackendFormat.isExternal();
    }

    /*
     * Copies a snapshot of the {@link GLImageInfo} struct into the passed in pointer.
     */
    /*public void getGLImageInfo(GLImageInfo info) {
        info.set(mInfo);
    }*/

    public GLImageDesc getGLImageInfo() {
        return mInfo;
    }

    @Override
    public void glTextureParametersModified() {
        if (mParams != null) {
            mParams.invalidate();
        }
    }

    @Nonnull
    @Override
    public BackendFormat getBackendFormat() {
        return mBackendFormat;
    }

    @Override
    public boolean isProtected() {
        return false;
    }

    @Override
    public boolean isSameImage(BackendImage image) {
        if (image instanceof GLBackendImage that) {
            return handle == that.handle;
        }
        return false;
    }

    @Override
    public String toString() {
        return "{" +
                "mBackend=OpenGL" +
                ", mInfo=" + mInfo +
                ", mParams=" + mParams +
                ", mBackendFormat=" + mBackendFormat +
                '}';
    }
}
