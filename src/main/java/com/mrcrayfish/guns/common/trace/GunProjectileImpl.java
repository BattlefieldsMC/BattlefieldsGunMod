package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.common.SpreadTracker;
import com.mrcrayfish.guns.entity.ProjectileEntity;
import com.mrcrayfish.guns.item.GunItem;
import com.mrcrayfish.guns.object.EntityResult;
import com.mrcrayfish.guns.object.Gun;
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

public class GunProjectileImpl extends AbstractGunProjectile
{
    private final Gun modifiedGun;

    public GunProjectileImpl(LivingEntity shooter, GunItem item, Gun modifiedGun)
    {
        this.modifiedGun = modifiedGun;
        this.setShooter(shooter.getUniqueID());

        Vec3d dir = getDirection(shooter.getRNG(), shooter, item, modifiedGun);
        this.setMotion(dir.getX() * modifiedGun.projectile.speed, dir.getY() * modifiedGun.projectile.speed, dir.getZ() * modifiedGun.projectile.speed);

        /* Spawn the projectile half way between the previous and current position */
        this.setPosition(shooter.lastTickPosX + (shooter.getPosX() - shooter.lastTickPosX) / 2.0, shooter.lastTickPosY + (shooter.getPosY() - shooter.lastTickPosY) / 2.0 + shooter.getEyeHeight(), shooter.lastTickPosZ + (shooter.getPosZ() - shooter.lastTickPosZ) / 2.0);

        Item ammo = ForgeRegistries.ITEMS.getValue(modifiedGun.projectile.item);
        if (ammo != null)
            this.setBullet(new ItemStack(ammo));
    }

    private GunProjectileImpl(int shooterId, ItemStack bullet, Gun modifiedGun, double x, double y, double z, double motionX, double motionY, double motionZ)
    {
        this.setShooterId(shooterId);
        this.setBullet(bullet);
        this.modifiedGun = modifiedGun;
        this.setLastPosition(x, y, z);
        this.setPosition(x, y, z);
        this.setMotion(motionX, motionY, motionZ);
    }

    @Override
    public void tick(World world)
    {
        super.tick(world);

        Entity entity = world.getEntityByID(this.getShooterId());

        if (entity == null)
        {
            this.complete();
            return;
        }

        Vec3d startVec = new Vec3d(this.getX(), this.getY(), this.getZ());
        Vec3d endVec = startVec.add(this.getMotionX(), this.getMotionY(), this.getMotionZ());
        RayTraceResult result = ProjectileEntity.rayTraceBlocks(world, new RayTraceContext(startVec, endVec, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity), IGNORE_LEAVES);
        if (result.getType() != RayTraceResult.Type.MISS)
        {
            endVec = result.getHitVec();
        }

        EntityResult entityResult = this.findEntityOnPath(world, this.modifiedGun.projectile.size, startVec, endVec);
        if (entityResult != null)
        {
            result = new EntityRayTraceResult(entityResult.entity, entityResult.hitVec);
        }

        if (result instanceof EntityRayTraceResult && ((EntityRayTraceResult) result).getEntity() instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) ((EntityRayTraceResult) result).getEntity();

            if (world.getEntityByID(this.getShooterId()) instanceof PlayerEntity && !((PlayerEntity) world.getEntityByID(this.getShooterId())).canAttackPlayer(player))
            {
                result = null;
            }
        }

        if (result != null)
        {
            this.onHit(world, this.getDamage(), result);
        }

        this.setPosition(this.getX() + this.getMotionX(), this.getY() + this.getMotionY(), this.getZ() + this.getMotionZ());

        if (this.modifiedGun.projectile.gravity)
            this.setMotionY(this.getMotionY() - 0.05);

        if (this.getTicksExisted() >= this.modifiedGun.projectile.life)
        {
            if (!this.isComplete())
            {
                this.onExpired(world);
            }
            this.complete();
        }
    }

    public float getDamage()
    {
        float damage = (this.modifiedGun.projectile.damage + this.getAdditionalDamage()) * this.getDamageModifier();
        if (this.modifiedGun.projectile.damageReduceOverLife)
        {
            float modifier = ((float) this.modifiedGun.projectile.life - (float) (this.getTicksExisted() - 1)) / (float) this.modifiedGun.projectile.life;
            damage *= modifier;
        }
        return damage / this.modifiedGun.general.projectileAmount;
    }

    public void encode(PacketBuffer buf)
    {
        buf.writeVarInt(this.getShooterId());
        buf.writeItemStack(this.getBullet());
        buf.writeCompoundTag(this.modifiedGun.serializeNBT());
        buf.writeDouble(this.getX());
        buf.writeDouble(this.getY());
        buf.writeDouble(this.getZ());
        buf.writeDouble(this.getMotionX());
        buf.writeDouble(this.getMotionY());
        buf.writeDouble(this.getMotionZ());
    }

    public static GunProjectileImpl decode(PacketBuffer buf)
    {
        return new GunProjectileImpl(buf.readVarInt(), buf.readItemStack(), Gun.create(buf.readCompoundTag()), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
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
