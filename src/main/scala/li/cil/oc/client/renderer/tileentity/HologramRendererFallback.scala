package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.common.tileentity.Hologram
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}

class HologramRendererFallback(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[Hologram] {
  var text = "Requires OpenGL 1.5"

  override def render(hologram: Hologram, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    val fontRenderer = Minecraft.getInstance().font

    poseStack.pushPose()
    poseStack.translate(0.5, 0.75, 0.5)
    poseStack.scale(1 / 128f, -1 / 128f, 1 / 128f)
    
    val textWidth = fontRenderer.width(text)
    fontRenderer.drawInBatch(text, -textWidth / 2f, 0, 0xFFFFFFFF, false, poseStack.last().pose(), bufferSource, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight)

    poseStack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}

/**
 * Companion object for creating the renderer
 */
object HologramRendererFallback {
  def apply(context: BlockEntityRendererProvider.Context): HologramRendererFallback = new HologramRendererFallback(context)
}
