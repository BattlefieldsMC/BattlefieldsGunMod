package com.mrcrayfish.guns.hook;

import com.mrcrayfish.guns.common.trace.GunProjectile;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.eventbus.api.Event;

/**
 * <p>Fired when a projectile hits a block or entity.</p>
 *
 * @author Ocelot
 */
public class GunProjectileHitEvent extends Event
{
    private final RayTraceResult result;
    private final GunProjectile projectile;

    public GunProjectileHitEvent(RayTraceResult result, GunProjectile projectile)
    {
        this.result = result;
        this.projectile = projectile;
    }

    /**
     * @return The result of the entity's ray trace
     */
    public RayTraceResult getRayTrace()
    {
        return result;
    }

    /**
     * @return The projectile that hit
     */
    public GunProjectile getProjectile()
    {
        return projectile;
    }
}
