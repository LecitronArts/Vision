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

package icyllis.arc3d.vulkan;

import icyllis.arc3d.engine.*;
import org.lwjgl.vulkan.EXTQueueFamilyForeign;

import static icyllis.arc3d.vulkan.VKCore.*;

/**
 * When wrapping a {@link BackendImage} or {@link BackendRenderTarget}, the {@link #mCurrentQueueFamily}
 * should either be {@link VKCore#VK_QUEUE_FAMILY_IGNORED}, {@link VKCore#VK_QUEUE_FAMILY_EXTERNAL},
 * or {@link EXTQueueFamilyForeign#VK_QUEUE_FAMILY_FOREIGN_EXT}. If {@link #mSharingMode} is
 * {@link VKCore#VK_SHARING_MODE_EXCLUSIVE}, then {@link #mCurrentQueueFamily} can also be the graphics
 * queue index passed into pipeline.
 * <p>
 * Note the image type is always {@link VKCore#VK_IMAGE_TYPE_2D}. When importing external memory,
 * {@link #mMemoryHandle} is POSIX file descriptor or Win32 NT handle (though <code>HANDLE</code> is defined
 * as <code>void*</code>, we can safely truncate it because Win32 handles are 32-bit significant).
 * If it is an NT handle, it must be released manually by the memory exporter (e.g. Vulkan).
 */
public final class VulkanImageDesc extends ImageDesc {

    public long mImage = VK_NULL_HANDLE;
    public int mImageLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    public int mImageTiling = VK_IMAGE_TILING_OPTIMAL;
    public int mFormat = VK_FORMAT_UNDEFINED;
    public int mSharingMode = VK_SHARING_MODE_EXCLUSIVE;
    public int mImageUsageFlags = 0;
    public int mSampleCount = 1;
    public int mCurrentQueueFamily = VK_QUEUE_FAMILY_IGNORED;
    public boolean mProtected = false;

    public VulkanImageDesc(int width, int height, int depth, int arraySize, int mipLevelCount, int sampleCount, int flags) {
        super(width, height, depth, arraySize, mipLevelCount, sampleCount, flags);
    }

    @Override
    public int getBackend() {
        return Engine.BackendApi.kVulkan;
    }

    @Override
    public int getImageType() {
        return 0;
    }

    public void set(VulkanImageDesc info) {
        //super.set(info);
        mImage = info.mImage;
        mImageLayout = info.mImageLayout;
        mImageTiling = info.mImageTiling;
        mFormat = info.mFormat;
        mSharingMode = info.mSharingMode;
        mImageUsageFlags = info.mImageUsageFlags;
        mSampleCount = info.mSampleCount;
        //mLevelCount = info.mLevelCount;
        mCurrentQueueFamily = info.mCurrentQueueFamily;
       // mMemoryHandle = info.mMemoryHandle;
        mProtected = info.mProtected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        VulkanImageDesc that = (VulkanImageDesc) o;
        if (mImage != that.mImage) return false;
        if (mImageLayout != that.mImageLayout) return false;
        if (mImageTiling != that.mImageTiling) return false;
        if (mFormat != that.mFormat) return false;
        if (mSharingMode != that.mSharingMode) return false;
        if (mImageUsageFlags != that.mImageUsageFlags) return false;
        if (mSampleCount != that.mSampleCount) return false;
        //if (mLevelCount != that.mLevelCount) return false;
        if (mCurrentQueueFamily != that.mCurrentQueueFamily) return false;
        //if (mMemoryHandle != that.mMemoryHandle) return false;
        return mProtected == that.mProtected;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (mImage ^ (mImage >>> 32));
        result = 31 * result + mImageLayout;
        result = 31 * result + mImageTiling;
        result = 31 * result + mFormat;
        result = 31 * result + mSharingMode;
        result = 31 * result + mImageUsageFlags;
        result = 31 * result + mSampleCount;
       // result = 31 * result + mLevelCount;
        result = 31 * result + mCurrentQueueFamily;
        //result = 31 * result + mMemoryHandle;
        result = 31 * result + (mProtected ? 1 : 0);
        return result;
    }
}
