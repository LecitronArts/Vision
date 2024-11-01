/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
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

import javax.annotation.Nonnull;

/**
 * An object with identity. This can be used to track content changes through
 * reference equality '==', and as keys of {@link java.util.IdentityHashMap}.
 * Compared to using int or long, this will never conflict on runtime.
 *
 * @see System#identityHashCode(Object)
 */
public final class GenerationID {

    public GenerationID() {
    }

    @Nonnull
    @Override
    public String toString() {
        return "GenerationID@" + Integer.toHexString(hashCode());
    }
}
