package com.mrcrayfish.guns.interfaces;

import com.mrcrayfish.guns.common.trace.GunProjectile;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Author: MrCrayfish
 */
public interface IDamageable
{
    void onBlockDamaged(World world, BlockState state, BlockPos pos, GunProjectile projectile, float rawDamage, int damage);
}
