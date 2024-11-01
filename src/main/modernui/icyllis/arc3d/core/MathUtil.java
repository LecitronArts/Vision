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

package icyllis.arc3d.core;

/**
 * Utility class that provides auxiliary operations.
 */
public class MathUtil {

    // compile-time
    private static final boolean USE_SIN_TABLE = false;

    // 256 kB
    private static final float[] SIN_TABLE;

    public static final float PI = (float) Math.PI;
    public static final float PI2 = (float) (Math.PI * 2.0);
    public static final float PI3 = (float) (Math.PI * 3.0);
    public static final float PI4 = (float) (Math.PI * 4.0);
    public static final float PI_O_2 = (float) (Math.PI / 2.0);
    public static final float PI_O_3 = (float) (Math.PI / 3.0);
    public static final float PI_O_4 = (float) (Math.PI / 4.0);
    public static final float PI_O_6 = (float) (Math.PI / 6.0);

    public static final float EPS = 1.0e-5f;
    public static final float INV_EPS = 1.0e5f;

    // DEG_TO_RAD == 1.0 / RAD_TO_DEG
    public static final float DEG_TO_RAD = 0.01745329251994329576923690768489f;
    public static final float RAD_TO_DEG = 57.295779513082320876798154814105f;

    // SQRT2 == INV_SQRT2 * 2.0
    public static final float SQRT2 = 1.4142135623730951f;
    public static final float INV_SQRT2 = 0.7071067811865476f;

    /**
     * The tolerance (error) for 2D renderer.
     */
    // PATH_TOLERANCE == Math.ulp(2500.0f) == 1f / 4096
    public static final float PATH_TOLERANCE = 1f / (1 << 12);

    static {
        if (USE_SIN_TABLE) {
            float[] v = new float[0x10000];
            for (int i = 0; i < 0x10000; i++)
                v[i] = (float) Math.sin(i * 9.587379924285257E-5);
            SIN_TABLE = v;
        } else {
            SIN_TABLE = null;
        }
    }

    // fast sin, error +- 0.000152, in radians
    private static float fsin(float a) {
        return SIN_TABLE[Math.round(a * 10430.378f) & 0xffff];
    }

    // fast cos, error +- 0.000152, in radians
    private static float fcos(float a) {
        return SIN_TABLE[(Math.round(a * 10430.378f) + 0x4000) & 0xffff];
    }

    // sin
    public static float sin(float a) {
        return (float) Math.sin(a);
    }

    // cos
    public static float cos(float a) {
        return (float) Math.cos(a);
    }

    // tan
    public static float tan(float a) {
        return (float) Math.tan(a);
    }

    /**
     * Decomposes <var>x</var> into a floating-point significand in the
     * range <code>(-1,-0.5], [0.5,1.0)</code>, and an integral exponent of two,
     * such that
     * <pre>x = significand * 2<sup>exponent</sup></pre>
     * If <var>x</var> is +- zero, +- Inf, or NaN, returns the input value
     * as-is, and sets <var>exp</var> to 0.
     *
     * @param x   the input value
     * @param exp the resulting exponent
     * @return the resulting significand
     */
    public static float frexp(float x, int[] exp) { // fraction exponent
        int bits = Float.floatToRawIntBits(x);
        int high = bits & 0x7fffffff;
        exp[0] = 0;
        if (high == 0 | high >= 0x7f800000) {
            // 0, Inf, or NaN
            return x;
        }
        if (high < 0x00800000) {
            // denorm
            bits = Float.floatToRawIntBits(0x1p25f * x);
            high = bits & 0x7fffffff;
            exp[0] = -25;
        }
        exp[0] += (high >> 23) - 126;
        return Float.intBitsToFloat((bits & 0x807fffff) | 0x3f000000);
    }

