package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Printer
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.world.item.ItemDisplayContext

class PrinterRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[Printer] {
  override def render(printer: Printer, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    if (printer.data.stateOff.nonEmpty) {
      val stack = printer.data.createItemStack()

      poseStack.pushPose()
      poseStack.translate(0.5, 0.5 + 0.3, 0.5)

      val rotation = (System.currentTimeMillis() % 20000) / 20000f * 360
      poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(rotation).toFloat))
      poseStack.scale(0.75f, 0.75f, 0.75f)

      // Render the item using the modern item renderer
      val itemRenderer = Minecraft.getInstance().getItemRenderer
      itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, printer.getLevel, 0)

      poseStack.popPose()
    }

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}

/**
 * Companion object for creating the renderer
 */
object PrinterRenderer {
  def apply(context: BlockEntityRendererProvider.Context): PrinterRenderer = new PrinterRenderer(context)
}
