package com.mrcrayfish.guns.client.render.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mrcrayfish.guns.client.util.RenderUtil;
import com.mrcrayfish.guns.entity.ProjectileEntity;
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

@OnlyIn(Dist.CLIENT)
public class ProjectileRenderer extends EntityRenderer<ProjectileEntity>
{
    public ProjectileRenderer(EntityRendererManager renderManager)
    {
        super(renderManager);
    }

    @Override
    public ResourceLocation getEntityTexture(ProjectileEntity entity)
    {
        return PlayerContainer.LOCATION_BLOCKS_TEXTURE;
    }

    @Override
    public void render(ProjectileEntity entity, float entityYaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, int light)
    {
        if(!entity.getProjectile().isVisible() || entity.ticksExisted <= 1)
        {
            return;
        }

        matrixStack.push();
        if(!RenderUtil.getModel(entity.getBullet()).isGui3d())
        {
            matrixStack.rotate(this.renderManager.getCameraOrientation());
            matrixStack.rotate(Vector3f.YP.rotationDegrees(180.0F));
            Minecraft.getInstance().getItemRenderer().renderItem(entity.getBullet(), ItemCameraTransforms.TransformType.GROUND, light, OverlayTexture.NO_OVERLAY, matrixStack, renderTypeBuffer);
        }
        else
        {
            matrixStack.rotate(Vector3f.YP.rotationDegrees(180F));
            matrixStack.rotate(Vector3f.YP.rotationDegrees(entityYaw));
            matrixStack.rotate(Vector3f.XP.rotationDegrees(entity.rotationPitch));
            Minecraft.getInstance().getItemRenderer().renderItem(entity.getBullet(), ItemCameraTransforms.TransformType.NONE, light, OverlayTexture.NO_OVERLAY, matrixStack, renderTypeBuffer);
        }
        matrixStack.pop();
    }
}
