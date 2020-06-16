package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.Reference;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class ServerGunTracer extends GunTracerImpl
{
    static ServerGunTracer INSTANCE = new ServerGunTracer();

    @Override
    protected void tick()
    {
        System.out.println("Server");
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.WorldTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
            INSTANCE.tick();
    }
}
