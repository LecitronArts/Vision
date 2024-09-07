package com.logisticscraft.occlusionculling.util;

import net.minecraft.util.Mth;

/**
 * Contains MathHelper methods
 */
public final class MathUtilities {

    private MathUtilities() {
    }

    public static int floor(double d) {
        return Mth.floor(d);
    }

    public static int fastFloor(double d) {
        return (int) (d + 1024.0) - 1024;
    }

    public static int ceil(double d) {
        return Mth.ceil(d);
    }

}
