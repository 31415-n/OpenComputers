package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.core.Direction
import org.joml.Matrix4f

class NetSplitterRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[tileentity.NetSplitter] {
  override def render(splitter: tileentity.NetSplitter, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    if (splitter.openSides.contains(!splitter.isInverted)) {
      poseStack.pushPose()
      poseStack.translate(0.5, 0.5, 0.5)
      poseStack.scale(1.0025f, -1.0025f, 1.0025f)
      poseStack.translate(-0.5f, -0.5f, -0.5f)

      val buffer = bufferSource.getBuffer(RenderType.cutout())
      val pose = poseStack.last().pose()

      val sideActivity = Textures.getSprite(Textures.Block.NetSplitterOn)

      if (splitter.isSideOpen(Direction.DOWN)) {
        addVertex(buffer, pose, 0, 1, 0, sideActivity.getU1, sideActivity.getV0, packedLight, packedOverlay)
        addVertex(buffer, pose, 1, 1, 0, sideActivity.getU0, sideActivity.getV0, packedLight, packedOverlay)
        addVertex(buffer, pose, 1, 1, 1, sideActivity.getU0, sideActivity.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 0, 1, 1, sideActivity.getU1, sideActivity.getV1, packedLight, packedOverlay)
      }

      if (splitter.isSideOpen(Direction.UP)) {
        addVertex(buffer, pose, 0, 0, 0, sideActivity.getU1, sideActivity.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 0, 0, 1, sideActivity.getU1, sideActivity.getV0, packedLight, packedOverlay)
        addVertex(buffer, pose, 1, 0, 1, sideActivity.getU0, sideActivity.getV0, packedLight, packedOverlay)
        addVertex(buffer, pose, 1, 0, 0, sideActivity.getU0, sideActivity.getV1, packedLight, packedOverlay)
      }

      if (splitter.isSideOpen(Direction.NORTH)) {
        addVertex(buffer, pose, 1, 1, 0, sideActivity.getU0, sideActivity.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 0, 1, 0, sideActivity.getU1, sideActivity.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 0, 0, 0, sideActivity.getU1, sideActivity.getV0, packedLight, packedOverlay)
        addVertex(buffer, pose, 1, 0, 0, sideActivity.getU0, sideActivity.getV0, packedLight, packedOverlay)
      }

      if (splitter.isSideOpen(Direction.SOUTH)) {
        addVertex(buffer, pose, 0, 1, 1, sideActivity.getU0, sideActivity.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 1, 1, 1, sideActivity.getU1, sideActivity.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 1, 0, 1, sideActivity.getU1, sideActivity.getV0, packedLight, packedOverlay)
        addVertex(buffer, pose, 0, 0, 1, sideActivity.getU0, sideActivity.getV0, packedLight, packedOverlay)
      }

      if (splitter.isSideOpen(Direction.WEST)) {
        addVertex(buffer, pose, 0, 1, 0, sideActivity.getU0, sideActivity.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 0, 1, 1, sideActivity.getU1, sideActivity.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 0, 0, 1, sideActivity.getU1, sideActivity.getV0, packedLight, packedOverlay)
        addVertex(buffer, pose, 0, 0, 0, sideActivity.getU0, sideActivity.getV0, packedLight, packedOverlay)
      }

      if (splitter.isSideOpen(Direction.EAST)) {
        addVertex(buffer, pose, 1, 1, 1, sideActivity.getU0, sideActivity.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 1, 1, 0, sideActivity.getU1, sideActivity.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 1, 0, 0, sideActivity.getU1, sideActivity.getV0, packedLight, packedOverlay)
        addVertex(buffer, pose, 1, 0, 1, sideActivity.getU0, sideActivity.getV0, packedLight, packedOverlay)
      }

      poseStack.popPose()
    }

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  private def addVertex(buffer: VertexConsumer, pose: Matrix4f, x: Float, y: Float, z: Float, u: Float, v: Float, packedLight: Int, packedOverlay: Int): Unit = {
    buffer.vertex(pose, x, y, z)
      .uv(u, v)
      .overlayCoords(packedOverlay)
      .uv2(packedLight)
      .normal(0.0f, 1.0f, 0.0f)
      .endVertex()
  }
}

/**
 * Companion object for creating the renderer
 */
object NetSplitterRenderer {
  def apply(context: BlockEntityRendererProvider.Context): NetSplitterRenderer = new NetSplitterRenderer(context)
}
