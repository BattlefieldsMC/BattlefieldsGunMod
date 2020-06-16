package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.Reference;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class ServerGunTracer extends GunTracerImpl
{
    static ServerGunTracer INSTANCE = new ServerGunTracer();

    @Override
    protected void tick(World world)
    {
        if (!(world instanceof ServerWorld))
            return;
        
        this.projectiles.values().forEach(set ->
        {
            set.forEach(projectile -> ((ServerWorld) world).getServer().execute(() -> projectile.tick(world)));
            set.removeIf(GunProjectile::isComplete);
        });
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.WorldTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
            INSTANCE.tick(event.world);
    }
}
