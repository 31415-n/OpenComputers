package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.gui.widget.ProgressBar
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.container.ComponentSlot
import li.cil.oc.common.template.AssemblerTemplates
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.GuiGraphics
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot
import net.minecraft.network.chat.Component

import scala.jdk.CollectionConverters._

class Assembler(playerInventory: Inventory, val assembler: tileentity.Assembler) extends DynamicGuiContainer(new container.Assembler(playerInventory, assembler)) {
  imageWidth = 176
  imageHeight = 192

  for (slot <- menu.slots.asScala) slot match {
    case component: ComponentSlot => component.changeListener = Option(onSlotChanged)
    case _ =>
  }

  private def onSlotChanged(slot: Slot): Unit = {
    runButton.enabled = canBuild
    runButton.toggled = !runButton.enabled
    info = validate
  }

  var info: Option[(Boolean, Component, Array[Component])] = None

  protected var runButton: ImageButton = _

  private val progress = addWidget(new ProgressBar(28, 92))

  private def validate = AssemblerTemplates.select(menu.getSlot(0).getItem).map(_.validate(menu.asInstanceOf[container.Assembler].assembler))

  private def canBuild = !menu.asInstanceOf[container.Assembler].isAssembling && validate.exists(_._1)

  override def init(): Unit = {
    super.init()
    runButton = new ImageButton(leftPos + 7, topPos + 89, 18, 18, Textures.GUI.ButtonRun, canToggle = true, onPress = _ => {
      if (canBuild) {
        ClientPacketSender.sendRobotAssemblerStart(assembler)
      }
    })
    addRenderableWidget(runButton)
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int): Unit = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    
    val assemblerContainer = menu.asInstanceOf[container.Assembler]
    if (!assemblerContainer.isAssembling) {
      val message =
        if (menu.getSlot(0).getItem.isEmpty) {
          Localization.Assembler.InsertTemplate
        }
        else info match {
          case Some((_, value, _)) if value != null => value.getString
          case _ if !menu.getSlot(0).getItem.isEmpty => Localization.Assembler.CollectResult
          case _ => ""
        }
      if (message.nonEmpty) {
        guiGraphics.drawString(font, message, 30, 94, 0x404040)
      }
    }
  }
  
  override def render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    super.render(guiGraphics, mouseX, mouseY, partialTicks)
    
    val assemblerContainer = menu.asInstanceOf[container.Assembler]
    // Handle tooltips
    if (!assemblerContainer.isAssembling && runButton.isMouseOver(mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[Component]
      tooltip.add(Component.literal(Localization.Assembler.Run))
      info.foreach {
        case (valid, _, warnings) => if (valid && warnings.length > 0) {
          warnings.foreach(warning => tooltip.add(warning))
        }
      }
      guiGraphics.renderTooltip(font, tooltip.asScala.map(_.getVisualOrderText).toList.asJava, mouseX, mouseY)
    }
    else if (assemblerContainer.isAssembling && isHovering(progress.x, progress.y, progress.width, progress.height, mouseX, mouseY)) {
      val timeRemaining = formatTime(assemblerContainer.assemblyRemainingTime)
      val tooltip = Component.literal(Localization.Assembler.Progress(assemblerContainer.assemblyProgress, timeRemaining))
      guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY)
    }
  }

  private def formatTime(seconds: Int) = {
    // Assembly times should not / rarely exceed one hour, so this is good enough.
    if (seconds < 60) f"0:$seconds%02d"
    else f"${seconds / 60}:${seconds % 60}%02d"
  }

  override def renderBg(guiGraphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int): Unit = {
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f) // Required under Linux.
    guiGraphics.blit(Textures.GUI.RobotAssembler, leftPos, topPos, 0, 0, imageWidth, imageHeight)
    val assemblerContainer = menu.asInstanceOf[container.Assembler]
    if (assemblerContainer.isAssembling) progress.level = assemblerContainer.assemblyProgress / 100.0
    else progress.level = 0
    drawInventorySlots(guiGraphics)
    renderWidgets(guiGraphics)
  }

  override protected def drawDisabledSlot(slot: ComponentSlot): Unit = {}
}
