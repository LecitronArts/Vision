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

package icyllis.arc3d.granite.shading;

import icyllis.arc3d.core.SLDataType;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.granite.ShaderCodeSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Comparator;

import static icyllis.arc3d.engine.Engine.ShaderFlags;

/**
 * Class that builds a uniform block.
 * <p>
 * The uniform blocks are generally defined as:
 * <pre><code>
 * // Anonymous block
 * layout(std140, binding = 0) uniform UniformBlock {
 *     layout(offset = 0) vec4 SV_Projection;
 *     // per-effect uniforms...
 * }</code></pre>
 * Per-effect uniforms are updated more frequently (generally, each draw op).
 * We should limit the UBO size to 128 bytes.
 */
public class UniformHandler {

    public static final String NO_MANGLE_PREFIX = "SV_";

    /**
     * The 2D orthographic projection matrix has only 4 values (others are identity),
     * so this is a vec4. Projection maps world space into clip space.
     */
    public static final String PROJECTION_NAME = "SV_Projection";
    /**
     * The non-premultiplied paint color, when it cannot be simplified.
     * This value is shared between stages and deduplicated when building uniforms.
     *
     * @see icyllis.arc3d.granite.UniformDataGatherer#writePaintColor
     */
    public static final String PAINT_COLOR_NAME = "SV_PaintColor";

    public static class UniformInfo {

        public ShaderVar mVariable;
        public int mVisibility;
        //public Processor mOwner;
        public String mRawName;

        /**
         * The offset using std140 layout, only valid for non-samplers.
         */
        public int mOffset;
    }

    /**
     * Marks an integer as an opaque handle to a uniform resource.
     */
    @Retention(RetentionPolicy.SOURCE)
    public @interface UniformHandle {
    }

    /**
     * Marks an integer as an opaque handle to a sampler resource.
     */
    @Retention(RetentionPolicy.SOURCE)
    public @interface SamplerHandle {
    }

    /**
     * Layouts.
     */
    public static final int Std140Layout = 0;
    public static final int Std430Layout = 1;

    /**
     * Binding a descriptor set invalidates all higher index descriptor sets. We must bind
     * in the order of this enumeration. Samplers are after Uniforms because Ops can specify
     * GP textures as dynamic state, meaning they get rebound for each draw in a pipeline while
     * uniforms are bound once before all the draws. We bind input attachments after samplers
     * so those also need to be rebound if we bind new samplers.
     */
    public static final int MAIN_DESC_SET = 0;
    public static final int SAMPLER_DESC_SET = 1;
    public static final int INPUT_DESC_SET = 2;

    /*
     * The bindings for the main descriptor set.
     */
    //public static final int UNIFORM_BINDING = 0; // will use Push Constants if possible

    //public static final String UNIFORM_BLOCK_NAME = "UniformBlock";

    /**
     * The bindings for the input descriptor set.
     */
    public static final int INPUT_BINDING = 0;

    protected final ShaderCaps mShaderCaps;
    private final int mLayout;

    public final ArrayList<UniformInfo> mUniforms = new ArrayList<>();
    public final ArrayList<UniformInfo> mSamplers = new ArrayList<>();

    private ArrayList<UniformInfo> mReorderedUniforms;

    public int mCurrentOffset;
    boolean mFinished;

    public UniformHandler(ShaderCaps shaderCaps, int layout) {
        mShaderCaps = shaderCaps;
        mLayout = layout;
    }

    /**
     * Add a uniform variable to the current program, that has visibility in one or more shaders.
     * visibility is a bitfield of ShaderFlag values indicating from which shaders the uniform
     * should be accessible. At least one bit must be set. Geometry shader uniforms are not
     * supported at this time. The actual uniform name will be mangled. The final uniform name
     * can be retrieved by {@link #getUniformName(int)} with the UniformHandle. Use the
     * {@link #addUniformArray(int, byte, String, int, int)} variant to add an array of
     * uniforms.
     * <p>
     * If the name starts with {@link #NO_MANGLE_PREFIX}, the uniform will be assigned to Render
     * Block rather than Effect Block, which may be shared across stages and pipelines. Also,
     * the UniformHandle and its data manager may be only visible and internally handled by
     * implementations.
     *
     * @param visibility combination of ShaderFlags can be zero as placeholder
     * @param type       see {@link SLDataType}
     * @param name       the raw name (pre-mangling), cannot be null or empty
     * @return UniformHandle either from Render Block or Effect Block
     */
    @UniformHandle
    public final int addUniform(int visibility,
                                byte type,
                                String name,
                                int manglingSuffix) {
        assert (name != null && !name.isEmpty());
        assert ((visibility & ~(ShaderFlags.kVertex | ShaderFlags.kFragment)) == 0);
        assert (SLDataType.checkSLType(type));
        assert (!SLDataType.isCombinedSamplerType(type));
        return internalAddUniformArray(visibility, type, name, ShaderVar.kNonArray, manglingSuffix);
    }

