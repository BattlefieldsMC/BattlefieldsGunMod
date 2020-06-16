package com.mrcrayfish.guns.entity;

import com.mrcrayfish.guns.Reference;
import com.mrcrayfish.guns.common.trace.GunProjectile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IndirectEntityDamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Author: MrCrayfish
 */
public class DamageSourceProjectile extends IndirectEntityDamageSource
{
    private static final String[] DEATH_TYPES = {"killed", "eliminated", "executed", "annihilated", "decimated"};
    private static final Random RAND = new Random();

    private final GunProjectile source;
    private final ItemStack weapon;

    public DamageSourceProjectile(String damageTypeIn, GunProjectile source, @Nullable Entity indirectEntityIn, ItemStack weapon)
    {
        super(damageTypeIn, source instanceof Entity ? (Entity) source : null, indirectEntityIn);
        this.source = source;
        this.weapon = weapon;
    }

    public ItemStack getWeapon()
    {
        return weapon;
    }

    @Override
    public ITextComponent getDeathMessage(LivingEntity entityLivingBaseIn)
    {
        ITextComponent textComponent = this.getTrueSource() == null ? this.damageSourceEntity.getDisplayName() : this.getTrueSource().getDisplayName();
        String deathKey = String.format("death.attack.%s.%s.%s", Reference.MOD_ID, this.damageType, DEATH_TYPES[RAND.nextInt(DEATH_TYPES.length)]);
        return new TranslationTextComponent(deathKey, entityLivingBaseIn.getDisplayName(), textComponent);
    }

    @Nullable
    @Override
    public Vec3d getDamageLocation()
    {
        return this.source instanceof Entity ? super.getDamageLocation() : new Vec3d(this.source.getX(), this.source.getY(), this.source.getZ());
    }
}
