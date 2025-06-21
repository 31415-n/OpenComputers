package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Screen
import li.cil.oc.integration.util.Wrench
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14

class ScreenRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[Screen] {
  private val maxRenderDistanceSq = Settings.get.maxScreenTextRenderDistance * Settings.get.maxScreenTextRenderDistance

  private val fadeDistanceSq = Settings.get.screenTextFadeStartDistance * Settings.get.screenTextFadeStartDistance

  private val fadeRatio = 1.0 / (maxRenderDistanceSq - fadeDistanceSq)

  private var screen: Screen = null
  private var poseStack: PoseStack = null
  private var bufferSource: MultiBufferSource = null
  private var packedLight: Int = 0
  private var packedOverlay: Int = 0

  private val canUseBlendColor = true // OpenGL 1.4 is always available in modern MC

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  override def render(screen: Screen, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    this.poseStack = poseStack
    this.bufferSource = bufferSource
    this.packedLight = packedLight
    this.packedOverlay = packedOverlay
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    this.screen = screen
    if (!screen.isOrigin) {
      return
    }

    val distance = playerDistanceSq() / math.min(screen.width, screen.height)
    if (distance > maxRenderDistanceSq) {
      return
    }

    // Get camera position for visibility check
    val cameraPos = context.getBlockEntityRenderDispatcher.camera.getPosition
    val screenPos = screen.getBlockPos
    val relativePos = cameraPos.subtract(screenPos.getX + 0.5, screenPos.getY + 0.5, screenPos.getZ + 0.5)
    
    // Crude check whether screen text can be seen by the local player based
    // on the player's position -> angle relative to screen.
    val screenFacing = screen.facing.getOpposite
    if (screenFacing.getStepX * relativePos.x + screenFacing.getStepY * relativePos.y + screenFacing.getStepZ * relativePos.z < 0) {
      return
    }

    RenderState.checkError(getClass.getName + ".render: checks")

    RenderSystem.enableBlend()
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)

    poseStack.pushPose()
    poseStack.translate(0.5, 0.5, 0.5)

    RenderState.checkError(getClass.getName + ".render: setup")

    drawOverlay()

    RenderState.checkError(getClass.getName + ".render: overlay")

