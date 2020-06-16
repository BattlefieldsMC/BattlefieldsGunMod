package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.item.GunItem;
import com.mrcrayfish.guns.object.Gun;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;

/**
 * <p>Manages the tracing of bullets from their starting position to their ending position. This is to reduce overhead and used as a replacement to entities.</p>
 *
 * @author Ocelot
 */
public interface GunTracer
{
    /**
     * Fires a bullet from the specified living entity
     *
     * @param shooter     The entity shooting the bullet
     * @param item        The ammo item (used for rendering tracers)
     * @param modifiedGun The modified gun used to fire
     */
    void fire(LivingEntity shooter, GunItem item, Gun modifiedGun);

    /**
     * Fetches the correct gun tracer for the specified world.
     *
     * @param world The world to get the gun tracer from
     * @return The gun tracer for that specified world
     */
    static GunTracer get(IWorld world)
    {
        if (world.isRemote())
        {
            ClientGunTracer.INSTANCE.setWorld(world);
            return ClientGunTracer.INSTANCE;
        }
        if (!(world instanceof ServerWorld))
            throw new IllegalArgumentException("Server side world is not ServerWorld?");
        ServerGunTracer.INSTANCE.setWorld((ServerWorld) world);
        return ServerGunTracer.INSTANCE;
    }
}
