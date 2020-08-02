package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

import static net.minecraftforge.api.distmarker.Dist.CLIENT;

@SuppressWarnings("unused")
@Mod.EventBusSubscriber(value = CLIENT, modid = Reference.MOD_ID)
public final class ClientGunTracer implements GunTracer {
    static ClientGunTracer INSTANCE = new ClientGunTracer();

    private final Map<DimensionType, Set<GunProjectile>> projectiles;

    private ClientGunTracer() {
        this.projectiles = new HashMap<>();
    }

    private void tick(World world) {
        if (this.projectiles.containsKey(world.getDimension().getType())) {
            Set<GunProjectile> projectiles = this.projectiles.get(world.getDimension().getType());
            projectiles.forEach(projectile -> projectile.tick(world));
            projectiles.removeIf(GunProjectile::isComplete);
        }
        this.projectiles.values().removeIf(Collection::isEmpty);
    }

    private void clear() {
        this.projectiles.values().forEach(set ->
        {
            set.forEach(projectile -> projectile.complete(null));
            set.clear();
        });
        this.projectiles.values().clear();
    }

    @Override
    public void add(IWorldReader world, GunProjectile projectile) {
        this.projectiles.computeIfAbsent(world.getDimension().getType(), key -> new HashSet<>()).add(projectile);
    }

    @SubscribeEvent
    public static void onEvent(TickEvent.ClientTickEvent event) {
        if (Minecraft.getInstance().world == null)
            return;
        if (event.phase == TickEvent.Phase.END)
            INSTANCE.tick(Minecraft.getInstance().world);
    }

    @SubscribeEvent
    public static void onEvent(WorldEvent.Unload event) {
        Minecraft.getInstance().execute(INSTANCE::clear);
    }
}