    /**
     * Array version of {@link #addUniform(int, byte, String, int)}.
     *
     * @param visibility combination of ShaderFlags, can be zero as placeholder
     * @param type       see {@link SLDataType}
     * @param name       the raw name (pre-mangling), cannot be null or empty
     * @param arraySize  the number of elements, cannot be zero
     * @return UniformHandle either from Render Block or Effect Block
     */
    @UniformHandle
    public final int addUniformArray(int visibility,
                                     byte type,
                                     String name,
                                     int arraySize,
                                     int manglingSuffix) {
        assert (name != null && !name.isEmpty());
        assert ((visibility & ~(ShaderFlags.kVertex | ShaderFlags.kFragment)) == 0);
        assert (SLDataType.checkSLType(type));
        assert (!SLDataType.isCombinedSamplerType(type));
        return internalAddUniformArray(visibility, type, name, arraySize, manglingSuffix);
    }

    /**
     * @param handle UniformHandle from Effect Block
     */
    public ShaderVar getUniformVariable(@UniformHandle int handle) {
        return mUniforms.get(handle).mVariable;
    }

    /**
     * Shortcut for getUniformVariable(handle).getName()
     *
     * @param handle UniformHandle from Effect Block
     */
    public final String getUniformName(@UniformHandle int handle) {
        return getUniformVariable(handle).getName();
    }

    public int numUniforms() {
        return mUniforms.size();
    }

    public int numSamplers() {
        return mSamplers.size();
    }

    public UniformInfo uniform(int index) {
        return mUniforms.get(index);
    }

    // Looks up a uniform that was added by 'owner' with the given 'rawName' (pre-mangling).
    // If there is no such uniform, null is returned.
    /*@Nullable
    public final ShaderVar getUniformMapping(Processor owner, String rawName) {
        for (int i = numUniforms() - 1; i >= 0; i--) {
            final UniformInfo u = uniform(i);
            if (u.mOwner == owner && u.mRawName.equals(rawName)) {
                return u.mVariable;
            }
        }
        return null;
    }*/

    // Like getUniformMapping(), but if the uniform is found it also marks it as accessible in
    // the vertex shader.
    /*@Nullable
    public final ShaderVar liftUniformToVertexShader(Processor owner, String rawName) {
        for (int i = numUniforms() - 1; i >= 0; i--) {
            final UniformInfo u = uniform(i);
            if (u.mOwner == owner && u.mRawName.equals(rawName)) {
                u.mVisibility |= ShaderFlags.kVertex;
                return u.mVariable;
            }
        }
        // Uniform not found; it's better to return a void variable than to assert because sample
        // matrices that are uniform are treated the same for most of the code. When the sample
        // matrix expression can't be found as a uniform, we can infer it's a constant.
        return null;
    }*/

    @UniformHandle
    protected int internalAddUniformArray(int visibility,
                                          byte type,
                                          String name,
                                          int arraySize,
                                          int manglingSuffix) {
        assert (SLDataType.canBeUniformValue(type));
        assert (visibility != 0);

        assert (!name.contains("__"));
        String resolvedName;
        if (name.startsWith(NO_MANGLE_PREFIX)) {
            // deduplicate system variables
            for (int i = 0; i < mUniforms.size(); i++) {
                if (mUniforms.get(i).mRawName.equals(name)) {
                    return i;
                }
            }
            resolvedName = name;
        } else {
            resolvedName = ShaderCodeSource.getMangledName(name, manglingSuffix);
        }
        assert (!resolvedName.contains("__"));

        int handle = mUniforms.size();

        var tempInfo = new UniformInfo();
        tempInfo.mVariable = new ShaderVar(resolvedName,
                type,
                ShaderVar.kNone_TypeModifier,
                arraySize);
        tempInfo.mVisibility = visibility;
        tempInfo.mRawName = name;

        mUniforms.add(tempInfo);
        return handle;
    }

    @SamplerHandle
    public int addSampler(byte type,
                          String name,
                          int manglingSuffix) {
        assert (name != null && !name.isEmpty());

        String resolvedName = ShaderCodeSource.getMangledName(name, manglingSuffix);

        int handle = mSamplers.size();

        String layoutQualifier;
        if (mShaderCaps.mUseUniformBinding) {
            // ARB_shading_language_420pack
            // equivalent to setting texture unit to index
            layoutQualifier = "binding = " + handle;
        } else {
            layoutQualifier = "";
        }

        var tempInfo = new UniformInfo();
        tempInfo.mVariable = new ShaderVar(resolvedName,
                type,
                ShaderVar.kUniform_TypeModifier,
                ShaderVar.kNonArray,
                layoutQualifier,
                "");
        tempInfo.mVisibility = ShaderFlags.kFragment;
        tempInfo.mRawName = name;

        mSamplers.add(tempInfo);
        return handle;
    }

