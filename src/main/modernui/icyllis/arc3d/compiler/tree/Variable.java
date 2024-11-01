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

package icyllis.arc3d.compiler.tree;

import icyllis.arc3d.compiler.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;

/**
 * Represents a variable symbol, whether local, global, or a function parameter.
 * This represents the variable itself (the storage location), which is shared
 * between all VariableReferences which read or write that storage location.
 */
public final class Variable extends Symbol {

    public static final byte
            kLocal_Storage = 0,
            kGlobal_Storage = 1,
            kParameter_Storage = 2;

    private final Modifiers mModifiers;
    private final Type mType;
    private final byte mStorage;
    private final boolean mBuiltin;

    private Node mDecl;
    private WeakReference<InterfaceBlock> mInterfaceBlock;

    public Variable(int position, Modifiers modifiers,
                    Type type, String name, byte storage, boolean builtin) {
        super(position, name);
        mModifiers = modifiers;
        mType = type;
        mStorage = storage;
        mBuiltin = builtin;
    }

    @Nonnull
    public static Variable convert(@Nonnull Context context,
                                   int pos,
                                   @Nonnull Modifiers modifiers,
                                   @Nonnull Type type,
                                   @Nonnull String name,
                                   byte storage) {
        if (context.getKind().isCompute() && (modifiers.layoutFlags() & Layout.kBuiltin_LayoutFlag) == 0) {
            if (storage == Variable.kGlobal_Storage) {
                if ((modifiers.flags() & Modifiers.kIn_Flag) != 0) {
                    context.error(pos, "pipeline inputs not permitted in compute shaders");
                } else if ((modifiers.flags() & Modifiers.kOut_Flag) != 0) {
                    context.error(pos, "pipeline outputs not permitted in compute shaders");
                }
            }
        }
        if (storage == kParameter_Storage) {
            // The `in` modifier on function parameters is implicit, so we can replace `in float x` with
            // `float x`. This prevents any ambiguity when matching a function by its param types.
            if ((modifiers.flags() & (Modifiers.kOut_Flag | Modifiers.kIn_Flag)) == Modifiers.kIn_Flag) {
                modifiers.clearFlag(Modifiers.kOut_Flag | Modifiers.kIn_Flag);
            }
        }
        return make(pos, modifiers, type, name, storage, context.isBuiltin());
    }

    @Nonnull
    public static Variable make(int pos,
                                @Nonnull Modifiers modifiers,
                                @Nonnull Type type,
                                @Nonnull String name,
                                byte storage,
                                boolean builtin) {
        return new Variable(pos, modifiers, type, name, storage, builtin);
    }

    @Nonnull
    @Override
    public SymbolKind getKind() {
        return SymbolKind.VARIABLE;
    }

    @Nonnull
    @Override
    public Type getType() {
        return mType;
    }

    public Modifiers getModifiers() {
        return mModifiers;
    }

    public boolean isBuiltin() {
        return mBuiltin;
    }

    public byte getStorage() {
        return mStorage;
    }

    @Nullable
    public Expression initialValue() {
        VariableDecl decl = getVariableDecl();
        return decl != null ? decl.getInit() : null;
    }

    @Nullable
    public VariableDecl getVariableDecl() {
        if (mDecl instanceof VariableDecl) {
            return (VariableDecl) mDecl;
        }
        if (mDecl instanceof GlobalVariableDecl) {
            return ((GlobalVariableDecl) mDecl).getVariableDecl();
        }
        return null;
    }

    @Nullable
    public GlobalVariableDecl getGlobalVariableDecl() {
        if (mDecl instanceof GlobalVariableDecl) {
            return (GlobalVariableDecl) mDecl;
        }
        return null;
    }

    public void setVariableDecl(VariableDecl decl) {
        if (mDecl != null && decl.getVariable() != this) {
            throw new AssertionError();
        }
        if (mDecl == null) {
            mDecl = decl;
        }
    }

    public void setGlobalVariableDecl(GlobalVariableDecl globalDecl) {
        if (mDecl != null && globalDecl.getVariableDecl().getVariable() != this) {
            throw new AssertionError();
        }
        mDecl = globalDecl;
    }

    @Nullable
    public InterfaceBlock getInterfaceBlock() {
        return mInterfaceBlock != null ? mInterfaceBlock.get() : null;
    }

    public void setInterfaceBlock(InterfaceBlock interfaceBlock) {
        mInterfaceBlock = new WeakReference<>(interfaceBlock);
    }

    @Nonnull
    @Override
    public String toString() {
        return mModifiers.toString() + mType.getName() + " " + getName();
    }
}
