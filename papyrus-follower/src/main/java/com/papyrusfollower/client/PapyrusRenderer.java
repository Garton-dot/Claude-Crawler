package com.papyrusfollower.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.papyrusfollower.PapyrusFollower;
import com.papyrusfollower.entity.PapyrusEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Draws Papyrus as the camera-facing 2D overworld sprite from Undertale instead of a 3D model.
 * The texture is a 4x4 atlas of 27x44 frames. Rows are the direction he is facing on screen
 * (down, left, right, up) and columns are the walk cycle (idle, step, idle, step).
 */
public class PapyrusRenderer extends EntityRenderer<PapyrusEntity> {
    private static final ResourceLocation TEXTURE = PapyrusFollower.id("textures/entity/papyrus.png");

    private static final int DOWN = 0;
    private static final int LEFT = 1;
    private static final int RIGHT = 2;
    private static final int UP = 3;

    private static final float HEIGHT = 1.9F;
    private static final float WIDTH = HEIGHT * 27.0F / 44.0F;

    public PapyrusRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4F;
    }

    @Override
    public ResourceLocation getTextureLocation(PapyrusEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(PapyrusEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        // Which way is he facing relative to the camera? Looking the same way as the camera
        // means we see his back; looking back at the camera means we see his front.
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        float relative = Mth.wrapDegrees(bodyYaw - this.entityRenderDispatcher.camera.getYRot());
        int row;
        if (relative > -45.0F && relative <= 45.0F) {
            row = UP;
        } else if (relative > 45.0F && relative <= 135.0F) {
            row = RIGHT;
        } else if (relative > -135.0F && relative <= -45.0F) {
            row = LEFT;
        } else {
            row = DOWN;
        }

        int column = 0;
        if (entity.walkAnimation.speed(partialTick) > 0.05F) {
            column = Math.floorMod((int) (entity.walkAnimation.position(partialTick) * 0.45F), 4);
        }

        float u0 = column / 4.0F;
        float u1 = (column + 1) / 4.0F;
        float v0 = row / 4.0F;
        float v1 = (row + 1) / 4.0F;

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0F);

        // After the camera rotation, local +X points to the left of the screen.
        float hw = WIDTH / 2.0F;
        vertex(consumer, matrix, pose, hw, 0.0F, u0, v1, overlay, packedLight);
        vertex(consumer, matrix, pose, -hw, 0.0F, u1, v1, overlay, packedLight);
        vertex(consumer, matrix, pose, -hw, HEIGHT, u1, v0, overlay, packedLight);
        vertex(consumer, matrix, pose, hw, HEIGHT, u0, v0, overlay, packedLight);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, PoseStack.Pose pose,
                               float x, float y, float u, float v, int overlay, int light) {
        consumer.addVertex(matrix, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, -1.0F);
    }
}