    public String samplerVariable(int handle) {
        return mSamplers.get(handle).mVariable.getName();
    }

    @SamplerHandle
    protected int addInputSampler(short swizzle, String name) {
        // vulkan only
        throw new UnsupportedOperationException();
    }

    protected String inputSamplerVariable(@SamplerHandle int handle) {
        // vulkan only
        throw new UnsupportedOperationException();
    }

    // reorder to minimize block size
    public void finish(boolean reorderUniforms) {
        if (mFinished) return;
        mFinished = true;

        mReorderedUniforms = new ArrayList<>(mUniforms);
        // we can only use std140 layout in OpenGL
        if (reorderUniforms) {
            var cmp = Comparator.comparingInt(
                    (UniformInfo u) -> getAlignmentMask(
                            u.mVariable.getType(),
                            !u.mVariable.isArray(),
                            mLayout)
            );
            // larger alignment first, stable sort
            mReorderedUniforms.sort(cmp.reversed());
        }

        for (UniformInfo u : mReorderedUniforms) {
            int offset = getAlignedOffset(mCurrentOffset,
                    u.mVariable.getType(),
                    u.mVariable.getArraySize(),
                    mLayout);
            mCurrentOffset = offset + getAlignedStride(u.mVariable.getType(),
                    u.mVariable.getArraySize(),
                    mLayout);

            if (mShaderCaps.mUseBlockMemberOffset) {
                // ARB_enhanced_layouts or GLSL 440
                // this is used for validation, since we use standard layout
                u.mVariable.addLayoutQualifier("offset", offset);
            }

            u.mOffset = offset;
        }
    }

    /**
     * Append one block declaration.
     *
     * @param visibility one of ShaderFlags
     * @return true if block is not empty
     */
    public boolean appendUniformDecls(int visibility, int binding, String blockName, StringBuilder out) {
        assert (visibility != 0);
        finish(false);

        boolean firstMember = false;
        boolean firstVisible = false;
        for (var uniform : mReorderedUniforms) {
            assert (SLDataType.canBeUniformValue(uniform.mVariable.getType()));
            if (!firstMember) {
                // Check to make sure we are starting our offset at 0 so the offset qualifier we
                // set on each variable in the uniform block is valid.
                assert (uniform.mOffset == 0);
                firstMember = true;
            }
            if ((uniform.mVisibility & visibility) != 0) {
                firstVisible = true;
            }
        }
        // The uniform block definition for all shader stages must be exactly the same
        if (firstVisible) {
            out.append("layout(");
            out.append(mLayout == Std430Layout ? "std430" : "std140");
            if (mShaderCaps.mUseUniformBinding) {
                // ARB_shading_language_420pack
                out.append(", binding = ");
                out.append(binding);
            }
            out.append(") uniform ");
            out.append(blockName);
            out.append(" {\n");
            for (var uniform : mReorderedUniforms) {
                uniform.mVariable.appendDecl(out);
                out.append(";\n");
            }
            out.append("};\n");
        }
        return firstVisible;
    }

    /**
     * @param visibility one of ShaderFlags
     */
    public void appendSamplerDecls(int visibility, StringBuilder out) {
        for (var sampler : mSamplers) {
            assert (sampler.mVariable.getType() == SLDataType.kSampler2D);
            if ((sampler.mVisibility & visibility) == 0) {
                continue;
            }
            sampler.mVariable.appendDecl(out);
            out.append(";\n");
        }
    }

