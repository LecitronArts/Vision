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

import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.UniformDataManager;
import icyllis.arc3d.granite.shading.UniformHandler;

import java.util.List;

import static org.lwjgl.opengl.GL15C.nglBufferSubData;
import static org.lwjgl.opengl.GL31C.GL_UNIFORM_BUFFER;

/**
 * Uploads a UBO for a Uniform Interface Block with std140 layout.
 */
public class GLUniformDataManager extends UniformDataManager {

    private int mRTWidth;
    private int mRTHeight;
    private boolean mRTFlipY;

    private GLUniformBuffer mUniformBuffer;

    /**
     * Created by {@link GLGraphicsPipeline}.
     *
     * @param uniforms    the uniforms
     * @param uniformSize the uniform block size in bytes
     */
    GLUniformDataManager(List<UniformHandler.UniformInfo> uniforms, int uniformSize) {
        super(uniforms.size(), uniformSize);
        for (int i = 0, e = uniforms.size(); i < e; i++) {
            UniformHandler.UniformInfo uniformInfo = uniforms.get(i);
            assert ((uniformInfo.mOffset & 0xFFFFFF) == uniformInfo.mOffset);
            assert (MathUtil.isAlign4(uniformInfo.mOffset));
            assert (SLDataType.canBeUniformValue(uniformInfo.mVariable.getType()));
            mUniforms[i] = uniformInfo.mOffset | (uniformInfo.mVariable.getType() << 24);
        }
    }

    @Override
    protected void deallocate() {
        super.deallocate();
        mUniformBuffer = RefCnt.move(mUniformBuffer);
    }

    /**
     * Set the orthographic projection vector.
     */
    public void setProjection(@UniformHandler.UniformHandle int u, int width, int height, boolean flipY) {
        if (width != mRTWidth || height != mRTHeight || flipY != mRTFlipY) {
            if (flipY) {
                set4f(u, 2.0f / width, -1.0f, -2.0f / height, 1.0f);
            } else {
                set4f(u, 2.0f / width, -1.0f, 2.0f / height, -1.0f);
            }
            mRTWidth = width;
            mRTHeight = height;
            mRTFlipY = flipY;
        }
    }

    public boolean bindAndUploadUniforms(GLDevice device,
                                         GLCommandBuffer commandBuffer) {
        if (!mUniformsDirty) {
            return true;
        }
        if (mUniformBuffer == null) {
            mUniformBuffer = GLUniformBuffer.make(device, mUniformSize, 0/*UniformHandler.UNIFORM_BINDING*/);
        }
        if (mUniformBuffer == null) {
            return false;
        }
        //commandBuffer.bindUniformBuffer(mUniformBuffer);
        nglBufferSubData(GL_UNIFORM_BUFFER, 0, mUniformSize, mUniformData);
        mUniformsDirty = false;
        return true;
    }
}
