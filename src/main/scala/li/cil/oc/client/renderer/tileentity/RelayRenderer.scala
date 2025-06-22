package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.{PoseStack, Tesselator, DefaultVertexFormat, VertexFormat}
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.client.renderer.{MultiBufferSource, GameRenderer}
import net.minecraft.client.renderer.texture.TextureAtlasSprite

class RelayRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[tileentity.Relay] {
  override def render(relay: tileentity.Relay, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    val activity = math.max(0, 1 - (System.currentTimeMillis() - relay.lastMessage) / 1000.0)
    if (activity > 0) {
      RenderState.pushAttrib()

      RenderState.disableEntityLighting()
      RenderState.makeItBlend()
      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, activity.toFloat)

      poseStack.pushPose()

      poseStack.translate(0.5, 0.5, 0.5)
      poseStack.scale(1.0025f, -1.0025f, 1.0025f)
      poseStack.translate(-0.5f, -0.5f, -0.5f)

      val tesselator = Tesselator.getInstance
      val builder = tesselator.getBuilder

      RenderSystem.setShader(() => GameRenderer.getPositionTexShader())
      RenderSystem.setShaderTexture(0, Textures.Block.getAtlasLocation)
      builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)

      val icon = Textures.getSprite(Textures.Block.SwitchSideOn)
      val matrix = poseStack.last().pose()
      
      // Front face (z=0)
      builder.vertex(matrix, 1, 1, 0).uv(icon.getU0, icon.getV1).endVertex()
      builder.vertex(matrix, 0, 1, 0).uv(icon.getU1, icon.getV1).endVertex()
      builder.vertex(matrix, 0, 0, 0).uv(icon.getU1, icon.getV0).endVertex()
      builder.vertex(matrix, 1, 0, 0).uv(icon.getU0, icon.getV0).endVertex()

      // Back face (z=1)
      builder.vertex(matrix, 0, 1, 1).uv(icon.getU0, icon.getV1).endVertex()
      builder.vertex(matrix, 1, 1, 1).uv(icon.getU1, icon.getV1).endVertex()
      builder.vertex(matrix, 1, 0, 1).uv(icon.getU1, icon.getV0).endVertex()
      builder.vertex(matrix, 0, 0, 1).uv(icon.getU0, icon.getV0).endVertex()

      // Right face (x=1)
      builder.vertex(matrix, 1, 1, 1).uv(icon.getU0, icon.getV1).endVertex()
      builder.vertex(matrix, 1, 1, 0).uv(icon.getU1, icon.getV1).endVertex()
      builder.vertex(matrix, 1, 0, 0).uv(icon.getU1, icon.getV0).endVertex()
      builder.vertex(matrix, 1, 0, 1).uv(icon.getU0, icon.getV0).endVertex()

      // Left face (x=0)
      builder.vertex(matrix, 0, 1, 0).uv(icon.getU0, icon.getV1).endVertex()
      builder.vertex(matrix, 0, 1, 1).uv(icon.getU1, icon.getV1).endVertex()
      builder.vertex(matrix, 0, 0, 1).uv(icon.getU1, icon.getV0).endVertex()
      builder.vertex(matrix, 0, 0, 0).uv(icon.getU0, icon.getV0).endVertex()

      tesselator.end()

      RenderSystem.disableBlend()
      RenderState.enableEntityLighting()

      poseStack.popPose()
      RenderState.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
