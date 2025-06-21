package li.cil.oc.client.gui

import li.cil.oc.client.PacketSender
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.OldScaledResolution
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.GuiGraphics
import com.mojang.blaze3d.systems.RenderSystem
import org.lwjgl.glfw.GLFW

class Waypoint(val waypoint: tileentity.Waypoint) extends Screen(net.minecraft.network.chat.Component.literal("Waypoint")) {
  var guiLeft = 0
  var guiTop = 0
  var xSize = 0
  var ySize = 0

  var textField: EditBox = _

  override def tick(): Unit = {
    super.tick()
    if (minecraft.player.distanceToSqr(waypoint.getBlockPos.getX + 0.5, waypoint.getBlockPos.getY + 0.5, waypoint.getBlockPos.getZ + 0.5) > 64) {
      minecraft.player.closeContainer()
    }
  }

  override def isPauseScreen(): Boolean = false

  override def init(): Unit = {
    super.init()

    val (midX, midY) = (width / 2, height / 2)
    guiLeft = midX - 88
    guiTop = midY - 12
    xSize = 176
    ySize = 24

    textField = new EditBox(font, guiLeft + 7, guiTop + 8, 164 - 12, 12, net.minecraft.network.chat.Component.literal("waypoint"))
    textField.setMaxLength(32)
    textField.setBordered(false)
    textField.setCanLoseFocus(false)
    textField.setFocused(true)
    textField.setTextColor(0xFFFFFF)
    textField.setValue(waypoint.label)
    addRenderableWidget(textField)
  }

  override def removed(): Unit = {
    super.removed()
  }

  override def keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = {
    if (keyCode == GLFW.GLFW_KEY_ENTER) {
      val label = textField.getValue.take(32)
      if (label != waypoint.label) {
        waypoint.label = label
        PacketSender.sendWaypointLabel(waypoint)
        minecraft.player.closeContainer()
      }
      true
    } else {
      textField.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers)
    }
  }

  override def render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float): Unit = {
    renderBackground(guiGraphics)
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    guiGraphics.blit(Textures.GUI.Waypoint, guiLeft, guiTop, 0, 0, xSize, ySize)
    super.render(guiGraphics, mouseX, mouseY, partialTick)
  }
}
