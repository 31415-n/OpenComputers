package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}

/**
 * Printer block entity renderer for 1.20.1.
 * Ported from 1.12.2 TileEntitySpecialRenderer system to modern BlockEntityRenderer.
 */
class PrinterRendererNew(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[tileentity.Printer] {
  override def render(printer: tileentity.Printer, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")
    
    // TODO: Implement printer rendering logic
    // For now, this is a minimal implementation to allow compilation
    
    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}

/**
 * Companion object for creating the renderer
 */
object PrinterRendererNew {
  def apply(context: BlockEntityRendererProvider.Context): PrinterRendererNew = new PrinterRendererNew(context)
}