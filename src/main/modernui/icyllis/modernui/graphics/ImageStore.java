/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;

@Deprecated //TODO will be replaced by Resources
public class ImageStore {

    private static final ImageStore INSTANCE = new ImageStore();

    private final Object mLock = new Object();
    private HashMap<String, HashMap<String, WeakReference<Image>>> mImages = new HashMap<>();

    private ImageStore() {
    }

    /**
     * @return the global texture manager instance
     */
    public static ImageStore getInstance() {
        return INSTANCE;
    }

    // internal use
    public void clear() {
        synchronized (mLock) {
            for (var cache : mImages.values()) {
                for (var entry : cache.values()) {
                    var image = entry.get();
                    if (image != null) {
                        image.close();
                    }
                }
            }
            mImages = new HashMap<>();
        }
    }

    /**
     * Get or create a texture image from the given resource.
     *
     * @param namespace the application namespace
     * @param path      the path to the resource
     * @return texture image
     */
    @Nullable
    public Image getOrCreate(@NonNull String namespace, @NonNull String path) {
        synchronized (mLock) {
            var cache = mImages.computeIfAbsent(namespace, __ -> new HashMap<>());
            var imageRef = cache.get(path);
            Image image;
            if (imageRef != null && (image = imageRef.get()) != null && !image.isClosed()) {
                return image;
            }
        }
        try (var stream = ModernUI.getInstance().getResourceStream(namespace, path);
             var bitmap = BitmapFactory.decodeStream(stream)) {
            var newImage = Image.createTextureFromBitmap(bitmap);
            synchronized (mLock) {
                var cache = mImages.computeIfAbsent(namespace, __ -> new HashMap<>());
                var imageRef = cache.get(path);
                Image image;
                if (imageRef != null && (image = imageRef.get()) != null && !image.isClosed()) {
                    // race
                    if (newImage != null) {
                        newImage.close();
                    }
                    return image;
                }
                if (newImage != null) {
                    cache.put(path, new WeakReference<>(newImage));
                    return newImage;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
