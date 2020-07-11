package com.mrcrayfish.guns.entity;

import com.mrcrayfish.guns.Config;
import com.mrcrayfish.guns.item.GunItem;
import com.mrcrayfish.guns.object.Gun;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

/**
 * Author: MrCrayfish
 */
public class MissileEntity extends ProjectileEntity
{
    public MissileEntity(EntityType<? extends ProjectileEntity> entityType, World worldIn)
    {
        super(entityType, worldIn);
    }

    public MissileEntity(EntityType<? extends ProjectileEntity> entityType, World worldIn, LivingEntity shooter, GunItem item, Gun modifiedGun)
    {
        super(entityType, worldIn, shooter, item, modifiedGun);
    }

    @Override
    public void tick(World world)
    {
        super.tick(world);
        if (this.world.isRemote())
        {
            for (int i = 5; i > 0; i--)
            {
                this.world.addParticle(ParticleTypes.CLOUD, true, this.getPosX() - (this.getMotion().getX() / i), this.getPosY() - (this.getMotion().getY() / i), this.getPosZ() - (this.getMotion().getZ() / i), 0, 0, 0);
            }
            if (this.world.rand.nextInt(2) == 0)
            {
                this.world.addParticle(ParticleTypes.SMOKE, true, this.getPosX(), this.getPosY(), this.getPosZ(), 0, 0, 0);
                this.world.addParticle(ParticleTypes.FLAME, true, this.getPosX(), this.getPosY(), this.getPosZ(), 0, 0, 0);
            }
        }
    }

    @Override
    public void onHitEntity(World world, float damage, Entity entity, double x, double y, double z)
    {
        createExplosion(this);
    }

    @Override
    public void onHitBlock(World world, float damage, BlockState state, BlockPos pos, double x, double y, double z)
    {
        createExplosion(this);
    }

    @Override
    public void onExpired(World world)
    {
        createExplosion(this);
    }

    private static void createExplosion(MissileEntity entity)
    {
        World world = entity.world;
        if (world.isRemote())
            return;

        Explosion.Mode mode = Config.COMMON.gameplay.enableGunGriefing.get() ? Explosion.Mode.DESTROY : Explosion.Mode.NONE;
        world.createExplosion(entity, entity.getPosX(), entity.getPosY(), entity.getPosZ(), Config.COMMON.missiles.explosionRadius.get().floatValue(), false, mode);
    }
}
