package com.mrcrayfish.guns;

import com.mrcrayfish.guns.client.ClientHandler;
import com.mrcrayfish.guns.client.CustomGunManager;
import com.mrcrayfish.guns.client.settings.GunOptions;
import com.mrcrayfish.guns.common.*;
import com.mrcrayfish.guns.common.trace.GunProjectile;
import com.mrcrayfish.guns.common.BoundingBoxManager;
import com.mrcrayfish.guns.common.CustomGunLoader;
import com.mrcrayfish.guns.common.NetworkGunManager;
import com.mrcrayfish.guns.common.ProjectileManager;
import com.mrcrayfish.guns.enchantment.EnchantmentTypes;
import com.mrcrayfish.guns.entity.GrenadeEntity;
import com.mrcrayfish.guns.entity.MissileEntity;
import com.mrcrayfish.guns.init.*;
import com.mrcrayfish.guns.item.GunItem;
import com.mrcrayfish.guns.network.PacketHandler;
import com.mrcrayfish.guns.object.Gun;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

@SuppressWarnings("unused")
@Mod(Reference.MOD_ID)
public class GunMod
{
    public static boolean controllableLoaded = false;
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_ID);
    public static final ItemGroup GROUP = new ItemGroup(Reference.MOD_ID)
    {
        @Override
        public ItemStack createIcon()
        {
            ItemStack stack = new ItemStack(ModItems.PISTOL.get());
            stack.getOrCreateTag().putInt("AmmoCount", ModItems.PISTOL.get().getModifiedGun(stack).getGeneral().getMaxAmmo());
            return stack;
        }

        @Override
        public void fill(NonNullList<ItemStack> items)
        {
            super.fill(items);
            CustomGunManager.fill(items);
        }
    }.setRelevantEnchantmentTypes(EnchantmentTypes.GUN, EnchantmentTypes.SEMI_AUTO_GUN);

    @OnlyIn(Dist.CLIENT)
    private static GunOptions options;
    private static NetworkGunManager networkGunManager;
    private static CustomGunLoader customGunLoader;

    public GunMod()
    {
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.clientSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.commonSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.serverSpec);
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.register(this);
        ModBlocks.REGISTER.register(bus);
        ModContainers.REGISTER.register(bus);
        ModEffects.REGISTER.register(bus);
        ModEnchantments.REGISTER.register(bus);
        ModEntities.REGISTER.register(bus);
        ModItems.REGISTER.register(bus);
        ModParticleTypes.REGISTER.register(bus);
        ModPotions.REGISTER.register(bus);
        ModRecipeSerializers.REGISTER.register(bus);
        ModSounds.REGISTER.register(bus);
        ModTileEntities.REGISTER.register(bus);
        ModSyncedDataKeys.register();
        bus.addListener(this::onCommonSetup);
        bus.addListener(this::onClientSetup);
        controllableLoaded = ModList.get().isLoaded("controllable");
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
        ProjectileManager.getInstance().registerFactory(ModItems.GRENADE.get(), new ProjectileFactory<GunProjectile>()
        {
            @Override
            public GunProjectile create(World world, LivingEntity entity, ItemStack weapon, GunItem item, Gun modifiedGun)
            {
                return new GrenadeEntity(ModEntities.GRENADE.get(), world, entity, weapon, item, modifiedGun);
            }

            @Override
            public void encode(PacketBuffer buf, GunProjectile projectile)
            {
            }

            @Override
            public GunProjectile decode(PacketBuffer buf)
            {
                return null;
            }
        });
        ProjectileManager.getInstance().registerFactory(ModItems.MISSILE.get(), new ProjectileFactory<GunProjectile>()
        {
            @Override
            public GunProjectile create(World world, LivingEntity entity, ItemStack weapon, GunItem item, Gun modifiedGun)
            {
                return new MissileEntity(ModEntities.MISSILE.get(), world, entity, weapon, item, modifiedGun);
            }

            @Override
            public void encode(PacketBuffer buf, GunProjectile projectile)
            {

            }

            @Override
            public GunProjectile decode(PacketBuffer buf)
            {
                return null;
            }
        });

        PacketHandler.init();

        if (Config.COMMON.gameplay.improvedHitboxes.get())
        {
            MinecraftForge.EVENT_BUS.register(new BoundingBoxManager());
        }
    }

    private void onClientSetup(FMLClientSetupEvent event)
    {
        Minecraft mc = event.getMinecraftSupplier().get();
        GunMod.options = new GunOptions(mc.gameDir);
        ClientHandler.setup();
    }

    @SubscribeEvent
    public void onServerStart(FMLServerAboutToStartEvent event)
    {
        NetworkGunManager networkGunManager = new NetworkGunManager();
        event.getServer().getResourceManager().addReloadListener(networkGunManager);
        GunMod.networkGunManager = networkGunManager;

        CustomGunLoader customGunLoader = new CustomGunLoader();
        event.getServer().getResourceManager().addReloadListener(customGunLoader);
        GunMod.customGunLoader = customGunLoader;
    }

    @SubscribeEvent
    public void onServerStart(FMLServerStoppedEvent event)
    {
        GunMod.networkGunManager = null;
    }

    /**
     * Gets the network gun manager. This will be null if the client isn't running an integrated
     * server or the client is connected to a dedicated server.
     *
     * @return the network gun manager
     */
    @Nullable
    public static NetworkGunManager getNetworkGunManager()
    {
        return networkGunManager;
    }

    /**
     * @return
     */
    @Nullable
    public static CustomGunLoader getCustomGunLoader()
    {
        return customGunLoader;
    }

    /**
     * @return
     */
    @OnlyIn(Dist.CLIENT)
    public static GunOptions getOptions()
    {
        return options;
    }
}
