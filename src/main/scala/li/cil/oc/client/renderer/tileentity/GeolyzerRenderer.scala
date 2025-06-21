package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Geolyzer
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import org.joml.Matrix4f

class GeolyzerRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[Geolyzer] {
  override def render(geolyzer: Geolyzer, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    poseStack.pushPose()
    poseStack.translate(0.5, 0.5, 0.5)
    poseStack.scale(1.0025f, -1.0025f, 1.0025f)
    poseStack.translate(-0.5f, -0.5f, -0.5f)

    val buffer = bufferSource.getBuffer(RenderType.cutout())
    val pose = poseStack.last().pose()

    val icon = Textures.getSprite(Textures.Block.GeolyzerTopOn)
    addVertex(buffer, pose, 0, 0, 1, icon.getU0, icon.getV1, packedLight, packedOverlay)
    addVertex(buffer, pose, 1, 0, 1, icon.getU1, icon.getV1, packedLight, packedOverlay)
    addVertex(buffer, pose, 1, 0, 0, icon.getU1, icon.getV0, packedLight, packedOverlay)
    addVertex(buffer, pose, 0, 0, 0, icon.getU0, icon.getV0, packedLight, packedOverlay)

    poseStack.popPose()

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

object GeolyzerRenderer {
  def apply(context: BlockEntityRendererProvider.Context): GeolyzerRenderer = new GeolyzerRenderer(context)
}
