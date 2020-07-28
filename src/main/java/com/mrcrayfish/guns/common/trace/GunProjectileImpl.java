package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.common.SpreadTracker;
import com.mrcrayfish.guns.entity.ProjectileEntity;
import com.mrcrayfish.guns.item.GunItem;
import com.mrcrayfish.guns.object.EntityResult;
import com.mrcrayfish.guns.object.Gun;
import com.mrcrayfish.guns.util.GunModifierHelper;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;

/**
 * @author Ocelot
 */
public class GunProjectileImpl extends AbstractGunProjectile
{
    private final ItemStack weapon;
    private final Gun modifiedGun;
    private final double modifiedGravity;

    public GunProjectileImpl(LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun)
    {
        this.weapon = weapon;
        this.modifiedGun = modifiedGun;
        this.modifiedGravity = GunModifierHelper.getModifiedProjectileGravity(weapon, 0.05);
        this.setShooter(shooter.getUniqueID());

        Vec3d dir = getDirection(shooter.getRNG(), shooter, item, modifiedGun);
        this.setMotion(dir.getX() * modifiedGun.projectile.speed, dir.getY() * modifiedGun.projectile.speed, dir.getZ() * modifiedGun.projectile.speed);

        /* Spawn the projectile half way between the previous and current position */
        this.setPosition(shooter.lastTickPosX + (shooter.getPosX() - shooter.lastTickPosX) / 2.0, shooter.lastTickPosY + (shooter.getPosY() - shooter.lastTickPosY) / 2.0 + shooter.getEyeHeight(), shooter.lastTickPosZ + (shooter.getPosZ() - shooter.lastTickPosZ) / 2.0);

        Item ammo = ForgeRegistries.ITEMS.getValue(modifiedGun.projectile.item);
        if (ammo != null)
            this.setBullet(new ItemStack(ammo));
    }

    private GunProjectileImpl(int shooterId, ItemStack weapon, ItemStack bullet, Gun modifiedGun, double modifiedGravity, double x, double y, double z, double motionX, double motionY, double motionZ)
    {
        this.setShooterId(shooterId);
        this.weapon = weapon;
        this.setBullet(bullet);
        this.modifiedGun = modifiedGun;
        this.modifiedGravity = modifiedGravity;
        this.setLastPosition(x, y, z);
        this.setPosition(x, y, z);
        this.setMotion(motionX, motionY, motionZ);
    }

    @Override
    public void tick(World world)
    {
        super.tick(world);
        this.tickStep(world, this.modifiedGun.projectile.size, this.modifiedGun.projectile.life, this.modifiedGravity, this.modifiedGun.projectile.spawnBulletHole, true);
    }

    @Override
    public float getDamage()
    {
        float initialDamage = (this.modifiedGun.projectile.damage + this.getAdditionalDamage());
        if (this.modifiedGun.projectile.damageReduceOverLife)
        {
            float modifier = ((float) this.modifiedGun.projectile.life - (float) (this.getTicksExisted() - 1)) / (float) this.modifiedGun.projectile.life;
            initialDamage *= modifier;
        }
        float damage = initialDamage / this.modifiedGun.general.projectileAmount;
        return GunModifierHelper.getModifiedDamage(this.weapon, this.modifiedGun, damage);
    }

    public void encode(PacketBuffer buf)
    {
        buf.writeVarInt(this.getShooterId());
        buf.writeItemStack(this.weapon);
        buf.writeItemStack(this.getBullet());
        buf.writeCompoundTag(this.modifiedGun.serializeNBT());
        buf.writeDouble(this.modifiedGravity);
        buf.writeDouble(this.getX());
        buf.writeDouble(this.getY());
        buf.writeDouble(this.getZ());
        buf.writeDouble(this.getMotionX());
        buf.writeDouble(this.getMotionY());
        buf.writeDouble(this.getMotionZ());
    }

    public static GunProjectileImpl decode(PacketBuffer buf)
    {
        return new GunProjectileImpl(buf.readVarInt(), buf.readItemStack(), buf.readItemStack(), Gun.create(buf.readCompoundTag()), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static Vec3d getVectorFromRotation(float pitch, float yaw)
    {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d(f1 * f2, f3, f * f2);
    }

    private static Vec3d getDirection(Random random, LivingEntity shooter, GunItem item, Gun modifiedGun)
    {
        float gunSpread = modifiedGun.general.spread;

        if (gunSpread == 0F)
        {
            return getVectorFromRotation(shooter.rotationPitch, shooter.rotationYaw);
        }

        if (!modifiedGun.general.alwaysSpread)
        {
            gunSpread *= SpreadTracker.get(shooter.getUniqueID()).getSpread(item);
        }

        return getVectorFromRotation(shooter.rotationPitch - (gunSpread / 2.0F) + random.nextFloat() * gunSpread, shooter.getRotationYawHead() - (gunSpread / 2.0F) + random.nextFloat() * gunSpread);
    }
}
