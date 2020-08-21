package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.common.SpreadTracker;
import com.mrcrayfish.guns.item.GunItem;
import com.mrcrayfish.guns.object.Gun;
import com.mrcrayfish.guns.util.GunModifierHelper;
import com.sun.javafx.geom.Vec3d;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Random;

/**
 * @author Ocelot
 */
public class GunProjectileImpl extends AbstractGunProjectile {
    private final ItemStack weapon;
    private final Gun modifiedGun;
    private final double modifiedGravity;

    public GunProjectileImpl(LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun) {
        this.weapon = weapon;
        this.modifiedGun = modifiedGun;
        this.modifiedGravity = GunModifierHelper.getModifiedProjectileGravity(weapon, 0.05);
        this.setShooter(shooter);

        Vector3d dir = getDirection(shooter.getRNG(), shooter, item, modifiedGun);
        this.setMotion(dir.getX() * modifiedGun.getProjectile().getSpeed(), dir.getY() * modifiedGun.getProjectile().getSpeed(), dir.getZ() * modifiedGun.getProjectile().getSpeed());
        this.setPosition(shooter.getPosX(), shooter.getPosY() + shooter.getEyeHeight(), shooter.getPosZ());

        Item ammo = ForgeRegistries.ITEMS.getValue(modifiedGun.getProjectile().getItem());
        if (ammo != null)
            this.setBullet(new ItemStack(ammo));
    }

    private GunProjectileImpl(int shooterId, ItemStack weapon, ItemStack bullet, Gun modifiedGun, double modifiedGravity, double x, double y, double z, double motionX, double motionY, double motionZ) {
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
    public void tick(World world) {
        super.tick(world);
        this.tickStep(world, this.modifiedGun.getProjectile().getSize(), this.modifiedGun.getProjectile().getLife(), this.modifiedGravity, this.modifiedGun.getProjectile().isSpawnBulletHole(), true);
    }

    @Override
    public float getDamage() {
        float initialDamage = (this.modifiedGun.getProjectile().getDamage() + this.getAdditionalDamage());
        if (this.modifiedGun.getProjectile().isDamageReduceOverLife()) {
            float modifier = ((float) this.modifiedGun.getProjectile().getLife() - (float) (this.getTicksExisted() - 1)) / (float) this.modifiedGun.getProjectile().getLife();
            initialDamage *= modifier;
        }
        float damage = initialDamage / this.modifiedGun.getGeneral().getProjectileAmount();
        return GunModifierHelper.getModifiedDamage(this.weapon, this.modifiedGun, damage);
    }

    public void encode(PacketBuffer buf) {
        buf.writeVarInt(this.getShooterId());
        buf.writeItemStack(this.weapon, false);
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

    public static GunProjectileImpl decode(PacketBuffer buf) {
        return new GunProjectileImpl(buf.readVarInt(), buf.readItemStack(), buf.readItemStack(), Gun.create(buf.readCompoundTag()), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    private static Vector3d getVectorFromRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vector3d(f1 * f2, f3, f * f2);
    }

    private static Vector3d getDirection(Random random, LivingEntity shooter, GunItem item, Gun modifiedGun) {
        float gunSpread = modifiedGun.getGeneral().getSpread();

        if (gunSpread == 0F) {
            return getVectorFromRotation(shooter.rotationPitch, shooter.rotationYaw);
        }

        if (!modifiedGun.getGeneral().isAlwaysSpread()) {
            gunSpread *= SpreadTracker.get(shooter.getUniqueID()).getSpread(item);
        }

        return getVectorFromRotation(shooter.rotationPitch - (gunSpread / 2.0F) + random.nextFloat() * gunSpread, shooter.getRotationYawHead() - (gunSpread / 2.0F) + random.nextFloat() * gunSpread);
    }
}
