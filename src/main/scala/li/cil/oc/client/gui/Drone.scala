package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.font.TextBufferRenderData
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.entity
import li.cil.oc.util.PackedColor
import li.cil.oc.util.RenderState
import li.cil.oc.util.TextBuffer
import net.minecraft.client.gui.components.Button
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.world.entity.player.Inventory
import org.lwjgl.opengl.GL11

import scala.jdk.CollectionConverters._

class Drone(playerInventory: Inventory, val drone: entity.Drone) extends DynamicGuiContainer(new container.Drone(playerInventory, drone)) with traits.DisplayBuffer {
  imageWidth = 176
  imageHeight = 148

  protected var powerButton: ImageButton = _

  private val buffer = new TextBuffer(20, 2, new PackedColor.SingleBitFormat(0x33FF33))
  private val bufferRenderer = new TextBufferRenderData {
    private var _dirty = true

    override def dirty = _dirty

    override def dirty_=(value: Boolean) = _dirty = value

    override def data = buffer

    override def viewport: (Int, Int) = buffer.size
  }

  override protected val bufferX = 9
  override protected val bufferY = 9
  override protected val bufferColumns = 80
  override protected val bufferRows = 16

  private val inventoryX = 97
  private val inventoryY = 7

  private val power = addWidget(new ProgressBar(28, 48))

  private val selectionSize = 20
  private val selectionsStates = 17
  private val selectionStepV = 1 / selectionsStates.toDouble

  protected def onButtonClick(button: Button): Unit = {
    if (button == powerButton) {
      ClientPacketSender.sendDronePower(drone, !drone.isRunning)
    }
  }

  override def render(guiGraphics: net.minecraft.client.gui.GuiGraphics, mouseX: Int, mouseY: Int, dt: Float) {
    powerButton.toggled = drone.isRunning
    bufferRenderer.dirty = drone.statusText.split("\n").zipWithIndex.exists {
      case (line, i) => buffer.set(0, i, line, vertical = false)
    }
    super.render(guiGraphics, mouseX, mouseY, dt)
  }

  override def init() {
    super.init()
    powerButton = new ImageButton(leftPos + 7, topPos + 45, 18, 18, Textures.GUI.ButtonPower, canToggle = true, onPress = _ => onButtonClick(powerButton))
    addRenderableWidget(powerButton)
  }

  override protected def drawBuffer() {
    RenderSystem.getModelViewStack().pushPose()
    RenderSystem.getModelViewStack().translate(bufferX, bufferY, 0)
    RenderState.disableEntityLighting()
    RenderState.makeItBlend()
    RenderSystem.getModelViewStack().scale(scale.toFloat, scale.toFloat, 1.0f)
    RenderState.pushAttrib()
    RenderSystem.depthMask(false)
    RenderSystem.setShaderColor(0.5f, 0.5f, 1f, 1.0f)
    TextBufferRenderCache.render(bufferRenderer)
    RenderState.popAttrib()
  }

  override protected def changeSize(w: Double, h: Double, recompile: Boolean) = 2.0

  override protected def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) {
    drawBufferLayer()
    RenderState.pushAttrib()
    if (isHovering(power.x, power.y, power.width, power.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      val format = Localization.Computer.Power + ": %d%% (%d/%d)"
      tooltip.add(format.format(
        drone.globalBuffer * 100 / math.max(drone.globalBufferSize, 1),
        drone.globalBuffer,
        drone.globalBufferSize))
      copiedDrawHoveringText(guiGraphics, tooltip, mouseX - leftPos, mouseY - topPos, font)
    }
    if (powerButton.isMouseOver(mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.add(if (drone.isRunning) Localization.Computer.TurnOff else Localization.Computer.TurnOn)
      copiedDrawHoveringText(guiGraphics, tooltip, mouseX - leftPos, mouseY - topPos, font)
    }
    RenderState.popAttrib()
  }

  override protected def renderBg(guiGraphics: net.minecraft.client.gui.GuiGraphics, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    Textures.bind(Textures.GUI.Drone)
    guiGraphics.blit(Textures.GUI.Drone, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    power.level = drone.globalBuffer.toDouble / math.max(drone.globalBufferSize.toDouble, 1.0)
    renderWidgets(guiGraphics)
    if (drone.mainInventory.getContainerSize > 0) {
      drawSelection(guiGraphics)
    }

    drawInventorySlots(guiGraphics)
  }

  // No custom slots, we just extend DynamicGuiContainer for the highlighting.
  override protected def drawSlotBackground(guiGraphics: net.minecraft.client.gui.GuiGraphics, x: Int, y: Int) {}

  private def drawSelection(guiGraphics: net.minecraft.client.gui.GuiGraphics) {
    val slot = drone.selectedSlot
    if (slot >= 0 && slot < 16) {
      RenderState.makeItBlend()
      Textures.bind(Textures.GUI.RobotSelection)
      val now = System.currentTimeMillis() / 1000.0
      val offsetV = ((now - now.toInt) * selectionsStates).toInt * selectionStepV
      val x = leftPos + inventoryX - 1 + (slot % 4) * (selectionSize - 2)
      val y = topPos + inventoryY - 1 + (slot / 4) * (selectionSize - 2)

      val t = Tesselator.getInstance
      val r = t.getBuilder
      r.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
      r.vertex(x, y, 0).uv(0, offsetV.toFloat).endVertex()
      r.vertex(x, y + selectionSize, 0).uv(0, (offsetV + selectionStepV).toFloat).endVertex()
      r.vertex(x + selectionSize, y + selectionSize, 0).uv(1, (offsetV + selectionStepV).toFloat).endVertex()
      r.vertex(x + selectionSize, y, 0).uv(1, offsetV.toFloat).endVertex()
      t.end()
    }
  }
}
