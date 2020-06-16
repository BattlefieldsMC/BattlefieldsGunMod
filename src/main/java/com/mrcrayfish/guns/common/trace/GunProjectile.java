package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.object.Gun;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public class GunProjectile
{
    private int shooterId;
    private LivingEntity shooter;
    private Gun.General general;
    private Gun.Projectile projectile;
    private ItemStack weapon = ItemStack.EMPTY;
    private ItemStack item = ItemStack.EMPTY;
    private float damageModifier = 1.0F;
    private float additionalDamage = 0.0F;
}
