package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.tileentity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

/**
 * Block entity type registration for OpenComputers 1.20.1.
 * Replaces the old GameRegistry.registerTileEntity system from 1.12.2.
 */
object BlockEntityTypes {
  
  val BLOCK_ENTITIES: DeferredRegister[BlockEntityType[_]] = 
    DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, OpenComputers.ID)

  // Register all block entity types with proper suppliers
  val ADAPTER: RegistryObject[BlockEntityType[tileentity.Adapter]] = 
    BLOCK_ENTITIES.register("adapter", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Adapter(),
        getAdapterBlock
      ).build(null)
    )

  // Helper methods to get blocks - these would be implemented based on actual block registration
  private def getAdapterBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Adapter).block()
  private def getAssemblerBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Assembler).block()
  private def getCableBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Cable).block()
  private def getCapacitorBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Capacitor).block()
  private def getCarpetedCapacitorBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.CarpetedCapacitor).block()
  private def getCaseBlocks = Array(
    li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.CaseTier1).block(),
    li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.CaseTier2).block(),
    li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.CaseTier3).block()
  )
  private def getChargerBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Charger).block()
  private def getDiskDriveBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.DiskDrive).block()
  private def getDisassemblerBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Disassembler).block()
  private def getKeyboardBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Keyboard).block()
  private def getHologramBlocks = Array(
    li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.HologramTier1).block(),
    li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.HologramTier2).block()
  )
  private def getGeolyzerBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Geolyzer).block()
  private def getMicrocontrollerBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Microcontroller).block()
  private def getMotionSensorBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.MotionSensor).block()
  private def getNetSplitterBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.NetSplitter).block()
  private def getPowerConverterBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.PowerConverter).block()
  private def getPowerDistributorBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.PowerDistributor).block()
  private def getPrintBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Print).block()
  private def getPrinterBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Printer).block()
  private def getRaidBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Raid).block()
  private def getRedstoneBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Redstone).block()
  private def getRelayBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Relay).block()
  private def getRobotBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Robot).block()
  private def getScreenBlocks = Array(
    li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.ScreenTier1).block(),
    li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.ScreenTier2).block(),
    li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.ScreenTier3).block()
  )
  private def getRackBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Rack).block()
  private def getTransposerBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Transposer).block()
  private def getWaypointBlock = li.cil.oc.api.Items.get(li.cil.oc.Constants.BlockName.Waypoint).block()
  private def getSwitchBlock = li.cil.oc.api.Items.get("switch").block() // This needs proper constant
  private def getAccessPointBlock = li.cil.oc.api.Items.get("accessPoint").block() // This needs proper constant

  val ASSEMBLER: RegistryObject[BlockEntityType[tileentity.Assembler]] = 
    BLOCK_ENTITIES.register("assembler", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Assembler(),
        getAssemblerBlock
      ).build(null)
    )

  val CABLE: RegistryObject[BlockEntityType[tileentity.Cable]] = 
    BLOCK_ENTITIES.register("cable", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Cable(),
        getCableBlock
      ).build(null)
    )

  val CAPACITOR: RegistryObject[BlockEntityType[tileentity.Capacitor]] = 
    BLOCK_ENTITIES.register("capacitor", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Capacitor(),
        getCapacitorBlock
      ).build(null)
    )

  val CARPETED_CAPACITOR: RegistryObject[BlockEntityType[tileentity.CarpetedCapacitor]] = 
    BLOCK_ENTITIES.register("carpeted_capacitor", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.CarpetedCapacitor(),
        getCarpetedCapacitorBlock
      ).build(null)
    )

  val CASE: RegistryObject[BlockEntityType[tileentity.Case]] = 
    BLOCK_ENTITIES.register("case", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Case(),
        getCaseBlocks: _*
      ).build(null)
    )

  val CHARGER: RegistryObject[BlockEntityType[tileentity.Charger]] = 
    BLOCK_ENTITIES.register("charger", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Charger(),
        getChargerBlock
      ).build(null)
    )

  val DISK_DRIVE: RegistryObject[BlockEntityType[tileentity.DiskDrive]] = 
    BLOCK_ENTITIES.register("disk_drive", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.DiskDrive(),
        getDiskDriveBlock
      ).build(null)
    )

  val DISASSEMBLER: RegistryObject[BlockEntityType[tileentity.Disassembler]] = 
    BLOCK_ENTITIES.register("disassembler", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Disassembler(),
        getDisassemblerBlock
      ).build(null)
    )

  val KEYBOARD: RegistryObject[BlockEntityType[tileentity.Keyboard]] = 
    BLOCK_ENTITIES.register("keyboard", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Keyboard(),
        getKeyboardBlock
      ).build(null)
    )

  val HOLOGRAM: RegistryObject[BlockEntityType[tileentity.Hologram]] = 
    BLOCK_ENTITIES.register("hologram", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Hologram(),
        getHologramBlocks: _*
      ).build(null)
    )

  val GEOLYZER: RegistryObject[BlockEntityType[tileentity.Geolyzer]] = 
    BLOCK_ENTITIES.register("geolyzer", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Geolyzer(),
        getGeolyzerBlock
      ).build(null)
    )

  val MICROCONTROLLER: RegistryObject[BlockEntityType[tileentity.Microcontroller]] = 
    BLOCK_ENTITIES.register("microcontroller", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Microcontroller(),
        getMicrocontrollerBlock
      ).build(null)
    )

  val MOTION_SENSOR: RegistryObject[BlockEntityType[tileentity.MotionSensor]] = 
    BLOCK_ENTITIES.register("motion_sensor", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.MotionSensor(),
        getMotionSensorBlock
      ).build(null)
    )

  val NET_SPLITTER: RegistryObject[BlockEntityType[tileentity.NetSplitter]] = 
    BLOCK_ENTITIES.register("net_splitter", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.NetSplitter(),
        getNetSplitterBlock
      ).build(null)
    )

  val POWER_CONVERTER: RegistryObject[BlockEntityType[tileentity.PowerConverter]] = 
    BLOCK_ENTITIES.register("power_converter", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.PowerConverter(),
        getPowerConverterBlock
      ).build(null)
    )

  val POWER_DISTRIBUTOR: RegistryObject[BlockEntityType[tileentity.PowerDistributor]] = 
    BLOCK_ENTITIES.register("power_distributor", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.PowerDistributor(),
        getPowerDistributorBlock
      ).build(null)
    )

  val PRINT: RegistryObject[BlockEntityType[tileentity.Print]] = 
    BLOCK_ENTITIES.register("print", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Print(),
        getPrintBlock
      ).build(null)
    )

  val PRINTER: RegistryObject[BlockEntityType[tileentity.Printer]] = 
    BLOCK_ENTITIES.register("printer", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Printer(),
        getPrinterBlock
      ).build(null)
    )

  val RAID: RegistryObject[BlockEntityType[tileentity.Raid]] = 
    BLOCK_ENTITIES.register("raid", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Raid(),
        getRaidBlock
      ).build(null)
    )

  val REDSTONE: RegistryObject[BlockEntityType[tileentity.Redstone]] = 
    BLOCK_ENTITIES.register("redstone", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Redstone(),
        getRedstoneBlock
      ).build(null)
    )

  val RELAY: RegistryObject[BlockEntityType[tileentity.Relay]] = 
    BLOCK_ENTITIES.register("relay", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Relay(),
        getRelayBlock
      ).build(null)
    )

  val ROBOT_PROXY: RegistryObject[BlockEntityType[tileentity.RobotProxy]] = 
    BLOCK_ENTITIES.register("robot", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.RobotProxy(),
        getRobotBlock
      ).build(null)
    )

  val SCREEN: RegistryObject[BlockEntityType[tileentity.Screen]] = 
    BLOCK_ENTITIES.register("screen", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Screen(),
        getScreenBlocks: _*
      ).build(null)
    )

  val RACK: RegistryObject[BlockEntityType[tileentity.Rack]] = 
    BLOCK_ENTITIES.register("rack", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Rack(),
        getRackBlock
      ).build(null)
    )

  val TRANSPOSER: RegistryObject[BlockEntityType[tileentity.Transposer]] = 
    BLOCK_ENTITIES.register("transposer", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Transposer(),
        getTransposerBlock
      ).build(null)
    )

  val WAYPOINT: RegistryObject[BlockEntityType[tileentity.Waypoint]] = 
    BLOCK_ENTITIES.register("waypoint", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Waypoint(),
        getWaypointBlock
      ).build(null)
    )

  val SWITCH: RegistryObject[BlockEntityType[tileentity.Switch]] = 
    BLOCK_ENTITIES.register("switch", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.Switch(),
        getSwitchBlock
      ).build(null)
    )

  val ACCESS_POINT: RegistryObject[BlockEntityType[tileentity.AccessPoint]] = 
    BLOCK_ENTITIES.register("access_point", () => 
      BlockEntityType.Builder.of(
        (pos, state) => new tileentity.AccessPoint(),
        getAccessPointBlock
      ).build(null)
    )
}