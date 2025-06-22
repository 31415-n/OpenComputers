package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.client.{PacketSender => ClientPacketSender}
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.components.Button
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.world.entity.player.Inventory
import net.minecraft.core.Direction
import net.minecraft.client.gui.GuiGraphics

import scala.jdk.CollectionConverters._

class Rack(playerInventory: Inventory, val rack: tileentity.Rack) extends DynamicGuiContainer(new container.Rack(playerInventory, rack)) {
  imageHeight = 210

  final val busMasterBlankUVs = (195, 14, 3, 5)
  final val busMasterPresentUVs = (194, 20, 5, 5)
  final val busSlaveBlankUVs = (195, 1, 3, 4)
  final val busSlavePresentUVs = (194, 6, 5, 4)

  final val connectorMasterUVs = (194, 26, 1, 3)
  final val connectorSlaveUVs = (194, 11, 1, 2)

  final val hoverMasterSize = (3, 3)
  final val hoverSlaveSize = (3, 2)

  final val wireMasterUVs = Array(
    (186, 16, 6, 3),
    (186, 20, 6, 3),
    (186, 24, 6, 3),
    (186, 28, 6, 3),
    (186, 32, 6, 3)
  )
  final val wireSlaveUVs = Array(
    (186, 1, 6, 2),
    (186, 4, 6, 2),
    (186, 7, 6, 2),
    (186, 10, 6, 2),
    (186, 13, 6, 2)
  )

  final val busStart = Array(
    (45, 22),
    (56, 22),
    (67, 22),
    (78, 22),
    (89, 22)
  )

  final val busGap = 3

  final val connectorStart = Array(
    (37, 23),
    (37, 43),
    (37, 63),
    (37, 83)
  )

  final val connectorGap = 2

  final val relayModeUVs = (195, 30, 4, 2)

  final val wireRelay = Array(
    (50, 104),
    (61, 104),
    (72, 104),
    (83, 104)
  )

  final val busToSide = Direction.values().filter(_ != Direction.SOUTH)
  final val sideToBus = busToSide.zipWithIndex.toMap

  var relayButton: ImageButton = _

  // bus -> mountable -> connectable  
  var wireButtons: Array[Array[Array[ImageButton]]] = null
  
  private def initWireButtons(): Unit = {
    wireButtons = Array.fill(rack.getContainerSize)(Array.fill(4)(Array.fill(5)(null: ImageButton)))
  }

  def sideName(side: Direction) = side match {
    case Direction.UP => Localization.Rack.Top
    case Direction.DOWN => Localization.Rack.Bottom
    case Direction.WEST => Localization.Rack.Right
    case Direction.EAST => Localization.Rack.Left
    case Direction.NORTH => Localization.Rack.Back
    case _ => Localization.Rack.None
  }

  def encodeButtonId(mountable: Int, connectable: Int, bus: Int) = {
    // +1 to offset for relay button
    1 + mountable * 4 * 5 + connectable * 5 + bus
  }

  def decodeButtonId(buttonId: Int) = {
    // -1 to offset for relay button
    val bus = (buttonId - 1) % 5
    val connectable = ((buttonId - 1) / 5) % 4
    val mountable = (buttonId - 1) / 5 / 4
    (mountable, connectable, bus)
  }

  protected def onButtonClick(button: Button): Unit = {
    // Button ID handling needs to be implemented differently in 1.20.1
    // For now, we'll use button reference comparison
    if (button == relayButton) {
      ClientPacketSender.sendRackRelayState(rack, !rack.isRelayEnabled)
    }
    else {
      // Find button in wireButtons array to get coordinates
      var found = false
      var mountable = 0
      var connectable = 0
      var bus = 0
      
      for (m <- wireButtons.indices if !found) {
        for (c <- wireButtons(m).indices if !found) {
          for (b <- wireButtons(m)(c).indices if !found) {
            if (wireButtons(m)(c)(b) == button) {
              mountable = m
              connectable = c
              bus = b
              found = true
            }
          }
        }
      }
      
      if (found) {
        if (rack.nodeMapping(mountable)(connectable).contains(busToSide(bus))) {
          ClientPacketSender.sendRackMountableMapping(rack, mountable, connectable, None)
        }
        else {
          ClientPacketSender.sendRackMountableMapping(rack, mountable, connectable, Option(busToSide(bus)))
        }
      }
    }
  }

