package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.Config;
import com.mrcrayfish.guns.entity.DamageSourceProjectile;
import com.mrcrayfish.guns.entity.ProjectileEntity;
import com.mrcrayfish.guns.hook.GunProjectileHitEvent;
import com.mrcrayfish.guns.interfaces.IDamageable;
import com.mrcrayfish.guns.network.PacketHandler;
import com.mrcrayfish.guns.network.message.MessageBlood;
import com.mrcrayfish.guns.network.message.MessageBulletHole;
import com.mrcrayfish.guns.object.EntityResult;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPlaySoundPacket;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * <p>A base for all gun projectiles.</p>
 *
 * @author Ocelot
 */
public interface GunProjectile
{
    Predicate<Entity> PROJECTILE_TARGETS = input -> input != null && !input.isSpectator() && input.canBeCollidedWith();
    Predicate<BlockState> IGNORE_LEAVES = input -> input != null && Config.COMMON.gameplay.ignoreLeaves.get() && input.getBlock() instanceof LeavesBlock;

    /**
     * Updates the last position of this projectile.
     *
     * @param world The world the projectile is in
     */
    void tick(World world);

    /**
     * Ticks the movement of this projectile to the next step.
     *
     * @param world           The world the projectile is in
     * @param size            The size of the projectile
     * @param life            The amount of ticks the projectile lives for
     * @param gravity         Whether or not the projectile is affected by gravity
     * @param spawnBulletHole Whether or not a bullet hole should be spawned by this projectile
     */
    default void tickStep(World world, float size, int life, boolean gravity, boolean spawnBulletHole)
    {
        Entity shooter = world.getEntityByID(this.getShooterId());

        Vec3d startVec = new Vec3d(this.getX(), this.getY(), this.getZ());
        Vec3d endVec = startVec.add(this.getMotionX(), this.getMotionY(), this.getMotionZ());
        RayTraceResult result = rayTraceBlocks(world, new RayTraceContext(startVec, endVec, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, shooter), IGNORE_LEAVES);
        if (result.getType() != RayTraceResult.Type.MISS)
        {
            endVec = result.getHitVec();
        }

        EntityResult entityResult = GunProjectile.findEntityOnPath(this, world, size, startVec, endVec);
        if (entityResult != null)
        {
            result = new EntityRayTraceResult(entityResult.entity, entityResult.hitVec);
        }

        if (result instanceof EntityRayTraceResult && ((EntityRayTraceResult) result).getEntity() instanceof PlayerEntity)
        {
            PlayerEntity player = (PlayerEntity) ((EntityRayTraceResult) result).getEntity();

            if (shooter instanceof PlayerEntity && !((PlayerEntity) shooter).canAttackPlayer(player))
            {
                result = null;
            }
        }

        if (result != null)
        {
            this.onHit(world, this.getDamage(), spawnBulletHole, result);
        }

        this.setPosition(this.getX() + this.getMotionX(), this.getY() + this.getMotionY(), this.getZ() + this.getMotionZ());

        if (gravity)
            this.setMotionY(this.getMotionY() - 0.05);

        if (this.getTicksExisted() >= life)
        {
            if (!this.isComplete())
            {
                this.onExpired(world);
            }
            this.complete();
        }
    }

    /**
     * Called when this projectile is removed.
     */
    default void onExpired(World world)
    {
    }

