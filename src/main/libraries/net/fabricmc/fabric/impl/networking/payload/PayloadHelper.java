/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.impl.networking.payload;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class PayloadHelper {
	public static void write(FriendlyByteBuf byteBuf, FriendlyByteBuf data) {
		byteBuf.writeBytes(data.copy());
	}

	public static FriendlyByteBuf read(FriendlyByteBuf byteBuf, int maxSize) {
		assertSize(byteBuf, maxSize);

		FriendlyByteBuf newBuf = PacketByteBufs.create();
		newBuf.writeBytes(byteBuf.copy());
		byteBuf.skipBytes(byteBuf.readableBytes());
		return newBuf;
	}

	public static ResolvablePayload readCustom(ResourceLocation id, FriendlyByteBuf buf, int maxSize, boolean retain) {
		assertSize(buf, maxSize);

		if (retain) {
			RetainedPayload payload = new RetainedPayload(id, PacketByteBufs.retainedSlice(buf));
			buf.skipBytes(buf.readableBytes());
			return payload;
		} else {
			return new UntypedPayload(id, read(buf, maxSize));
		}
	}

	private static void assertSize(FriendlyByteBuf buf, int maxSize) {
		int size = buf.readableBytes();

		if (size < 0 || size > maxSize) {
			throw new IllegalArgumentException("Payload may not be larger than " + maxSize + " bytes");
		}
	}
}
