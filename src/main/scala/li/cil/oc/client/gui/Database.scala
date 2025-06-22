package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import li.cil.oc.common.Tier
import li.cil.oc.common.container
import li.cil.oc.common.inventory.DatabaseInventory
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.world.entity.player.Inventory

class Database(playerInventory: Inventory, val databaseInventory: DatabaseInventory) extends DynamicGuiContainer(new container.Database(playerInventory, databaseInventory)) with traits.LockedHotbar {
  imageHeight = 256

  override def lockedStack = databaseInventory.container

  override def drawSecondaryForegroundLayer(guiGraphics: net.minecraft.client.gui.GuiGraphics, mouseX: Int, mouseY: Int) {}

  override protected def renderBg(guiGraphics: net.minecraft.client.gui.GuiGraphics, dt: Float, mouseX: Int, mouseY: Int): Unit = {
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    Textures.bind(Textures.GUI.Database)
    guiGraphics.blit(Textures.GUI.Database, leftPos, topPos, 0, 0, imageWidth, imageHeight)

    if (databaseInventory.tier > Tier.One) {
      Textures.bind(Textures.GUI.Database1)
      guiGraphics.blit(Textures.GUI.Database1, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    }

    if (databaseInventory.tier > Tier.Two) {
      Textures.bind(Textures.GUI.Database2)
      guiGraphics.blit(Textures.GUI.Database2, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    }
  }
}
