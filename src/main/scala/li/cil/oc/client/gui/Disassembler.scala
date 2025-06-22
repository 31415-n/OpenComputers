package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.world.entity.player.Inventory

class Disassembler(playerInventory: Inventory, val disassembler: tileentity.Disassembler) extends DynamicGuiContainer(new container.Disassembler(playerInventory, disassembler)) {
  val progress = addWidget(new ProgressBar(18, 65))

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    guiGraphics.drawString(font,
      Localization.localizeImmediately(disassembler.getDisplayName.getString),
      8, 6, 0x404040)
  }

  override def renderBg(guiGraphics: net.minecraft.client.gui.GuiGraphics, dt: Float, mouseX: Int, mouseY: Int) {
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    Textures.bind(Textures.GUI.Disassembler)
    guiGraphics.blit(Textures.GUI.Disassembler, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    
    // Get actual progress from container
    val container = inventoryContainer.asInstanceOf[li.cil.oc.common.container.Disassembler]
    progress.level = container.disassemblyProgress / 100.0
    
    // Draw progress bar widget
    progress.render(guiGraphics)
  }
}
