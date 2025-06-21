package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.item.data.DriveData
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack

class Drive(playerInventory: Inventory, val driveStack: () => ItemStack) extends Screen(net.minecraft.network.chat.Component.literal("Drive")) with traits.Window {
  override val windowHeight = 120

  override def backgroundImage = Textures.GUI.Drive

  protected var managedButton: ImageButton = _
  protected var unmanagedButton: ImageButton = _
  protected var lockedButton: ImageButton = _

  protected override def actionPerformed(button: Button): Unit = {
    if (button.id == 0) {
      ClientPacketSender.sendDriveMode(unmanaged = false)
      DriveData.setUnmanaged(driveStack(), unmanaged = false)
    } else if (button.id == 1) {
      ClientPacketSender.sendDriveMode(unmanaged = true)
      DriveData.setUnmanaged(driveStack(), unmanaged = true)
    } else if (button.id == 2) {
      ClientPacketSender.sendDriveLock()
      DriveData.lock(driveStack(), playerInventory.player)
    }
    updateButtonStates()
  }

  def updateButtonStates(): Unit = {
    val data = new DriveData(driveStack())
    unmanagedButton.toggled = data.isUnmanaged
    managedButton.toggled = !unmanagedButton.toggled
    lockedButton.toggled = data.isLocked
    lockedButton.enabled = !data.isLocked
  }

  override def init(): Unit = {
    super.init()
    managedButton = new ImageButton(0, guiLeft + 11, guiTop + 11, 74, 18, Textures.GUI.ButtonDriveMode, text = Localization.Drive.Managed, textColor = 0x608060, canToggle = true)
    unmanagedButton = new ImageButton(1, guiLeft + 91, guiTop + 11, 74, 18, Textures.GUI.ButtonDriveMode, text = Localization.Drive.Unmanaged, textColor = 0x608060, canToggle = true)
    lockedButton = new ImageButton(2, guiLeft + 11, guiTop + windowHeight - 42, 44, 18, Textures.GUI.ButtonDriveMode, text = Localization.Drive.ReadOnlyLock, textColor = 0x608060, canToggle = true)
    addRenderableWidget(managedButton)
    addRenderableWidget(unmanagedButton)
    addRenderableWidget(lockedButton)
    updateButtonStates()
  }

  override def tick(): Unit = {
    super.tick()
  }

  override def render(guiGraphics: net.minecraft.client.gui.GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float): Unit = {
    renderBackground(guiGraphics)
    super.render(guiGraphics, mouseX, mouseY, partialTick)
    guiGraphics.drawWordWrap(font, net.minecraft.network.chat.Component.literal(Localization.Drive.Warning), guiLeft + 11, guiTop + 37, xSize - 20, 0x404040)
    guiGraphics.drawWordWrap(font, net.minecraft.network.chat.Component.literal(Localization.Drive.LockWarning), guiLeft + 61, guiTop + windowHeight - 48, xSize - 68, 0x404040)
  }
}
