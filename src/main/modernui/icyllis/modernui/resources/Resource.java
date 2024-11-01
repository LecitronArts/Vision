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

package icyllis.modernui.resources;

public class Resource {

    public static final int
            TYPE_ANIM = 0,
            TYPE_ANIMATOR = 1,
            TYPE_ARRAY = 2,
            TYPE_ATTR = 3,
            TYPE_ATTR_PRIVATE = 4,
            TYPE_BOOL = 5,
            TYPE_COLOR = 6,
            TYPE_CONFIG_VARYING = 7,
            TYPE_DIMEN = 8,
            TYPE_DRAWABLE = 9,
            TYPE_FONT = 10,
            TYPE_FRACTION = 11,
            TYPE_ID = 12,
            TYPE_INTEGER = 13,
            TYPE_INTERPOLATOR = 14,
            TYPE_LAYOUT = 15,
            TYPE_MACRO = 16,
            TYPE_MENU = 17,
            TYPE_MIPMAP = 18,
            TYPE_NAVIGATION = 19,
            TYPE_PLURALS = 20,
            TYPE_RAW = 21,
            TYPE_STRING = 22,
            TYPE_STYLE = 23,
            TYPE_STYLEABLE = 24,
            TYPE_TRANSITION = 25,
            TYPE_XML = 26;

    public static String getTypeName(int type) {
        return switch (type) {
            case TYPE_ANIM -> "anim";
            case TYPE_ANIMATOR -> "animator";
            case TYPE_ARRAY -> "array";
            case TYPE_ATTR -> "attr";
            case TYPE_ATTR_PRIVATE -> "^attr-private";
            case TYPE_BOOL -> "bool";
            case TYPE_COLOR -> "color";
            case TYPE_DIMEN -> "dimen";
            case TYPE_DRAWABLE -> "drawable";
            case TYPE_FONT -> "font";
            case TYPE_FRACTION -> "fraction";
            case TYPE_ID -> "id";
            case TYPE_INTEGER -> "integer";
            case TYPE_INTERPOLATOR -> "interpolator";
            case TYPE_LAYOUT -> "layout";
            case TYPE_RAW -> "raw";
            case TYPE_STRING -> "string";
            case TYPE_STYLE -> "style";
            case TYPE_STYLEABLE -> "styleable";
            case TYPE_TRANSITION -> "transition";
            case TYPE_XML -> "xml";
            default -> "";
        };
    }

    // A resource's name. This can uniquely identify
    // a resource in the ResourceTable.
    // "namespace:type/entry"
    public static class ResourceName {

        public String namespace;

        // Pair of type name as in ResourceTable and actual resource type.
        // Corresponds to the 'type' in "namespace:type/entry".
        // This is to support resource types with custom names inside resource tables.
        public String typename;
        public int type;

        public String entry;

        public ResourceName() {
            namespace = "";
            typename = "";
            type = TYPE_RAW;
            entry = "";
        }

        public ResourceName(String namespace, int type, String entry) {
            this.namespace = namespace;
            this.typename = getTypeName(type);
            this.type = type;
            this.entry = entry;
        }

        public void setType(int type) {
            this.typename = getTypeName(type);
            this.type = type;
        }

        @Override
        public String toString() {
            return (namespace.isEmpty() ? "" : namespace + ":") + typename + "/" + entry;
        }
    }
}
