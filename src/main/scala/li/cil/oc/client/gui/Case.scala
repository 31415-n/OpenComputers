package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.player.Inventory
import net.minecraft.client.gui.Font

import scala.jdk.CollectionConverters._

class Case(playerInventory: Inventory, val computer: tileentity.Case) extends DynamicGuiContainer(new container.Case(playerInventory, computer)) {
  protected var powerButton: ImageButton = _

  protected def onButtonClick(button: Button): Unit = {
    // Button ID handling changed in 1.20.1, we'll use instance comparison
    if (button == powerButton) {
      ClientPacketSender.sendComputerPower(computer, !computer.isRunning)
    }
  }

  override def render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    powerButton.toggled = computer.isRunning
    super.render(guiGraphics, mouseX, mouseY, dt)
  }

  override def init(): Unit = {
    super.init()
    powerButton = new ImageButton(leftPos + 70, topPos + 33, 18, 18, Textures.GUI.ButtonPower, canToggle = true, onPress = _ => onButtonClick(powerButton))
    addRenderableWidget(powerButton)
  }

  override protected def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    guiGraphics.drawString(font,
      Localization.localizeImmediately(computer.getDisplayName.getString),
      8, 6, 0x404040)
    if (powerButton.isMouseOver(mouseX, mouseY)) {
      val tooltipText = if (computer.isRunning) Localization.Computer.TurnOff else Localization.Computer.TurnOn
      guiGraphics.renderTooltip(font, net.minecraft.network.chat.Component.literal(tooltipText), mouseX - leftPos, mouseY - topPos)
    }
  }

  override def drawSecondaryBackgroundLayer(guiGraphics: GuiGraphics): Unit = {
    Textures.bind(Textures.GUI.Computer)
    guiGraphics.blit(Textures.GUI.Computer, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }
}
