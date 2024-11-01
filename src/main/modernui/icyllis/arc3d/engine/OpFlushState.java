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

import icyllis.arc3d.core.Rect2i;
import icyllis.arc3d.engine.trash.GraphicsPipelineDesc_Old;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Set;

/**
 * Tracks the state across all the GrOps (really just the GrDrawOps) in a OpsTask flush.
 */
@Deprecated
public class OpFlushState implements MeshDrawTarget {

    private final Device mDevice;

    private OpsRenderPass mOpsRenderPass;

    public OpFlushState(Device device,
                        ResourceProvider resourceProvider) {
        mDevice = device;
    }

    public Device getDevice() {
        return mDevice;
    }

    public final GraphicsPipeline findOrCreateGraphicsPipeline(
            final GraphicsPipelineDesc_Old graphicsPipelineDesc) {
        //return mDevice.getContext().findOrCreateGraphicsPipeline(graphicsPipelineDesc);
        return null;
    }

    @Override
    public long makeVertexSpace(Mesh mesh) {
        return mDevice.getVertexPool().makeSpace(mesh);
    }

    @Override
    public long makeInstanceSpace(Mesh mesh) {
        return mDevice.getInstancePool().makeSpace(mesh);
    }

    @Override
    public long makeIndexSpace(Mesh mesh) {
        return mDevice.getIndexPool().makeSpace(mesh);
    }

    @Nullable
    @Override
    public ByteBuffer makeVertexWriter(Mesh mesh) {
        return mDevice.getVertexPool().makeWriter(mesh);
    }

    @Nullable
    @Override
    public ByteBuffer makeInstanceWriter(Mesh mesh) {
        return mDevice.getInstancePool().makeWriter(mesh);
    }

    @Nullable
    @Override
    public ByteBuffer makeIndexWriter(Mesh mesh) {
        return mDevice.getIndexPool().makeWriter(mesh);
    }

    public OpsRenderPass getOpsRenderPass() {
        return mOpsRenderPass;
    }

    public OpsRenderPass beginOpsRenderPass(ImageProxyView writeView,
                                            Rect2i contentBounds,
                                            byte colorOps,
                                            byte stencilOps,
                                            float[] clearColor,
                                            Set<SurfaceProxy> sampledTextures,
                                            int pipelineFlags) {
        assert (mOpsRenderPass == null);
        OpsRenderPass opsRenderPass = mDevice.getOpsRenderPass(writeView, contentBounds,
                colorOps, stencilOps, clearColor,
                sampledTextures, pipelineFlags);
        if (opsRenderPass == null) {
            return null;
        }
        mOpsRenderPass = opsRenderPass;
        opsRenderPass.begin();
        return opsRenderPass;
    }

    public void reset() {
    }
}
