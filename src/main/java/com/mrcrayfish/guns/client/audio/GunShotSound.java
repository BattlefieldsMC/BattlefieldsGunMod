package com.mrcrayfish.guns.client.audio;

import com.mrcrayfish.guns.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.LocatableSound;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class GunShotSound extends LocatableSound
{
    public GunShotSound(SoundEvent soundIn, SoundCategory categoryIn, float x, float y, float z, float volume, float pitch)
    {
        super(soundIn, categoryIn);
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.attenuationType = AttenuationType.NONE;

        ClientPlayerEntity player = Minecraft.getInstance().player;
        if(player != null)
        {
            this.volume = volume * (1.0F - ((float) Math.sqrt(player.getDistanceSq(x, y, z)) / Config.SERVER.gunShotMaxDistance.get().floatValue()));
            this.volume *= this.volume; //Ease the volume instead of linear
        }
    }
}
