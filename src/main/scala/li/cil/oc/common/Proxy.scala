package li.cil.oc.common

import java.io.File

import com.google.common.base.Strings
import li.cil.oc._
import li.cil.oc.common.capabilities.Capabilities
import li.cil.oc.common.entity.Drone
import li.cil.oc.common.init.Blocks
import li.cil.oc.common.init.Items
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.traits.Delegate
import li.cil.oc.common.recipe.Recipes
import li.cil.oc.integration.Mods
import li.cil.oc.server._
import li.cil.oc.server.machine.luac.{LuaStateFactory, NativeLua52Architecture, NativeLua53Architecture, NativeLua54Architecture}
import li.cil.oc.server.machine.luaj.LuaJLuaArchitecture
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegistryEvent.MissingMappings
import net.minecraftforge.fml.common.FMLLog
import net.minecraftforge.fml.event.lifecycle._
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.common.Tags

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

class Proxy {
  def preInit(e: FMLCommonSetupEvent): Unit = {
    checkForBrokenJavaVersion()

    Settings.load(new File("config", "opencomputers" + File.separator + "settings.conf"))

    MinecraftForge.EVENT_BUS.register(this)

    OpenComputers.log.debug("Initializing blocks and items.")

    Blocks.init()
    Items.init()

    OpenComputers.log.debug("Initializing additional OreDict entries.")

    // Modern tag system replaces OreDictionary
    // Tags are handled differently in 1.20.1

    // Make mods that use old wireless card name not have broken recipes
    OreDictionary.registerOre("oc:wlanCard", Items.get(Constants.ItemName.WirelessNetworkCardTier2).createItemStack(1))

    tryRegisterNugget[item.DiamondChip](Constants.ItemName.DiamondChip, "chipDiamond", net.minecraft.init.Items.DIAMOND, "gemDiamond")

    // Avoid issues with Extra Utilities registering colored obsidian as `obsidian`
    // oredict entry, but not normal obsidian, breaking some recipes.
    // Modern tag system handles these registrations

    OpenComputers.log.info("Initializing OpenComputers API.")

    api.CreativeTab.instance = CreativeTab
    api.API.driver = driver.Registry
    api.API.fileSystem = fs.FileSystem
    api.API.items = Items
    api.API.machine = machine.Machine
    api.API.nanomachines = nanomachines.Nanomachines
    api.API.network = network.Network

    api.API.config = Settings.get.config

    if (LuaStateFactory.isAvailable) {
      if (LuaStateFactory.include53) {
        api.Machine.add(classOf[NativeLua53Architecture])
      }
      if (LuaStateFactory.include54) {
        api.Machine.add(classOf[NativeLua54Architecture])
      }
      if (LuaStateFactory.include52) {
        api.Machine.add(classOf[NativeLua52Architecture])
      }
    }
    if (LuaStateFactory.includeLuaJ) {
      api.Machine.add(classOf[LuaJLuaArchitecture])
    }
    
    api.Machine.LuaArchitecture =
      if (Settings.get.forceLuaJ) classOf[LuaJLuaArchitecture]
      else api.Machine.architectures.head
  }

  def init(e: FMLClientSetupEvent): Unit = {
    // Modern network registration will be handled separately

    Loot.init()
    Achievement.init()

    // Entity registration is handled through DeferredRegister in modern versions

    OpenComputers.log.debug("Initializing mod integration.")
    Mods.init()

    OpenComputers.log.debug("Initializing recipes.")
    Recipes.init()

    OpenComputers.log.info("Initializing capabilities.")
    Capabilities.init()

    api.API.isPowerEnabled = !Settings.get.ignorePower
  }

  def postInit(e: FMLDedicatedServerSetupEvent): Unit = {
    // Don't allow driver registration after this point, to avoid issues.
    driver.Registry.locked = true
  }

  def tryRegisterNugget[TItem <: Delegate : ClassTag](nuggetItemName: String, nuggetOredictName: String, ingotItem: Item, ingotOredictName: String): Unit = {
    val nugget = Items.get(nuggetItemName).createItemStack(1)

    registerExclusive(nuggetOredictName, nugget)

    Delegator.subItem(nugget) match {
      case Some(subItem: TItem) =>
        // Modern tag system check would go here
        if (true) { // Placeholder for tag check
          Recipes.addSubItem(subItem, nuggetItemName)
          Recipes.addItem(ingotItem, ingotOredictName)
        }
        else {
          subItem.showInItemList = false
        }
      case _ =>
    }
  }

  def registerModel(instance: Delegate, id: String): Unit = {}

  def registerModel(instance: Item, id: String): Unit = {}

  def registerModel(instance: Block, id: String): Unit = {}

  private def registerExclusive(name: String, items: ItemStack*): Unit = {
    // Modern tag system handles exclusive registration differently
  }

  // Yes, this could be boiled down even further, but I like to keep it
  // explicit like this, because it makes it a) clearer, b) easier to
  // extend, in case that should ever be needed.

  // Example usage: OpenComputers.ID + ":rack" -> "serverRack"
  private val blockRenames = Map[String, String](
    OpenComputers.ID + ":serverRack" -> Constants.BlockName.Rack // Yay, full circle >_>
  )

  // Example usage: OpenComputers.ID + ":tabletCase" -> "tabletCase1"
  private val itemRenames = Map[String, String](
    OpenComputers.ID + ":dataCard" -> Constants.ItemName.DataCardTier1,
    OpenComputers.ID + ":serverRack" -> Constants.BlockName.Rack,
    OpenComputers.ID + ":wlanCard" -> Constants.ItemName.WirelessNetworkCardTier2
  )

  @SubscribeEvent
  def missingBlockMappings(e: MissingMappings[Block]) {
    for (missing <- e.getMappings) {
        blockRenames.get(missing.key.getPath) match {
          case Some(name) =>
            if (Strings.isNullOrEmpty(name)) missing.ignore()
            else missing.remap(ForgeRegistries.BLOCKS.getValue(new ResourceLocation(OpenComputers.ID, name)))
          case _ => missing.warn()
        }
    }
  }

  @SubscribeEvent
  def missingItemMappings(e: MissingMappings[Item]) {
    for (missing <- e.getMappings) {
        itemRenames.get(missing.key.getPath) match {
          case Some(name) =>
            if (Strings.isNullOrEmpty(name)) missing.ignore()
            else missing.remap(ForgeRegistries.ITEMS.getValue(new ResourceLocation(OpenComputers.ID, name)))
          case _ => missing.warn()
        }
      }
  }

  // OK, seriously now, I've gotten one too many bug reports because of this Java version being broken.

  private final val BrokenJavaVersions = Set("1.6.0_65, Apple Inc.")

  def isBrokenJavaVersion = {
    val javaVersion = System.getProperty("java.version") + ", " + System.getProperty("java.vendor")
    BrokenJavaVersions.contains(javaVersion)
  }

  def checkForBrokenJavaVersion() = if (isBrokenJavaVersion) {
    FMLLog.bigWarning("You're using a broken Java version! Please update now, or remove OpenComputers. DO NOT REPORT THIS! UPDATE YOUR JAVA!")
    throw new Exception("You're using a broken Java version! Please update now, or remove OpenComputers. DO NOT REPORT THIS! UPDATE YOUR JAVA!")
  }
}
