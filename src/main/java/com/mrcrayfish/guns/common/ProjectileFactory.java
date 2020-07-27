package com.mrcrayfish.guns.common;

import com.google.common.annotations.Beta;
import com.mrcrayfish.guns.common.trace.GunProjectile;
import com.mrcrayfish.guns.item.GunItem;
import com.mrcrayfish.guns.object.Gun;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;

/**
 * Author: MrCrayfish
 */
@Beta
public interface ProjectileFactory<T extends GunProjectile>
{
    T create(World worldIn, LivingEntity entity, ItemStack weapon, GunItem item, Gun modifiedGun);

    void encode(PacketBuffer buf, T projectile);

    T decode(PacketBuffer buf);
}
