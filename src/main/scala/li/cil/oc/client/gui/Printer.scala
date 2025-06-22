package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.common.container
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.world.entity.player.Inventory
import net.minecraft.client.gui.GuiGraphics

class Printer(playerInventory: Inventory, val printer: tileentity.Printer) extends DynamicGuiContainer(new container.Printer(playerInventory, printer)) {
  imageWidth = 176
  imageHeight = 166

  private val materialBar = addWidget(new ProgressBar(40, 21) {
    override def width = 62

    override def height = 12

    override def barTexture = Textures.GUI.PrinterMaterial
  })
  private val inkBar = addWidget(new ProgressBar(40, 53) {
    override def width = 62

    override def height = 12

    override def barTexture = Textures.GUI.PrinterInk
  })
  private val progressBar = addWidget(new ProgressBar(105, 20) {
    override def width = 46

    override def height = 46

    override def barTexture = Textures.GUI.PrinterProgress
  })

  override def init() {
    super.init()
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    guiGraphics.drawString(font,
      Localization.localizeImmediately("tile.oc.printer.name"),
      8, 6, 0x404040)
    RenderState.pushAttrib()
    if (isHovering(materialBar.x, materialBar.y, materialBar.width, materialBar.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.add(menu.asInstanceOf[container.Printer].amountMaterial + "/" + printer.maxAmountMaterial)
      copiedDrawHoveringText(guiGraphics, tooltip, mouseX - leftPos, mouseY - topPos, font)
    }
    if (isHovering(inkBar.x, inkBar.y, inkBar.width, inkBar.height, mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.add(menu.asInstanceOf[container.Printer].amountInk + "/" + printer.maxAmountInk)
      copiedDrawHoveringText(guiGraphics, tooltip, mouseX - leftPos, mouseY - topPos, font)
    }
    RenderState.popAttrib()
  }

  override def renderBg(guiGraphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    Textures.bind(Textures.GUI.Printer)
    guiGraphics.blit(Textures.GUI.Printer, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    val printerContainer = menu.asInstanceOf[container.Printer]
    materialBar.level = printerContainer.amountMaterial / printer.maxAmountMaterial.toDouble
    inkBar.level = printerContainer.amountInk / printer.maxAmountInk.toDouble
    progressBar.level = printerContainer.progress
    renderWidgets(guiGraphics)
    drawInventorySlots(guiGraphics)
  }

  override protected def drawDisabledSlot(guiGraphics: GuiGraphics, slot: ComponentSlot) {}
}
