package com.mrcrayfish.guns.entity;

import com.mrcrayfish.guns.item.GunItem;
import com.mrcrayfish.guns.object.Gun;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Author: MrCrayfish
 */
public class GrenadeEntity extends ProjectileEntity
{
    public GrenadeEntity(EntityType<? extends ProjectileEntity> entityType, World world)
    {
        super(entityType, world);
    }

    public GrenadeEntity(EntityType<? extends ProjectileEntity> entityType, World world, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun)
    {
        super(entityType, world, shooter, weapon, item, modifiedGun);
    }

    @Override
    public void onHitEntity(World world, float damage, Entity entity, Vec3d hitVec, Vec3d startVec, Vec3d endVec, boolean headShot)
    {
        createExplosion(this, damage / 5f);
    }

    @Override
    public void onHitBlock(World world, float damage, BlockState state, BlockPos pos, Vec3d hitVec, Vec3d startVec, Vec3d endVec)
    {
        createExplosion(this, damage / 5f);
    }

    @Override
    public void onExpired(World world)
    {
        createExplosion(this, this.getDamage() / 5f);
    }
}
