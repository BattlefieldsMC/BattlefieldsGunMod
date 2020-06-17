package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.Reference;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class ServerGunTracer implements GunTracer
{
    static ServerGunTracer INSTANCE = new ServerGunTracer();

    private final Map<DimensionType, Set<GunProjectile>> projectiles;

    private ServerGunTracer()
    {
        this.projectiles = new HashMap<>();
    }

    private void tick(World world)
    {
        if (!(world instanceof ServerWorld))
            return;

        this.projectiles.values().forEach(set ->
        {
            set.forEach(projectile -> ((ServerWorld) world).getServer().execute(() -> projectile.tick(world)));
            set.removeIf(GunProjectile::isComplete);
        });
        ((ServerWorld) world).getServer().execute(() -> this.projectiles.values().removeIf(Collection::isEmpty));
    }

    @Override
    public void add(IWorldReader world, GunProjectile projectile)
    {
        this.projectiles.computeIfAbsent(world.getDimension().getType(), key -> new HashSet<>()).add(projectile);
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.WorldTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
            INSTANCE.tick(event.world);
    }
}
