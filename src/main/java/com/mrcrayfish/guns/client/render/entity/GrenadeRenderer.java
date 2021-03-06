package com.mrcrayfish.guns.client.render.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mrcrayfish.guns.entity.GrenadeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class GrenadeRenderer extends EntityRenderer<GrenadeEntity>
{
    public GrenadeRenderer(EntityRendererManager renderManager)
    {
        super(renderManager);
    }

    @Override
    public ResourceLocation getEntityTexture(GrenadeEntity entity)
    {
        return PlayerContainer.LOCATION_BLOCKS_TEXTURE;
    }

    @Override
    public void render(GrenadeEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, int light)
    {
        if(!entity.getProjectile().isVisible() || entity.ticksExisted <= 1)
        {
            return;
        }

        matrixStack.push();
        matrixStack.rotate(Vector3f.YP.rotationDegrees(180F));
        matrixStack.rotate(Vector3f.YP.rotationDegrees(entityYaw));
        matrixStack.rotate(Vector3f.XP.rotationDegrees(entity.rotationPitch));

        /* Offsets to the center of the grenade before applying rotation */
        float rotation = entity.ticksExisted + partialTicks;
        matrixStack.translate(0, 0.15, 0);
        matrixStack.rotate(Vector3f.XN.rotationDegrees(rotation * 20));
        matrixStack.translate(0, -0.15, 0);

        matrixStack.translate(0.0, 0.5, 0.0);

        Minecraft.getInstance().getItemRenderer().renderItem(entity.getBullet(), ItemCameraTransforms.TransformType.NONE, light, OverlayTexture.NO_OVERLAY, matrixStack, renderTypeBuffer);
        matrixStack.pop();
    }
}
