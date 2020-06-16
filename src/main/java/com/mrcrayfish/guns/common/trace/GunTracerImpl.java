package com.mrcrayfish.guns.common.trace;

import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;

import java.util.*;

/**
 * <p>A common implementation of {@link GunTracer}. Used to share code between the server and client.</p>
 *
 * @author Ocelot
 */
public class GunTracerImpl implements GunTracer
{
    protected final Map<DimensionType, Set<GunProjectile>> projectiles;
    private IWorld world;

    protected GunTracerImpl()
    {
        this.projectiles = new HashMap<>();
    }

    protected void tick(World world)
    {
        this.projectiles.values().forEach(set ->
        {
            set.forEach(projectile -> projectile.tick(world));
            set.removeIf(GunProjectile::isComplete);
        });
        this.projectiles.values().removeIf(Collection::isEmpty);
    }

    @Override
    public void add(GunProjectile projectile)
    {
        this.projectiles.computeIfAbsent(this.world.getDimension().getType(), key -> new HashSet<>()).add(projectile);
    }

    void setWorld(IWorld world)
    {
        this.world = world;
    }
}
