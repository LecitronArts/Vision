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

package icyllis.modernui.mc.text;

import icyllis.arc3d.core.MathUtil;
import icyllis.modernui.graphics.text.CharSequenceBuilder;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;

/**
 * The layout key that iterates base {@link FormattedText} or
 * {@link FormattedCharSequence} to build multi-component texts that
 * match the vanilla's non-parallel style transition mechanism. Fast digit replacement
 * is not applicable here.
 *
 * @author BloCamLimb
 * @see CharacterStyle
 * @see VanillaLayoutKey
 */
public class FormattedLayoutKey {

    /**
     * Texts use String as their backing store, none of them contains {@link ChatFormatting} codes.
     */
    private String[] mTexts;

    /**
     * References to the font set which layers are decorated.
     *
     * @see Style#getFont()
     */
    private Object[] mFonts;

    /**
     * Packed fields that contain RGB color and appearance-affecting bit flags
     * which layers are decorated.
     *
     * @see CharacterStyle#flatten(Style)
     */
    private int[] mCodes;

    /**
     * Cached hash code.
     */
    int mHash;

    private FormattedLayoutKey() {
    }

    private FormattedLayoutKey(String[] texts,
                               Object[] fonts,
                               int[] codes, int hash) {
        mTexts = texts;
        mFonts = fonts;
        mCodes = codes;
        mHash = hash;
    }

    @Override
    public int hashCode() {
        int h = mHash;

        if (h == 0) {
            h = 1;
            var codes = mCodes;
            for (int i = 0, e = codes.length; i < e; i++) {
                h = 31 * h + mTexts[i].hashCode();
                h = 31 * h + mFonts[i].hashCode();
                h = 31 * h + codes[i];
            }
            mHash = h;
        }

        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (o.getClass() != FormattedLayoutKey.class) {
            return false;
        }
        FormattedLayoutKey key = (FormattedLayoutKey) o;
        return Arrays.equals(mCodes, key.mCodes) &&
                Arrays.equals(mFonts, key.mFonts) &&
                Arrays.equals(mTexts, key.mTexts);
    }

    @Override
    public String toString() {
        return "FormattedLayoutKey{" +
                "mTexts=" + Arrays.toString(mTexts) +
                ", mFonts=" + Arrays.toString(mFonts) +
                ", mCodes=" + Arrays.toString(mCodes) +
                ", mHash=" + mHash +
                '}';
    }

    /**
     * @return approximate memory usage
     */
    public int getMemorySize() {
        int size = 0;
        // take copied buffers into account
        for (CharSequence s : mTexts) {
            size += MathUtil.align4(s.length()) << 1;
        }
        // shared pointers are memoryless, see JDK memory layout
        size += (16 + (MathUtil.align2(mTexts.length) << 2)) * 3;
        return size + 32;
    }

    /**
     * Designed for performance, this also ensures hashCode() and equals() of Key
     * strictly matched in various cases.
     */
    public static class Lookup extends FormattedLayoutKey {

        private final ObjectArrayList<CharSequence> mTexts = new ObjectArrayList<>();
        private final ObjectArrayList<ResourceLocation> mFonts = new ObjectArrayList<>();
        private final IntArrayList mCodes = new IntArrayList();

        private final ContentBuilder mContentBuilder = new ContentBuilder();

        /**
         * Always in logical order.
         */
        private class ContentBuilder implements FormattedText.StyledContentConsumer<Object> {

            @Nonnull
            @Override
            public Optional<Object> accept(@Nonnull Style style, @Nonnull String content) {
                mTexts.add(content);
                mFonts.add(style.getFont());
                mCodes.add(CharacterStyle.flatten(style));
                return Optional.empty(); // continue
            }
        }

        private final SequenceBuilder mSequenceBuilder = new SequenceBuilder();

        /**
         * Always LTR. Build multi-component text.
         *
         * @see FormattedTextWrapper#accept(FormattedCharSink)
         */
        private class SequenceBuilder implements FormattedCharSink {

