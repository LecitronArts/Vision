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

import icyllis.arc3d.compiler.ShaderCompiler;
import icyllis.arc3d.compiler.Context;

import javax.annotation.Nonnull;

/**
 * Represents an ill-formed expression. This is needed so that parser can go further.
 */
public final class Poison extends Expression {

    private Poison(int position, Type type) {
        super(position, type);
    }

    @Nonnull
    public static Expression make(@Nonnull Context context, int position) {
        return new Poison(position, context.getTypes().mPoison);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.POISON;
    }

    @Override
    public boolean accept(@Nonnull TreeVisitor visitor) {
        return false;
    }

    @Nonnull
    @Override
    public Expression clone(int position) {
        return new Poison(position, getType());
    }

    @Nonnull
    @Override
    public String toString(int parentPrecedence) {
        return ShaderCompiler.POISON_TAG;
    }
}
