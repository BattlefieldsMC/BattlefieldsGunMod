package com.mrcrayfish.guns.item;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class ItemAmmo extends Item
{
    public ItemAmmo(ResourceLocation id)
    {
        this.setTranslationKey(id.getNamespace() + "." + id.getPath());
        this.setRegistryName(id);
        AmmoRegistry.getInstance().register(this);
    }
}
