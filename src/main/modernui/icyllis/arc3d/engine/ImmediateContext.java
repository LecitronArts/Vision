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

import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.core.SharedPtr;
import icyllis.arc3d.engine.task.Task;
import icyllis.arc3d.granite.RendererProvider;
import icyllis.arc3d.granite.StaticBufferManager;
import icyllis.arc3d.vulkan.VkBackendContext;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import javax.annotation.Nullable;

/**
 * Immediate context is used for command list execution and queue submission.
 * That thread is also known as command execution thread and submission thread.
 */
public final class ImmediateContext extends Context {

    private QueueManager mQueueManager;

    /**
     * Use backend utility class to create context.
     */
    @ApiStatus.Internal
    public ImmediateContext(Device device, QueueManager queueManager) {
        super(device);
        mQueueManager = queueManager;
    }

    /**
     * Creates a DirectContext for a backend context, using default context options.
     *
     * @return context or null if failed to create
     * @see #makeVulkan(VkBackendContext, ContextOptions)
     */
    @Nullable
    public static ImmediateContext makeVulkan(VkBackendContext backendContext) {
        return makeVulkan(backendContext, new ContextOptions());
    }

    /**
     * Creates a DirectContext for a backend context, using specified context options.
     * <p>
     * The Vulkan context (VkQueue, VkDevice, VkInstance) must be kept alive until the returned
     * DirectContext is destroyed. This also means that any objects created with this
     * DirectContext (e.g. Surfaces, Images, etc.) must also be released as they may hold
     * refs on the DirectContext. Once all these objects and the DirectContext are released,
     * then it is safe to delete the Vulkan objects.
     *
     * @return context or null if failed to create
     */
    @Nullable
    public static ImmediateContext makeVulkan(VkBackendContext backendContext, ContextOptions options) {
        return null;
    }

    @Nullable
    @SharedPtr
    public RecordingContext makeRecordingContext() {
        return makeRecordingContext(new RecordingContextOptions());
    }

    @Nullable
    @SharedPtr
    public RecordingContext makeRecordingContext(RecordingContextOptions options) {
        RecordingContext rContext = new RecordingContext(mDevice);
        if (rContext.init(options)) {
            return rContext;
        }
        rContext.unref();
        return null;
    }

    /**
     * The context normally assumes that no outsider is setting state
     * within the underlying 3D API's context/device. This call informs
     * the context that the state was modified and it should resend.
     * <p>
     * The flag bits, state, is dependent on which backend is used by the
     * context, only GL.
     *
     * @see Engine.GLBackendState
     */
    public void resetContext(int state) {
        checkOwnerThread();
        mDevice.markContextDirty(state);
    }

    public boolean isDiscarded() {
        /*if (super.isDiscarded()) {
            return true;
        }*/
        if (mDevice != null && mDevice.isDeviceLost()) {
            //discard();
            return true;
        }
        return false;
    }

    @ApiStatus.Internal
    public QueueManager getQueueManager() {
        return mQueueManager;
    }

    public boolean addTask(@RawPtr Task task) {
        return mQueueManager.addTask(task);
    }

    public boolean submit() {
        checkOwnerThread();
        boolean success = mQueueManager.submit();
        mQueueManager.checkForFinishedWork();
        return success;
    }

    public void checkForFinishedWork() {
        checkOwnerThread();
        mQueueManager.checkForFinishedWork();
    }

    @VisibleForTesting
    @ApiStatus.Internal
    public CommandBuffer currentCommandBuffer() {
        if (mQueueManager.prepareCommandBuffer(mResourceProvider)) {
            return mQueueManager.mCurrentCommandBuffer;
        }
        return null;
    }

    @Override
    public boolean isDeviceLost() {
        if (mDevice != null && mDevice.isDeviceLost()) {
            //discard();
            return true;
        }
        return false;
    }

    @Override
    public void freeGpuResources() {
        checkForFinishedWork();
        mResourceProvider.freeGpuResources();
        mDevice.freeGpuResources();
    }

    @Override
    public void performDeferredCleanup(long msNotUsed) {
        checkForFinishedWork();
        long timeMillis = System.currentTimeMillis() - msNotUsed;
        mResourceProvider.purgeResourcesNotUsedSince(timeMillis);
        mDevice.purgeResourcesNotUsedSince(timeMillis);
    }

    public boolean init() {
        assert isOwnerThread();
        if (mDevice == null) {
            return false;
        }

        //mContextInfo.init(mDevice);
        if (!super.init(mDevice.getOptions())) {
            return false;
        }
        mQueueManager.mContext = this;
        var staticBufferManager = new StaticBufferManager(mResourceProvider, getCaps());
        var rendererProvider = new RendererProvider(getCaps(), staticBufferManager);

        var result = staticBufferManager.flush(mQueueManager, getSharedResourceCache());
        if (result == StaticBufferManager.RESULT_FAILURE) {
            return false;
        }
        if (result == StaticBufferManager.RESULT_SUCCESS &&
                !mQueueManager.submit()) {
            return false;
        }
        mDevice.mRendererProvider = rendererProvider;

        //assert getThreadSafeCache() != null;

        //mResourceProvider = mDevice.createResourceProvider(this);
        return true;
    }

    /*@Override
    public void discard() {
        if (super.isDiscarded()) {
            return;
        }
        super.discard();
        if (mResourceProvider != null) {
            mResourceProvider.destroy(false);
        }
        if (mDevice != null) {
            mDevice.disconnect(false);
        }
    }*/

    @Override
    protected void deallocate() {
        super.deallocate();
        /*if (mResourceProvider != null) {
            mResourceProvider.destroy(true);
        }*/
        if (mDevice != null) {
            mDevice.disconnect(true);
        }
        mQueueManager.finishOutstandingWork();
    }
}
