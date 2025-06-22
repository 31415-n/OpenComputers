package li.cil.oc.client.renderer

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraft.nbt.Tag
import net.minecraftforge.eventbus.api.SubscribeEvent
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.{DefaultVertexFormat, PoseStack, Tesselator, VertexFormat}
import net.minecraft.client.renderer.{GameRenderer, MultiBufferSource}
import org.lwjgl.opengl.GL11

object MFUTargetRenderer {
  private val color = 0x00FF00
  private lazy val mfu = api.Items.get(Constants.ItemName.MFU)

  @SubscribeEvent
  def onRenderLevelStage(e: RenderLevelStageEvent): Unit = {
    if (e.getStage != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return
    
    val mc = Minecraft.getInstance
    val player = mc.player
    if (player == null) return
    
    player.getMainHandItem match {
      case stack: ItemStack if api.Items.get(stack) == mfu && stack.hasTag =>
        val data = stack.getTag
        if (data.contains(Settings.namespace + "coord", Tag.TAG_INT_ARRAY)) {
          val Array(x, y, z, dimension, side) = data.getIntArray(Settings.namespace + "coord")
          if (player.level().dimension().location().toString != dimension.toString) return
          if (player.distanceToSqr(x, y, z) > 64 * 64) return

          val bounds = BlockPosition(x, y, z).bounds.inflate(0.1, 0.1, 0.1)
          val camera = mc.gameRenderer.getMainCamera
          val cameraPos = camera.getPosition

          val poseStack = e.getPoseStack
          poseStack.pushPose()
          poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)
          
          RenderSystem.enableBlend()
          RenderSystem.defaultBlendFunc()
          RenderSystem.disableDepthTest()
          RenderSystem.disableCull()
          RenderSystem.setShader(() => GameRenderer.getPositionColorShader)
          RenderSystem.setShaderColor(
            ((color >> 16) & 0xFF) / 255f,
            ((color >> 8) & 0xFF) / 255f,
            ((color >> 0) & 0xFF) / 255f,
            0.25f)

          val tesselator = Tesselator.getInstance
          val buffer = tesselator.getBuilder
          val matrix = poseStack.last().pose()
          
          // Draw wireframe box
          buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR)
          drawBoxLines(buffer, matrix, bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ)
          tesselator.end()
          
          // Draw filled face
          buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
          drawFace(buffer, matrix, bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ, side)
          tesselator.end()

          RenderSystem.enableDepthTest()
          RenderSystem.enableCull()
          RenderSystem.disableBlend()
          
          poseStack.popPose()
        }
      case _ => // Nothing
    }
  }

  private def drawBoxLines(buffer: com.mojang.blaze3d.vertex.VertexConsumer, matrix: org.joml.Matrix4f, minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Unit = {
    val r = ((color >> 16) & 0xFF) / 255f
    val g = ((color >> 8) & 0xFF) / 255f
    val b = ((color >> 0) & 0xFF) / 255f
    val a = 1.0f
    
    // Bottom face lines
    buffer.vertex(matrix, minX.toFloat, minY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, maxX.toFloat, minY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, maxX.toFloat, minY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, maxX.toFloat, minY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, maxX.toFloat, minY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, minX.toFloat, minY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, minX.toFloat, minY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, minX.toFloat, minY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
    
    // Top face lines
    buffer.vertex(matrix, minX.toFloat, maxY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, maxX.toFloat, maxY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, maxX.toFloat, maxY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, maxX.toFloat, maxY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, maxX.toFloat, maxY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, minX.toFloat, maxY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, minX.toFloat, maxY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, minX.toFloat, maxY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
    
    // Vertical lines
    buffer.vertex(matrix, minX.toFloat, minY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, minX.toFloat, maxY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, maxX.toFloat, minY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, maxX.toFloat, maxY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, maxX.toFloat, minY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, maxX.toFloat, maxY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, minX.toFloat, minY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
    buffer.vertex(matrix, minX.toFloat, maxY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
  }

  private def drawFace(buffer: com.mojang.blaze3d.vertex.VertexConsumer, matrix: org.joml.Matrix4f, minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double, side: Int): Unit = {
    val r = ((color >> 16) & 0xFF) / 255f
    val g = ((color >> 8) & 0xFF) / 255f
    val b = ((color >> 0) & 0xFF) / 255f
    val a = 0.25f
    
    side match {
      case 0 => // Down
        buffer.vertex(matrix, minX.toFloat, minY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, minX.toFloat, minY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, maxX.toFloat, minY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, maxX.toFloat, minY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
      case 1 => // Up
        buffer.vertex(matrix, maxX.toFloat, maxY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, maxX.toFloat, maxY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, minX.toFloat, maxY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, minX.toFloat, maxY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
      case 2 => // North
        buffer.vertex(matrix, minX.toFloat, minY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, maxX.toFloat, minY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, maxX.toFloat, maxY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, minX.toFloat, maxY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
      case 3 => // South
        buffer.vertex(matrix, maxX.toFloat, maxY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, maxX.toFloat, minY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, minX.toFloat, minY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, minX.toFloat, maxY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
      case 4 => // West
        buffer.vertex(matrix, minX.toFloat, minY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, minX.toFloat, maxY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, minX.toFloat, maxY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, minX.toFloat, minY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
      case 5 => // East
        buffer.vertex(matrix, maxX.toFloat, minY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, maxX.toFloat, minY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, maxX.toFloat, maxY.toFloat, maxZ.toFloat).color(r, g, b, a).endVertex()
        buffer.vertex(matrix, maxX.toFloat, maxY.toFloat, minZ.toFloat).color(r, g, b, a).endVertex()
      case _ => // WTF?
    }
  }

}