    /**
     * Returns the base alignment mask in bytes taken up in UBO for SLTypes.
     *
     * @param type     see {@link SLDataType}
     * @param nonArray true for a single scalar or vector, false for an array of scalars or vectors
     * @param layout   1 for std430 layout, 0 for std140 layout
     * @return base alignment mask
     */
    public static int getAlignmentMask(byte type, boolean nonArray, int layout) {
        switch (type) {
            case SLDataType.kBool:   // fall through
            case SLDataType.kInt:    // fall through
            case SLDataType.kUInt:   // fall through
            case SLDataType.kFloat:  // fall through
                return layout == Std430Layout || nonArray ? 0x3 : 0xF; // N - 1
            case SLDataType.kBool2:  // fall through
            case SLDataType.kInt2:  // fall through
            case SLDataType.kUInt2:  // fall through
            case SLDataType.kFloat2:   // fall through
                return layout == Std430Layout || nonArray ? 0x7 : 0xF; // 2N - 1
            case SLDataType.kBool3:  // fall through
            case SLDataType.kBool4:  // fall through
            case SLDataType.kInt3:  // fall through
            case SLDataType.kInt4:  // fall through
            case SLDataType.kUInt3:  // fall through
            case SLDataType.kUInt4:  // fall through
            case SLDataType.kFloat3:   // fall through
            case SLDataType.kFloat4:   // fall through
            case SLDataType.kFloat3x3:   // fall through
            case SLDataType.kFloat4x4:   // fall through
                return 0xF; // 4N - 1
            case SLDataType.kFloat2x2:
                return layout == Std430Layout ? 0x7 : 0xF; // as an array of Vec2

            // This query is only valid for certain types.
            case SLDataType.kVoid:
            case SLDataType.kSampler2D:
            case SLDataType.kTexture2D:
            case SLDataType.kSampler:
            case SLDataType.kSubpassInput:
                throw new IllegalStateException(String.valueOf(type));
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Returns the size in bytes taken up in UBO for SLTypes.
     * This includes paddings between components, but does not include paddings at the end of the element.
     *
     * @param type   see {@link SLDataType}
     * @param layout 1 for std430 layout, 0 for std140 layout
     * @return size in bytes
     * @see UniformDataManager
     */
    public static int getSize(byte type, int layout) {
        switch (type) {
            case SLDataType.kFloat:
                return Float.BYTES;
            case SLDataType.kFloat2:
                return 2 * Float.BYTES;
            case SLDataType.kFloat3:
                return 3 * Float.BYTES;
            case SLDataType.kFloat4:
                return 4 * Float.BYTES;
            case SLDataType.kBool:   // fall through
            case SLDataType.kInt:    // fall through
            case SLDataType.kUInt:
                return Integer.BYTES;
            case SLDataType.kBool2:  // fall through
            case SLDataType.kInt2:  // fall through
            case SLDataType.kUInt2:
                return 2 * Integer.BYTES;
            case SLDataType.kBool3:  // fall through
            case SLDataType.kInt3:  // fall through
            case SLDataType.kUInt3:
                return 3 * Integer.BYTES;
            case SLDataType.kBool4:  // fall through
            case SLDataType.kInt4:  // fall through
            case SLDataType.kUInt4:
                return 4 * Integer.BYTES;
            case SLDataType.kFloat2x2:
                return layout == Std430Layout ? 2 * 2 * Float.BYTES : 2 * 4 * Float.BYTES;
            case SLDataType.kFloat3x3:
                return 3 * 4 * Float.BYTES;
            case SLDataType.kFloat4x4:
                return 4 * 4 * Float.BYTES;

            // This query is only valid for certain types.
            case SLDataType.kVoid:
            case SLDataType.kSampler2D:
            case SLDataType.kTexture2D:
            case SLDataType.kSampler:
            case SLDataType.kSubpassInput:
                throw new IllegalStateException(String.valueOf(type));
        }
        throw new IllegalArgumentException(String.valueOf(type));
    }

    /**
     * Given the current offset into the UBO data, calculate the offset for the uniform we're trying to
     * add taking into consideration all alignment requirements. Use aligned offset plus
     * {@link #getAlignedStride(byte, int, int)} to get the offset to the end of the new uniform.
     *
     * @param offset    the current offset
     * @param type      see {@link SLDataType}
     * @param arraySize see {@link ShaderVar}
     * @param layout    1 for std430 layout, 0 for std140 layout
     * @return the aligned offset for the new uniform
     */
    public static int getAlignedOffset(int offset,
                                       byte type,
                                       int arraySize,
                                       int layout) {
        assert (SLDataType.checkSLType(type));
        assert (arraySize == ShaderVar.kNonArray) || (arraySize >= 1);
        int alignmentMask = getAlignmentMask(type, arraySize == ShaderVar.kNonArray, layout);
        return (offset + alignmentMask) & ~alignmentMask;
    }

    /**
     * @see UniformDataManager
     */
    public static int getAlignedStride(byte type,
                                       int arraySize,
                                       int layout) {
        assert (SLDataType.checkSLType(type));
        assert (arraySize == ShaderVar.kNonArray) || (arraySize >= 1);
        if (arraySize == ShaderVar.kNonArray) {
            return getSize(type, layout);
        } else {
            final int elementSize;
            if (layout == Std430Layout) {
                elementSize = getSize(type, Std430Layout);
            } else {
                // std140, round up to Vec4
                // currently, values greater than 16 are already multiples of 16, so just use max
                elementSize = Math.max(getSize(type, Std140Layout), 16);
            }
            assert ((elementSize & (0xF)) == 0);
            return elementSize * arraySize;
        }
    }
}
