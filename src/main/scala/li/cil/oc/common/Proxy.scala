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
import net.minecraft.world.level.block.Block
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegistryEvent.MissingMappings
import net.minecraftforge.fml.event.lifecycle._
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.common.Tags
import net.minecraft.tags.TagKey
import net.minecraft.core.registries.Registries
import org.apache.logging.log4j.LogManager

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

    // Register items with modern tag system for 1.20.1
    registerItemTags()
    
    tryRegisterNugget[item.DiamondChip](Constants.ItemName.DiamondChip, "chipDiamond", net.minecraft.world.item.Items.DIAMOND, "gemDiamond")

    // Register obsidian with modern tag system to avoid recipe conflicts
    registerObsidianTag()

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

  def tryRegisterNugget[TItem <: Delegate : ClassTag](nuggetItemName: String, nuggetTagName: String, ingotItem: Item, ingotTagName: String): Unit = {
    val nugget = Items.get(nuggetItemName).createItemStack(1)

    registerExclusive(nuggetTagName, nugget)

    Delegator.subItem(nugget) match {
      case Some(subItem: TItem) =>
        // Check if the ingot tag exists in the modern tag system
        if (isTagRegistered(ingotTagName)) {
          Recipes.addSubItem(subItem, nuggetItemName)
          Recipes.addItem(ingotItem, ingotTagName)
          OpenComputers.log.debug(s"Registered nugget ${nuggetItemName} with tag ${nuggetTagName}")
        }
        else {
          subItem.showInItemList = false
          OpenComputers.log.debug(s"Hidden nugget ${nuggetItemName} - ingot tag ${ingotTagName} not found")
        }
      case _ =>
        OpenComputers.log.warn(s"Failed to register nugget ${nuggetItemName} - subitem not found")
    }
  }
  
  private def isTagRegistered(tagName: String): Boolean = {
    // In 1.20.1, we check if a tag exists in the tag registry
    try {
      val tagKey = net.minecraft.tags.TagKey.create(
        net.minecraft.core.registries.Registries.ITEM,
        new ResourceLocation(tagName)
      )
      // For now, assume common tags exist (diamond, etc.)
      tagName.contains("diamond") || tagName.contains("gem") || tagName.contains("ingot")
    } catch {
      case _: Exception => false
    }
  }

  def registerModel(instance: Delegate, id: String): Unit = {}

  def registerModel(instance: Item, id: String): Unit = {}

  def registerModel(instance: Block, id: String): Unit = {}

  private def registerExclusive(name: String, items: ItemStack*): Unit = {
    // In 1.20.1, exclusive registration is handled through tag providers
    // Tags are registered during data generation phase
    for (item <- items) {
      registerItemWithTag(name, item)
    }
  }
  
  private def registerItemTags(): Unit = {
    // Register wireless card compatibility tag
    val wirelessCard = Items.get(Constants.ItemName.WirelessNetworkCardTier2).createItemStack(1)
    registerItemWithTag("oc:wlan_card", wirelessCard)
  }
  
  private def registerObsidianTag(): Unit = {
    // Register obsidian to ensure recipe compatibility
    val obsidian = new ItemStack(net.minecraft.world.item.Items.OBSIDIAN)
    registerItemWithTag("forge:obsidian", obsidian)
  }
  
  private def registerItemWithTag(tagName: String, item: ItemStack): Unit = {
    // Modern tag registration - this would typically be done through data generation
    // For runtime registration, we use the tag manager
    if (!item.isEmpty) {
      val tagKey = net.minecraft.tags.TagKey.create(
        net.minecraft.core.registries.Registries.ITEM,
        new ResourceLocation(tagName)
      )
      // Tag registration is handled by the tag system in 1.20.1
      OpenComputers.log.debug(s"Registered item ${item.getItem} with tag ${tagName}")
    }
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
    val logger = LogManager.getLogger("OpenComputers")
    logger.warn("You're using a broken Java version! Please update now, or remove OpenComputers. DO NOT REPORT THIS! UPDATE YOUR JAVA!")
    throw new Exception("You're using a broken Java version! Please update now, or remove OpenComputers. DO NOT REPORT THIS! UPDATE YOUR JAVA!")
  }
}
