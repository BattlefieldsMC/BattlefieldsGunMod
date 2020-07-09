package com.mrcrayfish.guns.client.render.gun;

import com.google.common.collect.ImmutableMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class ModelOverrides
{
    private static final Map<Item, IOverrideModel> MODEL_MAP = new HashMap<>();

    public static void register(ItemStack stack, IOverrideModel model)
    {
        MODEL_MAP.putIfAbsent(stack.getItem(), model);
        /* Register model overrides as an event for ease. Doesn't create an extra overhead because
         * Forge will just ignore it if it contains no events */
        MinecraftForge.EVENT_BUS.register(model);
    }

    public static boolean hasModel(ItemStack stack)
    {
        return MODEL_MAP.containsKey(stack.getItem());
    }

    @Nullable
    public static IOverrideModel getModel(ItemStack stack)
    {
        return MODEL_MAP.get(stack.getItem());
    }

    public static Map<Item, IOverrideModel> getModelMap()
    {
        return ImmutableMap.copyOf(MODEL_MAP);
    }
}