    /**
     * Called when a block is hit with this bullet.
     *
     * @param world           The world this bullet hit in
     * @param damage          The damage this bullet deals
     * @param spawnBulletHole Whether or not a bullet hole should be spawned by this projectile
     * @param result          The block trace result
     */
    default void onHit(World world, float damage, boolean spawnBulletHole, RayTraceResult result)
    {
        MinecraftForge.EVENT_BUS.post(new GunProjectileHitEvent(result, this));

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
                ((IDamageable) block).onBlockDamaged(world, state, pos, this, damage, (int) Math.ceil(damage / 2.0) + 1);

            Vec3d hitVec = blockRayTraceResult.getHitVec();
            if (spawnBulletHole && !world.isRemote())
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
     * @return The amount of damage this projectile deals
     */
    float getDamage();

    /**
     * @return The damage factor for when dealing damage
     */
    float getDamageModifier();

    /**
     * @return The amount of damage added on top of the core damage
     */
    float getAdditionalDamage();

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

    /**
     * Ray traces for the specified projectile at the start vec to the end vec.
     *
     * @param projectile The projectile to ray trace
     * @param world      The world the projectile is in
     * @param bulletSize The size of the bullet
     * @param startVec   The starting position of the movement
     * @param endVec     The ending position of the movement
     * @return Whether or not an entity was hit by the projectile
     */
    @Nullable
    static EntityResult findEntityOnPath(GunProjectile projectile, World world, float bulletSize, Vec3d startVec, Vec3d endVec)
    {
        Entity shooter = world.getEntityByID(projectile.getShooterId());
        if (shooter == null)
            return null;

        Vec3d hitVec = null;
        Entity hitEntity = null;
        List<Entity> entities = world.getEntitiesInAABBexcluding(null, new AxisAlignedBB(projectile.getX() - bulletSize / 2, projectile.getY() - bulletSize / 2, projectile.getZ() - bulletSize / 2, projectile.getX() + bulletSize / 2, projectile.getY() + bulletSize / 2, projectile.getZ() + bulletSize / 2).expand(projectile.getMotionX(), projectile.getMotionY(), projectile.getMotionZ()).grow(1.0), PROJECTILE_TARGETS);
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : entities)
        {
            if (entity.getEntityId() != projectile.getShooterId())
            {
                AxisAlignedBB boundingBox = entity.getBoundingBox();
                Optional<Vec3d> hitPos = boundingBox.rayTrace(startVec, endVec);
                Optional<Vec3d> grownHitPos = boundingBox.grow(Config.COMMON.gameplay.growBoundingBoxAmount.get(), 0, Config.COMMON.gameplay.growBoundingBoxAmount.get()).rayTrace(startVec, endVec);
                if (!hitPos.isPresent() && grownHitPos.isPresent())
                {
                    RayTraceResult raytraceresult = rayTraceBlocks(world, new RayTraceContext(startVec, grownHitPos.get(), RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, shooter), IGNORE_LEAVES);
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

    /**
     * A custom implementation of {@link IWorldReader#rayTraceBlocks(RayTraceContext)}
     * that allows you to pass a predicate to ignore certain blocks when checking for collisions.
     *
     * @param world     the world to perform the ray trace
     * @param context   the ray trace context
     * @param predicate the block state predicate
     * @return a result of the raytrace
     */
    static BlockRayTraceResult rayTraceBlocks(World world, RayTraceContext context, java.util.function.Predicate<BlockState> predicate)
    {
        return func_217300_a(context, (rayTraceContext, blockPos) ->
        {
            BlockState blockState = world.getBlockState(blockPos);
            if (predicate.test(blockState)) return null;
            IFluidState fluidState = world.getFluidState(blockPos);
            Vec3d startVec = rayTraceContext.getStartVec();
            Vec3d endVec = rayTraceContext.getEndVec();
            VoxelShape blockShape = rayTraceContext.getBlockShape(blockState, world, blockPos);
            BlockRayTraceResult blockResult = world.rayTraceBlocks(startVec, endVec, blockPos, blockShape, blockState);
            VoxelShape fluidShape = rayTraceContext.getFluidShape(fluidState, world, blockPos);
            BlockRayTraceResult fluidResult = fluidShape.rayTrace(startVec, endVec, blockPos);
            double blockDistance = blockResult == null ? Double.MAX_VALUE : rayTraceContext.getStartVec().squareDistanceTo(blockResult.getHitVec());
            double fluidDistance = fluidResult == null ? Double.MAX_VALUE : rayTraceContext.getStartVec().squareDistanceTo(fluidResult.getHitVec());
            return blockDistance <= fluidDistance ? blockResult : fluidResult;
        }, (rayTraceContext) ->
        {
            Vec3d vec3d = rayTraceContext.getStartVec().subtract(rayTraceContext.getEndVec());
            return BlockRayTraceResult.createMiss(rayTraceContext.getEndVec(), Direction.getFacingFromVector(vec3d.x, vec3d.y, vec3d.z), new BlockPos(rayTraceContext.getEndVec()));
        });
    }

    static <T> T func_217300_a(RayTraceContext context, BiFunction<RayTraceContext, BlockPos, T> hitFunction, Function<RayTraceContext, T> p_217300_2_)
    {
        Vec3d startVec = context.getStartVec();
        Vec3d endVec = context.getEndVec();
        if (startVec.equals(endVec))
        {
            return p_217300_2_.apply(context);
        }
        else
        {
            double d0 = MathHelper.lerp(-1.0E-7D, endVec.x, startVec.x);
            double d1 = MathHelper.lerp(-1.0E-7D, endVec.y, startVec.y);
            double d2 = MathHelper.lerp(-1.0E-7D, endVec.z, startVec.z);
            double d3 = MathHelper.lerp(-1.0E-7D, startVec.x, endVec.x);
            double d4 = MathHelper.lerp(-1.0E-7D, startVec.y, endVec.y);
            double d5 = MathHelper.lerp(-1.0E-7D, startVec.z, endVec.z);
            int i = MathHelper.floor(d3);
            int j = MathHelper.floor(d4);
            int k = MathHelper.floor(d5);
            BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable(i, j, k);
            T t = hitFunction.apply(context, blockpos$mutable);
            if (t != null)
            {
                return t;
            }
            else
            {
                double d6 = d0 - d3;
                double d7 = d1 - d4;
                double d8 = d2 - d5;
                int l = MathHelper.signum(d6);
                int i1 = MathHelper.signum(d7);
                int j1 = MathHelper.signum(d8);
                double d9 = l == 0 ? Double.MAX_VALUE : (double) l / d6;
                double d10 = i1 == 0 ? Double.MAX_VALUE : (double) i1 / d7;
                double d11 = j1 == 0 ? Double.MAX_VALUE : (double) j1 / d8;
                double d12 = d9 * (l > 0 ? 1.0D - MathHelper.frac(d3) : MathHelper.frac(d3));
                double d13 = d10 * (i1 > 0 ? 1.0D - MathHelper.frac(d4) : MathHelper.frac(d4));
                double d14 = d11 * (j1 > 0 ? 1.0D - MathHelper.frac(d5) : MathHelper.frac(d5));

                while (d12 <= 1.0D || d13 <= 1.0D || d14 <= 1.0D)
                {
                    if (d12 < d13)
                    {
                        if (d12 < d14)
                        {
                            i += l;
                            d12 += d9;
                        }
                        else
                        {
                            k += j1;
                            d14 += d11;
                        }
                    }
                    else if (d13 < d14)
                    {
                        j += i1;
                        d13 += d10;
                    }
                    else
                    {
                        k += j1;
                        d14 += d11;
                    }

                    T t1 = hitFunction.apply(context, blockpos$mutable.setPos(i, j, k));
                    if (t1 != null)
                    {
                        return t1;
                    }
                }

                return p_217300_2_.apply(context);
            }
        }
    }
}
