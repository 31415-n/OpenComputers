package li.cil.oc.client.renderer.tileentity

import com.google.common.base.Strings
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.driver.item.UpgradeRenderer
import li.cil.oc.api.driver.item.UpgradeRenderer.MountPointName
import li.cil.oc.api.event.RobotRenderEvent
import li.cil.oc.client.Textures
import li.cil.oc.common.EventHandler
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.core.Direction
import net.minecraft.world.item.{ItemStack, Items}
import net.minecraft.world.phys.Vec3
import net.minecraft.ChatFormatting
import net.minecraftforge.common.MinecraftForge
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11

import scala.jdk.CollectionConverters._
import scala.collection.mutable
import scala.language.implicitConversions

/**
 * Robot block entity renderer for 1.20.1.
 * Ported from 1.12.2 TileEntitySpecialRenderer system to modern BlockEntityRenderer.
 * 
 * Maintains full functionality from 1.12.2:
 * - Robot chassis rendering with animation
 * - Item rendering in robot hand
 * - Upgrade rendering on mount points
 * - Name tag rendering
 * - Movement and turn animations
 * - Light effects when running
 * - Hover animation
 */
class RobotRendererNew(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[tileentity.RobotProxy] {

  private val mountPoints = new Array[RobotRenderEvent.MountPoint](7)

  private val slotNameMapping = Map(
    UpgradeRenderer.MountPointName.TopLeft -> 0,
    UpgradeRenderer.MountPointName.TopRight -> 1,
    UpgradeRenderer.MountPointName.TopBack -> 2,
    UpgradeRenderer.MountPointName.BottomLeft -> 3,
    UpgradeRenderer.MountPointName.BottomRight -> 4,
    UpgradeRenderer.MountPointName.BottomBack -> 5,
    UpgradeRenderer.MountPointName.BottomFront -> 6
  )

  for ((name, index) <- slotNameMapping) {
    mountPoints(index) = new RobotRenderEvent.MountPoint(name)
  }

  private val size = 0.4f
  private val l = 0.5f - size
  private val h = 0.5f + size
  private val gap = 1.0f / 28.0f
  private val gt = 0.5f + gap
  private val gb = 0.5f - gap

  /**
   * Draws the top part of the robot chassis
   */
  private def drawTop(buffer: VertexConsumer, pose: Matrix4f, packedLight: Int, packedOverlay: Int): Unit = {
    // Triangle fan for top
    val centerX = 0.5f
    val centerY = 1f
    val centerZ = 0.5f
    val centerU = 0.25f
    val centerV = 0.25f
    
    // Center vertex
    addVertexWithNormal(buffer, pose, centerX, centerY, centerZ, centerU, centerV, 0, 0.2f, 1, packedLight, packedOverlay)
    
    // Surrounding vertices
    addVertexWithNormal(buffer, pose, l, gt, h, 0, 0.5f, 0, 0.2f, 1, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, h, gt, h, 0.5f, 0.5f, 0, 0.2f, 1, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, h, gt, l, 0.5f, 0, 1, 0.2f, 0, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, l, gt, l, 0, 0, 0, 0.2f, -1, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, l, gt, h, 0, 0.5f, -1, 0.2f, 0, packedLight, packedOverlay)

    // Top face quad
    addVertexWithNormal(buffer, pose, l, gt, h, 0, 1, 0, -1, 0, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, l, gt, l, 0, 0.5f, 0, -1, 0, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, h, gt, l, 0.5f, 0.5f, 0, -1, 0, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, h, gt, h, 0.5f, 1, 0, -1, 0, packedLight, packedOverlay)
  }

  /**
   * Draws the bottom part of the robot chassis
   */
  private def drawBottom(buffer: VertexConsumer, pose: Matrix4f, packedLight: Int, packedOverlay: Int): Unit = {
    // Triangle fan for bottom
    val centerX = 0.5f
    val centerY = 0.03f
    val centerZ = 0.5f
    val centerU = 0.75f
    val centerV = 0.25f
    
    // Center vertex
    addVertexWithNormal(buffer, pose, centerX, centerY, centerZ, centerU, centerV, 0, -0.2f, 1, packedLight, packedOverlay)
    
    // Surrounding vertices
    addVertexWithNormal(buffer, pose, l, gb, l, 0.5f, 0, 0, -0.2f, 1, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, h, gb, l, 1, 0, 0, -0.2f, 1, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, h, gb, h, 1, 0.5f, 1, -0.2f, 0, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, l, gb, h, 0.5f, 0.5f, 0, -0.2f, -1, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, l, gb, l, 0.5f, 0, -1, -0.2f, 0, packedLight, packedOverlay)

    // Bottom face quad
    addVertexWithNormal(buffer, pose, l, gb, l, 0, 0.5f, 0, 1, 0, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, l, gb, h, 0, 1, 0, 1, 0, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, h, gb, h, 0.5f, 1, 0, 1, 0, packedLight, packedOverlay)
    addVertexWithNormal(buffer, pose, h, gb, l, 0.5f, 0.5f, 0, 1, 0, packedLight, packedOverlay)
  }

  /**
   * Helper method to add vertex with normal
   */
  private def addVertexWithNormal(buffer: VertexConsumer, pose: Matrix4f, x: Float, y: Float, z: Float, u: Float, v: Float, nx: Float, ny: Float, nz: Float, packedLight: Int, packedOverlay: Int): Unit = {
    buffer.vertex(pose, x, y, z)
      .uv(u, v)
      .overlayCoords(packedOverlay)
      .uv2(packedLight)
      .normal(nx, ny, nz)
      .endVertex()
  }

  /**
   * Resets mount points for upgrade rendering
   */
  def resetMountPoints(running: Boolean): Unit = {
    val offset = if (running) 0 else -0.06f

    // Left top.
    mountPoints(0).offset.setX(0)
    mountPoints(0).offset.setY(0.2f)
    mountPoints(0).offset.setZ(0.24f)
    mountPoints(0).rotation.setX(0)
    mountPoints(0).rotation.setY(1)
    mountPoints(0).rotation.setZ(0)
    mountPoints(0).rotation.setW(90)

    // Right top.
    mountPoints(1).offset.setX(0)
    mountPoints(1).offset.setY(0.2f)
    mountPoints(1).offset.setZ(0.24f)
    mountPoints(1).rotation.setX(0)
    mountPoints(1).rotation.setY(1)
    mountPoints(1).rotation.setZ(0)
    mountPoints(1).rotation.setW(-90)

    // Back top.
    mountPoints(2).offset.setX(0)
    mountPoints(2).offset.setY(0.2f)
    mountPoints(2).offset.setZ(0.24f)
    mountPoints(2).rotation.setX(0)
    mountPoints(2).rotation.setY(1)
    mountPoints(2).rotation.setZ(0)
    mountPoints(2).rotation.setW(180)

    // Left bottom.
    mountPoints(3).offset.setX(0)
    mountPoints(3).offset.setY(-0.2f - offset)
    mountPoints(3).offset.setZ(0.24f)
    mountPoints(3).rotation.setX(0)
    mountPoints(3).rotation.setY(1)
    mountPoints(3).rotation.setZ(0)
    mountPoints(3).rotation.setW(90)

    // Right bottom.
    mountPoints(4).offset.setX(0)
    mountPoints(4).offset.setY(-0.2f - offset)
    mountPoints(4).offset.setZ(0.24f)
    mountPoints(4).rotation.setX(0)
    mountPoints(4).rotation.setY(1)
    mountPoints(4).rotation.setZ(0)
    mountPoints(4).rotation.setW(-90)

    // Back bottom.
    mountPoints(5).offset.setX(0)
    mountPoints(5).offset.setY(-0.2f - offset)
    mountPoints(5).offset.setZ(0.24f)
    mountPoints(5).rotation.setX(0)
    mountPoints(5).rotation.setY(1)
    mountPoints(5).rotation.setZ(0)
    mountPoints(5).rotation.setW(180)

    // Front bottom.
    mountPoints(6).offset.setX(0)
    mountPoints(6).offset.setY(-0.2f - offset)
    mountPoints(6).offset.setZ(0.24f)
    mountPoints(6).rotation.setX(0)
    mountPoints(6).rotation.setY(1)
    mountPoints(6).rotation.setZ(0)
    mountPoints(6).rotation.setW(0)
  }

  /**
   * Renders the robot chassis with all effects
   */
  def renderChassis(poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int, robot: tileentity.Robot = null, offset: Double = 0, isRunningOverride: Boolean = false): Unit = {
    val isRunning = if (robot == null) isRunningOverride else robot.isRunning

    val size = 0.3f
    val l = 0.5f - size
    val h = 0.5f + size
    val vStep = 1.0f / 32.0f

    val offsetV = ((offset - offset.toInt) * 16).toInt * vStep
    val (u0, u1, v0, v1) = {
      if (isRunning)
        (0.5f, 1f, 0.5f + offsetV, 0.5f + vStep + offsetV)
      else
        (0.25f - vStep, 0.25f + vStep, 0.75f - vStep, 0.75f + vStep)
    }

    resetMountPoints(robot != null && robot.isRunning)
    val event = new RobotRenderEvent(robot, mountPoints)
    MinecraftForge.EVENT_BUS.post(event)
    if (!event.isCanceled) {
      RenderSystem.setShaderTexture(0, Textures.Model.Robot)
      
      val buffer = bufferSource.getBuffer(RenderType.entityCutout(Textures.Model.Robot))
      val pose = poseStack.last().pose()
      
      if (!isRunning) {
        poseStack.translate(0, -2 * gap, 0)
      }
      
      drawBottom(buffer, pose, packedLight, packedOverlay)
      
      if (!isRunning) {
        poseStack.translate(0, -2 * gap, 0)
      }

      drawTop(buffer, pose, packedLight, packedOverlay)
      RenderSystem.setShaderColor(1, 1, 1, 1)

      if (isRunning) {
        // Additive blending for the light.
        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
        
        // Light color.
        val lightColor = if (robot != null && robot.info != null) robot.info.lightColor else 0xF23030
        val r = (lightColor >>> 16) & 0xFF
        val g = (lightColor >>> 8) & 0xFF
        val b = (lightColor >>> 0) & 0xFF
        RenderSystem.setShaderColor(r / 255f, g / 255f, b / 255f, 1.0f)

        val lightBuffer = bufferSource.getBuffer(RenderType.entityTranslucent(Textures.Model.Robot))
        
        // Render light effect on sides
        addVertexWithNormal(lightBuffer, pose, l, gt, l, u0, v0, -1, 0, 0, packedLight, packedOverlay)
        addVertexWithNormal(lightBuffer, pose, l, gb, l, u0, v1, -1, 0, 0, packedLight, packedOverlay)
        addVertexWithNormal(lightBuffer, pose, l, gb, h, u1, v1, -1, 0, 0, packedLight, packedOverlay)
        addVertexWithNormal(lightBuffer, pose, l, gt, h, u1, v0, -1, 0, 0, packedLight, packedOverlay)

        addVertexWithNormal(lightBuffer, pose, l, gt, h, u0, v0, 0, 0, 1, packedLight, packedOverlay)
        addVertexWithNormal(lightBuffer, pose, l, gb, h, u0, v1, 0, 0, 1, packedLight, packedOverlay)
        addVertexWithNormal(lightBuffer, pose, h, gb, h, u1, v1, 0, 0, 1, packedLight, packedOverlay)
        addVertexWithNormal(lightBuffer, pose, h, gt, h, u1, v0, 0, 0, 1, packedLight, packedOverlay)

        addVertexWithNormal(lightBuffer, pose, h, gt, h, u0, v0, 1, 0, 0, packedLight, packedOverlay)
        addVertexWithNormal(lightBuffer, pose, h, gb, h, u0, v1, 1, 0, 0, packedLight, packedOverlay)
        addVertexWithNormal(lightBuffer, pose, h, gb, l, u1, v1, 1, 0, 0, packedLight, packedOverlay)
        addVertexWithNormal(lightBuffer, pose, h, gt, l, u1, v0, 1, 0, 0, packedLight, packedOverlay)

        addVertexWithNormal(lightBuffer, pose, h, gt, l, u0, v0, 0, 0, -1, packedLight, packedOverlay)
        addVertexWithNormal(lightBuffer, pose, h, gb, l, u0, v1, 0, 0, -1, packedLight, packedOverlay)
        addVertexWithNormal(lightBuffer, pose, l, gb, l, u1, v1, 0, 0, -1, packedLight, packedOverlay)
        addVertexWithNormal(lightBuffer, pose, l, gt, l, u1, v0, 0, 0, -1, packedLight, packedOverlay)

        RenderSystem.disableBlend()
        RenderSystem.setShaderColor(1, 1, 1, 1)
      }
    }
  }

  override def render(proxy: tileentity.RobotProxy, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    val robot = proxy.robot
    val worldTime = robot.getLevel.getGameTime + partialTick

    poseStack.pushPose()
    poseStack.translate(0.5, 0.5, 0.5)

    // If the move started while we were rendering and we have a reference to
    // the *old* proxy the robot would be rendered at the wrong position, so we
    // correct for the offset.
    if (robot.proxy != proxy) {
      poseStack.translate(robot.proxy.getBlockPos.getX - proxy.getBlockPos.getX, robot.proxy.getBlockPos.getY - proxy.getBlockPos.getY, robot.proxy.getBlockPos.getZ - proxy.getBlockPos.getZ)
    }

    if (robot.isAnimatingMove) {
      val remaining = (robot.animationTicksLeft - partialTick) / robot.animationTicksTotal.toDouble
      val delta = robot.moveFrom.get.subtract(robot.getBlockPos)
      poseStack.translate(delta.getX * remaining, delta.getY * remaining, delta.getZ * remaining)
    }

    val timeJitter = robot.hashCode ^ 0xFF
    val hover =
      if (robot.isRunning) (Math.sin(timeJitter + worldTime / 20.0) * 0.03).toFloat
      else -0.03f
    poseStack.translate(0, hover, 0)

    poseStack.pushPose()

    RenderSystem.enableDepthTest()

    if (robot.isAnimatingTurn) {
      val remaining = (robot.animationTicksLeft - partialTick) / robot.animationTicksTotal.toFloat
      poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(90 * remaining * robot.turnAxis).toFloat))
    }

    robot.yaw match {
      case Direction.WEST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(-90).toFloat))
      case Direction.NORTH => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(180).toFloat))
      case Direction.EAST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(90).toFloat))
      case _ => // No yaw.
    }

    poseStack.translate(-0.5f, -0.5f, -0.5f)

    val offset = timeJitter + worldTime / 20.0
    renderChassis(poseStack, bufferSource, packedLight, packedOverlay, robot, offset)

    // Render held item
    val cameraPos = context.getBlockEntityRenderDispatcher.camera.getPosition
    val robotPos = robot.getBlockPos
    val distanceSq = cameraPos.distanceToSqr(robotPos.getX + 0.5, robotPos.getY + 0.5, robotPos.getZ + 0.5)
    
    if (!robot.renderingErrored && distanceSq < 24 * 24) {
      val itemRenderer = Minecraft.getInstance().getItemRenderer
      StackOption(robot.getItem(0)) match {
        case SomeStack(stack) =>
          poseStack.pushPose()
          try {
            // Copy-paste from player render code, with minor adjustments for robot scale.
            poseStack.scale(1, -1, -1)
            poseStack.translate(0, -8 * 0.0625F - 0.0078125F, -0.5F)

            if (robot.isAnimatingSwing) {
              val wantedTicksPerCycle = 10
              val cycles = math.max(robot.animationTicksTotal / wantedTicksPerCycle, 1)
              val ticksPerCycle = robot.animationTicksTotal / cycles
              val remaining = (robot.animationTicksLeft - partialTick) / ticksPerCycle.toDouble
              poseStack.mulPose(org.joml.Quaternionf().rotateX(Math.toRadians(Math.sin((remaining - remaining.toInt) * Math.PI) * 45).toFloat))
            }

            val item = stack.getItem
            if (item.canFitInsideContainerItems()) {
              poseStack.mulPose(org.joml.Quaternionf().rotateX(Math.toRadians(-90).toFloat))
              poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(180).toFloat))
              val scale = 0.625F
              poseStack.scale(scale, scale, scale)
            }
            else if (item == Items.BOW) {
              poseStack.translate(1.5f/16f, -0.125F, -0.125F)
              poseStack.mulPose(org.joml.Quaternionf().rotateZ(Math.toRadians(10).toFloat))
              val scale = 0.625F
              poseStack.scale(scale, -scale, scale)
            }
            else {
              poseStack.translate(0.0F, 0.1875F, 0.0F)
              poseStack.translate(0.0625F, -0.125F, -2/16F)
              val scale = 0.625F
              poseStack.scale(scale, -scale, scale)
            }

            itemRenderer.renderStatic(stack, net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, packedLight, packedOverlay, poseStack, bufferSource, robot.getLevel, 0)
          }
          catch {
            case e: Throwable =>
              OpenComputers.log.warn("Failed rendering equipped item.", e)
              robot.renderingErrored = true
          }
          poseStack.popPose()
        case _ =>
      }

      // Render upgrades
      lazy val availableSlots = slotNameMapping.keys.to[mutable.Set]
      lazy val wildcardRenderers = mutable.Buffer.empty[(ItemStack, UpgradeRenderer)]
      lazy val slotMapping = Array.fill(mountPoints.length)(null: (ItemStack, UpgradeRenderer))

      val renderers = (robot.componentSlots ++ robot.containerSlots).map(robot.getItem).
        collect { case stack if !stack.isEmpty && stack.getItem.isInstanceOf[UpgradeRenderer] => (stack, stack.getItem.asInstanceOf[UpgradeRenderer]) }

      for ((stack, renderer) <- renderers) {
        val preferredSlot = renderer.computePreferredMountPoint(stack, robot, availableSlots)
        if (availableSlots.remove(preferredSlot)) {
          slotMapping(slotNameMapping(preferredSlot)) = (stack, renderer)
        }
        else if (preferredSlot == MountPointName.Any) {
          wildcardRenderers += ((stack, renderer))
        }
      }

      var firstEmpty = slotMapping.indexOf(null)
      for (entry <- wildcardRenderers if firstEmpty >= 0) {
        slotMapping(firstEmpty) = entry
        firstEmpty = slotMapping.indexOf(null)
      }

      for ((info, mountPoint) <- (slotMapping, mountPoints).zipped if info != null) try {
        val (stack, renderer) = info
        poseStack.pushPose()
        poseStack.translate(0.5f, 0.5f, 0.5f)
        renderer.render(stack, mountPoint, robot, partialTick)
        poseStack.popPose()
      }
      catch {
        case e: Throwable =>
          OpenComputers.log.warn("Failed rendering equipped upgrade.", e)
          robot.renderingErrored = true
      }
    }
    poseStack.popPose()

    // Render name tag
    val name = robot.name
    if (Settings.get.robotLabels && !Strings.isNullOrEmpty(name) && distanceSq < 64 * 64) {
      poseStack.pushPose()

      val font = context.getFont
      val scale = 1.6f / 60f
      val width = font.width(name)
      val halfWidth = width / 2

      poseStack.translate(0, 0.8, 0)
      RenderSystem.setShaderColor(1, 1, 1, 1)

      val camera = context.getBlockEntityRenderDispatcher.camera
      poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(-camera.getYRot).toFloat))
      poseStack.mulPose(org.joml.Quaternionf().rotateX(Math.toRadians(camera.getXRot).toFloat))
      poseStack.scale(-scale, -scale, scale)

      RenderSystem.enableBlend()
      RenderSystem.depthMask(false)

      val backgroundBuffer = bufferSource.getBuffer(RenderType.gui())
      val pose = poseStack.last().pose()
      
      // Background
      backgroundBuffer.vertex(pose, -halfWidth - 1, -1, 0).color(0, 0, 0, 0.5f).endVertex()
      backgroundBuffer.vertex(pose, -halfWidth - 1, 8, 0).color(0, 0, 0, 0.5f).endVertex()
      backgroundBuffer.vertex(pose, halfWidth + 1, 8, 0).color(0, 0, 0, 0.5f).endVertex()
      backgroundBuffer.vertex(pose, halfWidth + 1, -1, 0).color(0, 0, 0, 0.5f).endVertex()

      // Text
      val displayName = (if (EventHandler.isItTime) ChatFormatting.OBFUSCATED.toString else "") + name
      font.drawInBatch(displayName, -halfWidth, 0, 0xFFFFFFFF, false, pose, bufferSource, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, packedLight)

      RenderSystem.depthMask(true)
      RenderSystem.disableBlend()

      poseStack.popPose()
    }

    poseStack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}

/**
 * Companion object for creating the renderer
 */
object RobotRendererNew {
  def apply(context: BlockEntityRendererProvider.Context): RobotRendererNew = new RobotRendererNew(context)
}