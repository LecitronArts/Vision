package net.mehvahdjukaar.modelfix;

import net.minecraft.resources.ResourceLocation;

public class ModelFixGeom {

    private static final ResourceLocation BLOCK_ATLAS = new ResourceLocation("textures/atlas/blocks.png");

    //who needs anti atlas bleeding when it doesn't occur even on mipmap 4 high render distance lol
    //apparently on mac os it does waaa
    public static float getShrinkRatio(ResourceLocation atlasLocation, float defaultValue, float returnValue) {
        if (atlasLocation.equals(BLOCK_ATLAS) && defaultValue == returnValue) {
            return 0;
        }
        return -1;
    }

}
