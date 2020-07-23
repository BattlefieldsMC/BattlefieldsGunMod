package com.mrcrayfish.guns.init;

import com.mrcrayfish.guns.Reference;
import com.mrcrayfish.guns.enchantment.CollateralEnchantment;
import com.mrcrayfish.guns.enchantment.LightweightEnchantment;
import com.mrcrayfish.guns.enchantment.QuickHandsEnchantment;
import com.mrcrayfish.guns.enchantment.TriggerFingerEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Author: MrCrayfish
 */
public class ModEnchantments
{
    public static final DeferredRegister<Enchantment> REGISTER = new DeferredRegister<>(ForgeRegistries.ENCHANTMENTS, Reference.MOD_ID);

    public static final RegistryObject<Enchantment> QUICK_HANDS = REGISTER.register("quick_hands", QuickHandsEnchantment::new);
    public static final RegistryObject<Enchantment> TRIGGER_FINGER = REGISTER.register("trigger_finger", TriggerFingerEnchantment::new);
    public static final RegistryObject<Enchantment> LIGHTWEIGHT = REGISTER.register("lightweight", LightweightEnchantment::new);
    public static final RegistryObject<Enchantment> COLLATERAL = REGISTER.register("collateral", CollateralEnchantment::new);
    //Collateral
    //Explosive
    //Critical (3 levels) - gives a chance for a projectile to do extra damage
    //Projectile Accelerator (2 levels) - increases the speed of a projectile
    //Fire Starter (1 level) - sets the target block or entity on fire
    //Gravity Impulse (3 levels) - nearby entities will get knocked away from the target location
    //Inifinty eqiv
    //More ammo capacity

}