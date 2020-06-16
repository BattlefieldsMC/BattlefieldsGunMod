package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.Config;
import com.mrcrayfish.guns.entity.DamageSourceProjectile;
import com.mrcrayfish.guns.interfaces.IDamageable;
import com.mrcrayfish.guns.network.PacketHandler;
import com.mrcrayfish.guns.network.message.MessageBlood;
import com.mrcrayfish.guns.network.message.MessageBulletHole;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BreakableBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPlaySoundPacket;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

/**
 * <p>A base for all gun projectiles.</p>
 *
 * @author Ocelot
 */
public interface GunProjectile
{
    /**
     * Updates the last position of this projectile.
     *
     * @param world The world the projectile is in
     */
    void tick(World world);

    /**
     * Called when this projectile is removed.
     */
    default void onExpired(World world)
    {
    }

    /**
     * Called when a block is hit with this bullet.
     *
     * @param world  The world this bullet hit in
     * @param damage The damage this bullet deals
     * @param result The block trace result
     */
    default void onHit(World world, float damage, RayTraceResult result)
    {
        if (result instanceof BlockRayTraceResult)
        {
            BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) result;
            if (blockRayTraceResult.getType() == RayTraceResult.Type.MISS)
            {
                return;
            }

            BlockPos pos = blockRayTraceResult.getPos();
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            if (!world.isRemote() && Config.COMMON.gameplay.enableGunGriefing.get() && (block instanceof BreakableBlock || block instanceof PaneBlock) && state.getMaterial() == Material.GLASS)
                world.destroyBlock(blockRayTraceResult.getPos(), false);

            if (!state.getMaterial().isReplaceable())
                this.complete();

            if (block instanceof IDamageable)
                ((IDamageable) block).onBlockDamaged(world, state, pos, (int) Math.ceil(damage / 2.0) + 1);

            Vec3d hitVec = blockRayTraceResult.getHitVec();
            if (!world.isRemote())
            {
                double holeX = hitVec.getX() + 0.005 * blockRayTraceResult.getFace().getXOffset();
                double holeY = hitVec.getY() + 0.005 * blockRayTraceResult.getFace().getYOffset();
                double holeZ = hitVec.getZ() + 0.005 * blockRayTraceResult.getFace().getZOffset();
                Direction direction = blockRayTraceResult.getFace();
                PacketHandler.getPlayChannel().send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkAt(blockRayTraceResult.getPos())), new MessageBulletHole(holeX, holeY, holeZ, direction, pos));
            }

            this.onHitBlock(world, damage, state, pos, result.getHitVec().x, result.getHitVec().y, result.getHitVec().z);

            return;
        }

        if (result instanceof EntityRayTraceResult)
        {
            EntityRayTraceResult entityRayTraceResult = (EntityRayTraceResult) result;
            Entity entity = entityRayTraceResult.getEntity();
            if (entity.getEntityId() == this.getShooterId())
                return;
            this.onHitEntity(world, damage, entity, result.getHitVec().x, result.getHitVec().y, result.getHitVec().z);
            this.complete();
            if (!world.isRemote())
                entity.hurtResistantTime = 0;
        }
    }

    /**
     * Called when an entity is hit with this bullet.
     *
     * @param world  The world this bullet hit in
     * @param damage The damage this bullet deals
     * @param entity The entity hit
     * @param x      The x position hit on the entity
     * @param y      The y position hit on the entity
     * @param z      The z position hit on the entity
     */
    default void onHitEntity(World world, float damage, Entity entity, double x, double y, double z)
    {
        if (world.isRemote())
            return;

        boolean headShot = false;
        if (Config.COMMON.gameplay.enableHeadShots.get() && entity instanceof PlayerEntity)
        {
            AxisAlignedBB boundingBox = entity.getBoundingBox().expand(0, !entity.isCrouching() ? 0.0625 : 0, 0);
            if (boundingBox.maxY - y <= 8.0 * 0.0625 && boundingBox.grow(0.001).contains(new Vec3d(x, y, z)))
            {
                headShot = true;
                damage *= Config.COMMON.gameplay.headShotDamageMultiplier.get();
            }
        }

        Entity shooter = world.getEntityByID(this.getShooterId());

        DamageSource source = new DamageSourceProjectile("bullet", this, shooter, this.getWeapon()).setProjectile();
        entity.attackEntityFrom(source, damage);

        if (entity instanceof PlayerEntity && shooter instanceof ServerPlayerEntity)
        {
            SoundEvent event = headShot ? SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP : SoundEvents.ENTITY_PLAYER_HURT;
            ServerPlayerEntity shooterPlayer = (ServerPlayerEntity) shooter;
            shooterPlayer.connection.sendPacket(new SPlaySoundPacket(Objects.requireNonNull(event.getRegistryName()), SoundCategory.PLAYERS, new Vec3d(shooter.getPosX(), shooter.getPosY(), shooter.getPosZ()), 0.75F, 1.0F));
        }

        /* Send blood particle to tracking clients. */
        //TODO maybe make clients send settings to server to prevent sending unnecessary packets
        PacketHandler.getPlayChannel().send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new MessageBlood(x, y, z));
    }

    default void onHitBlock(World world, float damage, BlockState state, BlockPos pos, double x, double y, double z)
    {
        if (world.isRemote())
            return;

        ((ServerWorld) world).spawnParticle(new BlockParticleData(ParticleTypes.BLOCK, state), x, y, z, (int) damage, 0.0, 0.0, 0.0, 0.05);
        world.playSound(null, x, y, z, state.getSoundType().getBreakSound(), SoundCategory.BLOCKS, 0.75F, 2.0F);
    }

    /**
     * Marks this projectile as completed.
     */
    void complete();

    /**
     * @return The amount of ticks this projectile has existed for
     */
    int getTicksExisted();

    /**
     * @return Whether or not this projectile has completed it's tracing
     */
    boolean isComplete();

    /**
     * @return The last x position this projectile was at
     */
    double getLastX();

    /**
     * @return The last y position this projectile was at
     */
    double getLastY();

    /**
     * @return The last z position this projectile was at
     */
    double getLastZ();

    /**
     * @return The x position this projectile is currently at
     */
    double getX();

    /**
     * @return The y position this projectile is currently at
     */
    double getY();

    /**
     * @return The z position this projectile is currently at
     */
    double getZ();

    /**
     * @return The motion of this projectile in the x direction
     */
    double getMotionX();

    /**
     * @return The motion of this projectile in the y direction
     */
    double getMotionY();

    /**
     * @return The motion of this projectile in the z direction
     */
    double getMotionZ();

    /**
     * @return The entity that shot this bullet
     */
    int getShooterId();

    /**
     * @return The entity that shot this bullet
     */
    @Nullable
    UUID getShooter();

    /**
     * @return The weapon that fired this projectile
     */
    ItemStack getWeapon();

    /**
     * @return The item stack this bullet should display as
     */
    ItemStack getBullet();

    /**
     * @return The damage factor for when dealing damage
     */
    float getDamageModifier();

    /**
     * @return The amount of damage added on top of the core damage
     */
    float getAdditionalDamage();

    /**
     * Sets the amount of ticks this projectile existed for.
     *
     * @param ticksExisted The new amount of ticks existed
     */
    void setTicksExisted(int ticksExisted);

    /**
     * Sets the last x position of this projectile
     *
     * @param lastX The new last x position
     */
    void setLastX(double lastX);

    /**
     * Sets the last y position of this projectile
     *
     * @param lastY The new last y position
     */
    void setLastY(double lastY);

    /**
     * Sets the last z position of this projectile
     *
     * @param lastZ The new last z position
     */
    void setLastZ(double lastZ);

    /**
     * Sets the last position of this projectile
     *
     * @param lastX The new last x position
     * @param lastY The new last y position
     * @param lastZ The new last z position
     */
    default void setLastPosition(double lastX, double lastY, double lastZ)
    {
        this.setLastX(lastX);
        this.setLastY(lastY);
        this.setLastZ(lastZ);
    }

    /**
     * Sets the x position of this projectile.
     *
     * @param x The new x position of this projectile
     */
    void setX(double x);

    /**
     * Sets the y position of this projectile.
     *
     * @param y The new y position of this projectile
     */
    void setY(double y);

    /**
     * Sets the z position of this projectile.
     *
     * @param z The new z position of this projectile
     */
    void setZ(double z);

    /**
     * Sets the position of this projectile.
     *
     * @param x The new x position of this projectile
     * @param y The new y position of this projectile
     * @param z The new z position of this projectile
     */
    default void setPosition(double x, double y, double z)
    {
        this.setX(x);
        this.setY(y);
        this.setZ(z);
    }

    /**
     * Sets the motion of this projectile in the x direction.
     *
     * @param motionX The new motion in the x direction
     */
    void setMotionX(double motionX);

    /**
     * Sets the motion of this projectile in the y direction.
     *
     * @param motionY The new motion in the y direction
     */
    void setMotionY(double motionY);

    /**
     * Sets the motion of this projectile in the z direction.
     *
     * @param motionZ The new motion in the z direction
     */
    void setMotionZ(double motionZ);

    /**
     * Sets the motion of this projectile.
     *
     * @param motionX The new motion in the x direction
     * @param motionY The new motion in the y direction
     * @param motionZ The new motion in the z direction
     */
    default void setMotion(double motionX, double motionY, double motionZ)
    {
        this.setMotionX(motionX);
        this.setMotionY(motionY);
        this.setMotionZ(motionZ);
    }

    /**
     * Sets the entity that shot this projectile.
     *
     * @param shooterId The new entity that shot this projectile
     */
    void setShooterId(int shooterId);

    /**
     * Sets the entity that shot this projectile.
     *
     * @param shooterId The new entity that shot this projectile
     */
    void setShooter(UUID shooterId);

    /**
     * Sets the weapon used to shoot this projectile.
     *
     * @param weapon The new weapon used to shoot this projectile
     */
    void setWeapon(ItemStack weapon);

    /**
     * Sets the bullet fired as this projectile.
     *
     * @param bullet The new wbullet fired as this projectile
     */
    void setBullet(ItemStack bullet);

    /**
     * Sets the damage factor.
     *
     * @param damageModifier The new damage factor
     */
    void setDamageModifier(float damageModifier);

    /**
     * Sets the amount of additional damage this projectile should deal.
     *
     * @param additionalDamage The new amount of damage added on top of the core damage
     */
    void setAdditionalDamage(float additionalDamage);
}
