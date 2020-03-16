package com.mrcrayfish.guns.item;

import com.mrcrayfish.guns.ItemStackUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;

import java.util.HashSet;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
public class ItemColored extends Item {

    public static final Set<Item> COLORED_ITEMS = new HashSet<>();

    public ItemColored() {
        COLORED_ITEMS.add(this);
    }

    public boolean hasColor(ItemStack stack) {
        NBTTagCompound tagCompound = ItemStackUtil.createTagCompound(stack);
        return tagCompound.hasKey("color", Constants.NBT.TAG_INT);
    }

    public int getColor(ItemStack stack) {
        NBTTagCompound tagCompound = ItemStackUtil.createTagCompound(stack);
        return tagCompound.getInteger("color");
    }

    public void setColor(ItemStack stack, int color) {
        NBTTagCompound tagCompound = ItemStackUtil.createTagCompound(stack);
        tagCompound.setInteger("color", color);
    }

    public void removeColor(ItemStack stack) {
        NBTTagCompound tagCompound = ItemStackUtil.createTagCompound(stack);
        tagCompound.removeTag("color");
    }
}
