package com.mrcrayfish.guns.entity;

import com.mrcrayfish.guns.Config;
import com.mrcrayfish.guns.common.BoundingBoxManager;
import com.mrcrayfish.guns.common.SpreadTracker;
import com.mrcrayfish.guns.common.trace.GunProjectile;
import com.mrcrayfish.guns.init.ModSyncedDataKeys;
import com.mrcrayfish.guns.interfaces.IExplosionDamageable;
import com.mrcrayfish.guns.item.GunItem;
import com.mrcrayfish.guns.object.Gun;
import com.mrcrayfish.guns.object.Gun.Projectile;
import com.mrcrayfish.guns.util.GunEnchantmentHelper;
import com.mrcrayfish.guns.util.GunModifierHelper;
import com.mrcrayfish.obfuscate.common.data.SyncedPlayerData;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BreakableBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SExplosionPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.UUID;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class ProjectileEntity extends Entity implements IEntityAdditionalSpawnData, GunProjectile
{
    protected int shooterId;
    protected LivingEntity shooter;
    protected Gun modifiedGun;
    protected Gun.General general;
    protected Projectile projectile;
    private ItemStack weapon = ItemStack.EMPTY;
    private ItemStack item = ItemStack.EMPTY;
    protected float additionalDamage = 0.0F;
    protected EntitySize entitySize;
    protected double modifiedGravity;
    protected int life;

    public ProjectileEntity(EntityType<? extends Entity> entityType, World worldIn)
    {
        super(entityType, worldIn);
    }

    public ProjectileEntity(EntityType<? extends Entity> entityType, World worldIn, LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun)
    {
        this(entityType, worldIn);
        this.setShooter(shooter);
        this.modifiedGun = modifiedGun;
        this.general = modifiedGun.getGeneral();
        this.projectile = modifiedGun.getProjectile();
        this.entitySize = new EntitySize(this.projectile.getSize(), this.projectile.getSize(), false);
        this.modifiedGravity = GunModifierHelper.getModifiedProjectileGravity(weapon, 0.05);
        this.life = GunModifierHelper.getModifiedProjectileLife(weapon, this.projectile.getLife());

        Vector3d dir = this.getDirection(shooter, weapon, item, modifiedGun);
        double speedModifier = GunEnchantmentHelper.getProjectileSpeedModifier(weapon, modifiedGun);
        double speed = GunModifierHelper.getModifiedProjectileSpeed(weapon, this.projectile.getSpeed() * speedModifier);
        this.setMotion(dir.x * speed, dir.y * speed, dir.z * speed);
        this.updateHeading();

        /* Spawn the projectile half way between the previous and current position */
        double posX = shooter.lastTickPosX + (shooter.getPosX() - shooter.lastTickPosX) / 2.0;
        double posY = shooter.lastTickPosY + (shooter.getPosY() - shooter.lastTickPosY) / 2.0 + shooter.getEyeHeight();
        double posZ = shooter.lastTickPosZ + (shooter.getPosZ() - shooter.lastTickPosZ) / 2.0;
        this.setPosition(posX, posY, posZ);

        Item ammo = ForgeRegistries.ITEMS.getValue(this.projectile.getItem());
        if (ammo != null)
            this.item = new ItemStack(ammo);
    }

    @Override
    protected void registerData()
    {
    }

    @Override
    public EntitySize getSize(Pose pose)
    {
        return this.entitySize;
    }

    private Vector3d getDirection(LivingEntity shooter, ItemStack weapon, GunItem item, Gun modifiedGun)
    {
        float gunSpread = GunModifierHelper.getModifiedSpread(weapon, modifiedGun.getGeneral().getSpread());

        if (gunSpread == 0F)
        {
            return this.getVectorFromRotation(shooter.rotationPitch, shooter.rotationYaw);
        }

        if (shooter instanceof PlayerEntity)
        {
            if (!modifiedGun.getGeneral().isAlwaysSpread())
            {
                gunSpread *= SpreadTracker.get(shooter.getUniqueID()).getSpread(item);
            }

            if (SyncedPlayerData.instance().get((PlayerEntity) shooter, ModSyncedDataKeys.AIMING))
            {
                gunSpread *= 0.5F;
            }
        }

        return this.getVectorFromRotation(shooter.rotationPitch - (gunSpread / 2.0F) + rand.nextFloat() * gunSpread, shooter.getRotationYawHead() - (gunSpread / 2.0F) + rand.nextFloat() * gunSpread);
    }

    @Override
    public void setWeapon(ItemStack weapon)
    {
        this.weapon = weapon.copy();
    }

    @Override
    public void setBullet(ItemStack bullet)
    {
        this.item = bullet.copy();
    }

    @Override
    public ItemStack getWeapon()
    {
        return weapon;
    }

    @Override
    public ItemStack getBullet()
    {
        return item;
    }

    @Override
    public float getAdditionalDamage()
    {
        return additionalDamage;
    }

    @Override
    public void setLastX(double lastX)
    {
        super.lastTickPosX = lastX;
    }

    @Override
    public void setLastY(double lastY)
    {
        super.lastTickPosY = lastY;
    }

//<<<<<<< HEAD
    @Override
    public void setLastZ(double lastZ)
    {
        super.lastTickPosZ = lastZ;
    }
//=======
//        if(!this.world.isRemote())
//        {
//            Vector3d startVec = this.getPositionVec();
//            Vector3d endVec = startVec.add(this.getMotion());
//            RayTraceResult result = rayTraceBlocks(this.world, new RayTraceContext(startVec, endVec, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this), IGNORE_LEAVES);
//            if(result.getType() != RayTraceResult.Type.MISS)
//            {
//                endVec = result.getHitVec();
//            }
//>>>>>>> upstream/1.16.X

    @Override
    public void setX(double x)
    {
        super.setPosition(x, super.getPosY(), super.getPosZ());
    }

    @Override
    public void setY(double y)
    {
        super.setPosition(super.getPosX(), y, super.getPosZ());
    }

    @Override
    public void setZ(double z)
    {
        super.setPosition(super.getPosX(), super.getPosY(), z);
    }

    @Override
    public void setMotionX(double motionX)
    {
        super.setMotion(motionX, super.getMotion().getY(), super.getMotion().getZ());
    }

    @Override
    public void setMotionY(double motionY)
    {
        super.setMotion(super.getMotion().getX(), motionY, super.getMotion().getZ());
    }

    @Override
    public void setMotionZ(double motionZ)
    {
        super.setMotion(super.getMotion().getX(), super.getMotion().getY(), motionZ);
    }

    @Override
    public void setShooterId(int shooterId)
    {
        this.shooterId = shooterId;
        Entity entity = this.world.getEntityByID(shooterId);
        this.shooter = entity instanceof LivingEntity ? (LivingEntity) entity : null;
    }

//<<<<<<< HEAD
    @Override
    public void setShooter(UUID shooterId)
    {
        if (this.world instanceof ServerWorld)
        {
            Entity shooter = ((ServerWorld) this.world).getEntityByUuid(shooterId);
            if (shooter instanceof LivingEntity)
                this.shooter = (LivingEntity) shooter;
//=======
//    @Nullable
//    protected EntityResult findEntityOnPath(Vector3d startVec, Vector3d endVec)
//    {
//        Vector3d hitVec = null;
//        Entity hitEntity = null;
//        boolean headshot = false;
//        List<Entity> entities = this.world.getEntitiesInAABBexcluding(this, this.getBoundingBox().expand(this.getMotion()).grow(1.0), PROJECTILE_TARGETS);
//        double closestDistance = Double.MAX_VALUE;
//        for(Entity entity : entities)
//        {
//            if(!entity.equals(this.shooter))
//            {
//                HitResult result = this.getHitResult(entity, startVec, endVec);
//                Optional<Vector3d> hitPos = result.getHitPos();
//                if(!hitPos.isPresent())
//                {
//                    continue;
//                }
//
//                double distanceToHit = startVec.distanceTo(hitPos.get());
//                if(distanceToHit < closestDistance)
//                {
//                    hitVec = hitPos.get();
//                    hitEntity = entity;
//                    closestDistance = distanceToHit;
//                    headshot = result.isHeadshot();
//                }
//            }
//>>>>>>> upstream/1.16.X
        }
    }

//<<<<<<< HEAD
    @Override
    public void setAdditionalDamage(float additionalDamage)
    {
        this.additionalDamage = additionalDamage;
    }

    @Override
    public void tick(World world)
//=======
//    @Nullable
//    protected List<EntityResult> findEntitiesOnPath(Vector3d startVec, Vector3d endVec)
//    {
//        List<EntityResult> hitEntities = new ArrayList<>();
//        List<Entity> entities = this.world.getEntitiesInAABBexcluding(this, this.getBoundingBox().expand(this.getMotion()).grow(1.0), PROJECTILE_TARGETS);
//        for(Entity entity : entities)
//        {
//            if(!entity.equals(this.shooter))
//            {
//                HitResult result = this.getHitResult(entity, startVec, endVec);
//                Optional<Vector3d> hitPos = result.getHitPos();
//                if(!hitPos.isPresent())
//                {
//                    continue;
//                }
//                hitEntities.add(new EntityResult(entity, hitPos.get(), result.isHeadshot()));
//            }
//        }
//        return hitEntities;
//    }
//
//    @SuppressWarnings("unchecked")
//    private HitResult getHitResult(Entity entity, Vector3d startVec, Vector3d endVec)
//>>>>>>> upstream/1.16.X
    {
        if (!this.world.isRemote())
        {
            this.tickStep(this.world, this.projectile.getSize(), this.projectile.getLife(), this.modifiedGravity, this.projectile.isSpawnBulletHole(), false);
        }
//<<<<<<< HEAD
        else
//=======
//        boundingBox = boundingBox.expand(0, expandHeight, 0);
//
//        Vector3d hitPos = boundingBox.rayTrace(startVec, endVec).orElse(null);
//        Vector3d grownHitPos = boundingBox.grow(Config.COMMON.gameplay.growBoundingBoxAmount.get(), 0, Config.COMMON.gameplay.growBoundingBoxAmount.get()).rayTrace(startVec, endVec).orElse(null);
//        if(hitPos == null && grownHitPos != null)
//>>>>>>> upstream/1.16.X
        {
            this.setPosition(this.getX() + this.getMotionX(), this.getY() + this.getMotionY(), this.getZ() + this.getMotionZ());

//<<<<<<< HEAD
            if (this.projectile.isGravity())
                this.setMotionY(this.getMotionY() - GunModifierHelper.getModifiedProjectileGravity(this.getWeapon(), 0.04));
//=======
//        /* Check for headshot */
//        boolean headshot = false;
//        if(Config.COMMON.gameplay.enableHeadShots.get() && entity instanceof LivingEntity)
//        {
//            IHeadshotBox<LivingEntity> headshotBox = (IHeadshotBox<LivingEntity>) BoundingBoxManager.getHeadshotBoxes(entity.getType());
//            if(headshotBox != null)
//            {
//                AxisAlignedBB box = headshotBox.getHeadshotBox((LivingEntity) entity);
//                if(box != null)
//                {
//                    box = box.offset(entity.getPosX(), entity.getPosY(), entity.getPosZ());
//                    Optional<Vector3d> headshotHitPos = box.rayTrace(startVec, endVec);
//                    if(!headshotHitPos.isPresent())
//                    {
//                        box = box.grow(Config.COMMON.gameplay.growBoundingBoxAmount.get(), 0, Config.COMMON.gameplay.growBoundingBoxAmount.get());
//                        headshotHitPos = box.rayTrace(startVec, endVec);
//                    }
//                    if(headshotHitPos.isPresent() && (hitPos == null || headshotHitPos.get().distanceTo(hitPos) < 0.5))
//                    {
//                        hitPos = headshotHitPos.get();
//                        headshot = true;
//                    }
//                }
//            }
//>>>>>>> upstream/1.16.X
        }
    }

//<<<<<<< HEAD
    @Override
    public void tick()
    {
        super.tick();
        this.updateHeading();
        this.tick(this.world);
    }
//=======
//    private void onHit(RayTraceResult result, Vector3d startVec, Vector3d endVec)
//    {
//        if(result instanceof BlockRayTraceResult)
//        {
//            BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) result;
//            if(blockRayTraceResult.getType() == RayTraceResult.Type.MISS)
//            {
//                return;
//            }
//
//            BlockPos pos = blockRayTraceResult.getPos();
//            BlockState state = this.world.getBlockState(pos);
//            Block block = state.getBlock();
//
//            if(Config.COMMON.gameplay.enableGunGriefing.get() && (block instanceof BreakableBlock || block instanceof PaneBlock) && state.getMaterial() == Material.GLASS)
//            {
//                this.world.destroyBlock(blockRayTraceResult.getPos(), false);
//            }
//
//            if(!state.getMaterial().isReplaceable())
//            {
//                this.remove();
//            }
//
//            if(block instanceof IDamageable)
//            {
//                ((IDamageable) block).onBlockDamaged(this.world, state, pos, (int) Math.ceil(getDamage() / 2.0) + 1);
//            }
//
//            Vector3d hitVec = blockRayTraceResult.getHitVec();
//            double holeX = hitVec.getX() + 0.005 * blockRayTraceResult.getFace().getXOffset();
//            double holeY = hitVec.getY() + 0.005 * blockRayTraceResult.getFace().getYOffset();
//            double holeZ = hitVec.getZ() + 0.005 * blockRayTraceResult.getFace().getZOffset();
//            Direction direction = blockRayTraceResult.getFace();
//            PacketHandler.getPlayChannel().send(PacketDistributor.TRACKING_CHUNK.with(() -> this.world.getChunkAt(blockRayTraceResult.getPos())), new MessageBulletHole(holeX, holeY, holeZ, direction, pos));
//>>>>>>> upstream/1.16.X

    @Override
    public void complete(Vector3d completePos)
    {
        super.remove();
    }

//<<<<<<< HEAD
    @Override
    public int getTicksExisted()
    {
        return super.ticksExisted;
    }
//=======
//            int fireStarterLevel = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.FIRE_STARTER.get(), this.weapon);
//            if(fireStarterLevel > 0)
//            {
//                BlockPos offsetPos = pos.offset(blockRayTraceResult.getFace());
//                if(AbstractFireBlock.canLightBlock(this.world, offsetPos))
//                {
//                    BlockState fireState = AbstractFireBlock.getFireForPlacement(this.world, offsetPos);
//                    this.world.setBlockState(offsetPos, fireState, 11);
//                }
//            }
//            return;
//        }
//>>>>>>> upstream/1.16.X

    @Override
    public boolean isComplete()
    {
        return !super.isAlive();
    }

    @Nullable
    @Override
    public Vector3d getCompletePos()
    {
        return null;
    }

    @Override
    public double getLastX()
    {
        return super.lastTickPosX;
    }

    @Override
    public double getLastY()
    {
        return super.lastTickPosY;
    }

    @Override
    public double getLastZ()
    {
        return super.lastTickPosZ;
    }

//<<<<<<< HEAD
    @Override
    public double getX()
//=======
//    protected void onHitEntity(Entity entity, Vector3d hitVec, Vector3d startVec, Vector3d endVec, boolean headshot)
//>>>>>>> upstream/1.16.X
    {
        return super.getPosX();
    }

    @Override
    public double getY()
    {
        return super.getPosY();
    }

    @Override
    public double getZ()
    {
        return super.getPosZ();
    }

//<<<<<<< HEAD
    @Override
    public double getMotionX()
    {
        return super.getMotion().x;
    }
//=======
//        if(entity instanceof PlayerEntity && this.shooter instanceof ServerPlayerEntity)
//        {
//            SoundEvent event = headshot ? SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP : SoundEvents.ENTITY_PLAYER_HURT;
//            if(critical)
//            {
//                event = SoundEvents.ENTITY_ITEM_BREAK; //TODO change
//            }
//            ServerPlayerEntity shooterPlayer = (ServerPlayerEntity) this.shooter;
//            shooterPlayer.connection.sendPacket(new SPlaySoundPacket(event.getRegistryName(), SoundCategory.PLAYERS, new Vector3d(this.shooter.getPosX(), this.shooter.getPosY(), this.shooter.getPosZ()), 0.75F, 1.0F));
//        }
//        else if((critical || headshot) && this.shooter instanceof ServerPlayerEntity)
//        {
//            SoundEvent event = headshot ? SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP : SoundEvents.ENTITY_ITEM_BREAK;
//            ServerPlayerEntity shooterPlayer = (ServerPlayerEntity) this.shooter;
//            shooterPlayer.connection.sendPacket(new SPlaySoundPacket(event.getRegistryName(), SoundCategory.PLAYERS, new Vector3d(this.shooter.getPosX(), this.shooter.getPosY(), this.shooter.getPosZ()), 0.75F, 1.0F));
//        }
//>>>>>>> upstream/1.16.X

    @Override
    public double getMotionY()
    {
        return super.getMotion().y;
    }

    @Override
    public double getMotionZ()
    {
        return super.getMotion().z;
    }

    @Override
    protected void readAdditional(CompoundNBT compound)
    {
        this.projectile = new Projectile();
        this.projectile.deserializeNBT(compound.getCompound("Projectile"));
        this.general = new Gun.General();
        this.general.deserializeNBT(compound.getCompound("General"));
        this.modifiedGravity = compound.getDouble("ModifiedGravity");
        this.life = compound.getInt("MaxLife");
    }

    @Override
    protected void writeAdditional(CompoundNBT compound)
    {
        compound.put("Projectile", this.projectile.serializeNBT());
        compound.put("General", this.general.serializeNBT());
        compound.putDouble("ModifiedGravity", this.modifiedGravity);
        compound.putInt("MaxLife", this.life);
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer)
    {
        buffer.writeCompoundTag(this.projectile.serializeNBT());
        buffer.writeCompoundTag(this.general.serializeNBT());
        buffer.writeInt(this.shooterId);
        buffer.writeItemStack(this.weapon, false);
        buffer.writeItemStack(this.item);
        buffer.writeDouble(this.modifiedGravity);
        buffer.writeVarInt(this.life);
    }

    @Override
    public void readSpawnData(PacketBuffer buffer)
    {
        this.projectile = new Projectile();
        this.projectile.deserializeNBT(buffer.readCompoundTag());
        this.general = new Gun.General();
        this.general.deserializeNBT(buffer.readCompoundTag());
        this.shooterId = buffer.readInt();
        this.weapon = buffer.readItemStack();
        this.item = buffer.readItemStack();
        this.modifiedGravity = buffer.readDouble();
        this.life = buffer.readVarInt();
        this.entitySize = new EntitySize(this.projectile.getSize(), this.projectile.getSize(), false);
    }

    public void updateHeading()
    {
        float f = MathHelper.sqrt(this.getMotion().getX() * this.getMotion().getX() + this.getMotion().getZ() * this.getMotion().getZ());
        this.rotationYaw = (float) (MathHelper.atan2(this.getMotion().getX(), this.getMotion().getZ()) * (180D / Math.PI));
        this.rotationPitch = (float) (MathHelper.atan2(this.getMotion().getY(), (double) f) * (180D / Math.PI));
        this.prevRotationYaw = this.rotationYaw;
        this.prevRotationPitch = this.rotationPitch;
    }

    public Projectile getProjectile()
    {
        return projectile;
    }

    private Vector3d getVectorFromRotation(float pitch, float yaw)
    {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vector3d((double) (f1 * f2), (double) f3, (double) (f * f2));
    }

    @Override
    public UUID getShooter()
    {
        return this.shooter.getUniqueID();
    }

    @Override
    public int getShooterId()
    {
        return shooterId;
    }

    @Override
    public float getDamage()
    {
        float initialDamage = (this.projectile.getDamage() + this.additionalDamage);
        if (this.projectile.isDamageReduceOverLife())
        {
            float modifier = ((float) this.projectile.getLife() - (float) (this.ticksExisted - 1)) / (float) this.projectile.getLife();
            initialDamage *= modifier;
        }
        float damage = initialDamage / this.general.getProjectileAmount();
        return GunModifierHelper.getModifiedDamage(this.weapon, this.modifiedGun, damage);
    }

    @Override
    public boolean isInRangeToRenderDist(double distance)
    {
        return true;
    }

    @Override
    public IPacket<?> createSpawnPacket()
    {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    /**
     * Creates a projectile explosion for the specified entity.
     *
     * @param entity The entity to explode
     * @param radius The amount of radius the entity should deal
     */
    public static void createExplosion(Entity entity, float radius)
    {
        World world = entity.world;
        if (world.isRemote())
            return;

        Explosion explosion = new Explosion(world, entity, entity.getPosX(), entity.getPosY(), entity.getPosZ(), radius, false, Explosion.Mode.NONE);
        explosion.doExplosionA();
        explosion.getAffectedBlockPositions().forEach(pos ->
        {
            if (world.getBlockState(pos).getBlock() instanceof IExplosionDamageable)
                ((IExplosionDamageable) world.getBlockState(pos).getBlock()).onProjectileExploded(world, world.getBlockState(pos), pos, entity);
        });
        explosion.doExplosionB(true);
        explosion.clearAffectedBlockPositions();

        for (ServerPlayerEntity serverplayerentity : ((ServerWorld) world).getPlayers())
        {
            if (serverplayerentity.getDistanceSq(entity.getPosX(), entity.getPosY(), entity.getPosZ()) < 4096.0D)
            {
                serverplayerentity.connection.sendPacket(new SExplosionPacket(entity.getPosX(), entity.getPosY(), entity.getPosZ(), radius / 5f, Collections.emptyList(), explosion.getPlayerKnockbackMap().get(serverplayerentity)));
            }
        }
    }
}
