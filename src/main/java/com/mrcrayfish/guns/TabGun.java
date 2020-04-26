package com.mrcrayfish.guns;

import com.mrcrayfish.guns.init.ModGuns;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

public class TabGun extends CreativeTabs
{
	public TabGun() 
	{
		super("tabCGM");
	}

    @Override
    public ItemStack getTabIconItem()
    {
        //		return new ItemStack(ModGuns.PISTOL);
		/* Battlefields - Updated creative tab gun */
        ItemStack stack = new ItemStack(ModGuns.PISTOL);
        ItemStackUtil.createTagCompound(stack).setInteger("AmmoCount", ModGuns.PISTOL.getModifiedGun(stack).general.maxAmmo);
        return stack;
		/* Battlefields End */
    }
}
