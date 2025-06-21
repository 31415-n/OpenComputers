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

import scala.jdk.CollectionConverters._

class Server(playerInventory: Inventory, serverInventory: ServerInventory, val rack: Option[tileentity.Rack] = None, val slot: Int = 0) extends DynamicGuiContainer(new container.Server(playerInventory, serverInventory)) with traits.LockedHotbar {
  protected var powerButton: ImageButton = _

  override def lockedStack = serverInventory.container

  protected override def actionPerformed(button: Button): Unit = {
    if (button.id == 0) {
      rack match {
        case Some(t) => ClientPacketSender.sendServerPower(t, slot, !inventoryContainer.isRunning)
        case _ =>
      }
    }
  }

  override def drawScreen(mouseX: Int, mouseY: Int, dt: Float) {
    // Close GUI if item is removed from rack.
    rack match {
      case Some(t) if t.getStackInSlot(slot) != serverInventory.container =>
        Minecraft.getInstance().setScreen(null)
        return
      case _ =>
    }

    powerButton.visible = !inventoryContainer.isItem
    powerButton.toggled = inventoryContainer.isRunning
    super.drawScreen(mouseX, mouseY, dt)
  }

  override def initGui() {
    super.initGui()
    powerButton = new ImageButton(0, guiLeft + 48, guiTop + 33, 18, 18, Textures.GUI.ButtonPower, canToggle = true)
    addRenderableWidget(powerButton)
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      Localization.localizeImmediately(serverInventory.getName),
      8, 6, 0x404040)
    if (powerButton.isMouseOver) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.addAll((if (inventoryContainer.isRunning) Localization.Computer.TurnOff.lines else Localization.Computer.TurnOn.lines).asJava)
      copiedDrawHoveringText(tooltip, mouseX - guiLeft, mouseY - guiTop, fontRenderer)
  }
  }

  override def drawSecondaryBackgroundLayer() {
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    Textures.bind(Textures.GUI.Server)
    drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize)
  }
}
