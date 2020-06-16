package com.mrcrayfish.guns.client.event;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mrcrayfish.guns.Reference;
import com.mrcrayfish.guns.client.util.RenderUtil;
import com.mrcrayfish.guns.common.trace.GunProjectile;
import com.mrcrayfish.guns.object.Bullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: MrCrayfish
 */
@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public class BulletRenderer
{
    private static final ResourceLocation WHITE_GRADIENT = new ResourceLocation(Reference.MOD_ID, "textures/effect/white_gradient.png");
    private static final RenderState.AlphaState DEFAULT_ALPHA = new RenderState.AlphaState(0.0F);
    private static final RenderState.CullState CULL_DISABLED = new RenderState.CullState(false);
    private static final RenderState.TransparencyState TRANSLUCENT_TRANSPARENCY = new RenderState.TransparencyState("translucent_transparency", () ->
    {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }, RenderSystem::disableBlend);

    private List<Bullet> bullets = new ArrayList<>();

    /**
     * @param bullet
     */
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
            this.bullets.removeIf(bullet -> bullet.getProjectile().isComplete());
        }
    }

    @SubscribeEvent
    public void onRenderBullets(RenderWorldLastEvent event)
    {
        for (Bullet bullet : this.bullets)
        {
            this.renderBullet(bullet, event.getMatrixStack(), event.getPartialTicks());
        }
    }

    /**
     * @param bullet
     * @param matrixStack
     * @param partialTicks
     */
    private void renderBullet(Bullet bullet, MatrixStack matrixStack, float partialTicks)
    {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.getRenderViewEntity();
        GunProjectile projectile = bullet.getProjectile();
        if (entity == null || projectile.isComplete())
            return;

        matrixStack.push();

        Vec3d view = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
        double bulletX = projectile.getX() + projectile.getMotionX() * partialTicks;
        double bulletY = projectile.getY() + projectile.getMotionY() * partialTicks;
        double bulletZ = projectile.getZ() + projectile.getMotionZ() * partialTicks;
        matrixStack.translate(bulletX - view.getX(), bulletY - view.getY(), bulletZ - view.getZ());

        matrixStack.rotate(Vector3f.YP.rotationDegrees(bullet.getRotationYaw()));
        matrixStack.rotate(Vector3f.XP.rotationDegrees(-bullet.getRotationPitch() + 90));

        double motionLength = MathHelper.sqrt(projectile.getMotionX() * projectile.getMotionX() + projectile.getMotionY() * projectile.getMotionY() + projectile.getMotionZ() * projectile.getMotionZ());
        float trailLength = (float) ((motionLength / 3.0F) * bullet.getTrailLengthMultiplier());
        float red = (float) (bullet.getTrailColor() >> 16 & 255) / 255.0F;
        float green = (float) (bullet.getTrailColor() >> 8 & 255) / 255.0F;
        float blue = (float) (bullet.getTrailColor() & 255) / 255.0F;
        float alpha = 0.3F;

        Matrix4f matrix4f = matrixStack.getLast().getMatrix();
        IRenderTypeBuffer.Impl renderTypeBuffer = mc.getRenderTypeBuffers().getBufferSource();

        if (projectile.getShooterId() != entity.getEntityId())
        {
            RenderType bulletType = getBulletTrail();
            IVertexBuilder builder = renderTypeBuffer.getBuffer(bulletType);
            builder.pos(matrix4f, 0, 0, -0.035F).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, 0, 0, 0.035F).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, 0, -trailLength, 0.035F).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, 0, -trailLength, -0.035F).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, -0.035F, 0, 0).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, 0.035F, 0, 0).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, 0.035F, -trailLength, 0).color(red, green, blue, alpha).endVertex();
            builder.pos(matrix4f, -0.035F, -trailLength, 0).color(red, green, blue, alpha).endVertex();
            Minecraft.getInstance().getRenderTypeBuffers().getBufferSource().finish(bulletType);
        }

        // No point rendering item if empty, so return
        if (projectile.getBullet().isEmpty())
        {
            matrixStack.pop();
            return;
        }

        matrixStack.rotate(Vector3f.YP.rotationDegrees((projectile.getTicksExisted() + partialTicks) * (float) 50));
        matrixStack.scale(0.275F, 0.275F, 0.275F);

        int combinedLight = WorldRenderer.getCombinedLight(entity.world, entity.getPosition());
        ItemStack stack = projectile.getBullet();
        RenderType renderType = RenderTypeLookup.getRenderType(stack);
        RenderUtil.renderModel(stack, ItemCameraTransforms.TransformType.NONE, matrixStack, renderTypeBuffer, combinedLight, OverlayTexture.NO_OVERLAY);
        Minecraft.getInstance().getRenderTypeBuffers().getBufferSource().finish(renderType);

        matrixStack.pop();
    }

    private static RenderType getBulletTrail()
    {
        return RenderType.makeType("projectile_trail", DefaultVertexFormats.POSITION_COLOR, GL11.GL_QUADS, 256, false, true, RenderType.State.getBuilder().cull(CULL_DISABLED).alpha(DEFAULT_ALPHA).transparency(TRANSLUCENT_TRANSPARENCY).build(false));
    }
}
