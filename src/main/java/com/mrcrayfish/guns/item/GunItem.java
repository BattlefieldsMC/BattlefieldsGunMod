package com.mrcrayfish.guns.item;

import com.google.common.annotations.Beta;
import com.mrcrayfish.guns.Config;
import com.mrcrayfish.guns.Reference;
import com.mrcrayfish.guns.enchantment.EnchantmentTypes;
import com.mrcrayfish.guns.object.Gun;
import com.mrcrayfish.guns.util.GunEnchantmentHelper;
import com.mrcrayfish.guns.util.GunModifierHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
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
public class GunItem extends Item implements IColored
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

        tooltip.add(new TranslationTextComponent("info.cgm.damage", ItemStack.DECIMALFORMAT.format(modifiedGun.projectile.damage) + additionalDamageText).setStyle(new Style().setColor(TextFormatting.GRAY)));
        tooltip.add(new TranslationTextComponent("info.cgm.fire_rate", GunModifierHelper.getModifiedRate(stack, GunEnchantmentHelper.getRate(stack, modifiedGun))).setStyle(new Style().setColor(TextFormatting.GRAY)));
        tooltip.add(new TranslationTextComponent("info.cgm.reload_speed", modifiedGun.general.reloadSpeed).setStyle(new Style().setColor(TextFormatting.GRAY)));
        if (modifiedGun.general.spread == 0 || modifiedGun.general.alwaysSpread)
        {
            tooltip.add(new TranslationTextComponent("info.cgm.always_spread", ItemStack.DECIMALFORMAT.format(GunModifierHelper.getModifiedSpread(stack, modifiedGun.general.spread))).setStyle(new Style().setColor(TextFormatting.GRAY)));
        }
        else
        {
            tooltip.add(new TranslationTextComponent("info.cgm.spread", ItemStack.DECIMALFORMAT.format((1.0 / Config.COMMON.projectileSpread.maxCount.get().doubleValue()) * modifiedGun.general.spread)).setStyle(new Style().setColor(TextFormatting.GRAY)));
        }

        if (tagCompound != null)
        {
            if (tagCompound.getBoolean("IgnoreAmmo"))
            {
                tooltip.add(new TranslationTextComponent("info.cgm.ignore_ammo").setStyle(new Style().setColor(TextFormatting.GRAY)));
            }
            else
            {
                tooltip.add(new TranslationTextComponent("info.cgm.ammo", tagCompound.getInt("AmmoCount"), GunEnchantmentHelper.getAmmoCapacity(stack, modifiedGun)).setStyle(new Style().setColor(TextFormatting.GRAY)));
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
        return tagCompound == null || (!tagCompound.getBoolean("IgnoreAmmo") && tagCompound.getInt("AmmoCount") != GunEnchantmentHelper.getAmmoCapacity(stack, modifiedGun));
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack)
    {
        CompoundNBT tagCompound = stack.getTag();
        Gun modifiedGun = this.getModifiedGun(stack);
        return tagCompound == null ? 0.0 : 1.0 - (tagCompound.getInt("AmmoCount") / (double) GunEnchantmentHelper.getAmmoCapacity(stack, modifiedGun));
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

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment)
    {
        if (enchantment.type == EnchantmentTypes.SEMI_AUTO_GUN)
        {
            Gun modifiedGun = this.getModifiedGun(stack);
            return !modifiedGun.general.auto;
        }
        return super.canApplyAtEnchantingTable(stack, enchantment);
    }
}
