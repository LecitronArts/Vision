package dev.tr7zw.entityculling;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public class EntityCullingMod extends EntityCullingModBase {
    public static EntityCullingMod INSTANCE = new EntityCullingMod();

    @Override
    public AABB setupAABB(BlockEntity entity, BlockPos pos) {
        return new AABB(pos);
    }
}
