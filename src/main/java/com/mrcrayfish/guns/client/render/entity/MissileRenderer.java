package com.mrcrayfish.guns.client.render.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mrcrayfish.guns.client.SpecialModels;
import com.mrcrayfish.guns.client.util.RenderUtil;
import com.mrcrayfish.guns.entity.MissileEntity;
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
public class MissileRenderer extends EntityRenderer<MissileEntity>
{
    public MissileRenderer(EntityRendererManager renderManager)
    {
        super(renderManager);
    }

    @Override
    public ResourceLocation getEntityTexture(MissileEntity entity)
    {
        return PlayerContainer.LOCATION_BLOCKS_TEXTURE;
    }

    @Override
    public void render(MissileEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, int light)
    {
        if(!entity.getProjectile().visible || entity.ticksExisted <= 1)
        {
            return;
        }

        matrixStack.push();
        matrixStack.rotate(Vector3f.YP.rotationDegrees(180F));
        matrixStack.rotate(Vector3f.YP.rotationDegrees(entityYaw));
        matrixStack.rotate(Vector3f.XP.rotationDegrees(entity.rotationPitch - 90));
        Minecraft.getInstance().getItemRenderer().renderItem(entity.getBullet(), ItemCameraTransforms.TransformType.NONE, 15728880, OverlayTexture.NO_OVERLAY, matrixStack, renderTypeBuffer);
        matrixStack.translate(0, -1, 0);
        RenderUtil.renderModel(SpecialModels.FLAME.getModel(), entity.getBullet(), matrixStack, renderTypeBuffer, 15728880, OverlayTexture.NO_OVERLAY);
        matrixStack.pop();
    }
}
