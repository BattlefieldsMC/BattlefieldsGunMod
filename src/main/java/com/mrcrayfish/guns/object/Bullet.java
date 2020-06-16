package com.mrcrayfish.guns.object;

import com.mrcrayfish.guns.common.trace.GunProjectile;
import com.mrcrayfish.guns.network.message.MessageBullet;
import net.minecraft.util.math.MathHelper;

/**
 * Author: MrCrayfish
 */
public class Bullet
{
    private final GunProjectile projectile;
    private float rotationYaw;
    private float rotationPitch;
    private final int trailColor;
    private final double trailLengthMultiplier;

    public Bullet(MessageBullet message)
    {
        this.projectile = message.getProjectile();
        this.trailColor = message.getTrailColor();
        this.trailLengthMultiplier = message.getTrailLengthMultiplier();
        this.updateHeading();
    }

    private void updateHeading()
    {
        float d = MathHelper.sqrt(this.projectile.getMotionX() * this.projectile.getMotionX() + this.projectile.getMotionZ() * this.projectile.getMotionZ());
        this.rotationYaw = (float) (MathHelper.atan2(this.projectile.getMotionX(), this.projectile.getMotionZ()) * (180D / Math.PI));
        this.rotationPitch = (float) (MathHelper.atan2(this.projectile.getMotionY(), d) * (180D / Math.PI));
    }

    public void tick()
    {
        if (!this.projectile.isComplete())
            this.updateHeading();
    }

    public GunProjectile getProjectile()
    {
        return projectile;
    }

    public float getRotationYaw()
    {
        return this.rotationYaw;
    }

    public float getRotationPitch()
    {
        return this.rotationPitch;
    }

    public int getTrailColor()
    {
        return this.trailColor;
    }

    public double getTrailLengthMultiplier()
    {
        return this.trailLengthMultiplier;
    }
}