  override def render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, dt: Float): Unit = {
    for (bus <- 0 until 5) {
      for (mountable <- 0 until rack.getContainerSize) {
        val presence = menu.asInstanceOf[container.Rack].nodePresence(mountable)
        for (connectable <- 0 until 4) {
          wireButtons(mountable)(connectable)(bus).visible = presence(connectable)
        }
      }
    }
    relayButton.displayString = if (rack.isRelayEnabled) Localization.Rack.RelayEnabled else Localization.Rack.RelayDisabled
    super.render(guiGraphics, mouseX, mouseY, dt)
  }

  override def init(): Unit = {
    super.init()
    
    initWireButtons()

    relayButton = new ImageButton(leftPos + 101, topPos + 96, 65, 18, Textures.GUI.ButtonRelay, Localization.Rack.RelayDisabled, textIndent = 18, onPress = _ => onButtonClick(relayButton))
    addRenderableWidget(relayButton)

    val (mw, mh) = hoverMasterSize
    val (sw, sh) = hoverSlaveSize
    val (_, _, _, mbh) = busMasterBlankUVs
    val (_, _, _, sbh) = busSlaveBlankUVs
    for (bus <- 0 until 5) {
      for (mountable <- 0 until rack.getContainerSize) {
        val offset = mountable * (mbh + sbh * 3 + busGap)
        val (bx, by) = busStart(bus)

        {
          val button = new ImageButton(leftPos + bx, topPos + by + offset + 1, mw, mh, onPress = btn => onButtonClick(btn))
          addRenderableWidget(button)
          wireButtons(mountable)(0)(bus) = button
        }

        for (connectable <- 0 until 3) {
          val button = new ImageButton(leftPos + bx, topPos + by + offset + 1 + mbh + sbh * connectable, sw, sh, onPress = btn => onButtonClick(btn))
          addRenderableWidget(button)
          wireButtons(mountable)(connectable + 1)(bus) = button
        }
      }
    }
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    RenderState.pushAttrib() // Prevents NEI render glitch.

    guiGraphics.drawString(font,
      Localization.localizeImmediately("tile.oc.rack.name"),
      8, 6, 0x404040)

    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    RenderSystem.setShaderTexture(0, Textures.GUI.Rack)

    if (rack.isRelayEnabled) {
      val (left, top, w, h) = relayModeUVs
      for ((x, y) <- wireRelay) {
        drawRect(x, y, w, h, left, top)
      }
    }

    val (mcx, mcy, mcw, mch) = connectorMasterUVs
    val (mbx, mby, mbw, mbh) = busMasterBlankUVs
    val (mpx, mpy, mpw, mph) = busMasterPresentUVs
    val (scx, scy, scw, sch) = connectorSlaveUVs
    val (sbx, sby, sbw, sbh) = busSlaveBlankUVs
    val (spx, spy, spw, sph) = busSlavePresentUVs
    for (mountable <- 0 until rack.getContainerSize) {
      val presence = menu.asInstanceOf[container.Rack].nodePresence(mountable)

      // Draw connectable indicators next to item slots.
      val (cx, cy) = connectorStart(mountable)
      if (presence(0)) {
        drawRect(cx, cy, mcw, mch, mcx, mcy)
        rack.nodeMapping(mountable)(0) match {
          case Some(side) =>
            val bus = sideToBus(side)
            val (mwx, mwy, mww, mwh) = wireMasterUVs(bus)
            for (i <- 0 to bus) {
              val xOffset = mcw + i * (mpw + mww)
              drawRect(cx + xOffset, cy, mww, mwh, mwx, mwy)
            }
          case _ =>
        }
        for (connectable <- 1 until 4) {
          rack.nodeMapping(mountable)(connectable) match {
            case Some(side) =>
              val bus = sideToBus(side)
              val (swx, swy, sww, swh) = wireSlaveUVs(bus)
              val yOffset = (mch + connectorGap) + (sch + connectorGap) * (connectable - 1)
              for (i <- 0 to bus) {
                val xOffset = scw + i * (spw + sww)
                drawRect(cx + xOffset, cy + yOffset, sww, swh, swx, swy)
              }
            case _ =>
          }
        }
      }
      for (connectable <- 1 until 4) {
        if (presence(connectable)) {
          val yOffset = (mch + connectorGap) + (sch + connectorGap) * (connectable - 1)
          drawRect(cx, cy + yOffset, scw, sch, scx, scy)
        }
      }

      // Draw connection points on buses.
      val yOffset = mountable * (mbh + sbh * 3 + busGap)
      for (bus <- 0 until 5) {
        val (bx, by) = busStart(bus)
        if (presence(0)) {
          drawRect(bx - 1, by + yOffset, mpw, mph, mpx, mpy)
        }
        else {
          drawRect(bx, by + yOffset, mbw, mbh, mbx, mby)
        }
        for (connectable <- 0 until 3) {
          if (presence(connectable + 1)) {
            drawRect(bx - 1, by + yOffset + mph + sph * connectable, spw, sph, spx, spy)
          }
          else {
            drawRect(bx, by + yOffset + mbh + sbh * connectable, sbw, sbh, sbx, sby)
          }
        }
      }
    }

    for (bus <- 0 until 5) {
      val x = 122
      val y = 20 + bus * 11

      guiGraphics.drawString(font,
        Localization.localizeImmediately(sideName(busToSide(bus))),
        x, y, 0x404040)
    }

    if (mouseX >= leftPos + 122 && mouseY >= topPos + 20 && mouseX < leftPos + 158 && mouseY < topPos + 20 + 5 * 11) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.addAll(Localization.Rack.OrientationTooltip.split("\n").toList.asJava)
      copiedDrawHoveringText(guiGraphics, tooltip, mouseX - leftPos, mouseY - topPos, font)
    }

    if (relayButton.isMouseOver(mouseX, mouseY)) {
      val tooltip = new java.util.ArrayList[String]
      tooltip.addAll(Localization.Rack.RelayModeTooltip.split("\n").toList.asJava)
      copiedDrawHoveringText(guiGraphics, tooltip, mouseX - leftPos, mouseY - topPos, font)
    }

    RenderState.popAttrib()
  }

  override def renderBg(guiGraphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int): Unit = {
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f) // Required under Linux.
    RenderSystem.setShaderTexture(0, Textures.GUI.Rack)
    guiGraphics.blit(Textures.GUI.Rack, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }

  private def drawRect(x: Int, y: Int, w: Int, h: Int, u: Int, v: Int): Unit = {
    val u0 = u / 256f
    val v0 = v / 256f
    val u1 = u0 + w / 256f
    val v1 = v0 + h / 256f
    val t = Tesselator.getInstance
    val r = t.getBuilder
    r.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
    r.vertex(x, y, 0).uv(u0, v0).endVertex()
    r.vertex(x, y + h, 0).uv(u0, v1).endVertex()
    r.vertex(x + w, y + h, 0).uv(u1, v1).endVertex()
    r.vertex(x + w, y, 0).uv(u1, v0).endVertex()
    t.end()
  }
}
