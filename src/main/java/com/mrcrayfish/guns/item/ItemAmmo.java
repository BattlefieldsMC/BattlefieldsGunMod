package com.mrcrayfish.guns.item;

import com.google.common.annotations.Beta;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

/**
 * Author: MrCrayfish
 */
@Beta
public class ItemAmmo extends Item
{
    public ItemAmmo(ResourceLocation id)
    {
        this.setRegistryName(id);
        this.setUnlocalizedName(id.getResourceDomain() + "." + id.getResourcePath());
        AmmoRegistry.getInstance().register(this);
    }
}
