package com.mrcrayfish.guns.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mrcrayfish.guns.client.RenderTypes;
import com.mrcrayfish.guns.client.util.RenderUtil;
import com.mrcrayfish.guns.common.trace.GunProjectile;
import com.mrcrayfish.guns.object.Bullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: MrCrayfish
 */
@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public class BulletRenderer
{
    private final List<Bullet> bullets = new ArrayList<>();

    public void addBullet(Bullet bullet)
    {
        this.bullets.add(bullet);
    }

    @SubscribeEvent
    public void onTickBullets(TickEvent.ClientTickEvent event)
    {
        if (Minecraft.getInstance().world != null && event.phase == TickEvent.Phase.END)
        {
            this.bullets.forEach(Bullet::tick);
            this.bullets.removeIf(Bullet::isComplete);
        }
    }

    @SubscribeEvent
    public void onRenderBullets(RenderWorldLastEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.renderViewEntity == null)
            return;

        Vector3d view = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
        IRenderTypeBuffer.Impl buffer = mc.getRenderTypeBuffers().getBufferSource();
        MatrixStack matrixStack = event.getMatrixStack();

        matrixStack.push();
        matrixStack.translate(-view.getX(), -view.getY(), -view.getZ());

        for (Bullet bullet : this.bullets)
            this.renderBullet(mc.renderViewEntity, bullet, buffer, matrixStack, false, event.getPartialTicks());
        for (Bullet bullet : this.bullets)
            this.renderBullet(mc.renderViewEntity, bullet, buffer, matrixStack, true, event.getPartialTicks());

        buffer.finish();
        matrixStack.pop();
    }

    private void renderBullet(Entity entity, Bullet bullet, IRenderTypeBuffer buffer, MatrixStack matrixStack, boolean renderBullet, float partialTicks)
    {
        GunProjectile projectile = bullet.getProjectile();

        matrixStack.push();

        double bulletX = bullet.getPosX(partialTicks);
        double bulletY = bullet.getPosY(partialTicks);
        double bulletZ = bullet.getPosZ(partialTicks);
        matrixStack.translate(bulletX, bulletY, bulletZ);

        matrixStack.rotate(Vector3f.YP.rotationDegrees(bullet.getRotationYaw()));
        matrixStack.rotate(Vector3f.XP.rotationDegrees(-bullet.getRotationPitch() + 90));

        Vector3d motionVec = new Vector3d(projectile.getMotionX(), projectile.getMotionY(), projectile.getMotionZ());
        float trailLength = (float) ((motionVec.length() / 3.0F) * bullet.getTrailLengthMultiplier());
        float red = (float)(bullet.getTrailColor() >> 16 & 255) / 255.0F;
        float green = (float)(bullet.getTrailColor() >> 8 & 255) / 255.0F;
        float blue = (float)(bullet.getTrailColor() & 255) / 255.0F;
        float alpha = 0.3F;

        Matrix4f matrix4f = matrixStack.getLast().getMatrix();

        if (projectile.getShooterId() != entity.getEntityId())
        {
            IVertexBuilder builder = buffer.getBuffer(RenderTypes.getBulletTrail());
            builder.pos(matrix4f, 0, 0, -0.035F).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, 0, 0, 0.035F).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, 0, -trailLength, 0.035F).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, 0, -trailLength, -0.035F).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, -0.035F, 0, 0).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, 0.035F, 0, 0).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, 0.035F, -trailLength, 0).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, -0.035F, -trailLength, 0).color(red, green, blue, alpha).endVertex();
        }

        // No point rendering item if empty, so return
        if (!renderBullet || projectile.getBullet().isEmpty())
        {
            matrixStack.pop();
            return;
        }

        matrixStack.rotate(Vector3f.YP.rotationDegrees((bullet.getTicksExisted() + partialTicks) * (float) 50));
        matrixStack.scale(0.275F, 0.275F, 0.275F);

        int combinedLight = WorldRenderer.getCombinedLight(entity.world, new BlockPos(entity.getPositionVec()));
        ItemStack stack = bullet.getProjectile().getBullet();
        RenderType renderType = RenderTypeLookup.func_239219_a_(stack, false);
        RenderUtil.renderModel(stack, ItemCameraTransforms.TransformType.NONE, matrixStack, buffer, combinedLight, OverlayTexture.NO_OVERLAY);
        Minecraft.getInstance().getRenderTypeBuffers().getBufferSource().finish(renderType);

        matrixStack.pop();
    }
}
