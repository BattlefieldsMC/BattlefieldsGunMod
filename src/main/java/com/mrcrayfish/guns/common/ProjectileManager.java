package com.mrcrayfish.guns.common;

import com.google.common.annotations.Beta;
import com.mrcrayfish.guns.common.trace.GunProjectile;
import com.mrcrayfish.guns.common.trace.GunProjectileImpl;
import com.mrcrayfish.guns.item.GunItem;
import com.mrcrayfish.guns.object.Gun;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Author: MrCrayfish
 */
@Beta
public class ProjectileManager
{
    private static ProjectileManager instance = null;

    public static ProjectileManager getInstance()
    {
        if (instance == null)
        {
            instance = new ProjectileManager();
        }
        return instance;
    }

    private final ProjectileFactory<GunProjectileImpl> DEFAULT_FACTORY = new ProjectileFactory<GunProjectileImpl>()
    {
        @Override
        public GunProjectileImpl create(World world, LivingEntity entity, GunItem item, Gun modifiedGun)
        {
            return new GunProjectileImpl(entity, item, modifiedGun);
        }

        @Override
        public void encode(PacketBuffer buf, GunProjectileImpl projectile)
        {
            projectile.encode(buf);
        }

        @Override
        public GunProjectileImpl decode(PacketBuffer buf)
        {
            return GunProjectileImpl.decode(buf);
        }
    };
    private final Map<ResourceLocation, ProjectileFactory<?>> projectileFactoryMap = new HashMap<>();

    public void registerFactory(Item ammo, ProjectileFactory<? extends GunProjectile> factory)
    {
        this.projectileFactoryMap.put(ammo.getRegistryName(), factory);
    }

    @SuppressWarnings("unchecked")
    public <T extends GunProjectile> ProjectileFactory<T> getFactory(ResourceLocation id)
    {
        return (ProjectileFactory<T>) this.projectileFactoryMap.getOrDefault(id, DEFAULT_FACTORY);
    }
}
