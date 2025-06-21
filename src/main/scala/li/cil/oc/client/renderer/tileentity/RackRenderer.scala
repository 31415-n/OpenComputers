package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.api.event.RackMountableRenderEvent
import li.cil.oc.common.tileentity.Rack
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.core.Direction
import net.minecraftforge.common.MinecraftForge

class RackRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[Rack] {
  private final val vOffset = 2 / 16f
  private final val vSize = 3 / 16f

  override def render(rack: Rack, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    poseStack.pushPose()
    poseStack.translate(0.5, 0.5, 0.5)

    rack.yaw match {
      case Direction.WEST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(-90).toFloat))
      case Direction.NORTH => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(180).toFloat))
      case Direction.EAST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(90).toFloat))
      case _ => // No yaw.
    }

    poseStack.translate(-0.5, 0.5, 0.505 - 0.5f / 16f)
    poseStack.scale(1, -1, 1)

    // Note: we manually sync the rack inventory for this to work.
    for (i <- 0 until rack.getContainerSize) {
      if (!rack.getItem(i).isEmpty) {
        poseStack.pushPose()
        RenderState.pushAttrib()

        val v0 = vOffset + i * vSize
        val v1 = vOffset + (i + 1) * vSize
        val event = new RackMountableRenderEvent.TileEntity(rack, i, rack.lastData(i), v0, v1)
        MinecraftForge.EVENT_BUS.post(event)

        RenderState.popAttrib()
        poseStack.popPose()
      }
    }

    poseStack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}

/**
 * Companion object for creating the renderer
 */
object RackRenderer {
  def apply(context: BlockEntityRendererProvider.Context): RackRenderer = new RackRenderer(context)
}
