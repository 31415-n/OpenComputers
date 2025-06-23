package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.common.tileentity
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu

import scala.jdk.CollectionConverters._

class Server(playerInventory: Inventory, serverInventory: ServerInventory, val rack: Option[tileentity.Rack] = None, val slot: Int = 0) extends DynamicGuiContainer(new container.Server(playerInventory, serverInventory).asInstanceOf[AbstractContainerMenu]) with traits.LockedHotbar {
  
  private def serverContainer = menu.asInstanceOf[container.Server]
  protected var powerButton: ImageButton = _

  override def lockedStack = serverInventory.container

  protected override def actionPerformed(button: Button): Unit = {
    // Button ID system changed in 1.20.1, use button instance comparison
    if (button == powerButton) {
      rack match {
        case Some(t) => ClientPacketSender.sendServerPower(t, slot, !serverContainer.isRunning)
        case _ =>
      }
    }
  }

  override def render(guiGraphics: net.minecraft.client.gui.GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float): Unit = {
    // Close GUI if item is removed from rack.
    rack match {
      case Some(t) if t.getItem(slot) != serverInventory.container =>
        Minecraft.getInstance().setScreen(null)
        return
      case _ =>
    }

    powerButton.visible = !serverContainer.isItem
    powerButton.setToggled(serverContainer.isRunning)
    super.render(guiGraphics, mouseX, mouseY, partialTick)
  }

  override def init(): Unit = {
    super.init()
    powerButton = new ImageButton(leftPos + 48, topPos + 33, 18, 18, Textures.GUI.ButtonPower, canToggle = true)
    addRenderableWidget(powerButton)
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int): Unit = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    guiGraphics.drawString(
      font,
      Localization.localizeImmediately(serverInventory.getDisplayName.getString),
      8, 6, 0x404040)
    if (powerButton.isMouseOver(mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[net.minecraft.network.chat.Component]
      val lines = if (serverContainer.isRunning) Localization.Computer.TurnOff.lines else Localization.Computer.TurnOn.lines
      import scala.jdk.CollectionConverters._
      lines.iterator().asScala.foreach(line => tooltip.add(net.minecraft.network.chat.Component.literal(line)))
      guiGraphics.renderComponentTooltip(font, tooltip, mouseX - leftPos, mouseY - topPos)
    }
  }

  override def drawSecondaryBackgroundLayer(): Unit = {
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    guiGraphics.blit(Textures.GUI.Server, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }
}
