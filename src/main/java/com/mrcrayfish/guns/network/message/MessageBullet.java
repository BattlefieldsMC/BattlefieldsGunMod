package com.mrcrayfish.guns.network.message;

import com.mrcrayfish.guns.client.ClientHandler;
import com.mrcrayfish.guns.common.ProjectileManager;
import com.mrcrayfish.guns.common.trace.GunProjectile;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class MessageBullet implements IMessage
{
    private ResourceLocation item;
    private GunProjectile projectile;
    private int life;
    private int trailColor;
    private double trailLengthMultiplier;

    public MessageBullet()
    {
    }

    public MessageBullet(ResourceLocation item, GunProjectile projectile, int life, int trailColor, double trailLengthMultiplier)
    {
        this.item = item;
        this.projectile = projectile;
        this.life = life;
        this.trailColor = trailColor;
        this.trailLengthMultiplier = trailLengthMultiplier;
    }

    @Override
    public void encode(PacketBuffer buffer)
    {
        buffer.writeResourceLocation(this.item);
        ProjectileManager.getInstance().getFactory(this.item).encode(buffer, this.projectile);
        buffer.writeVarInt(this.life);
        buffer.writeVarInt(this.trailColor);
        buffer.writeDouble(this.trailLengthMultiplier);
    }

    @Override
    public void decode(PacketBuffer buffer)
    {
        this.item = buffer.readResourceLocation();
        this.projectile = ProjectileManager.getInstance().getFactory(this.item).decode(buffer);
        this.life = buffer.readVarInt();
        this.trailColor = buffer.readVarInt();
        this.trailLengthMultiplier = buffer.readDouble();
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> supplier)
    {
        supplier.get().enqueueWork(() -> ClientHandler.handleMessageBullet(this));
        supplier.get().setPacketHandled(true);
    }

    public GunProjectile getProjectile()
    {
        return projectile;
    }

    public int getLife() {
        return life;
    }

    public int getTrailColor()
    {
        return trailColor;
    }

    public double getTrailLengthMultiplier()
    {
        return trailLengthMultiplier;
    }
}
