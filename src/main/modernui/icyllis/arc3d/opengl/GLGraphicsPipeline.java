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
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.trash.GraphicsPipelineDesc_Old;
import icyllis.arc3d.granite.GeometryStep;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * This class manages a GPU program and records per-program information. It also records the vertex
 * and instance attribute layouts that are to be used with the program.
 * <p>
 * This class holds onto a {@link GLProgram} object that we use for draws. Besides storing the actual
 * {@link GLProgram} object, this class is also responsible handling all uniforms, buffers, samplers,
 * and other similar objects that are used along with the GLProgram and GLVertexArray in the draw.
 * This includes both allocating and freeing these objects, as well as updating their values.
 * <p>
 * Supports OpenGL 3.3 and OpenGL 4.5.
 */
public final class GLGraphicsPipeline extends GraphicsPipeline {

    @SharedPtr
    private GLProgram mProgram;
    @SharedPtr
    private GLVertexArray mVertexArray;

    private byte mPrimitiveType;
    private BlendInfo mBlendInfo;
    private DepthStencilSettings mDepthStencilSettings;

    private GLUniformDataManager mDataManager;

    private int mNumTextureSamplers;

    private CompletableFuture<GLGraphicsPipelineBuilder> mAsyncWork;

    GLGraphicsPipeline(GLDevice device,
                       CompletableFuture<GLGraphicsPipelineBuilder> asyncWork) {
        super(device);
        mAsyncWork = asyncWork;
    }

    void init(@SharedPtr GLProgram program,
              @SharedPtr GLVertexArray vertexArray,
              byte primitiveType,
              BlendInfo blendInfo,
              DepthStencilSettings depthStencilSettings/*,
              List<UniformHandler.UniformInfo> uniforms,
              int uniformSize,
              List<UniformHandler.UniformInfo> samplers*/) {
        mProgram = program;
        mVertexArray = vertexArray;
        mPrimitiveType = primitiveType;
        mBlendInfo = blendInfo;
        mDepthStencilSettings = depthStencilSettings;
        /*mDataManager = new GLUniformDataManager(uniforms, uniformSize);
        mNumTextureSamplers = samplers.size();*/
    }

    public void discard() {
        if (mAsyncWork != null) {
            mAsyncWork.cancel(true);
            mAsyncWork = null;
        }
        if (mProgram != null) {
            mProgram.discard();
            if (mVertexArray.unique()) {
                mVertexArray.discard();
            }
        }
    }

    @Override
    protected void deallocate() {
        if (mAsyncWork != null) {
            mAsyncWork.cancel(true);
            mAsyncWork = null;
        }
        mProgram = RefCnt.move(mProgram);
        mVertexArray = RefCnt.move(mVertexArray);
        mDataManager = RefCnt.move(mDataManager);
    }

    private void checkAsyncWork() {
        boolean success = mAsyncWork.join().finish(this);
        var stats = getDevice().getSharedResourceCache().getStats();
        if (success) {
            stats.incNumCompilationSuccesses();
        } else {
            stats.incNumCompilationFailures();
        }
        mAsyncWork = null;
    }

    @Nullable // raw ptr
    public GLProgram getProgram() {
        if (mAsyncWork != null) {
            checkAsyncWork();
        }
        return mProgram;
    }

    @Nullable // raw ptr
    public GLVertexArray getVertexArray() {
        if (mAsyncWork != null) {
            checkAsyncWork();
        }
        return mVertexArray;
    }

    @Deprecated
    public boolean bindUniforms(GLCommandBuffer commandBuffer,
                                GraphicsPipelineDesc_Old graphicsPipelineDesc,
                                int width, int height) {
        mDataManager.setProjection(0, width, height,
                graphicsPipelineDesc.origin() == Engine.SurfaceOrigin.kLowerLeft);
        //mGPImpl.setData(mDataManager, graphicsPipelineDesc.geomProc());
        //TODO FP and upload

        return mDataManager.bindAndUploadUniforms(getDevice(), commandBuffer);
    }

    /**
     * Binds all geometry processor and fragment processor textures.
     */
    @Deprecated
    public boolean bindTextures(GLCommandBuffer commandBuffer,
                                GraphicsPipelineDesc_Old graphicsPipelineDesc,
                                ImageViewProxy[] geomTextures) {
        int unit = 0;
        for (int i = 0, n = graphicsPipelineDesc.geomProc().numTextureSamplers(); i < n; i++) {
            GLTexture texture = (GLTexture) geomTextures[i].getImage();
            /*commandBuffer.bindTextureSampler(unit++, texture,
                    graphicsPipelineDesc.geomProc().textureSamplerState(i),
                    graphicsPipelineDesc.geomProc().textureSamplerSwizzle(i));*/
        }
        //TODO bind FP textures

        assert unit == mNumTextureSamplers;
        return true;
    }

    public byte getPrimitiveType() {
        return mPrimitiveType;
    }

    public BlendInfo getBlendInfo() {
        return mBlendInfo;
    }

    public DepthStencilSettings getDepthStencilSettings() {
        return mDepthStencilSettings;
    }

    public int getVertexBindingCount() {
        return mVertexArray.getBindingCount();
    }

    public int getVertexStride(int binding) {
        return mVertexArray.getStride(binding);
    }

    public int getVertexInputRate(int binding) {
        return mVertexArray.getInputRate(binding);
    }

    /**
     * Set element buffer (index buffer).
     * <p>
     * Bind pipeline first.
     *
     * @param buffer the element buffer object, raw ptr
     */
    public void bindIndexBuffer(@Nonnull @RawPtr GLBuffer buffer) {
        if (mVertexArray != null) {
            mVertexArray.bindIndexBuffer(buffer);
        }
    }

    /**
     * Set the buffer that stores the attribute data.
     * <p>
     * The stride, the distance to the next vertex data, in bytes, is determined in constructor.
     * <p>
     * Bind pipeline first.
     *
     * @param binding the binding index
     * @param buffer  the vertex buffer object, raw ptr
     * @param offset  first vertex data to the head of the buffer, in bytes
     */
    public void bindVertexBuffer(int binding, @Nonnull @RawPtr GLBuffer buffer, long offset) {
        if (mVertexArray != null) {
            mVertexArray.bindVertexBuffer(binding, buffer, offset);
        }
    }

    @Override
    protected GLDevice getDevice() {
        return (GLDevice) super.getDevice();
    }
}
