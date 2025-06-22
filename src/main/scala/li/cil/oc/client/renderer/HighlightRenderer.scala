package li.cil.oc.client.renderer

import li.cil.oc.client.Textures
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.{BlockPosition, RenderState}
import li.cil.oc.{Constants, Settings, api, common}
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.{DefaultVertexFormat, PoseStack, Tesselator, VertexConsumer}
import net.minecraft.client.renderer.{GameRenderer, LevelRenderer, MultiBufferSource, RenderType}
import net.minecraft.core.Direction
import net.minecraft.world.phys.{BlockHitResult, HitResult, Vec3}
import net.minecraftforge.client.event.RenderHighlightEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.lwjgl.opengl.GL11

import scala.util.Random

object HighlightRenderer {
  private val random = new Random()

  lazy val tablet = api.Items.get(Constants.ItemName.Tablet)

  @SubscribeEvent
  def onDrawBlockHighlight(e: RenderHighlightEvent.Block): Unit = if (e.getTarget != null && e.getTarget.getBlockPos != null) {
    val hitInfo = e.getTarget
    val camera = e.getCamera
    val world = camera.getEntity.level()
    val blockPos = BlockPosition(hitInfo.getBlockPos, world)
    
    // Get player from camera entity
    val player = camera.getEntity match {
      case p: net.minecraft.world.entity.player.Player => p
      case _ => return
    }
    
    if (hitInfo.getType == HitResult.Type.BLOCK && api.Items.get(player.getMainHandItem) == tablet) {
      val isAir = world.isAirBlock(blockPos)
      if (!isAir) {
        val block = world.getBlock(blockPos)
        val state = world.getBlockState(hitInfo.getBlockPos)
        val bounds = state.getShape(world, hitInfo.getBlockPos).bounds()
        val sideHit = hitInfo.getDirection
        val cameraPos = camera.getPosition
        val renderPos = new Vec3(blockPos.x - cameraPos.x, blockPos.y - cameraPos.y, blockPos.z - cameraPos.z)

        val poseStack = e.getPoseStack
        poseStack.pushPose()
        
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShader(() => GameRenderer.getPositionTexShader)
        RenderSystem.setShaderColor(0.0F, 1.0F, 0.0F, 0.4F)
        
        poseStack.translate(renderPos.x, renderPos.y, renderPos.z)
        poseStack.scale(1.002f, 1.002f, 1.002f)

        if (Settings.get.hologramFlickerFrequency > 0 && random.nextDouble() < Settings.get.hologramFlickerFrequency) {
          val (sx, sy, sz) = (1 - math.abs(sideHit.getStepX), 1 - math.abs(sideHit.getStepY), 1 - math.abs(sideHit.getStepZ))
          poseStack.scale((1 + random.nextGaussian() * 0.01).toFloat, (1 + random.nextGaussian() * 0.001).toFloat, (1 + random.nextGaussian() * 0.01).toFloat)
          poseStack.translate(random.nextGaussian() * 0.01 * sx, random.nextGaussian() * 0.01 * sy, random.nextGaussian() * 0.01 * sz)
        }

        val tesselator = Tesselator.getInstance()
        val buffer = tesselator.getBuilder
        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        
        val matrix = poseStack.last().pose()
        sideHit match {
          case Direction.UP =>
            buffer.vertex(matrix, bounds.maxX.toFloat, (bounds.maxY + 0.002).toFloat, bounds.maxZ.toFloat).uv((bounds.maxZ * 16).toFloat, (bounds.maxX * 16).toFloat).endVertex()
            buffer.vertex(matrix, bounds.maxX.toFloat, (bounds.maxY + 0.002).toFloat, bounds.minZ.toFloat).uv((bounds.minZ * 16).toFloat, (bounds.maxX * 16).toFloat).endVertex()
            buffer.vertex(matrix, bounds.minX.toFloat, (bounds.maxY + 0.002).toFloat, bounds.minZ.toFloat).uv((bounds.minZ * 16).toFloat, (bounds.minX * 16).toFloat).endVertex()
            buffer.vertex(matrix, bounds.minX.toFloat, (bounds.maxY + 0.002).toFloat, bounds.maxZ.toFloat).uv((bounds.maxZ * 16).toFloat, (bounds.minX * 16).toFloat).endVertex()
          case Direction.DOWN =>
            buffer.vertex(matrix, bounds.maxX.toFloat, (bounds.minY - 0.002).toFloat, bounds.minZ.toFloat).uv((bounds.minZ * 16).toFloat, (bounds.maxX * 16).toFloat).endVertex()
            buffer.vertex(matrix, bounds.maxX.toFloat, (bounds.minY - 0.002).toFloat, bounds.maxZ.toFloat).uv((bounds.maxZ * 16).toFloat, (bounds.maxX * 16).toFloat).endVertex()
            buffer.vertex(matrix, bounds.minX.toFloat, (bounds.minY - 0.002).toFloat, bounds.maxZ.toFloat).uv((bounds.maxZ * 16).toFloat, (bounds.minX * 16).toFloat).endVertex()
            buffer.vertex(matrix, bounds.minX.toFloat, (bounds.minY - 0.002).toFloat, bounds.minZ.toFloat).uv((bounds.minZ * 16).toFloat, (bounds.minX * 16).toFloat).endVertex()
          case Direction.EAST =>
            buffer.vertex(matrix, (bounds.maxX + 0.002).toFloat, bounds.maxY.toFloat, bounds.minZ.toFloat).uv((bounds.minZ * 16).toFloat, (bounds.maxY * 16).toFloat).endVertex()
            buffer.vertex(matrix, (bounds.maxX + 0.002).toFloat, bounds.maxY.toFloat, bounds.maxZ.toFloat).uv((bounds.maxZ * 16).toFloat, (bounds.maxY * 16).toFloat).endVertex()
            buffer.vertex(matrix, (bounds.maxX + 0.002).toFloat, bounds.minY.toFloat, bounds.maxZ.toFloat).uv((bounds.maxZ * 16).toFloat, (bounds.minY * 16).toFloat).endVertex()
            buffer.vertex(matrix, (bounds.maxX + 0.002).toFloat, bounds.minY.toFloat, bounds.minZ.toFloat).uv((bounds.minZ * 16).toFloat, (bounds.minY * 16).toFloat).endVertex()
          case Direction.WEST =>
            buffer.vertex(matrix, (bounds.minX - 0.002).toFloat, bounds.maxY.toFloat, bounds.maxZ.toFloat).uv((bounds.maxZ * 16).toFloat, (bounds.maxY * 16).toFloat).endVertex()
            buffer.vertex(matrix, (bounds.minX - 0.002).toFloat, bounds.maxY.toFloat, bounds.minZ.toFloat).uv((bounds.minZ * 16).toFloat, (bounds.maxY * 16).toFloat).endVertex()
            buffer.vertex(matrix, (bounds.minX - 0.002).toFloat, bounds.minY.toFloat, bounds.minZ.toFloat).uv((bounds.minZ * 16).toFloat, (bounds.minY * 16).toFloat).endVertex()
            buffer.vertex(matrix, (bounds.minX - 0.002).toFloat, bounds.minY.toFloat, bounds.maxZ.toFloat).uv((bounds.maxZ * 16).toFloat, (bounds.minY * 16).toFloat).endVertex()
          case Direction.SOUTH =>
            buffer.vertex(matrix, bounds.maxX.toFloat, bounds.maxY.toFloat, (bounds.maxZ + 0.002).toFloat).uv((bounds.maxX * 16).toFloat, (bounds.maxY * 16).toFloat).endVertex()
            buffer.vertex(matrix, bounds.minX.toFloat, bounds.maxY.toFloat, (bounds.maxZ + 0.002).toFloat).uv((bounds.minX * 16).toFloat, (bounds.maxY * 16).toFloat).endVertex()
            buffer.vertex(matrix, bounds.minX.toFloat, bounds.minY.toFloat, (bounds.maxZ + 0.002).toFloat).uv((bounds.minX * 16).toFloat, (bounds.minY * 16).toFloat).endVertex()
            buffer.vertex(matrix, bounds.maxX.toFloat, bounds.minY.toFloat, (bounds.maxZ + 0.002).toFloat).uv((bounds.maxX * 16).toFloat, (bounds.minY * 16).toFloat).endVertex()
          case _ =>
            buffer.vertex(matrix, bounds.minX.toFloat, bounds.maxY.toFloat, (bounds.minZ - 0.002).toFloat).uv((bounds.minX * 16).toFloat, (bounds.maxY * 16).toFloat).endVertex()
            buffer.vertex(matrix, bounds.maxX.toFloat, bounds.maxY.toFloat, (bounds.minZ - 0.002).toFloat).uv((bounds.maxX * 16).toFloat, (bounds.maxY * 16).toFloat).endVertex()
            buffer.vertex(matrix, bounds.maxX.toFloat, bounds.minY.toFloat, (bounds.minZ - 0.002).toFloat).uv((bounds.maxX * 16).toFloat, (bounds.minY * 16).toFloat).endVertex()
            buffer.vertex(matrix, bounds.minX.toFloat, bounds.minY.toFloat, (bounds.minZ - 0.002).toFloat).uv((bounds.minX * 16).toFloat, (bounds.minY * 16).toFloat).endVertex()
        }
        tesselator.end()

        RenderSystem.disableBlend()
        poseStack.popPose()
      }
    }

    // Handle Print and Cable highlighting
    if (hitInfo.getType == HitResult.Type.BLOCK) world.getBlockEntity(hitInfo.getBlockPos) match {
      case print: common.tileentity.Print if print.shapes.nonEmpty =>
        val cameraPos = camera.getPosition
        val expansion = 0.002f

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShaderColor(0, 0, 0, 0.4f)
        RenderSystem.lineWidth(2)
        // RenderSystem.disableTexture() - method removed in 1.20.1
        RenderSystem.depthMask(false)

        for (shape <- print.shapes) {
          val bounds = shape.bounds.rotateTowards(print.facing)
          val expandedBounds = bounds.inflate(expansion, expansion, expansion)
            .move(blockPos.x - cameraPos.x, blockPos.y - cameraPos.y, blockPos.z - cameraPos.z)
          LevelRenderer.renderLineBox(e.getPoseStack, e.getMultiBufferSource.getBuffer(RenderType.lines()), 
            expandedBounds, 0, 0, 0, 0.4f)
        }

        RenderSystem.depthMask(true)
        // RenderSystem.enableTexture() - method removed in 1.20.1
        RenderSystem.disableBlend()

        e.setCanceled(true)
      case cable: common.tileentity.Cable =>
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShaderColor(0, 0, 0, 0.4f)
        RenderSystem.lineWidth(2)
        // RenderSystem.disableTexture() - method removed in 1.20.1
        RenderSystem.depthMask(false)
        
        val poseStack = e.getPoseStack
        poseStack.pushPose()

        val cameraPos = camera.getPosition
        poseStack.translate(
          blockPos.x - cameraPos.x,
          blockPos.y - cameraPos.y,
          blockPos.z - cameraPos.z
        )

        val mask = common.block.Cable.neighbors(world, hitInfo.getBlockPos)
        val tesselator = Tesselator.getInstance
        val buffer = tesselator.getBuilder

        buffer.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION)
        Cable.drawOverlay(buffer, mask, poseStack.last().pose())
        tesselator.end()

        poseStack.popPose()
        RenderSystem.depthMask(true)
        // RenderSystem.enableTexture() - method removed in 1.20.1
        RenderSystem.disableBlend()

        e.setCanceled(true)
      case _ =>
    }
  }

  private object Cable {
    private final val EXPAND = 0.002f
    private final val MIN = common.block.Cable.MIN - EXPAND
    private final val MAX = common.block.Cable.MAX + EXPAND

    def drawOverlay(buffer: VertexConsumer, mask: Int, matrix: org.joml.Matrix4f): Unit = {
      // Draw the cable arms
      for (side <- Direction.values()) {
        if (((1 << side.get3DDataValue()) & mask) != 0) {
          val offset = if (side.getAxisDirection == Direction.AxisDirection.NEGATIVE) -EXPAND else 1 + EXPAND
          val centre = if (side.getAxisDirection == Direction.AxisDirection.NEGATIVE) MIN else MAX

          // Draw the arm end quad
          drawLineAdjacent(buffer, matrix, side.getAxis, offset, MIN, MIN, MIN, MAX)
          drawLineAdjacent(buffer, matrix, side.getAxis, offset, MIN, MAX, MAX, MAX)
          drawLineAdjacent(buffer, matrix, side.getAxis, offset, MAX, MAX, MAX, MIN)
          drawLineAdjacent(buffer, matrix, side.getAxis, offset, MAX, MIN, MIN, MIN)

          // Draw the connecting lines to the middle
          drawLineAlong(buffer, matrix, side.getAxis, MIN, MIN, offset, centre)
          drawLineAlong(buffer, matrix, side.getAxis, MAX, MIN, offset, centre)
          drawLineAlong(buffer, matrix, side.getAxis, MAX, MAX, offset, centre)
          drawLineAlong(buffer, matrix, side.getAxis, MIN, MAX, offset, centre)
        }
      }

      // Draw the cable core
      drawCore(buffer, matrix, mask, Direction.WEST, Direction.DOWN, Direction.Axis.Z)
      drawCore(buffer, matrix, mask, Direction.WEST, Direction.UP, Direction.Axis.Z)
      drawCore(buffer, matrix, mask, Direction.EAST, Direction.DOWN, Direction.Axis.Z)
      drawCore(buffer, matrix, mask, Direction.EAST, Direction.UP, Direction.Axis.Z)

      drawCore(buffer, matrix, mask, Direction.WEST, Direction.NORTH, Direction.Axis.Y)
      drawCore(buffer, matrix, mask, Direction.WEST, Direction.SOUTH, Direction.Axis.Y)
      drawCore(buffer, matrix, mask, Direction.EAST, Direction.NORTH, Direction.Axis.Y)
      drawCore(buffer, matrix, mask, Direction.EAST, Direction.SOUTH, Direction.Axis.Y)

      drawCore(buffer, matrix, mask, Direction.DOWN, Direction.NORTH, Direction.Axis.X)
      drawCore(buffer, matrix, mask, Direction.DOWN, Direction.SOUTH, Direction.Axis.X)
      drawCore(buffer, matrix, mask, Direction.UP, Direction.NORTH, Direction.Axis.X)
      drawCore(buffer, matrix, mask, Direction.UP, Direction.SOUTH, Direction.Axis.X)
    }

    /** Draw part of the core object */
    private def drawCore(buffer: VertexConsumer, matrix: org.joml.Matrix4f, mask: Int, a: Direction, b: Direction, other: Direction.Axis): Unit = {
      if (((mask >> a.ordinal) & 1) != ((mask >> b.ordinal) & 1)) return

      val offA = if (a.getAxisDirection == Direction.AxisDirection.NEGATIVE) MIN else MAX
      val offB = if (b.getAxisDirection == Direction.AxisDirection.NEGATIVE) MIN else MAX
      drawLineAlong(buffer, matrix, other, offA, offB, MIN, MAX)
    }

    /** Draw a line parallel to an axis */
    private def drawLineAlong(buffer: VertexConsumer, matrix: org.joml.Matrix4f, axis: Direction.Axis, offA: Double, offB: Double, start: Double, end: Double): Unit = {
      axis match {
        case Direction.Axis.X =>
          buffer.vertex(matrix, start.toFloat, offA.toFloat, offB.toFloat).endVertex()
          buffer.vertex(matrix, end.toFloat, offA.toFloat, offB.toFloat).endVertex()
        case Direction.Axis.Y =>
          buffer.vertex(matrix, offA.toFloat, start.toFloat, offB.toFloat).endVertex()
          buffer.vertex(matrix, offA.toFloat, end.toFloat, offB.toFloat).endVertex()
        case Direction.Axis.Z =>
          buffer.vertex(matrix, offA.toFloat, offB.toFloat, start.toFloat).endVertex()
          buffer.vertex(matrix, offA.toFloat, offB.toFloat, end.toFloat).endVertex()
      }
    }

    /** Draw a line perpendicular to an axis */
    private def drawLineAdjacent(buffer: VertexConsumer, matrix: org.joml.Matrix4f, axis: Direction.Axis, offset: Double, startA: Double, startB: Double, endA: Double, endB: Double): Unit = {
      axis match {
        case Direction.Axis.X =>
          buffer.vertex(matrix, offset.toFloat, startA.toFloat, startB.toFloat).endVertex()
          buffer.vertex(matrix, offset.toFloat, endA.toFloat, endB.toFloat).endVertex()
        case Direction.Axis.Y =>
          buffer.vertex(matrix, startA.toFloat, offset.toFloat, startB.toFloat).endVertex()
          buffer.vertex(matrix, endA.toFloat, offset.toFloat, endB.toFloat).endVertex()
        case Direction.Axis.Z =>
          buffer.vertex(matrix, startA.toFloat, startB.toFloat, offset.toFloat).endVertex()
          buffer.vertex(matrix, endA.toFloat, endB.toFloat, offset.toFloat).endVertex()
      }
    }
  }

}
