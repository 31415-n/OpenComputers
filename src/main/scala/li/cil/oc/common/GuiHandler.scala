package li.cil.oc.common

import li.cil.oc.common.inventory.{DatabaseInventory, DiskDriveMountableInventory, ServerInventory}
import li.cil.oc.common.item.Delegator
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.server.component.{DiskDriveMountable, Server}
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.MenuProvider
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.server.level.ServerPlayer

abstract class GuiHandler {
  def getServerGuiElement(id: Int, player: Player, world: Level, x: Int, y: Int, z: Int): AbstractContainerMenu = {
    GuiType.Categories.get(id) match {
      case Some(GuiType.Category.Block) =>
        world.getBlockEntity(BlockPosition(x, GuiType.extractY(y), z)) match {
          case t: tileentity.Adapter if id == GuiType.Adapter.id =>
            new container.Adapter(player.getInventory, t)
          case t: tileentity.Assembler if id == GuiType.Assembler.id =>
            new container.Assembler(player.getInventory, t)
          case t: tileentity.Charger if id == GuiType.Charger.id =>
            new container.Charger(player.getInventory, t)
          case t: tileentity.Case if id == GuiType.Case.id =>
            new container.Case(player.getInventory, t)
          case t: tileentity.Disassembler if id == GuiType.Disassembler.id =>
            new container.Disassembler(player.getInventory, t)
          case t: tileentity.DiskDrive if id == GuiType.DiskDrive.id =>
            new container.DiskDrive(player.getInventory, t)
          case t: tileentity.Printer if id == GuiType.Printer.id =>
            new container.Printer(player.getInventory, t)
          case t: tileentity.Raid if id == GuiType.Raid.id =>
            new container.Raid(player.getInventory, t)
          case t: tileentity.Relay if id == GuiType.Relay.id =>
            new container.Relay(player.getInventory, t)
          case t: tileentity.RobotProxy if id == GuiType.Robot.id =>
            new container.Robot(player.getInventory, t.robot)
          case t: tileentity.Rack if id == GuiType.Rack.id =>
            new container.Rack(player.getInventory, t)
          case t: tileentity.Rack if id == GuiType.ServerInRack.id =>
            val slot = GuiType.extractSlot(y)
            val server = t.getMountable(slot).asInstanceOf[Server]
            new container.Server(player.getInventory, server, Option(server))
          case t: tileentity.Rack if id == GuiType.DiskDriveMountableInRack.id =>
            val slot = GuiType.extractSlot(y)
            val drive = t.getMountable(slot).asInstanceOf[DiskDriveMountable]
            new container.DiskDrive(player.getInventory, drive)
          case _ => null
        }
      case Some(GuiType.Category.Entity) =>
        world.getEntity(x) match {
          case drone: entity.Drone if id == GuiType.Drone.id =>
            new container.Drone(player.getInventory, drone)
          case _ => null
        }
      case Some(GuiType.Category.Item) => {
        val itemStackInUse = getItemStackInUse(id, player)
        Delegator.subItem(itemStackInUse) match {
          case Some(database: item.UpgradeDatabase) if id == GuiType.Database.id =>
            new container.Database(player.getInventory, new DatabaseInventory {
              override def container = itemStackInUse

              override def stillValid(player: Player) = player == player
            })
          case Some(server: item.Server) if id == GuiType.Server.id =>
            new container.Server(player.getInventory, new ServerInventory {
              override def container = itemStackInUse

              override def stillValid(player: Player) = player == player
            })
          case Some(tablet: item.Tablet) if id == GuiType.TabletInner.id =>
            val stack = itemStackInUse
            if (stack.hasTag)
              new container.Tablet(player.getInventory, item.Tablet.get(stack, player))
            else
              null
          case Some(drive: item.DiskDriveMountable) if id == GuiType.DiskDriveMountable.id =>
            new container.DiskDrive(player.getInventory, new DiskDriveMountableInventory {
              override def container: ItemStack = itemStackInUse

              override def stillValid(player: Player) = player == player
            })
          case _ => null
        }
      }
      case _ => null
    }
  }

  def getItemStackInUse(id: Int, player: Player): ItemStack = {
    val mainItem: ItemStack = player.getMainHandItem
    Delegator.subItem(mainItem) match {
      case Some(drive: item.traits.FileSystemLike) if id == GuiType.Drive.id => mainItem
      case Some(database: item.UpgradeDatabase) if id == GuiType.Database.id => mainItem
      case Some(server: item.Server) if id == GuiType.Server.id => mainItem
      case Some(tablet: item.Tablet) if id == GuiType.Tablet.id => mainItem
      case Some(tablet: item.Tablet) if id == GuiType.TabletInner.id => mainItem
      case Some(terminal: item.Terminal) if id == GuiType.Terminal.id => mainItem
      case Some(drive: item.DiskDriveMountable) if id == GuiType.DiskDriveMountable.id => mainItem
      case _ => player.getOffhandItem
    }
  }
}
