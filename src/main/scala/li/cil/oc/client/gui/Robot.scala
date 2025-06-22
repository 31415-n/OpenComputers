package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.internal.TextBuffer
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import li.cil.oc.integration.opencomputers
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.components.Button
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.world.entity.player.Inventory
import org.lwjgl.glfw.GLFW
import net.minecraft.client.gui.GuiGraphics

import scala.jdk.CollectionConverters._

class Robot(playerInventory: Inventory, val robot: tileentity.Robot) extends DynamicGuiContainer(new container.Robot(playerInventory, robot)) with traits.InputBuffer {
  override protected val buffer: TextBuffer = robot.components.collect {
    case Some(buffer: api.internal.TextBuffer) => buffer
  }.headOption.orNull

  override protected val hasKeyboard: Boolean = robot.info.components.map(api.Driver.driverFor(_, robot.getClass)).contains(opencomputers.DriverKeyboard)

  private val withScreenHeight = 256
  private val noScreenHeight = 108

  private val deltaY = if (buffer != null) 0 else withScreenHeight - noScreenHeight

  imageWidth = 256
  imageHeight = 256 - deltaY

  protected var powerButton: ImageButton = _

  protected var scrollButton: ImageButton = _

  // Scroll offset for robot inventory.
  private var inventoryOffset = 0
  private var isDragging = false

  private def canScroll = robot.inventorySize > 16

  private def maxOffset = robot.inventorySize / 4 - 4

  private val slotSize = 18

  private val maxBufferWidth = 240.0
  private val maxBufferHeight = 140.0

  private def bufferRenderWidth = math.min(maxBufferWidth, TextBufferRenderCache.renderer.charRenderWidth * Settings.screenResolutionsByTier(0)._1)

  private def bufferRenderHeight = math.min(maxBufferHeight, TextBufferRenderCache.renderer.charRenderHeight * Settings.screenResolutionsByTier(0)._2)

  override protected def bufferX: Int = (8 + (maxBufferWidth - bufferRenderWidth) / 2).toInt

  override protected def bufferY: Int = (8 + (maxBufferHeight - bufferRenderHeight) / 2).toInt

  private val inventoryX = 169
  private val inventoryY = 155 - deltaY

  private val scrollX = inventoryX + slotSize * 4 + 2
  private val scrollY = inventoryY
  private val scrollWidth = 8
  private val scrollHeight = 94

  private val power = addWidget(new ProgressBar(26, 156 - deltaY))

  private val selectionSize = 20
  private val selectionsStates = 17
  private val selectionStepV = 1 / selectionsStates.toDouble

  // Button handling for 1.20.1 - using button reference comparison
  private var buttonToActionMap = Map.empty[Button, () => Unit]
  
  protected def onButtonClick(button: Button): Unit = {
    buttonToActionMap.get(button).foreach(_.apply())
  }

