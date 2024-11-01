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

import icyllis.arc3d.core.Size;
import org.jetbrains.annotations.Contract;

/**
 * Represents a color component mapping. It's packed as a <code>short</code> value.
 * <p>
 * <b>Do NOT change the packing format, there are inlined code in other places</b>.
 */
//TODO Project Valhalla, make this as primitive (record) class
public final class Swizzle {

    // default value
    public static final short RGBA = 0x3210;
    public static final short BGRA = 0x3012;
    public static final short RGB1 = 0x5210;
    public static final short BGR1 = 0x5012;
    public static final short AAAA = 0x3333;

    public static final short INVALID = (short) 0xFFFF;

    static {
        // make them inline at compile-time
        assert make('r', 'g', 'b', 'a') == RGBA;
        assert make('b', 'g', 'r', 'a') == BGRA;
        assert make('r', 'g', 'b', '1') == RGB1;
        assert make('b', 'g', 'r', '1') == BGR1;
        assert make('a', 'a', 'a', 'a') == AAAA;
        assert concat(make('1', '1', '1', 'r'), AAAA) == make('r', 'r', 'r', 'r');
    }

    @Contract(pure = true)
    public static int charToIndex(char c) {
        return switch (c) {
            // r...a must map to 0...3 because other methods use them as indices into mSwiz.
            case 'r' -> 0;
            case 'g' -> 1;
            case 'b' -> 2;
            case 'a' -> 3;
            case '0' -> 4;
            case '1' -> 5;
            default -> throw new AssertionError(c);
        };
    }

    @Contract(pure = true)
    public static char indexToChar(int idx) {
        return switch (idx) {
            case 0 -> 'r';
            case 1 -> 'g';
            case 2 -> 'b';
            case 3 -> 'a';
            case 4 -> '0';
            case 5 -> '1';
            default -> throw new AssertionError(idx);
        };
    }

    /**
     * Compact representation of the swizzle suitable for a key. Letters must be lowercase.
     */
    @Contract(pure = true)
    public static short make(String s) {
        return make(s.charAt(0), s.charAt(1), s.charAt(2), s.charAt(3));
    }

    /**
     * Compact representation of the swizzle suitable for a key. Letters must be lowercase.
     */
    @Contract(pure = true)
    public static short make(char r, char g, char b, char a) {
        return (short) (charToIndex(r) | (charToIndex(g) << 4) | (charToIndex(b) << 8) | (charToIndex(a) << 12));
    }

    /**
     * Concatenates two swizzles (e.g. concat("111R", "AAAA") -> "RRRR").
     */
    public static short concat(short a, short b) {
        int swizzle = 0;
        for (int i = 0; i < 4; ++i) {
            int idx = (b >> (4 * i)) & 0xF;
            if (idx != 4 && idx != 5) {
                assert idx < 4;
                // Get the index value stored in 'a' at location 'idx'.
                idx = (a >> (4 * idx)) & 0xF;
            }
            swizzle |= (idx << (4 * i));
        }
        return (short) swizzle;
    }

    /**
     * Applies this swizzle to the input color and returns the swizzled color.
     */
    public static float[] apply(short swizzle, @Size(min = 4) float[] v) {
        final float
                r = v[0],
                g = v[1],
                b = v[2],
                a = v[3];
        for (int i = 0; i < 4; ++i) {
            v[i] = switch (swizzle & 0xF) {
                case 0 -> r;
                case 1 -> g;
                case 2 -> b;
                case 3 -> a;
                case 4 -> 0.0f;
                case 5 -> 1.0f;
                default -> throw new IllegalStateException();
            };
            swizzle >>= 4;
        }
        return v;
    }

    public static String toString(short swizzle) {
        return "" + indexToChar(swizzle & 0xF) +
                indexToChar((swizzle >> 4) & 0xF) +
                indexToChar((swizzle >> 8) & 0xF) +
                indexToChar(swizzle >>> 12);
    }
}
