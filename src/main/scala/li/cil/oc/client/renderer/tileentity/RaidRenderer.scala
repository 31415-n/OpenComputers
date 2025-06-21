package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Raid
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import org.joml.Matrix4f

class RaidRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[Raid] {
  override def render(raid: Raid, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    poseStack.pushPose()
    poseStack.translate(0.5, 0.5, 0.5)

    raid.yaw match {
      case Direction.WEST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(-90).toFloat))
      case Direction.NORTH => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(180).toFloat))
      case Direction.EAST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(90).toFloat))
      case _ => // No yaw.
    }

    poseStack.translate(-0.5, 0.5, 0.505)
    poseStack.scale(1, -1, 1)

    val buffer = bufferSource.getBuffer(RenderType.cutout())
    val pose = poseStack.last().pose()

    {
      val icon = Textures.getSprite(Textures.Block.RaidFrontError)
      for (slot <- 0 until raid.getContainerSize) {
        if (!raid.presence(slot)) {
          renderSlot(buffer, pose, slot, icon, packedLight, packedOverlay)
        }
      }
    }

    {
      val icon = Textures.getSprite(Textures.Block.RaidFrontActivity)
      for (slot <- 0 until raid.getContainerSize) {
        if (System.currentTimeMillis() - raid.lastAccess < 400 && raid.getLevel.random.nextDouble() > 0.1 && slot == raid.lastAccess % raid.getContainerSize) {
          renderSlot(buffer, pose, slot, icon, packedLight, packedOverlay)
        }
      }
    }

    poseStack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  private val u1 = 2 / 16f
  private val fs = 4 / 16f

  private def renderSlot(buffer: VertexConsumer, pose: Matrix4f, slot: Int, icon: TextureAtlasSprite, packedLight: Int, packedOverlay: Int): Unit = {
    val l = u1 + slot * fs
    val h = u1 + (slot + 1) * fs
    val uL = icon.getU(l * 16)
    val uH = icon.getU(h * 16)
    
    buffer.vertex(pose, l, 1, 0)
      .uv(uL, icon.getV1)
      .overlayCoords(packedOverlay)
      .uv2(packedLight)
      .normal(0.0f, 0.0f, 1.0f)
      .endVertex()
    
    buffer.vertex(pose, h, 1, 0)
      .uv(uH, icon.getV1)
      .overlayCoords(packedOverlay)
      .uv2(packedLight)
      .normal(0.0f, 0.0f, 1.0f)
      .endVertex()
    
    buffer.vertex(pose, h, 0, 0)
      .uv(uH, icon.getV0)
      .overlayCoords(packedOverlay)
      .uv2(packedLight)
      .normal(0.0f, 0.0f, 1.0f)
      .endVertex()
    
    buffer.vertex(pose, l, 0, 0)
      .uv(uL, icon.getV0)
      .overlayCoords(packedOverlay)
      .uv2(packedLight)
      .normal(0.0f, 0.0f, 1.0f)
      .endVertex()
  }
}

/**
 * Companion object for creating the renderer
 */
object RaidRenderer {
  def apply(context: BlockEntityRendererProvider.Context): RaidRenderer = new RaidRenderer(context)
}