    if (distance > fadeDistanceSq) {
      val alpha = math.max(0, 1 - ((distance - fadeDistanceSq) * fadeRatio).toFloat)
      if (canUseBlendColor) {
        RenderSystem.blendColor(0, 0, 0, alpha)
        RenderSystem.blendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE)
      }
    }

    RenderState.checkError(getClass.getName + ".render: fade")

    if (screen.buffer.isRenderingEnabled) {
      val profiler = Minecraft.getInstance().getProfiler
      profiler.push("opencomputers:screen_text")
      draw()
      profiler.pop()
    }

    RenderSystem.disableBlend()

    poseStack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  private def transform(): Unit = {
    screen.yaw match {
      case Direction.WEST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(-90).toFloat))
      case Direction.NORTH => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(180).toFloat))
      case Direction.EAST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(90).toFloat))
      case _ => // No yaw.
    }
    screen.pitch match {
      case Direction.DOWN => poseStack.mulPose(org.joml.Quaternionf().rotateX(Math.toRadians(90).toFloat))
      case Direction.UP => poseStack.mulPose(org.joml.Quaternionf().rotateX(Math.toRadians(-90).toFloat))
      case _ => // No pitch.
    }

    // Fit area to screen (bottom left = bottom left).
    poseStack.translate(-0.5f, -0.5f, 0.5f)
    poseStack.translate(0, screen.height, 0)

    // Flip text upside down.
    poseStack.scale(1, -1, 1)
  }

  private def isScreen(stack: ItemStack): Boolean = api.Items.get(stack) match {
    case i: ItemInfo => i.block() match {
      case _: li.cil.oc.common.block.Screen => true
      case _ => false
    }
    case _ => false
  }

  private def drawOverlay(): Unit = if (screen.facing == Direction.UP || screen.facing == Direction.DOWN) {
    // Show up vector overlay when holding same screen block.
    val stack = Minecraft.getInstance().player.getMainHandItem
    if (!stack.isEmpty) {
      if (Wrench.holdsApplicableWrench(Minecraft.getInstance().player, screen.getBlockPos) || isScreen(stack)) {
        poseStack.pushPose()
        transform()
        RenderSystem.depthMask(false)
        poseStack.translate(screen.width / 2f - 0.5f, screen.height / 2f - 0.5f, 0.05f)

        val buffer = bufferSource.getBuffer(RenderType.cutout())
        val pose = poseStack.last().pose()

        val icon = Textures.getSprite(Textures.Block.ScreenUpIndicator)
        
        buffer.vertex(pose, 0, 1, 0)
          .uv(icon.getU0, icon.getV1)
          .overlayCoords(packedOverlay)
          .uv2(packedLight)
          .normal(0.0f, 0.0f, 1.0f)
          .endVertex()
        
        buffer.vertex(pose, 1, 1, 0)
          .uv(icon.getU1, icon.getV1)
          .overlayCoords(packedOverlay)
          .uv2(packedLight)
          .normal(0.0f, 0.0f, 1.0f)
          .endVertex()
        
        buffer.vertex(pose, 1, 0, 0)
          .uv(icon.getU1, icon.getV0)
          .overlayCoords(packedOverlay)
          .uv2(packedLight)
          .normal(0.0f, 0.0f, 1.0f)
          .endVertex()
        
        buffer.vertex(pose, 0, 0, 0)
          .uv(icon.getU0, icon.getV0)
          .overlayCoords(packedOverlay)
          .uv2(packedLight)
          .normal(0.0f, 0.0f, 1.0f)
          .endVertex()

        RenderSystem.depthMask(true)
        poseStack.popPose()
      }
    }
  }

  private def draw(): Unit = {
    RenderState.checkError(getClass.getName + ".draw: entering (aka: wasntme)")

    val sx = screen.width
    val sy = screen.height
    val tw = sx * 16f
    val th = sy * 16f

    transform()

    // Offset from border.
    poseStack.translate(sx * 2.25f / tw, sy * 2.25f / th, 0)

    // Inner size (minus borders).
    val isx = sx - (4.5f / 16)
    val isy = sy - (4.5f / 16)

    // Scale based on actual buffer size.
    val sizeX = screen.buffer.renderWidth
    val sizeY = screen.buffer.renderHeight
    val scaleX = isx / sizeX
    val scaleY = isy / sizeY
    if (true) {
      if (scaleX > scaleY) {
        poseStack.translate(sizeX * 0.5f * (scaleX - scaleY), 0, 0)
        poseStack.scale(scaleY, scaleY, 1)
      }
      else {
        poseStack.translate(0, sizeY * 0.5f * (scaleY - scaleX), 0)
        poseStack.scale(scaleX, scaleX, 1)
      }
    }
    else {
      // Stretch to fit.
      poseStack.scale(scaleX, scaleY, 1)
    }

    // Slightly offset the text so it doesn't clip into the screen.
    poseStack.translate(0, 0, 0.01)

    RenderState.checkError(getClass.getName + ".draw: setup")

    // Render the actual text.
    screen.buffer.renderText()

    RenderState.checkError(getClass.getName + ".draw: text")
  }

  private def playerDistanceSq() = {
    val player = Minecraft.getInstance().player
    val bounds = screen.getRenderBoundingBox

    val px = player.getX
    val py = player.getY
    val pz = player.getZ

    val ex = bounds.maxX - bounds.minX
    val ey = bounds.maxY - bounds.minY
    val ez = bounds.maxZ - bounds.minZ
    val cx = bounds.minX + ex * 0.5
    val cy = bounds.minY + ey * 0.5
    val cz = bounds.minZ + ez * 0.5
    val dx = px - cx
    val dy = py - cy
    val dz = pz - cz

    (if (dx < -ex) {
      val d = dx + ex
      d * d
    }
    else if (dx > ex) {
      val d = dx - ex
      d * d
    }
    else 0) + (if (dy < -ey) {
      val d = dy + ey
      d * d
    }
    else if (dy > ey) {
      val d = dy - ey
      d * d
    }
    else 0) + (if (dz < -ez) {
      val d = dz + ez
      d * d
    }
    else if (dz > ez) {
      val d = dz - ez
      d * d
    }
    else 0)
  }
}

/**
 * Companion object for creating the renderer
 */
object ScreenRenderer {
  def apply(context: BlockEntityRendererProvider.Context): ScreenRenderer = new ScreenRenderer(context)
}
