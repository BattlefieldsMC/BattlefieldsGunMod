package com.mrcrayfish.guns.common.trace;

import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * <p>An abstract implementation of {@link GunProjectile}.</p>
 *
 * @author Ocelot
 */
public abstract class AbstractGunProjectile implements GunProjectile
{
    private int ticksExisted;
    private boolean complete;
    private Vector3d completePos;
    private double lastX;
    private double lastY;
    private double lastZ;
    private double x;
    private double y;
    private double z;
    private double motionX;
    private double motionY;
    private double motionZ;
    private UUID shooter;
    private int shooterId;
    private ItemStack weapon = ItemStack.EMPTY;
    private ItemStack bullet = ItemStack.EMPTY;
    private float additionalDamage;

    @Override
    public void tick(World world)
    {
        this.setLastPosition(this.getX(), this.getY(), this.getZ());
        this.setTicksExisted(this.getTicksExisted() + 1);
    }

    @Override
    public void complete(Vector3d completePos)
    {
        this.complete = true;
        this.completePos = completePos;
    }

    @Override
    public int getTicksExisted()
    {
        return ticksExisted;
    }

    @Override
    public boolean isComplete()
    {
        return complete;
    }

    @Override
    public Vector3d getCompletePos()
    {
        return completePos;
    }

    @Override
    public double getLastX()
    {
        return lastX;
    }

    @Override
    public double getLastY()
    {
        return lastY;
    }

    @Override
    public double getLastZ()
    {
        return lastZ;
    }

    @Override
    public double getX()
    {
        return x;
    }

    @Override
    public double getY()
    {
        return y;
    }

    @Override
    public double getZ()
    {
        return z;
    }

    @Override
    public double getMotionX()
    {
        return motionX;
    }

    @Override
    public double getMotionY()
    {
        return motionY;
    }

    @Override
    public double getMotionZ()
    {
        return motionZ;
    }

    @Override
    public int getShooterId()
    {
        return shooterId;
    }

    @Nullable
    @Override
    public UUID getShooter()
    {
        return shooter;
    }

    @Override
    public ItemStack getWeapon()
    {
        return weapon;
    }

    @Override
    public ItemStack getBullet()
    {
        return bullet;
    }

    @Override
    public float getAdditionalDamage()
    {
        return additionalDamage;
    }

    protected void setTicksExisted(int ticksExisted)
    {
        this.ticksExisted = ticksExisted;
    }

    @Override
    public void setLastX(double lastX)
    {
        this.lastX = lastX;
    }

    @Override
    public void setLastY(double lastY)
    {
        this.lastY = lastY;
    }

    @Override
    public void setLastZ(double lastZ)
    {
        this.lastZ = lastZ;
    }

    @Override
    public void setX(double x)
    {
        this.x = x;
    }

    @Override
    public void setY(double y)
    {
        this.y = y;
    }

    @Override
    public void setZ(double z)
    {
        this.z = z;
    }

    @Override
    public void setMotionX(double motionX)
    {
        this.motionX = motionX;
    }

    @Override
    public void setMotionY(double motionY)
    {
        this.motionY = motionY;
    }

    @Override
    public void setMotionZ(double motionZ)
    {
        this.motionZ = motionZ;
    }

    @Override
    public void setShooterId(int shooterId)
    {
        this.shooterId = shooterId;
    }

    @Override
    public void setShooter(UUID shooterId)
    {
        this.shooter = shooterId;
    }

    @Override
    public void setWeapon(ItemStack weapon)
    {
        this.weapon = weapon.copy();
    }

    @Override
    public void setBullet(ItemStack bullet)
    {
        this.bullet = bullet.copy();
    }

    @Override
    public void setAdditionalDamage(float additionalDamage)
    {
        this.additionalDamage = additionalDamage;
    }
}