            private final Pools.Pool<CharSequenceBuilder> mPool = Pools.newSimplePool(20);

            private CharSequenceBuilder mBuilder = null;
            private Style mStyle = null;

            private void allocate() {
                mBuilder = mPool.acquire();
                if (mBuilder == null) {
                    mBuilder = new CharSequenceBuilder();
                } else {
                    mBuilder.clear();
                }
            }

            @Override
            public boolean accept(int index, @Nonnull Style style, int codePoint) {
                if (mStyle == null) {
                    allocate();
                    mStyle = style;
                } else if (!CharacterStyle.equalsForTextLayout(mStyle, style)) {
                    // there's a style transition, break here and append last component
                    if (!mBuilder.isEmpty()) {
                        mTexts.add(mBuilder);
                        mFonts.add(mStyle.getFont());
                        mCodes.add(CharacterStyle.flatten(mStyle));
                        allocate();
                    }
                    mStyle = style;
                }
                mBuilder.addCodePoint(codePoint);
                return true; // continue
            }

            private void end() {
                // append last component
                if (mBuilder != null && !mBuilder.isEmpty()) {
                    mTexts.add(mBuilder);
                    mFonts.add(mStyle.getFont());
                    mCodes.add(CharacterStyle.flatten(mStyle));
                }
                // we later make copies to generate a Key, so we can release these builders
                // back into the pool now
                for (CharSequence s : mTexts) {
                    mPool.release((CharSequenceBuilder) s);
                }
                mBuilder = null;
                mStyle = null;
            }
        }

        private void reset() {
            assert mTexts.size() == mFonts.size() &&
                    mTexts.size() == mCodes.size();
            mTexts.clear();
            mFonts.clear();
            mCodes.clear();
            mHash = 0;
        }

        /**
         * Update this key.
         */
        @Nonnull
        public FormattedLayoutKey update(@Nonnull FormattedText text, @Nonnull Style style) {
            reset();
            text.visit(mContentBuilder, style);
            return this;
        }

        /**
         * Update this key.
         */
        @Nonnull
        public FormattedLayoutKey update(@Nonnull FormattedCharSequence sequence) {
            reset();
            sequence.accept(mSequenceBuilder);
            mSequenceBuilder.end();
            return this;
        }

        @Override
        public int hashCode() {
            int h = mHash;

            if (h == 0) {
                h = 1;
                final Object[] texts = mTexts.elements();
                final Object[] fonts = mFonts.elements();
                var codes = mCodes.elements();
                for (int i = 0, e = mCodes.size(); i < e; i++) {
                    h = 31 * h + texts[i].hashCode();
                    h = 31 * h + fonts[i].hashCode();
                    h = 31 * h + codes[i];
                }
                mHash = h;
            }

            return h;
        }

        @Override
        public boolean equals(Object o) {
            if (o.getClass() != FormattedLayoutKey.class) {
                return false;
            }
            FormattedLayoutKey key = (FormattedLayoutKey) o;
            final int length = mTexts.size();
            return length == key.mTexts.length &&
                    Arrays.equals(mCodes.elements(), 0, length, key.mCodes, 0, length) &&
                    Arrays.equals(mFonts.elements(), 0, length, key.mFonts, 0, length) &&
                    Arrays.equals(mTexts.elements(), 0, length, key.mTexts, 0, length);
        }

        @Override
        public String toString() {
            return "Lookup{" +
                    "mTexts=" + mTexts +
                    ", mFonts=" + mFonts +
                    ", mCodes=" + mCodes +
                    '}';
        }

        /**
         * Make a cache key. We always use String.hashCode() implementation.
         *
         * @return a storage key
         */
        @Nonnull
        public FormattedLayoutKey copy() {
            final int length = mTexts.size();
            String[] texts = new String[length];
            for (int i = 0; i < length; i++) {
                // String returns self, CharSequenceBuilder returns a new String
                texts[i] = mTexts.get(i).toString();
            }
            return new FormattedLayoutKey(texts,
                    mFonts.toArray(),
                    mCodes.toIntArray(), mHash);
        }
    }
}