    /**
     * Composes a floating-point number from <var>x</var> and the
     * corresponding integral exponent of two in <var>exp</var>, resulting
     * <pre>significand * 2<sup>exponent</sup></pre>
     * If <var>x</var> is +- zero, +- Inf, or NaN, returns the input value
     * as-is. If overflow or underflow occurs, returns a rounded value.
     *
     * @param x   the input significand
     * @param exp the input exponent
     * @return the resulting value
     */
    public static float ldexp(float x, int exp) { // load exponent
        if (exp > 127) {
            // large scaling
            x *= 0x1p127f;
            exp -= 127;
            if (exp > 127) {
                x *= 0x1p127f;
                exp -= 127;
                if (exp > 127)
                    exp = 127; // huge scaling
            }
        } else if (exp < -126) {
            // large scaling
            x *= 0x1p-126f;
            exp += 126;
            if (exp < -126) {
                x *= 0x1p-126f;
                exp += 126;
                if (exp < -126)
                    exp = -126; // huge scaling
            }
        }
        return x * Float.intBitsToFloat((exp + 127) << 23);
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero
     */
    public static boolean isApproxZero(float a) {
        return Math.abs(a) <= EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to zero within a given tolerance
     */
    public static boolean isApproxZero(float a,
                                       float tolerance) {
        assert tolerance >= 0;
        return Math.abs(a) <= tolerance;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code>
     */
    public static boolean isApproxEqual(float a, float b) {
        return Math.abs(a - b) <= EPS;
    }

    /**
     * @return true if <code>a</code> is approximately equal to <code>b</code> within a given tolerance
     */
    public static boolean isApproxEqual(float a, float b,
                                        float tolerance) {
        assert tolerance >= 0;
        return Math.abs(a - b) <= tolerance;
    }

    // square root
    public static float sqrt(float f) {
        return (float) Math.sqrt(f);
    }

    // asin
    public static float asin(float a) {
        return (float) Math.asin(a);
    }

    // acos
    public static float acos(float a) {
        return (float) Math.acos(a);
    }

    // atan2 (b, a)
    public static float atan2(float a, float b) {
        return (float) Math.atan2(a, b);
    }

    /**
     * If x compares less than min, returns min; otherwise if max compares less than x,
     * returns max; otherwise returns x.
     *
     * @return x clamped between min and max, inclusively.
     */
    public static int clamp(int x, int min, int max) {
        return Math.min(max, Math.max(x, min));
    }

    /**
     * If x compares less than min, returns min; otherwise if max compares less than x,
     * returns max; otherwise returns x.
     *
     * @return x clamped between min and max, inclusively.
     */
    public static long clamp(long x, long min, long max) {
        return Math.min(max, Math.max(x, min));
    }

    /**
     * Clamps x between min and max, exactly. If x is NaN, returns NaN.
     *
     * @return x clamped between min and max
     */
    public static float clamp(float x, float min, float max) {
        return Math.min(max, Math.max(x, min));
    }

    /**
     * Clamps x between min and max, exactly. If x is NaN, returns NaN.
     *
     * @return x clamped between min and max
     */
    public static double clamp(double x, double min, double max) {
        return Math.min(max, Math.max(x, min));
    }

    /**
     * Clamps x between min and max. If x is NaN, returns min.
     * Note the result is incorrect if min is negative zero.
     *
     * @return x clamped between min and max
     */
    @SuppressWarnings("ManualMinMaxCalculation")
    public static float pin(float x, float min, float max) {
        float y = max < x ? max : x;
        return min < y ? y : min;
    }

    /**
     * Clamps x between min and max. If x is NaN, returns min.
     * Note the result is incorrect if min is negative zero.
     *
     * @return x clamped between min and max
     */
    @SuppressWarnings("ManualMinMaxCalculation")
    public static double pin(double x, double min, double max) {
        double y = max < x ? max : x;
        return min < y ? y : min;
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static float min(float a, float b, float c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static float min3(float[] v) {
        return Math.min(Math.min(v[0], v[1]), v[2]);
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static double min(double a, double b, double c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static float min(float a, float b, float c, float d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    /**
     * Component-wise minimum of a vector.
     */
    public static double min(double a, double b, double c, double d) {
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static float max(float a, float b, float c) {
        return Math.max(Math.max(a, b), c);
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static float max3(float[] v) {
        return Math.max(Math.max(v[0], v[1]), v[2]);
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static double max(double a, double b, double c) {
        return Math.max(Math.max(a, b), c);
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static float max(float a, float b, float c, float d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * Component-wise maximum of a vector.
     */
    public static double max(double a, double b, double c, double d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * Median of three numbers.
     */
    public static float median(float a, float b, float c) {
        return clamp(c, Math.min(a, b), Math.max(a, b));
    }

    /**
     * Median of three numbers.
     */
    public static double median(double a, double b, double c) {
        return clamp(c, Math.min(a, b), Math.max(a, b));
    }

    /**
     * Linear interpolation between two values.
     */
    public static float lerp(float a, float b, float t) {
        return (b - a) * t + a;
    }

    /**
     * Linear interpolation between two values.
     */
    public static double lerp(double a, double b, double t) {
        return (b - a) * t + a;
    }

    /**
     * Linear interpolation between two values, matches GLSL {@code mix} intrinsic function.
     * Slower than {@link #lerp(float, float, float)} but without intermediate overflow or underflow.
     */
    public static float mix(float a, float b, float t) {
        return a * (1 - t) + b * t;
    }

    /**
     * Linear interpolation between two values, matches GLSL {@code mix} intrinsic function.
     * Slower than {@link #lerp(double, double, double)} but without intermediate overflow or underflow.
     */
    public static double mix(double a, double b, double t) {
        return a * (1 - t) + b * t;
    }

    /**
     * 2D bilinear interpolation between four values (a rect).
     */
    public static float biLerp(float q00, float q10, float q01, float q11,
                               float tx, float ty) {
        return lerp(
                lerp(q00, q10, tx),
                lerp(q01, q11, tx),
                ty
        );
    }

    /**
     * 2D bilinear interpolation between four values (a rect).
     */
    public static double biLerp(double q00, double q10, double q01, double q11,
                                double tx, double ty) {
        return lerp(
                lerp(q00, q10, tx),
                lerp(q01, q11, tx),
                ty
        );
    }

    /**
     * 3D trilinear interpolation between eight values (a cuboid).
     */
    public static float triLerp(float c000, float c100, float c010, float c110,
                                float c001, float c101, float c011, float c111,
                                float tx, float ty, float tz) {
        return lerp(
                biLerp(c000, c100, c010, c110, tx, ty),
                biLerp(c001, c101, c011, c111, tx, ty),
                tz
        );
    }

    /**
     * 3D trilinear interpolation between eight values (a cuboid).
     */
    public static double triLerp(double c000, double c100, double c010, double c110,
                                 double c001, double c101, double c011, double c111,
                                 double tx, double ty, double tz) {
        return lerp(
                biLerp(c000, c100, c010, c110, tx, ty),
                biLerp(c001, c101, c011, c111, tx, ty),
                tz
        );
    }

    /**
     * Returns true if all values are finite.
     *
     * @return true if no member is infinite or NaN
     */
    @SuppressWarnings("PointlessArithmeticExpression")
    public static boolean isFinite(float v0, float v1, float v2, float v3) {
        float prod = v0 - v0;
        prod = prod * v1 * v2 * v3;
        // At this point, `prod` will either be NaN or 0.
        return prod == prod;
    }

    /**
     * Returns true if all values are finite.
     *
     * @return true if no member is infinite or NaN
     */
    @SuppressWarnings("PointlessArithmeticExpression")
    public static boolean isFinite(float v0, float v1, float v2,
                                   float v3, float v4, float v5) {
        float prod = v0 - v0;
        prod = prod * v1 * v2 * v3 * v4 * v5;
        // At this point, `prod` will either be NaN or 0.
        return prod == prod;
    }

    /**
     * Returns true if all values are finite.
     *
     * @return true if no member is infinite or NaN
     */
    @SuppressWarnings("PointlessArithmeticExpression")
    public static boolean isFinite(float v0, float v1, float v2,
                                   float v3, float v4, float v5,
                                   float v6, float v7, float v8) {
        float prod = v0 - v0;
        prod = prod * v1 * v2 * v3 * v4 * v5 * v6 * v7 * v8;
        // At this point, `prod` will either be NaN or 0.
        return prod == prod;
    }

    /**
     * Returns true if all values are finite.
     *
     * @return true if no member is infinite or NaN
     */
    @SuppressWarnings("PointlessArithmeticExpression")
    public static boolean isFinite(float[] values, int offset, int count) {
        assert count > 0;
        // Subtracting a value from itself will result in zero, except for NAN or ±Inf, which make NAN.
        // Multiplying a group of values against zero will result in zero for each product, except for
        // NAN or ±Inf, which will result in NAN and continue resulting in NAN for the rest of the elements.
        float v0 = values[offset];
        float prod = v0 - v0;
        while (--count != 0) {
            prod *= values[++offset];
        }
        // At this point, `prod` will either be NaN or 0.
        return prod == prod;
    }

    /**
     * Aligns {@code a} up to 2 (half-word).
     */
    public static int align2(int a) {
        assert a >= 0 && a <= Integer.MAX_VALUE - 8;
        return (a + 1) & -2;
    }

    /**
     * Aligns {@code a} up to 4 (word).
     */
    public static int align4(int a) {
        assert a >= 0 && a <= Integer.MAX_VALUE - 8;
        return (a + 3) & -4;
    }

    /**
     * Aligns {@code a} up to 8 (double word).
     */
    public static int align8(int a) {
        assert a >= 0 && a <= Integer.MAX_VALUE - 8;
        return (a + 7) & -8;
    }

    /**
     * Aligns {@code a} up to 2 (half-word).
     */
    public static long align2(long a) {
        assert a >= 0 && a <= Long.MAX_VALUE - 16;
        return (a + 1) & -2;
    }

    /**
     * Aligns {@code a} up to 4 (word).
     */
    public static long align4(long a) {
        assert a >= 0 && a <= Long.MAX_VALUE - 16;
        return (a + 3) & -4;
    }

    /**
     * Aligns {@code a} up to 8 (double word).
     */
    public static long align8(long a) {
        assert a >= 0 && a <= Long.MAX_VALUE - 16;
        return (a + 7) & -8;
    }

    /**
     * Returns {@code true} if {@code a} is a multiple of 2. Asserts {@code a >= 0}.
     */
    public static boolean isAlign2(int a) {
        assert a >= 0;
        return (a & 1) == 0;
    }

    /**
     * Returns {@code true} if {@code a} is a multiple of 4. Asserts {@code a >= 0}.
     */
    public static boolean isAlign4(int a) {
        assert a >= 0;
        return (a & 3) == 0;
    }

    /**
     * Returns {@code true} if {@code a} is a multiple of 8. Asserts {@code a >= 0}.
     */
    public static boolean isAlign8(int a) {
        assert a >= 0;
        return (a & 7) == 0;
    }

    /**
     * Returns {@code true} if {@code a} is a multiple of 2. Asserts {@code a >= 0}.
     */
    public static boolean isAlign2(long a) {
        assert a >= 0;
        return (a & 1) == 0;
    }

    /**
     * Returns {@code true} if {@code a} is a multiple of 4. Asserts {@code a >= 0}.
     */
    public static boolean isAlign4(long a) {
        assert a >= 0;
        return (a & 3) == 0;
    }

    /**
     * Returns {@code true} if {@code a} is a multiple of 8. Asserts {@code a >= 0}.
     */
    public static boolean isAlign8(long a) {
        assert a >= 0;
        return (a & 7) == 0;
    }

    /**
     * Aligns {@code a} up to a power of two.
     */
    public static int alignTo(int a, int alignment) {
        assert alignment > 0 && (alignment & (alignment - 1)) == 0;
        return (a + alignment - 1) & -alignment;
    }

    /**
     * Aligns {@code a} up to a power of two.
     */
    public static long alignTo(long a, long alignment) {
        assert alignment > 0 && (alignment & (alignment - 1)) == 0;
        return (a + alignment - 1) & -alignment;
    }

    public static int alignUp(int a, int alignment) {
        assert alignment > 0;
        int r = a % alignment;
        return r == 0 ? a : a + alignment - r;
    }

    public static int alignUpPad(int a, int alignment) {
        assert alignment > 0;
        return (alignment - a % alignment) % alignment;
    }

    public static int alignDown(int a, int alignment) {
        assert alignment > 0;
        return (a / alignment) * alignment;
    }

    /**
     * Returns {@code true} if {@code a} is a power of 2. Asserts {@code a > 0}.
     */
    public static boolean isPow2(int a) {
        assert a > 0 : "undefined";
        return (a & a - 1) == 0;
    }

    /**
     * Returns {@code true} if {@code a} is a power of 2. Asserts {@code a > 0}.
     */
    public static boolean isPow2(long a) {
        assert a > 0 : "undefined";
        return (a & a - 1) == 0;
    }

    /**
     * Returns the log2 of {@code a}, were that value to be rounded up to the
     * next power of 2. Asserts {@code a > 0}. NextLog2.
     */
    public static int ceilLog2(int a) {
        assert a > 0 : "undefined";
        return Integer.SIZE - Integer.numberOfLeadingZeros(a - 1);
    }

    /**
     * Returns the log2 of {@code a}, were that value to be rounded up to the
     * next power of 2. Asserts {@code a > 0}. NextLog2.
     */
    public static int ceilLog2(long a) {
        assert a > 0 : "undefined";
        return Long.SIZE - Long.numberOfLeadingZeros(a - 1);
    }

    /**
     * Returns the smallest power of two greater than or equal to {@code a}.
     * Asserts {@code a > 0 && a <= 2^30}. NextPow2.
     */
    public static int ceilPow2(int a) {
        assert a > 0 && a <= (1 << (Integer.SIZE - 2)) : "undefined";
        return 1 << -Integer.numberOfLeadingZeros(a - 1);
    }

    /**
     * Returns the smallest power of two greater than or equal to {@code a}.
     * Asserts {@code a > 0 && a <= 2^62}. NextPow2.
     */
    public static long ceilPow2(long a) {
        assert a > 0 && a <= (1L << (Long.SIZE - 2)) : "undefined";
        return 1L << -Long.numberOfLeadingZeros(a - 1);
    }

    /**
     * Returns the log2 of {@code a}, were that value to be rounded down to the
     * previous power of 2. Asserts {@code a > 0}. PrevLog2.
     */
    public static int floorLog2(int a) {
        assert a > 0 : "undefined";
        return (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(a);
    }

    /**
     * Returns the log2 of {@code a}, were that value to be rounded down to the
     * previous power of 2. Asserts {@code a > 0}. PrevLog2.
     */
    public static int floorLog2(long a) {
        assert a > 0 : "undefined";
        return (Long.SIZE - 1) - Long.numberOfLeadingZeros(a);
    }

    /**
     * Returns the largest power of two less than or equal to {@code a}.
     * Asserts {@code a > 0}. PrevPow2.
     */
    public static int floorPow2(int a) {
        assert a > 0 : "undefined";
        return Integer.highestOneBit(a);
    }

    /**
     * Returns the largest power of two less than or equal to {@code a}.
     * Asserts {@code a > 0}. PrevPow2.
     */
    public static long floorPow2(long a) {
        assert a > 0 : "undefined";
        return Long.highestOneBit(a);
    }

    /**
     * Returns the log2 of the provided value, were that value to be rounded up to the next power of 2.
     * Returns 0 if value <= 0:<br>
     * Never returns a negative number, even if value is NaN.
     * <pre>
     *     ceilLog2((-inf..1]) -> 0
     *     ceilLog2((1..2])    -> 1
     *     ceilLog2((2..4])    -> 2
     *     ceilLog2((4..8])    -> 3
     *     ceilLog2(+inf)      -> 128
     *     ceilLog2(NaN)       -> 0
     * </pre>
     * NextLog2.
     */
    public static int ceilLog2(float v) {
        int exp = ((Float.floatToRawIntBits(v) + (1 << 23) - 1) >> 23) - 127;
        return exp & ~(exp >> 31);
    }

    /**
     * Returns ceil(log2(sqrt(x))):
     * <pre>
     *     log2(sqrt(x)) == log2(x^(1/2)) == log2(x)/2 == log2(x)/log2(4) == log4(x)
     * </pre>
     */
    public static int ceilLog4(float v) {
        return (ceilLog2(v) + 1) >> 1;
    }

    /**
     * Returns ceil(log2(sqrt(sqrt(x)))):
     * <pre>
     *     log2(sqrt(sqrt(x))) == log2(x^(1/4)) == log2(x)/4 == log2(x)/log2(16) == log16(x)
     * </pre>
     */
    public static int ceilLog16(float v) {
        return (ceilLog2(v) + 3) >> 2;
    }

    protected MathUtil() {
        throw new UnsupportedOperationException();
    }
}
