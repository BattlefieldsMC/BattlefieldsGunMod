package com.mrcrayfish.guns.item;

import com.mrcrayfish.guns.Reference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;

/**
 * Author: MrCrayfish
 */
public class ItemPart extends Item implements ISubItems
{
    private static final String[] PARTS = { "chain_gun_base", "chain_gun_barrels", "flash" };

    public ItemPart()
    {
        this.setRegistryName(new ResourceLocation(Reference.MOD_ID, "part"));
        this.setUnlocalizedName(Reference.MOD_ID + ".part");
        this.setHasSubtypes(true);
        this.setMaxDamage(0);
    }

    @Override
    public String getUnlocalizedName(ItemStack stack)
    {
        return super.getUnlocalizedName(stack) + "_" + PARTS[stack.getItemDamage() < 0 || stack.getItemDamage() >= PARTS.length ? 0 : stack.getItemDamage()]; // Battlefields - Fixed crash when given invalid metadata
    }

    @Override
    public NonNullList<ResourceLocation> getModels()
    {
        NonNullList<ResourceLocation> modelLocations = NonNullList.create();
        for(String part : ItemPart.PARTS)
        {
            modelLocations.add(new ResourceLocation(Reference.MOD_ID, "part_" + part));
        }
        return modelLocations;
    }
}
