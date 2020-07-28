package com.mrcrayfish.guns.common.trace;

import com.mrcrayfish.guns.Config;
import com.mrcrayfish.guns.common.BoundingBoxManager;
import com.mrcrayfish.guns.entity.DamageSourceProjectile;
import com.mrcrayfish.guns.hook.GunProjectileHitEvent;
import com.mrcrayfish.guns.init.ModEnchantments;
import com.mrcrayfish.guns.interfaces.IDamageable;
import com.mrcrayfish.guns.interfaces.IHeadshotBox;
import com.mrcrayfish.guns.network.PacketHandler;
import com.mrcrayfish.guns.network.message.MessageBlood;
import com.mrcrayfish.guns.network.message.MessageBulletHole;
import com.mrcrayfish.guns.object.EntityResult;
import com.mrcrayfish.guns.object.HitResult;
import com.mrcrayfish.guns.util.ExtendedEntityRayTraceResult;
import com.mrcrayfish.guns.util.GunEnchantmentHelper;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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
import java.util.*;
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
     * @param dealShotDamage  Whether or not the bullet itself should deal damage to the world
     */
    default void tickStep(World world, float size, int life, double gravity, boolean spawnBulletHole, boolean dealShotDamage)
    {
        Entity shooter = world.getEntityByID(this.getShooterId());

        Vec3d startVec = new Vec3d(this.getX(), this.getY(), this.getZ());
        Vec3d endVec = startVec.add(this.getMotionX(), this.getMotionY(), this.getMotionZ());
        RayTraceResult result = rayTraceBlocks(world, new RayTraceContext(startVec, endVec, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, shooter), IGNORE_LEAVES);
        if (result.getType() != RayTraceResult.Type.MISS)
        {
            endVec = result.getHitVec();
        }

        List<EntityResult> hitEntities = null;
        int level = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.COLLATERAL.get(), this.getWeapon());
        if (level == 0)
        {
            EntityResult entityResult = findEntityOnPath(this, world, size, startVec, endVec);
            if (entityResult != null)
            {
                hitEntities = Collections.singletonList(entityResult);
            }
        }
        else
        {
            hitEntities = findEntitiesOnPath(this, world, size, startVec, endVec);
        }

        if (hitEntities != null && hitEntities.size() > 0)
        {
            for (EntityResult entityResult : hitEntities)
            {
                result = new ExtendedEntityRayTraceResult(entityResult);
                if (((ExtendedEntityRayTraceResult) result).getEntity() instanceof PlayerEntity)
                {
                    PlayerEntity player = (PlayerEntity) ((ExtendedEntityRayTraceResult) result).getEntity();

                    if (this.getShooter() != null && world.getPlayerByUuid(this.getShooter()) != null && !world.getPlayerByUuid(this.getShooter()).canAttackPlayer(player))
                    {
                        result = null;
                    }
                }
                if (result != null)
                {
                    this.onHit(world, this.getWeapon(), this.getDamage(), spawnBulletHole, dealShotDamage, result, startVec, endVec);
                }
            }
        }
        else
        {
            this.onHit(world, this.getWeapon(), this.getDamage(), spawnBulletHole, dealShotDamage, result, startVec, endVec);
        }

        this.setPosition(this.getX() + this.getMotionX(), this.getY() + this.getMotionY(), this.getZ() + this.getMotionZ());

        if (gravity != 0)
            this.setMotionY(this.getMotionY() - gravity);

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
     * @param dealShotDamage  Whether or not the bullet should damage blocks
     * @param result          The block trace result
     * @param startVec        The ray trace start position
     * @param endVec          The ray trace end position
     */
    default void onHit(World world, ItemStack weapon, float damage, boolean spawnBulletHole, boolean dealShotDamage, RayTraceResult result, Vec3d startVec, Vec3d endVec)
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

            if (!world.isRemote() && dealShotDamage && Config.COMMON.gameplay.enableGunGriefing.get() && (block instanceof BreakableBlock || block instanceof PaneBlock) && state.getMaterial() == Material.GLASS)
                world.destroyBlock(blockRayTraceResult.getPos(), false);

            if (!state.getMaterial().isReplaceable())
                this.complete();

            if (dealShotDamage && block instanceof IDamageable)
                ((IDamageable) block).onBlockDamaged(world, state, pos, this, damage, (int) Math.ceil(damage / 2.0) + 1);

            if (!world.isRemote() && spawnBulletHole)
            {
                Vec3d hitVec = blockRayTraceResult.getHitVec();
                double holeX = hitVec.getX() + 0.005 * blockRayTraceResult.getFace().getXOffset();
                double holeY = hitVec.getY() + 0.005 * blockRayTraceResult.getFace().getYOffset();
                double holeZ = hitVec.getZ() + 0.005 * blockRayTraceResult.getFace().getZOffset();
                Direction direction = blockRayTraceResult.getFace();
                PacketHandler.getPlayChannel().send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunkAt(blockRayTraceResult.getPos())), new MessageBulletHole(holeX, holeY, holeZ, direction, pos));
            }

            this.onHitBlock(world, weapon, damage, state, pos, result.getHitVec(), startVec, endVec);

            if (!world.isRemote())
            {
                int level = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.FIRE_STARTER.get(), this.getWeapon());
                if (level > 0 && state.isSolidSide(world, pos, blockRayTraceResult.getFace()))
                {
                    BlockPos offsetPos = pos.offset(blockRayTraceResult.getFace());
                    BlockState offsetState = world.getBlockState(offsetPos);
                    if (offsetState.isAir(world, offsetPos))
                    {
                        BlockState fireState = ((FireBlock) Blocks.FIRE).getStateForPlacement(world, offsetPos);
                        world.setBlockState(offsetPos, fireState, 11);
                    }
                }
            }

            return;
        }

        if (result instanceof ExtendedEntityRayTraceResult)
        {
            ExtendedEntityRayTraceResult entityRayTraceResult = (ExtendedEntityRayTraceResult) result;
            Entity entity = entityRayTraceResult.getEntity();
            if (entity.getEntityId() == this.getShooterId())
                return;
            this.onHitEntity(world, weapon, damage, entity, result.getHitVec(), startVec, endVec, entityRayTraceResult.isHeadshot());
            if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.COLLATERAL.get(), weapon) == 0)
                this.complete();
            if (!world.isRemote())
                entity.hurtResistantTime = 0;
        }
    }

    /**
     * Called when an entity is hit with this bullet.
     *
     * @param world    The world this bullet hit in
     * @param weapon   The weapon used to hit the provided entity
     * @param damage   The damage this bullet deals
     * @param entity   The entity hit
     * @param hitVec   The position hit on the entity
     * @param startVec The starting position of the shot
     * @param endVec   The ending position of the shot
     * @param headShot Whether or not the shot was a headshot
     */
    default void onHitEntity(World world, ItemStack weapon, float damage, Entity entity, Vec3d hitVec, Vec3d startVec, Vec3d endVec, boolean headShot)
    {
        if (world.isRemote())
            return;

        Entity shooter = world.getEntityByID(this.getShooterId());
        if (shooter == null)
            return;

        float newDamage = GunEnchantmentHelper.getPuncturingDamage(weapon, world.getRandom(), damage);
        boolean critical = damage != newDamage;
        damage = newDamage;

        if (headShot)
        {
            damage *= Config.COMMON.gameplay.headShotDamageMultiplier.get();
        }

        DamageSource source = new DamageSourceProjectile("bullet", this, shooter, this.getWeapon()).setProjectile();
        entity.attackEntityFrom(source, damage);

        if (shooter instanceof ServerPlayerEntity)
        {
            if (entity instanceof PlayerEntity)
            {
                SoundEvent event = headShot ? SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP : SoundEvents.ENTITY_PLAYER_HURT;
                if (critical)
                {
                    event = SoundEvents.ENTITY_ITEM_BREAK; //TODO change
                }
                ServerPlayerEntity shooterPlayer = (ServerPlayerEntity) shooter;
                shooterPlayer.connection.sendPacket(new SPlaySoundPacket(event.getRegistryName(), SoundCategory.PLAYERS, new Vec3d(shooter.getPosX(), shooter.getPosY(), shooter.getPosZ()), 0.75F, 1.0F));
            }
            else if (critical || headShot)
            {
                SoundEvent event = headShot ? SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP : SoundEvents.ENTITY_ITEM_BREAK;
                ServerPlayerEntity shooterPlayer = (ServerPlayerEntity) shooter;
                shooterPlayer.connection.sendPacket(new SPlaySoundPacket(event.getRegistryName(), SoundCategory.PLAYERS, new Vec3d(shooter.getPosX(), shooter.getPosY(), shooter.getPosZ()), 0.75F, 1.0F));
            }
        }

        /* Send blood particle to tracking clients. */
        //TODO maybe make clients send settings to server to prevent sending unnecessary packets
        PacketHandler.getPlayChannel().send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new MessageBlood(hitVec.getX(), hitVec.getY(), hitVec.getZ()));
    }

    default void onHitBlock(World world, ItemStack weapon, float damage, BlockState state, BlockPos pos, Vec3d hitVec, Vec3d startVec, Vec3d endVec)
    {
        if (world.isRemote())
            return;

        ((ServerWorld) world).spawnParticle(new BlockParticleData(ParticleTypes.BLOCK, state), hitVec.getX(), hitVec.getY(), hitVec.getZ(), (int) damage, 0.0, 0.0, 0.0, 0.05);
        world.playSound(null, hitVec.getX(), hitVec.getY(), hitVec.getZ(), state.getSoundType().getBreakSound(), SoundCategory.BLOCKS, 0.75F, 2.0F);
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
        boolean headshot = false;
        List<Entity> entities = world.getEntitiesInAABBexcluding(shooter, new AxisAlignedBB(projectile.getX() - bulletSize / 2, projectile.getY() - bulletSize / 2, projectile.getZ() - bulletSize / 2, projectile.getX() + bulletSize / 2, projectile.getY() + bulletSize / 2, projectile.getZ() + bulletSize / 2).expand(projectile.getMotionX(), projectile.getMotionY(), projectile.getMotionZ()).grow(1.0), PROJECTILE_TARGETS);
        double closestDistance = Double.MAX_VALUE;
        for (Entity entity : entities)
        {
            if (!(entity instanceof GunProjectile))
            {
                HitResult result = getHitResult(world, entity, startVec, endVec);
                Optional<Vec3d> hitPos = result.getHitPos();
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
                    headshot = result.isHeadshot();
                }
            }
        }
        return hitEntity != null ? new EntityResult(hitEntity, hitVec, headshot) : null;

