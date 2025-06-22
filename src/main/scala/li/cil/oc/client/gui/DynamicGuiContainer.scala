package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.common.container.Player
import li.cil.oc.integration.Mods
import li.cil.oc.integration.rei.ModREI
import li.cil.oc.integration.util.ItemSearch
import li.cil.oc.util.RenderState
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.client.gui.GuiGraphics
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.Rect2i
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import org.joml.Matrix4f

import scala.jdk.CollectionConverters._

abstract class DynamicGuiContainer(container: AbstractContainerMenu) extends CustomGuiContainer(container) {
  protected var hoveredSlot: Option[Slot] = None

  protected var hoveredStackNEI: StackOption = EmptyStack

  protected def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int): Unit = {
    guiGraphics.drawString(font, 
      Localization.localizeImmediately("container.inventory"),
      8, imageHeight - 96 + 2, 0x404040)
  }
  
  // Store GuiGraphics reference for use in drawSecondaryForegroundLayer
  protected var guiGraphics: GuiGraphics = _

  override protected def renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int): Unit = {
    RenderState.pushAttrib()
    
    // Store reference for use in drawSecondaryForegroundLayer
    this.guiGraphics = guiGraphics
    
    drawSecondaryForegroundLayer(mouseX, mouseY)

    for (slot <- 0 until menu.slots.size()) {
      drawSlotHighlight(guiGraphics, menu.slots.get(slot))
    }

    RenderState.popAttrib()
  }

  protected def drawSecondaryBackgroundLayer(): Unit = {}

  override protected def renderBg(guiGraphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int): Unit = {
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    guiGraphics.blit(Textures.GUI.Background, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    drawSecondaryBackgroundLayer()

    RenderState.makeItBlend()
    RenderSystem.disableDepthTest()

    drawInventorySlots(guiGraphics)
    renderWidgets(guiGraphics)
    
    RenderSystem.enableDepthTest()
    RenderState.makeItBlend()
  }

  protected def drawInventorySlots(guiGraphics: GuiGraphics): Unit = {
    val poseStack = guiGraphics.pose()
    poseStack.pushPose()
    poseStack.translate(leftPos, topPos, 0)
    
    RenderSystem.enableBlend()
    for (slot <- 0 until menu.slots.size()) {
      drawSlotInventory(guiGraphics, menu.slots.get(slot))
    }
    RenderSystem.disableBlend()
    
    poseStack.popPose()
  }

  override def render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    hoveredSlot = (menu.slots.asScala collect {
      case slot: Slot if isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) => slot
    }).headOption
    hoveredStackNEI = ItemSearch.hoveredStack(this, mouseX, mouseY)

    super.render(guiGraphics, mouseX, mouseY, dt)

    if (Mods.RoughlyEnoughItems.isModAvailable) {
      drawREIHighlights()
    }
  }

  protected def drawSlotInventory(guiGraphics: GuiGraphics, slot: Slot): Unit = {
    slot match {
      case component: ComponentSlot if component.slot == common.Slot.None || component.tier == common.Tier.None =>
        if (slot.getItem.isEmpty && slot.x >= 0 && slot.y >= 0 && component.tierIcon != null) {
          drawDisabledSlot(guiGraphics, component)
        }
      case _ =>
        if (!isInPlayerInventory(slot)) {
          drawSlotBackground(guiGraphics, slot.x - 1, slot.y - 1)
        }
        if (slot.getItem.isEmpty) {
          slot match {
            case component: ComponentSlot =>
              if (component.tierIcon != null) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
                guiGraphics.blit(component.tierIcon, slot.x, slot.y, 0, 0, 16, 16, 16, 16)
              }
              if (component.hasBackground && component.backgroundLocation != null) {
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
                guiGraphics.blit(component.backgroundLocation, slot.x, slot.y, 0, 0, 16, 16, 16, 16)
              }
            case _ =>
          }
        }
    }
  }

  protected def drawSlotHighlight(guiGraphics: GuiGraphics, slot: Slot): Unit = {
    if (minecraft.player.containerMenu.getCarried.isEmpty) slot match {
      case component: ComponentSlot if component.slot == common.Slot.None || component.tier == common.Tier.None => // Ignore.
      case _ =>
        val currentIsInPlayerInventory = isInPlayerInventory(slot)
        val drawHighlight = hoveredSlot match {
          case Some(hovered) =>
            val hoveredIsInPlayerInventory = isInPlayerInventory(hovered)
            (currentIsInPlayerInventory != hoveredIsInPlayerInventory) &&
              ((currentIsInPlayerInventory && !slot.getItem.isEmpty && isSelectiveSlot(hovered) && hovered.mayPlace(slot.getItem)) ||
                (hoveredIsInPlayerInventory && !hovered.getItem.isEmpty && isSelectiveSlot(slot) && slot.mayPlace(hovered.getItem)))
          case _ => hoveredStackNEI match {
            case SomeStack(stack) => !currentIsInPlayerInventory && isSelectiveSlot(slot) && slot.mayPlace(stack)
            case _ => false
          }
        }
        if (drawHighlight) {
          guiGraphics.fillGradient(
            slot.x, slot.y,
            slot.x + 16, slot.y + 16,
            0x80FFFFFF, 0x80FFFFFF)
        }
    }
  }

  private def isSelectiveSlot(slot: Slot) = slot match {
    case component: ComponentSlot => component.slot != common.Slot.Any && component.slot != common.Slot.Tool
    case _ => false
  }

  protected def drawDisabledSlot(guiGraphics: GuiGraphics, slot: ComponentSlot): Unit = {
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    guiGraphics.blit(slot.tierIcon, slot.x, slot.y, 0, 0, 16, 16, 16, 16)
  }

  protected def drawSlotBackground(guiGraphics: GuiGraphics, x: Int, y: Int): Unit = {
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    guiGraphics.blit(Textures.GUI.Slot, x, y, 0, 0, 18, 18, 18, 18)
  }

  private def isInPlayerInventory(slot: Slot) = menu match {
    case player: Player => slot.container == player.playerInventory
    case _ => false
  }

  override def onClose(): Unit = {
    super.onClose()
    if(Mods.RoughlyEnoughItems.isModAvailable) {
      resetREIHighlights()
    }
  }

  private def drawREIHighlights(): Unit = {
    if (ModREI.isAvailable) {
      hoveredSlot match {
        case Some(hovered) if !isInPlayerInventory(hovered) && isSelectiveSlot(hovered) =>
          // Get all valid items for this slot and highlight them
          val validStacks = new java.util.ArrayList[net.minecraft.world.item.ItemStack]()
          // This would need to be implemented based on slot requirements
          ModREI.highlightStacks(validStacks)
        case _ => ModREI.clearHighlights()
      }
    }
  }

  private def resetREIHighlights(): Unit = {
    if (ModREI.isAvailable) {
      ModREI.clearHighlights()
    }
  }
}