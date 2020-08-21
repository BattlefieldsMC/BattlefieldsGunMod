package com.mrcrayfish.guns.object;

import com.mrcrayfish.guns.common.trace.GunProjectile;
import com.mrcrayfish.guns.network.message.MessageBullet;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.vector.Vector3d;

/**
 * Author: MrCrayfish
 */
public class Bullet {
    private final GunProjectile projectile;
    private double posX;
    private double posY;
    private double posZ;
    private double lastX;
    private double lastY;
    private double lastZ;
    private int ticksExisted;
    private boolean complete;
    private final double gravity;
    private final int maxLife;
    private float rotationYaw;
    private float rotationPitch;
    private final int trailColor;
    private final double trailLengthMultiplier;

    public Bullet(MessageBullet message) {
        this.projectile = message.getProjectile();
        this.posX = this.projectile.getX();
        this.posY = this.projectile.getY();
        this.posZ = this.projectile.getZ();
        this.lastX = this.projectile.getX();
        this.lastY = this.projectile.getY();
        this.lastZ = this.projectile.getZ();
        this.gravity = message.getGravity();
        this.maxLife = message.getLife();
        this.trailColor = message.getTrailColor();
        this.trailLengthMultiplier = message.getTrailLengthMultiplier();
        this.updateHeading();
    }

    private void updateHeading() {
        Vector3d motion;

        if (this.projectile.isComplete() && this.projectile.getCompletePos() == null)
            this.complete = true;
        if (this.projectile.isComplete() && this.projectile.getCompletePos() != null) {
            Vector3d projectileMotion = new Vector3d(this.projectile.getMotionX(), this.projectile.getMotionY() - this.gravity, this.projectile.getMotionZ());
            Vector3d difference = this.projectile.getCompletePos().subtract(this.posX, this.posY, this.posZ);
            if (difference.lengthSquared() < projectileMotion.lengthSquared()) {
                motion = difference;
                this.complete = true;
            } else {
                motion = projectileMotion;
            }
        } else {
            motion = new Vector3d(this.projectile.getMotionX(), this.projectile.getMotionY(), this.projectile.getMotionZ());
        }

        float d = MathHelper.sqrt(motion.getX() * motion.getX() + motion.getZ() * motion.getZ());
        this.rotationYaw = (float) (MathHelper.atan2(motion.getX(), motion.getZ()) * (180D / Math.PI));
        this.rotationPitch = (float) (MathHelper.atan2(motion.getY(), d) * (180D / Math.PI));

        this.posX += motion.getX();
        this.posY += motion.getY();
        this.posZ += motion.getZ();
    }

    public void tick() {
        this.lastX = this.posX;
        this.lastY = this.posY;
        this.lastZ = this.posZ;
        this.ticksExisted++;
        if (!this.complete)
            this.updateHeading();
        if (this.ticksExisted >= this.maxLife)
            this.complete = true;
    }

    public GunProjectile getProjectile() {
        return projectile;
    }

    public double getPosX(float partialTicks) {
        return MathHelper.lerp(partialTicks, this.lastX, this.posX);
    }

    public double getPosY(float partialTicks) {
        return MathHelper.lerp(partialTicks, this.lastY, this.posY);
    }

    public double getPosZ(float partialTicks) {
        return MathHelper.lerp(partialTicks, this.lastZ, this.posZ);
    }

    public int getTicksExisted() {
        return ticksExisted;
    }

    public boolean isComplete() {
        return this.complete && this.lastX == this.posX && this.lastY == this.posY && this.lastZ == this.posZ;
    }

    public float getRotationYaw() {
        return this.rotationYaw;
    }

    public float getRotationPitch() {
        return this.rotationPitch;
    }

    public int getTrailColor() {
        return this.trailColor;
    }

    public double getTrailLengthMultiplier() {
        return this.trailLengthMultiplier;
    }
}