//        Vec3d hitVec = null;
//        Entity hitEntity = null;
//        List<Entity> entities = world.getEntitiesInAABBexcluding(shooter, new AxisAlignedBB(projectile.getX() - bulletSize / 2, projectile.getY() - bulletSize / 2, projectile.getZ() - bulletSize / 2, projectile.getX() + bulletSize / 2, projectile.getY() + bulletSize / 2, projectile.getZ() + bulletSize / 2).expand(projectile.getMotionX(), projectile.getMotionY(), projectile.getMotionZ()).grow(1.0), PROJECTILE_TARGETS);
//        double closestDistance = Double.MAX_VALUE;
//        for (Entity entity : entities)
//        {
//            if (!(entity instanceof GunProjectile))
//            {
//                AxisAlignedBB boundingBox = entity.getBoundingBox();
//                Optional<Vec3d> hitPos = boundingBox.rayTrace(startVec, endVec);
//                Optional<Vec3d> grownHitPos = boundingBox.grow(Config.COMMON.gameplay.growBoundingBoxAmount.get(), 0, Config.COMMON.gameplay.growBoundingBoxAmount.get()).rayTrace(startVec, endVec);
//                if (!hitPos.isPresent() && grownHitPos.isPresent())
//                {
//                    RayTraceResult raytraceresult = rayTraceBlocks(world, new RayTraceContext(startVec, grownHitPos.get(), RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, shooter), IGNORE_LEAVES);
//                    if (raytraceresult.getType() == RayTraceResult.Type.BLOCK)
//                    {
//                        continue;
//                    }
//                    hitPos = grownHitPos;
//                }
//
//                if (!hitPos.isPresent())
//                {
//                    continue;
//                }
//
//                double distanceToHit = startVec.distanceTo(hitPos.get());
//                if (distanceToHit < closestDistance)
//                {
//                    hitVec = hitPos.get();
//                    hitEntity = entity;
//                    closestDistance = distanceToHit;
//                }
//            }
//        }
//        return hitEntity != null ? new EntityResult(hitEntity, hitVec, false) : null;
    }

    /**
     * Ray traces for the specified projectile at the start vec to the end vec and collects all entities hit.
     *
     * @param projectile The projectile to ray trace
     * @param world      The world the projectile is in
     * @param bulletSize The size of the bullet
     * @param startVec   The starting position of the movement
     * @param endVec     The ending position of the movement
     * @return The list of results collected by the ray trace
     */
    static List<EntityResult> findEntitiesOnPath(GunProjectile projectile, World world, float bulletSize, Vec3d startVec, Vec3d endVec)
    {
        Entity shooter = world.getEntityByID(projectile.getShooterId());
        if (shooter == null)
            return Collections.emptyList();

//        List<EntityResult> hitEntities = new ArrayList<>();
//        List<Entity> entities = world.getEntitiesInAABBexcluding(shooter, new AxisAlignedBB(projectile.getX() - bulletSize / 2, projectile.getY() - bulletSize / 2, projectile.getZ() - bulletSize / 2, projectile.getX() + bulletSize / 2, projectile.getY() + bulletSize / 2, projectile.getZ() + bulletSize / 2).expand(projectile.getMotionX(), projectile.getMotionY(), projectile.getMotionZ()).grow(1.0), PROJECTILE_TARGETS);
//        for (Entity entity : entities)
//        {
//            if (!(entity instanceof GunProjectile))
//            {
//                AxisAlignedBB boundingBox = entity.getBoundingBox();
//                Optional<Vec3d> hitPos = boundingBox.rayTrace(startVec, endVec);
//                Optional<Vec3d> grownHitPos = boundingBox.grow(Config.COMMON.gameplay.growBoundingBoxAmount.get(), 0, Config.COMMON.gameplay.growBoundingBoxAmount.get()).rayTrace(startVec, endVec);
//                if (!hitPos.isPresent() && grownHitPos.isPresent())
//                {
//                    RayTraceResult raytraceresult = rayTraceBlocks(world, new RayTraceContext(startVec, grownHitPos.get(), RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, shooter), IGNORE_LEAVES);
//                    if (raytraceresult.getType() == RayTraceResult.Type.BLOCK)
//                    {
//                        continue;
//                    }
//                    hitPos = grownHitPos;
//                }
//
//                if (!hitPos.isPresent())
//                {
//                    continue;
//                }
//
//                hitEntities.add(new EntityResult(entity, hitPos.get(), false));
//            }
//        }
//        return hitEntities;

        List<EntityResult> hitEntities = new ArrayList<>();
        List<Entity> entities = world.getEntitiesInAABBexcluding(shooter, new AxisAlignedBB(projectile.getX() - bulletSize / 2, projectile.getY() - bulletSize / 2, projectile.getZ() - bulletSize / 2, projectile.getX() + bulletSize / 2, projectile.getY() + bulletSize / 2, projectile.getZ() + bulletSize / 2).expand(projectile.getMotionX(), projectile.getMotionY(), projectile.getMotionZ()).grow(1.0), PROJECTILE_TARGETS);
        for (Entity entity : entities)
        {
            if (!(entity instanceof GunProjectile))
            {
                HitResult result = getHitResult(world, entity, startVec, endVec);
                Optional<Vec3d> hitPos = result.getHitPos();
                if (!hitPos.isPresent())
                {
                    continue;
                }
                hitEntities.add(new EntityResult(entity, hitPos.get(), result.isHeadshot()));
            }
        }
        return hitEntities;
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

    @SuppressWarnings("unchecked")
    static HitResult getHitResult(World world, Entity entity, Vec3d startVec, Vec3d endVec)
    {
        double expandHeight = entity instanceof PlayerEntity && !entity.isCrouching() ? 0.0625 : 0.0;
        AxisAlignedBB boundingBox = entity.getBoundingBox().expand(0, expandHeight, 0);

        Vec3d hitPos = boundingBox.rayTrace(startVec, endVec).orElse(null);
        Vec3d grownHitPos = boundingBox.grow(Config.COMMON.gameplay.growBoundingBoxAmount.get(), 0, Config.COMMON.gameplay.growBoundingBoxAmount.get()).rayTrace(startVec, endVec).orElse(null);
        if (hitPos == null && grownHitPos != null)
        {
            RayTraceResult raytraceresult = rayTraceBlocks(world, new RayTraceContext(startVec, grownHitPos, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, null), IGNORE_LEAVES);
            if (raytraceresult.getType() == RayTraceResult.Type.BLOCK)
            {
                return new HitResult(null, false);
            }
            hitPos = grownHitPos;
        }

        /* Check for headshot */
        boolean headshot = false;
        if (Config.COMMON.gameplay.enableHeadShots.get() && entity instanceof LivingEntity)
        {
            IHeadshotBox<LivingEntity> headshotBox = (IHeadshotBox<LivingEntity>) BoundingBoxManager.getHeadshotBoxes(entity.getType());
            if (headshotBox != null)
            {
                AxisAlignedBB box = headshotBox.getHeadshotBox((LivingEntity) entity);
                if (box != null)
                {
                    box = box.offset(entity.getPosX(), entity.getPosY(), entity.getPosZ());
                    Optional<Vec3d> headshotHitPos = box.rayTrace(startVec, endVec);
                    if (!headshotHitPos.isPresent())
                    {
                        box = box.grow(Config.COMMON.gameplay.growBoundingBoxAmount.get(), 0, Config.COMMON.gameplay.growBoundingBoxAmount.get());
                        headshotHitPos = box.rayTrace(startVec, endVec);
                    }
                    if (headshotHitPos.isPresent() && (hitPos == null || headshotHitPos.get().distanceTo(hitPos) < 0.5))
                    {
                        hitPos = headshotHitPos.get();
                        headshot = true;
                    }
                }
            }
        }
        return new HitResult(hitPos, headshot);
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
