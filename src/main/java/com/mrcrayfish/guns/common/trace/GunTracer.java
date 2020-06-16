package com.mrcrayfish.guns.common.trace;

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
     * Queues the specified bullet to be traced.
     *
     * @param projectile The projectile that was newly fired
     */
    void add(GunProjectile projectile);

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
        ServerGunTracer.INSTANCE.setWorld(world);
        return ServerGunTracer.INSTANCE;
    }
}
