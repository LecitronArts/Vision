/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2024 ViaVersion and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.ArrayType;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockPredicate {

    public static final Type<BlockPredicate> TYPE = new Type<BlockPredicate>(BlockPredicate.class) {
        @Override
        public BlockPredicate read(final ByteBuf buffer) throws Exception {
            final HolderSet holders = Type.OPTIONAL_HOLDER_SET.read(buffer);
            final StatePropertyMatcher[] propertyMatchers = buffer.readBoolean() ? StatePropertyMatcher.ARRAY_TYPE.read(buffer) : null;
            final CompoundTag tag = Type.OPTIONAL_COMPOUND_TAG.read(buffer);
            return new BlockPredicate(holders, propertyMatchers, tag);
        }

        @Override
        public void write(final ByteBuf buffer, final BlockPredicate value) throws Exception {
            Type.OPTIONAL_HOLDER_SET.write(buffer, value.holderSet);

            buffer.writeBoolean(value.propertyMatchers != null);
            if (value.propertyMatchers != null) {
                StatePropertyMatcher.ARRAY_TYPE.write(buffer, value.propertyMatchers);
            }

            Type.OPTIONAL_COMPOUND_TAG.write(buffer, value.tag);
        }
    };
    public static final Type<BlockPredicate[]> ARRAY_TYPE = new ArrayType<>(TYPE);

    private final HolderSet holderSet;
    private final StatePropertyMatcher[] propertyMatchers;
    private final CompoundTag tag;

    public BlockPredicate(@Nullable final HolderSet holderSet, final StatePropertyMatcher @Nullable [] propertyMatchers, @Nullable final CompoundTag tag) {
        this.holderSet = holderSet;
        this.propertyMatchers = propertyMatchers;
        this.tag = tag;
    }

    public @Nullable HolderSet holderSet() {
        return holderSet;
    }

    public StatePropertyMatcher @Nullable [] propertyMatchers() {
        return propertyMatchers;
    }

    public @Nullable CompoundTag tag() {
        return tag;
    }
}
