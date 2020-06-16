package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.item.GunItem;
import com.mrcrayfish.guns.object.Gun;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.IWorld;
import net.minecraft.world.dimension.DimensionType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>A common implementation of {@link GunTracer}. Used to share code between the server and client.</p>
 *
 * @author Ocelot
 */
public class GunTracerImpl implements GunTracer
{
    private final Map<DimensionType, Set<GunProjectile>> projectiles;
    private IWorld world;

    protected GunTracerImpl()
    {
        this.projectiles = new HashMap<>();
    }

    protected void tick()
    {
    }

    @Override
    public void fire(LivingEntity shooter, GunItem item, Gun modifiedGun)
    {
        Set<GunProjectile> projectiles = this.projectiles.computeIfAbsent(this.world.getDimension().getType(), key -> new HashSet<>());
    }

    void setWorld(IWorld world)
    {
        this.world = world;
    }
}
