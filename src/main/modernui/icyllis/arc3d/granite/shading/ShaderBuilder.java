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

import org.intellij.lang.annotations.PrintFormat;

/**
 * Interface for all shaders builders.
 */
public interface ShaderBuilder {

    /**
     * Writes the specified string to one of the shaders.
     */
    void codeAppend(String str);

    /**
     * Writes a formatted string to one of the shaders using the specified format
     * string and arguments.
     *
     * @see java.util.Formatter#format(String, Object...)
     */
    void codeAppendf(@PrintFormat String format, Object... args);

    /**
     * Similar to {@link #codeAppendf(String, Object...)}, but writes at the beginning.
     */
    void codePrependf(@PrintFormat String format, Object... args);

    /**
     * Generates a mangled name for a helper function in the fragment shader. Will give consistent
     * results if called more than once.
     */
    String getMangledName(String baseName);
}
