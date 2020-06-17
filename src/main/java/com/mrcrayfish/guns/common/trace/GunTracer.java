package com.mrcrayfish.guns.common.trace;

import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;

/**
 * <p>Manages the tracing of bullets from their starting position to their ending position. This is to reduce overhead and used as a replacement to entities.</p>
 *
 * @author Ocelot
 */
public interface GunTracer
{
    /**
     * Queues the specified bullet to be traced.
     *
     * @param world      The world to add the projectile to
     * @param projectile The projectile that was newly fired
     */
    void add(IWorldReader world, GunProjectile projectile);

    /**
     * Fetches the correct gun tracer for the specified world.
     *
     * @param world The world to get the gun tracer from
     * @return The gun tracer for that specified world
     */
    static GunTracer get(IWorldReader world)
    {
        return world.isRemote() ? ClientGunTracer.INSTANCE : ServerGunTracer.INSTANCE;
    }
}
