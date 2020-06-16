package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.Reference;
import com.mrcrayfish.guns.item.GunItem;
import com.mrcrayfish.guns.object.Gun;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.minecraftforge.api.distmarker.Dist.CLIENT;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(value = CLIENT, modid = Reference.MOD_ID)
public final class ServerGunTracer implements GunTracer
{
    static ServerGunTracer INSTANCE = new ServerGunTracer();

    private ServerWorld world;
    private Map<DimensionType, Set<GunProjectile>> projectiles;

    private ServerGunTracer()
    {
        this.projectiles = new HashMap<>();
    }

    private void tick()
    {
    }

    @Override
    public void fire(LivingEntity shooter, GunItem item, Gun modifiedGun)
    {
        Set<GunProjectile> projectiles = this.projectiles.computeIfAbsent(this.world.getDimension().getType(), key -> new HashSet<>());
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.ServerTickEvent event)
    {
        INSTANCE.tick();
    }

    void setWorld(ServerWorld world)
    {
        this.world = world;
    }
}