  override def render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    powerButton.toggled = robot.isRunning
    scrollButton.enabled = canScroll
    scrollButton.hoverOverride = isDragging
    if (robot.inventorySize < 16 + inventoryOffset * 4) {
      scrollTo(0)
    }
    super.render(guiGraphics, mouseX, mouseY, dt)
  }

  override def init(): Unit = {
    super.init()
    powerButton = new ImageButton(leftPos + 5, topPos + 153 - deltaY, 18, 18, Textures.GUI.ButtonPower, "", canToggle = true, onPress = btn => onButtonClick(btn))
    scrollButton = new ImageButton(leftPos + scrollX + 1, topPos + scrollY + 1, 6, 13, Textures.GUI.ButtonScroll, "", onPress = btn => onButtonClick(btn))
    addRenderableWidget(powerButton)
    addRenderableWidget(scrollButton)
    
    // Set up button actions
    buttonToActionMap += (powerButton -> (() => ClientPacketSender.sendComputerPower(robot, !robot.isRunning)))
    buttonToActionMap += (scrollButton -> (() => {})) // Scroll button doesn't need action
  }

  override def drawBuffer(): Unit = {
    if (buffer != null) {
      guiGraphics.pose().pushPose()
      guiGraphics.pose().translate(bufferX.toFloat, bufferY.toFloat, 0)
      RenderState.disableEntityLighting()
      guiGraphics.pose().pushPose()
      guiGraphics.pose().translate(-3.0f, -3.0f, 0)
      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
      BufferRenderer.drawBackground()
      guiGraphics.pose().popPose()
      RenderState.makeItBlend()
      val scaleX = bufferRenderWidth / buffer.renderWidth
      val scaleY = bufferRenderHeight / buffer.renderHeight
      val scale = math.min(scaleX, scaleY)
      if (scaleX > scale) {
        guiGraphics.pose().translate((buffer.renderWidth * (scaleX - scale) / 2).toFloat, 0, 0)
      }
      else if (scaleY > scale) {
        guiGraphics.pose().translate(0, (buffer.renderHeight * (scaleY - scale) / 2).toFloat, 0)
      }
      guiGraphics.pose().scale(scale.toFloat, scale.toFloat, scale.toFloat)
      guiGraphics.pose().scale(this.scale.toFloat, this.scale.toFloat, 1.0f)
      BufferRenderer.drawText(buffer)
      guiGraphics.pose().popPose()
    }
  }

  override protected def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int): Unit = {
    drawBufferLayer()
    RenderState.pushAttrib()
    if (isMouseOverWidget(power, mouseX, mouseY)) {
      val format = Localization.Computer.Power + ": %d%% (%d/%d)"
      val tooltipText = format.format(
        ((robot.globalBuffer / robot.globalBufferSize) * 100).toInt,
        robot.globalBuffer.toInt,
        robot.globalBufferSize.toInt)
      guiGraphics.renderTooltip(font, net.minecraft.network.chat.Component.literal(tooltipText), mouseX - leftPos, mouseY - topPos)
    }
    if (powerButton.isMouseOver(mouseX, mouseY)) {
      val tooltipText = if (robot.isRunning) Localization.Computer.TurnOff else Localization.Computer.TurnOn
      guiGraphics.renderTooltip(font, net.minecraft.network.chat.Component.literal(tooltipText), mouseX - leftPos, mouseY - topPos)
    }
    RenderState.popAttrib()
  }
  
  private def isMouseOverWidget(widget: ProgressBar, mouseX: Int, mouseY: Int): Boolean = {
    mouseX >= leftPos + widget.x && mouseX < leftPos + widget.x + widget.width &&
    mouseY >= topPos + widget.y && mouseY < topPos + widget.y + widget.height
  }

  override def renderBg(guiGraphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int): Unit = {
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    val texture = if (buffer != null) Textures.GUI.Robot else Textures.GUI.RobotNoScreen
    RenderSystem.setShaderTexture(0, texture)
    guiGraphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    drawWidgets(guiGraphics)
    if (robot.inventorySize > 0) {
      drawSelection(guiGraphics)
    }
  }

  // No custom slots, we just extend DynamicGuiContainer for the highlighting.
  override protected def drawSlotBackground(x: Int, y: Int): Unit = {}
  
  // Helper methods for widget drawing
  private def drawWidgets(guiGraphics: GuiGraphics): Unit = {
    power.level = robot.globalBuffer / robot.globalBufferSize
    power.render(guiGraphics)
  }
  


  override def keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = {
    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
      return super.keyPressed(keyCode, scanCode, modifiers)
    }
    false
  }

  override def mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    super.mouseClicked(mouseX, mouseY, button)
    if (canScroll && button == 0 && isCoordinateOverScrollBar(mouseX.toInt - leftPos, mouseY.toInt - topPos)) {
      isDragging = true
      scrollMouse(mouseY.toInt)
    }
    true
  }

  override def mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    super.mouseReleased(mouseX, mouseY, button)
    if (button == 0) {
      isDragging = false
    }
    true
  }

  override def mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = {
    super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    if (isDragging) {
      scrollMouse(mouseY.toInt)
    }
    true
  }

  private def scrollMouse(mouseY: Int): Unit = {
    scrollTo(math.round((mouseY - topPos - scrollY + 1 - 6.5) * maxOffset / (scrollHeight - 13.0)).toInt)
  }

  override def mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean = {
    val relativeX = mouseX.toInt - leftPos
    val relativeY = mouseY.toInt - topPos
    if (isCoordinateOverInventory(relativeX, relativeY) || isCoordinateOverScrollBar(relativeX, relativeY)) {
      if (delta < 0) scrollDown()
      else scrollUp()
      return true
    }
    super.mouseScrolled(mouseX, mouseY, delta)
  }

  private def isCoordinateOverInventory(x: Int, y: Int) =
    x >= inventoryX && x < inventoryX + slotSize * 4 &&
      y >= inventoryY && y < inventoryY + slotSize * 4

  private def isCoordinateOverScrollBar(x: Int, y: Int) =
    x > scrollX && x < scrollX + scrollWidth &&
      y >= scrollY && y < scrollY + scrollHeight

  private def scrollUp() = scrollTo(inventoryOffset - 1)

  private def scrollDown() = scrollTo(inventoryOffset + 1)

  private def scrollTo(row: Int): Unit = {
    inventoryOffset = math.max(0, math.min(maxOffset, row))
    
    // In 1.20.1, we need to handle scrolling differently since slot positions are final
    // The container.Robot class should handle the slot visibility logic
    // Here we just update the scroll button position and let the container handle the rest
    
    // Update scroll button position
    val yMin = topPos + scrollY + 1
    if (maxOffset > 0) {
      scrollButton.setY(yMin + (scrollHeight - 15) * inventoryOffset / maxOffset)
    }
    else {
      scrollButton.setY(yMin)
    }
  }

  override protected def changeSize(w: Double, h: Double, recompile: Boolean): Double = {
    val bw = w * TextBufferRenderCache.renderer.charRenderWidth
    val bh = h * TextBufferRenderCache.renderer.charRenderHeight
    val scaleX = math.min(bufferRenderWidth / bw, 1)
    val scaleY = math.min(bufferRenderHeight / bh, 1)
    if (recompile) {
      BufferRenderer.compileBackground(bufferRenderWidth.toInt, bufferRenderHeight.toInt, forRobot = true)
    }
    math.min(scaleX, scaleY)
  }

  private def drawSelection(guiGraphics: GuiGraphics): Unit = {
    val slot = robot.selectedSlot - inventoryOffset * 4
    if (slot >= 0 && slot < 16) {
      RenderState.makeItBlend()
      RenderSystem.setShaderTexture(0, Textures.GUI.RobotSelection)
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
