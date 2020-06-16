package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.Reference;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.minecraftforge.api.distmarker.Dist.CLIENT;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(value = CLIENT, modid = Reference.MOD_ID)
public final class ClientGunTracer extends GunTracerImpl
{
    static ClientGunTracer INSTANCE = new ClientGunTracer();

    @Override
    protected void tick()
    {
        System.out.println("Client");
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.ClientTickEvent event)
    {
        if (Minecraft.getInstance().world == null)
            return;
        if (event.phase == TickEvent.Phase.END)
            INSTANCE.tick();
    }
}
