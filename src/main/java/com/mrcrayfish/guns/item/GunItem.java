package com.mrcrayfish.guns.item;

import com.google.common.annotations.Beta;
import com.mrcrayfish.guns.Reference;
import com.mrcrayfish.guns.object.Gun;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

@Beta
public class GunItem extends ColoredItem
{
    private Gun gun = new Gun();

    public GunItem(Item.Properties properties)
    {
        super(properties);
    }

    public void setGun(Gun gun)
    {
        this.gun = gun;
    }

    public Gun getGun()
    {
        return this.gun;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flag)
    {
        Gun modifiedGun = this.getModifiedGun(stack);

        Item ammo = ForgeRegistries.ITEMS.getValue(modifiedGun.projectile.item);
        if (ammo != null)
            tooltip.add(new TranslationTextComponent("info.cgm.ammo_type", ammo.getName()).setStyle(new Style().setColor(TextFormatting.GRAY)));

        String additionalDamageText = "";
        CompoundNBT tagCompound = stack.getTag();
        if (tagCompound != null)
        {
            if (tagCompound.contains("AdditionalDamage", Constants.NBT.TAG_FLOAT))
            {
                float additionalDamage = tagCompound.getFloat("AdditionalDamage");
                if (additionalDamage > 0)
                {
                    additionalDamageText = TextFormatting.GREEN + " +" + ItemStack.DECIMALFORMAT.format(tagCompound.getFloat("AdditionalDamage"));
                }
                else if (additionalDamage < 0)
                {
                    additionalDamageText = TextFormatting.RED + " " + ItemStack.DECIMALFORMAT.format(tagCompound.getFloat("AdditionalDamage"));
                }
            }
        }

        tooltip.add(new TranslationTextComponent(I18n.format("info.cgm.damage", ItemStack.DECIMALFORMAT.format(modifiedGun.projectile.damage) + additionalDamageText)).setStyle(new Style().setColor(TextFormatting.GRAY)));

        if (tagCompound != null)
        {
            if (tagCompound.getBoolean("IgnoreAmmo"))
            {
                tooltip.add(new TranslationTextComponent("info.cgm.ignore_ammo").setStyle(new Style().setColor(TextFormatting.GRAY)));
            }
            else
            {
                tooltip.add(new TranslationTextComponent("info.cgm.ammo", tagCompound.getInt("AmmoCount"), modifiedGun.general.maxAmmo).setStyle(new Style().setColor(TextFormatting.GRAY)));
            }
        }

        tooltip.add(new TranslationTextComponent("info.cgm.attachment_help", new KeybindTextComponent("key." + Reference.MOD_ID + ".attachments")).setStyle(new Style().setColor(TextFormatting.GRAY)));
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity)
    {
        return true;
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> stacks)
    {
        if (this.isInGroup(group))
        {
            ItemStack stack = new ItemStack(this);
            stack.getOrCreateTag().putInt("AmmoCount", this.gun.general.maxAmmo);
            stacks.add(stack);
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack)
    {
        CompoundNBT tagCompound = stack.getTag();
        Gun modifiedGun = this.getModifiedGun(stack);
        return tagCompound == null || (!tagCompound.getBoolean("IgnoreAmmo") && tagCompound.getInt("AmmoCount") != modifiedGun.general.maxAmmo);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack)
    {
        CompoundNBT tagCompound = stack.getTag();
        Gun modifiedGun = this.getModifiedGun(stack);
        return tagCompound == null ? 0.0 : 1.0 - (tagCompound.getInt("AmmoCount") / (double) modifiedGun.general.maxAmmo);
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack)
    {
        return 0xff00ffff;
    }

    public Gun getModifiedGun(ItemStack stack)
    {
        CompoundNBT tagCompound = stack.getTag();
        if (tagCompound != null && tagCompound.contains("Gun", Constants.NBT.TAG_COMPOUND))
        {
            if (tagCompound.getBoolean("Custom"))
            {
                return Gun.create(tagCompound.getCompound("Gun"));
            }
            else
            {
                Gun gunCopy = this.gun.copy();
                gunCopy.deserializeNBT(tagCompound.getCompound("Gun"));
                return gunCopy;
            }
        }
        return this.gun;
    }
}
