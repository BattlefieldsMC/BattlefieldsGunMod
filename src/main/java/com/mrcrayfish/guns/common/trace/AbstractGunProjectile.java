package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.Config;
import com.mrcrayfish.guns.entity.ProjectileEntity;
import com.mrcrayfish.guns.object.EntityResult;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * <p>An abstract implementation of {@link GunProjectile}.</p>
 *
 * @author Ocelot
 */
public abstract class AbstractGunProjectile implements GunProjectile
{
    public static final Predicate<Entity> PROJECTILE_TARGETS = input -> input != null && !input.isSpectator() && input.canBeCollidedWith();
    public static final Predicate<BlockState> IGNORE_LEAVES = input -> input != null && Config.COMMON.gameplay.ignoreLeaves.get() && input.getBlock() instanceof LeavesBlock;

    private int ticksExisted;
    private boolean complete;
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
    private float damageModifier = 1;
    private float additionalDamage;

    @Override
    public void tick(World world)
    {
        this.setLastPosition(this.getX(), this.getY(), this.getZ());
        this.setTicksExisted(this.getTicksExisted() + 1);
    }

    @Nullable
    protected EntityResult findEntityOnPath(World world, float bulletSize, Vec3d startVec, Vec3d endVec)
    {
        Entity shooter = world.getEntityByID(this.getShooterId());
        if (shooter == null)
            return null;

        Vec3d hitVec = null;
        Entity hitEntity = null;
        List<Entity> entities = world.getEntitiesInAABBexcluding(null, new AxisAlignedBB(this.getX() - bulletSize / 2, this.getY() - bulletSize / 2, this.getZ() - bulletSize / 2, this.getX() + bulletSize / 2, this.getY() + bulletSize / 2, this.getZ() + bulletSize / 2).expand(this.getMotionX(), this.getMotionY(), this.getMotionZ()).grow(1.0), PROJECTILE_TARGETS);
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : entities)
        {
            if (entity.getEntityId() != this.getShooterId())
            {
                AxisAlignedBB boundingBox = entity.getBoundingBox();
                Optional<Vec3d> hitPos = boundingBox.rayTrace(startVec, endVec);
                Optional<Vec3d> grownHitPos = boundingBox.grow(Config.COMMON.gameplay.growBoundingBoxAmount.get(), 0, Config.COMMON.gameplay.growBoundingBoxAmount.get()).rayTrace(startVec, endVec);
                if (!hitPos.isPresent() && grownHitPos.isPresent())
                {
                    RayTraceResult raytraceresult = ProjectileEntity.rayTraceBlocks(world, new RayTraceContext(startVec, grownHitPos.get(), RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, shooter), IGNORE_LEAVES);
                    if (raytraceresult.getType() == RayTraceResult.Type.BLOCK)
                    {
                        continue;
                    }
                    hitPos = grownHitPos;
                }

                if (!hitPos.isPresent())
                {
                    continue;
                }

                double distanceToHit = startVec.distanceTo(hitPos.get());
                if (distanceToHit < closestDistance)
                {
                    hitVec = hitPos.get();
                    hitEntity = entity;
                    closestDistance = distanceToHit;
                }
            }
        }
        return hitEntity != null ? new EntityResult(hitEntity, hitVec) : null;
    }

    @Override
    public void complete()
    {
        this.complete = true;
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
    public float getDamageModifier()
    {
        return damageModifier;
    }

    @Override
    public float getAdditionalDamage()
    {
        return additionalDamage;
    }

    @Override
    public void setTicksExisted(int ticksExisted)
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
    public void setDamageModifier(float damageModifier)
    {
        this.damageModifier = damageModifier;
    }

    @Override
    public void setAdditionalDamage(float additionalDamage)
    {
        this.additionalDamage = additionalDamage;
    }
}
