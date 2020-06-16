package com.mrcrayfish.guns.init;

import com.mrcrayfish.guns.GunMod;
import com.mrcrayfish.guns.Reference;
import com.mrcrayfish.guns.item.*;
import com.mrcrayfish.guns.object.Barrel;
import com.mrcrayfish.guns.object.Scope;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems
{
    public static final DeferredRegister<Item> REGISTER = new DeferredRegister<>(ForgeRegistries.ITEMS, Reference.MOD_ID);

    public static final RegistryObject<GunItem> PISTOL = REGISTER.register("pistol", () -> new GunItem(new Item.Properties().maxStackSize(1).group(GunMod.GROUP)));
    public static final RegistryObject<GunItem> SHOTGUN = REGISTER.register("shotgun", () -> new GunItem(new Item.Properties().maxStackSize(1).group(GunMod.GROUP)));
    public static final RegistryObject<GunItem> RIFLE = REGISTER.register("rifle", () -> new GunItem(new Item.Properties().maxStackSize(1).group(GunMod.GROUP)));
    public static final RegistryObject<GunItem> GRENADE_LAUNCHER = REGISTER.register("grenade_launcher", () -> new GunItem(new Item.Properties().maxStackSize(1).group(GunMod.GROUP)));
    public static final RegistryObject<GunItem> BAZOOKA = REGISTER.register("bazooka", () -> new GunItem(new Item.Properties().maxStackSize(1).group(GunMod.GROUP)));
    public static final RegistryObject<GunItem> MINI_GUN = REGISTER.register("mini_gun", () -> new GunItem(new Item.Properties().maxStackSize(1).group(GunMod.GROUP)));
    public static final RegistryObject<GunItem> ASSAULT_RIFLE = REGISTER.register("assault_rifle", () -> new GunItem(new Item.Properties().maxStackSize(1).group(GunMod.GROUP)));
    public static final RegistryObject<GunItem> MACHINE_PISTOL = REGISTER.register("machine_pistol", () -> new GunItem(new Item.Properties().maxStackSize(1).group(GunMod.GROUP)));
    public static final RegistryObject<GunItem> HEAVY_RIFLE = REGISTER.register("heavy_rifle", () -> new GunItem(new Item.Properties().maxStackSize(1).group(GunMod.GROUP)));

    public static final RegistryObject<Item> BASIC_BULLET = REGISTER.register("basic_bullet", () -> new Item(new Item.Properties().group(GunMod.GROUP)));
    public static final RegistryObject<Item> ADVANCED_AMMO = REGISTER.register("advanced_bullet", () -> new Item(new Item.Properties().group(GunMod.GROUP)));
    public static final RegistryObject<Item> SHELL = REGISTER.register("shell", () -> new Item(new Item.Properties().group(GunMod.GROUP)));
    public static final RegistryObject<Item> MISSILE = REGISTER.register("missile", () -> new Item(new Item.Properties().group(GunMod.GROUP)));
    public static final RegistryObject<GrenadeItem> GRENADE = REGISTER.register("grenade", () -> new GrenadeItem(new Item.Properties().group(GunMod.GROUP), 20 * 4));
    public static final RegistryObject<StunGrenadeItem> STUN_GRENADE = REGISTER.register("stun_grenade", () -> new StunGrenadeItem(new Item.Properties().group(GunMod.GROUP), 72000));

    public static final RegistryObject<ScopeItem> SHORT_SCOPE = REGISTER.register("short_scope", () -> new ScopeItem(Scope.create(0.1F, 1.55F).viewFinderOffset(0.3), new Item.Properties().maxStackSize(1).group(GunMod.GROUP)));
    public static final RegistryObject<ScopeItem> MEDIUM_SCOPE = REGISTER.register("medium_scope", () -> new ScopeItem(Scope.create(0.25F, 1.625F).viewFinderOffset(0.3), new Item.Properties().maxStackSize(1).group(GunMod.GROUP)));
    public static final RegistryObject<ScopeItem> LONG_SCOPE = REGISTER.register("long_scope", () -> new ScopeItem(Scope.create(0.4F, 1.4F).viewFinderOffset(0.275), new Item.Properties().maxStackSize(1).group(GunMod.GROUP)));
    public static final RegistryObject<BarrelItem> SILENCER = REGISTER.register("silencer", () -> new BarrelItem(Barrel.create(8.0F), new Item.Properties().group(GunMod.GROUP)));
}
