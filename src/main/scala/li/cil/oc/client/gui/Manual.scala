package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.api
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.markdown.Document
import li.cil.oc.client.renderer.markdown.segment.InteractiveSegment
import li.cil.oc.client.renderer.markdown.segment.Segment
import li.cil.oc.client.{Manual => ManualAPI}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import com.mojang.blaze3d.systems.RenderSystem
import org.lwjgl.glfw.GLFW

import scala.jdk.CollectionConverters._

/**
 * Manual GUI screen providing in-game documentation for OpenComputers.
 * Features tabbed navigation, scrolling, and interactive content rendering.
 */
class Manual extends Screen(Component.literal("OpenComputers Manual")) with traits.Window {
  final val documentMaxWidth = 230
  final val documentMaxHeight = 176
  final val scrollPosX = 244
  final val scrollPosY = 6
  final val scrollWidth = 6
  final val scrollHeight = 180
  final val tabPosX = -23
  final val tabPosY = 7
  final val tabWidth = 23
  final val tabHeight = 26
  final val maxTabsPerSide = 7

  override val windowWidth = 256
  override val windowHeight = 192

  override def backgroundImage = Textures.GUI.Manual

  var isDragging = false
  var document: Segment = null
  var documentHeight = 0
  var currentSegment = None: Option[InteractiveSegment]
  protected var scrollButton: ImageButton = _

  private def canScroll = maxOffset > 0

  def offset = ManualAPI.history.top.offset

  def maxOffset = documentHeight - documentMaxHeight

  def resolveLink(path: String, current: String): String =
    if (path.startsWith("/")) path
    else {
      val splitAt = current.lastIndexOf('/')
      if (splitAt >= 0) current.splitAt(splitAt)._1 + "/" + path
      else path
    }

  def refreshPage(): Unit = {
    val content = Option(api.Manual.contentFor(ManualAPI.history.top.path)).
      getOrElse(Iterable("Document not found: " + ManualAPI.history.top.path).asJava)
    document = Document.parse(content)
    documentHeight = Document.height(document, documentMaxWidth, font)
    scrollTo(offset)
  }

  def pushPage(path: String): Unit = {
    if (path != ManualAPI.history.top.path) {
      ManualAPI.history.push(new ManualAPI.History(path))
      refreshPage()
    }
  }

  def popPage(): Unit = {
    if (ManualAPI.history.size > 1) {
      ManualAPI.history.pop()
      refreshPage()
    }
    else {
      Minecraft.getInstance().setScreen(null)
    }
  }

  def actionPerformed(button: Button): Unit = {
    if (button.id >= 0 && button.id < ManualAPI.tabs.length) {
      api.Manual.navigate(ManualAPI.tabs(button.id).path)
    }
  }

  override def init(): Unit = {
    super.init()

    for ((tab, i) <- ManualAPI.tabs.zipWithIndex if i < maxTabsPerSide) {
      val x = guiLeft + tabPosX
      val y = guiTop + tabPosY + i * (tabHeight - 1)
      addRenderableWidget(new ImageButton(i, x, y, tabWidth, tabHeight, Textures.GUI.ManualTab, button => actionPerformed(button)))
    }

    scrollButton = new ImageButton(-1, leftPos + scrollPosX, topPos + scrollPosY, 6, 13, Textures.GUI.ButtonScroll, button => {})
    addRenderableWidget(scrollButton)

    refreshPage()
  }

  override def render(guiGraphics: net.minecraft.client.gui.GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    this.renderBackground(guiGraphics)
    super.render(guiGraphics, mouseX, mouseY, dt)

    scrollButton.active = canScroll

    for ((tab, i) <- ManualAPI.tabs.zipWithIndex if i < maxTabsPerSide) {
      val button = children().get(i).asInstanceOf[ImageButton]
      guiGraphics.pose().pushPose()
      guiGraphics.pose().translate(button.getX + 5, button.getY + 5, 0)
      tab.renderer.render()
      guiGraphics.pose().popPose()
    }

    currentSegment = Document.render(document, leftPos + 8, topPos + 8, documentMaxWidth, documentMaxHeight, offset, font, mouseX, mouseY)

    if (!isDragging) currentSegment match {
      case Some(segment) =>
        segment.tooltip match {
          case Some(text) if text.nonEmpty => guiGraphics.renderTooltip(font, Localization.localizeImmediately(text).lines.toList.asJava, mouseX, mouseY)
          case _ =>
        }
      case _ =>
    }

    if (!isDragging) for ((tab, i) <- ManualAPI.tabs.zipWithIndex if i < maxTabsPerSide) {
      val button = children().get(i).asInstanceOf[ImageButton]
      if (mouseX > button.getX && mouseX < button.getX + tabWidth && mouseY > button.getY && mouseY < button.getY + tabHeight) tab.tooltip.foreach(text => {
        guiGraphics.renderTooltip(font, Localization.localizeImmediately(text).lines.toList.asJava, mouseX, mouseY)
      })
    }

    if (canScroll && (isCoordinateOverScrollBar(mouseX - leftPos, mouseY - topPos) || isDragging)) {
      guiGraphics.renderTooltip(font, List(Component.literal(s"${100 * offset / maxOffset}%")).asJava, leftPos + scrollPosX + scrollWidth, scrollButton.getY + scrollButton.getHeight + 1)
    }
  }

  override def keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = {
    if (keyCode == minecraft.options.keyJump.getKey.getValue) {
      popPage()
      true
    }
    else if (keyCode == minecraft.options.keyInventory.getKey.getValue) {
      minecraft.setScreen(null)
      true
    }
    else super.keyPressed(keyCode, scanCode, modifiers)
  }

  override def mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean = {
    if (delta < 0) scrollDown()
    else scrollUp()
    true
  }

  override def mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    super.mouseClicked(mouseX, mouseY, button)

    if (canScroll && button == 0 && isCoordinateOverScrollBar(mouseX.toInt - leftPos, mouseY.toInt - topPos)) {
      isDragging = true
      scrollMouse(mouseY.toInt)
    }
    else if (button == 0) currentSegment.foreach(_.onMouseClick(mouseX.toInt, mouseY.toInt))
    else if (button == 1) popPage()
    true
  }

  override def mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = {
    super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    if (isDragging) {
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

  private def scrollMouse(mouseY: Int) {
    scrollTo(math.round((mouseY - topPos - scrollPosY - 6.5) * maxOffset / (scrollHeight - 13.0)).toInt)
  }

  private def scrollUp() = scrollTo(offset - Document.lineHeight(font) * 3)

  private def scrollDown() = scrollTo(offset + Document.lineHeight(font) * 3)

  private def scrollTo(row: Int): Unit = {
    ManualAPI.history.top.offset = math.max(0, math.min(maxOffset, row))
    val yMin = topPos + scrollPosY
    if (maxOffset > 0) {
      scrollButton.setY(yMin + (scrollHeight - 13) * offset / maxOffset)
    }
    else {
      scrollButton.setY(yMin)
    }
  }

  private def isCoordinateOverScrollBar(x: Int, y: Int) =
    x > scrollPosX && x < scrollPosX + scrollWidth &&
      y >= scrollPosY && y < scrollPosY + scrollHeight
}
