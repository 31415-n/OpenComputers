package li.cil.oc.client.renderer.entity

import com.mojang.blaze3d.vertex.PoseStack
import li.cil.oc.client.Textures
import li.cil.oc.common.entity.Drone
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation

/**
 * Drone entity renderer for 1.20.1.
 * Ported from 1.12.2 TileEntitySpecialRenderer system to modern BlockEntityRenderer.
 * 
 * Original functionality:
 * - Renders drone model with proper positioning
 * - Uses drone texture from Textures.Model.Drone
 * - Handles matrix transformations for proper rendering
 */
class DroneRenderer(context: EntityRendererProvider.Context) extends EntityRenderer[Drone](context) {
  val model = new ModelQuadcopter()

  /**
   * Main render method for drone entity.
   * Equivalent to doRender from 1.12.2.
   */
  override def render(entity: Drone, yaw: Float, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int): Unit = {
    poseStack.pushPose()
    RenderState.pushAttrib()

    // Translate to proper position (equivalent to GlStateManager.translate)
    poseStack.translate(0, 2.0 / 16.0, 0)

    // Render the model (equivalent to model.render call)
    model.render(entity, 0, 0, 0, 0, 0, partialTick)

    RenderState.popAttrib()
    poseStack.popPose()
  }

  /**
   * Get texture for drone entity.
   * Equivalent to getEntityTexture from 1.12.2.
   */
  override def getTextureLocation(entity: Drone): ResourceLocation = Textures.Model.Drone
}