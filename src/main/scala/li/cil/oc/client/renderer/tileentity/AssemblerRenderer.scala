package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Assembler
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.level.block.entity.BlockEntity
import org.joml.Matrix4f

class AssemblerRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[Assembler] {

  override def render(assembler: Assembler, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    poseStack.pushPose()
    poseStack.translate(0.5, 0.5, 0.5)

    val buffer = bufferSource.getBuffer(RenderType.cutout())
    val pose = poseStack.last().pose()

    {
      val icon = Textures.getSprite(Textures.Block.AssemblerTopOn)
      addVertex(buffer, pose, -0.5f, 0.55f, 0.5f, icon.getU0, icon.getV1, packedLight, packedOverlay)
      addVertex(buffer, pose, 0.5f, 0.55f, 0.5f, icon.getU1, icon.getV1, packedLight, packedOverlay)
      addVertex(buffer, pose, 0.5f, 0.55f, -0.5f, icon.getU1, icon.getV0, packedLight, packedOverlay)
      addVertex(buffer, pose, -0.5f, 0.55f, -0.5f, icon.getU0, icon.getV0, packedLight, packedOverlay)
    }

    // TODO Unroll loop to draw all at once?
    val indent = 6 / 16f + 0.005f
    for (i <- 0 until 4) {
      if (assembler.isAssembling) {
        val icon = Textures.getSprite(Textures.Block.AssemblerSideAssembling)
        val u0 = icon.getU((0.5 - indent) * 16)
        val u1 = icon.getU((0.5 + indent) * 16)
        addVertex(buffer, pose, indent, 0.5f, -indent, u0, icon.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, indent, 0.5f, indent, u1, icon.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, indent, -0.5f, indent, u1, icon.getV0, packedLight, packedOverlay)
        addVertex(buffer, pose, indent, -0.5f, -indent, u0, icon.getV0, packedLight, packedOverlay)
      }

      {
        val icon = Textures.getSprite(Textures.Block.AssemblerSideOn)
        addVertex(buffer, pose, 0.5005f, 0.5f, -0.5f, icon.getU0, icon.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 0.5005f, 0.5f, 0.5f, icon.getU1, icon.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 0.5005f, -0.5f, 0.5f, icon.getU1, icon.getV0, packedLight, packedOverlay)
        addVertex(buffer, pose, 0.5005f, -0.5f, -0.5f, icon.getU0, icon.getV0, packedLight, packedOverlay)
      }

      poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(90).toFloat))
    }

    poseStack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  /**
   * Helper method to add a vertex to the buffer with proper format
   */
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
object AssemblerRenderer {
  def apply(context: BlockEntityRendererProvider.Context): AssemblerRenderer = new AssemblerRenderer(context)
}
