package li.cil.oc.client.renderer.block

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.item.CustomModel
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.traits.Delegate
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemBlockRenderTypes
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.client.renderer.block.model.ItemOverrides
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.resources.ResourceLocation
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraftforge.client.event.ModelEvent
import net.minecraftforge.client.model.data.ModelData
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.client.model.geometry.IGeometryBakingContext
import net.minecraftforge.client.model.geometry.IUnbakedGeometry
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.RenderType

import scala.jdk.CollectionConverters._
import scala.collection.mutable
import java.util.{List => JList}

/**
 * Model initialization system for OpenComputers blocks and items in 1.20.1.
 * Handles registration of custom models, block state mappers, and model baking.
 */
object ModelInitialization {
  // Model resource locations for special blocks
  final val CableBlockLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, Constants.BlockName.Cable), "")
  final val CableItemLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, Constants.BlockName.Cable), "inventory")
  final val NetSplitterBlockLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, Constants.BlockName.NetSplitter), "")
  final val NetSplitterItemLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, Constants.BlockName.NetSplitter), "inventory")
  final val PrintBlockLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, Constants.BlockName.Print), "")
  final val PrintItemLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, Constants.BlockName.Print), "inventory")
  final val RobotBlockLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, Constants.BlockName.Robot), "")
  final val RobotItemLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, Constants.BlockName.Robot), "inventory")
  final val RobotAfterimageBlockLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, Constants.BlockName.RobotAfterimage), "")
  final val RobotAfterimageItemLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, Constants.BlockName.RobotAfterimage), "inventory")
  final val RackBlockLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, Constants.BlockName.Rack), "")

  // Collections for deferred registration
  private val meshableItems = mutable.ArrayBuffer.empty[Item]
  private val itemDelegates = mutable.ArrayBuffer.empty[(String, Delegate)]
  private val itemDelegatesCustom = mutable.ArrayBuffer.empty[Delegate with CustomModel]
  private val registeredModels = mutable.Map.empty[ResourceLocation, BakedModel]

  /**
   * Pre-initialization phase - register event handlers and basic models.
   */
  def preInit(): Unit = {
    MinecraftForge.EVENT_BUS.register(this)

    // Register special block models
    registerSpecialBlockModel(Constants.BlockName.Cable, CableBlockLocation, CableItemLocation)
    registerSpecialBlockModel(Constants.BlockName.NetSplitter, NetSplitterBlockLocation, NetSplitterItemLocation)
    registerSpecialBlockModel(Constants.BlockName.Print, PrintBlockLocation, PrintItemLocation)
    registerSpecialBlockModel(Constants.BlockName.Robot, RobotBlockLocation, RobotItemLocation)
    registerSpecialBlockModel(Constants.BlockName.RobotAfterimage, RobotAfterimageBlockLocation, RobotAfterimageItemLocation)
  }

  /**
   * Handle model registration event in 1.20.1.
   */
  @SubscribeEvent
  def onRegisterModels(event: ModelEvent.RegisterAdditional): Unit = {
    registerAdditionalModels(event)
    registerItems()
    registerSubItems()
    registerSubItemsCustom()
  }

  /**
   * Handle model baking event in 1.20.1.
   */
  @SubscribeEvent
  def onBakeModels(event: ModelEvent.BakingCompleted): Unit = {
    val modelManager = event.getModelManager
    val modelBakery = event.getModelBakery

    // Replace models with custom implementations
    replaceCustomModels(modelManager, modelBakery)
    
    // Handle custom model delegates
    for (item <- itemDelegatesCustom) {
      item.bakeModels(event)
    }

    applyModelOverrides(modelManager, modelBakery)
  }

  // ----------------------------------------------------------------------- //

  /**
   * Register a delegate item model.
   */
  def registerModel(instance: Delegate, id: String): Unit = {
    instance match {
      case customModel: CustomModel => itemDelegatesCustom += customModel
      case _ => itemDelegates += id -> instance
    }
  }

  /**
   * Register a regular item model.
   */
  def registerModel(instance: Item, id: String): Unit = {
    meshableItems += instance
  }

  /**
   * Register a block model.
   */
  def registerModel(instance: Block, id: String): Unit = {
    val item = instance.asItem()
    registerModel(item, id)
  }

  // ----------------------------------------------------------------------- //

  /**
   * Register special block models that need custom handling.
   */
  private def registerSpecialBlockModel(blockName: String, blockLocation: ModelResourceLocation, itemLocation: ModelResourceLocation): Unit = {
    val descriptor = api.Items.get(blockName)
    val block = descriptor.block()
    val stack = descriptor.createItemStack(1)

    // Store model locations for later registration
    registeredModels += blockLocation.id() -> createCustomBlockModel(blockName)
    registeredModels += itemLocation.id() -> createCustomItemModel(blockName)
  }

  /**
   * Register additional models that need to be loaded.
   */
  private def registerAdditionalModels(event: ModelEvent.RegisterAdditional): Unit = {
    // Register all special model locations
    event.register(CableBlockLocation.id())
    event.register(CableItemLocation.id())
    event.register(NetSplitterBlockLocation.id())
    event.register(NetSplitterItemLocation.id())
    event.register(PrintBlockLocation.id())
    event.register(PrintItemLocation.id())
    event.register(RobotBlockLocation.id())
    event.register(RobotItemLocation.id())
    event.register(RobotAfterimageBlockLocation.id())
    event.register(RobotAfterimageItemLocation.id())
    event.register(RackBlockLocation.id())

    // Register models for all delegate items
    for ((id, _) <- itemDelegates) {
      val location = new ResourceLocation(Settings.resourceDomain, id)
      event.register(location)
    }

    // Register models for custom delegates
    for (item <- itemDelegatesCustom) {
      item.registerModelLocations()
    }
  }

  /**
   * Register regular items with dynamic model resolution.
   */
  private def registerItems(): Unit = {
    val minecraft = Minecraft.getInstance()
    val itemRenderer = minecraft.getItemRenderer
    val modelManager = itemRenderer.getItemModelShaper

    for (item <- meshableItems) {
      registerItemWithModelManager(item, modelManager)
    }
    meshableItems.clear()
  }

  /**
   * Register delegate sub-items.
   */
  private def registerSubItems(): Unit = {
    val minecraft = Minecraft.getInstance()
    val itemRenderer = minecraft.getItemRenderer
    val modelManager = itemRenderer.getItemModelShaper

    for ((id, item) <- itemDelegates) {
      val location = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, id), "inventory")
      modelManager.register(item.parent, item.itemId, location)
    }
    itemDelegates.clear()
  }

  /**
   * Register custom model delegates.
   */
  private def registerSubItemsCustom(): Unit = {
    val minecraft = Minecraft.getInstance()
    val itemRenderer = minecraft.getItemRenderer
    val modelManager = itemRenderer.getItemModelShaper

    for (item <- itemDelegatesCustom) {
      registerCustomModelWithManager(item, modelManager)
    }
  }

  /**
   * Replace models with custom implementations.
   */
  private def replaceCustomModels(modelManager: net.minecraft.client.resources.model.ModelManager, modelBakery: net.minecraft.client.resources.model.ModelBakery): Unit = {
    val models = modelManager.getModel _

    // Replace cable models
    replaceModelInManager(CableBlockLocation.id(), CableModel, modelManager)
    replaceModelInManager(CableItemLocation.id(), CableModel, modelManager)
    
    // Replace net splitter models
    replaceModelInManager(NetSplitterBlockLocation.id(), NetSplitterModel, modelManager)
    replaceModelInManager(NetSplitterItemLocation.id(), NetSplitterModel, modelManager)
    
    // Replace print models
    replaceModelInManager(PrintBlockLocation.id(), PrintModel, modelManager)
    replaceModelInManager(PrintItemLocation.id(), PrintModel, modelManager)
    
    // Replace robot models
    replaceModelInManager(RobotBlockLocation.id(), RobotModel, modelManager)
    replaceModelInManager(RobotItemLocation.id(), RobotModel, modelManager)
    
    // Replace robot afterimage models
    replaceModelInManager(RobotAfterimageBlockLocation.id(), NullModel, modelManager)
    replaceModelInManager(RobotAfterimageItemLocation.id(), NullModel, modelManager)
  }

  /**
   * Apply model overrides for specific blocks.
   */
  private def applyModelOverrides(modelManager: net.minecraft.client.resources.model.ModelManager, modelBakery: net.minecraft.client.resources.model.ModelBakery): Unit = {
    val modelOverrides = Map[String, BakedModel => BakedModel](
      Constants.BlockName.ScreenTier1 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier2 -> (_ => ScreenModel),
      Constants.BlockName.ScreenTier3 -> (_ => ScreenModel),
      Constants.BlockName.Rack -> (parent => new ServerRackModel(parent))
    )

    // Apply overrides to matching models
    for ((name, modelTransform) <- modelOverrides) {
      val pattern = s"^${Settings.resourceDomain}:$name.*"
      applyModelOverrideToManager(pattern, modelTransform, modelManager)
    }
  }

  // Helper methods for 1.20.1 model system

  private def registerItemWithModelManager(item: Item, modelManager: net.minecraft.client.renderer.ItemModelShaper): Unit = {
    // Create dynamic model resolver for OpenComputers items
    val dynamicResolver = new net.minecraft.client.renderer.ItemModelShaper.ItemModelResolver {
      override def resolve(stack: ItemStack): BakedModel = {
        Option(api.Items.get(stack)) match {
          case Some(descriptor) =>
            val location = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, descriptor.name()), "inventory")
            modelManager.getModelManager.getModel(location)
          case _ => modelManager.getModelManager.getMissingModel
        }
      }
    }
    
    // Register with model manager
    modelManager.register(item, dynamicResolver)
  }

  private def registerCustomModelWithManager(delegate: Delegate with CustomModel, modelManager: net.minecraft.client.renderer.ItemModelShaper): Unit = {
    val customResolver = new net.minecraft.client.renderer.ItemModelShaper.ItemModelResolver {
      override def resolve(stack: ItemStack): BakedModel = {
        Delegator.subItem(stack) match {
          case Some(subItem: CustomModel) => 
            val location = subItem.getModelLocation(stack)
            modelManager.getModelManager.getModel(location)
          case _ => modelManager.getModelManager.getMissingModel
        }
      }
    }
    
    modelManager.register(delegate.parent, customResolver)
  }

  private def replaceModelInManager(location: ResourceLocation, replacement: BakedModel, modelManager: net.minecraft.client.resources.model.ModelManager): Unit = {
    // Access the model registry through reflection or model manager API
    try {
      val modelRegistry = modelManager.getClass.getDeclaredField("models")
      modelRegistry.setAccessible(true)
      val models = modelRegistry.get(modelManager).asInstanceOf[java.util.Map[ResourceLocation, BakedModel]]
      models.put(location, replacement)
    } catch {
      case e: Exception => 
        // Fallback: store in our own registry
        registeredModels += location -> replacement
    }
  }

  private def applyModelOverrideToManager(pattern: String, transform: BakedModel => BakedModel, modelManager: net.minecraft.client.resources.model.ModelManager): Unit = {
    try {
      val modelRegistry = modelManager.getClass.getDeclaredField("models")
      modelRegistry.setAccessible(true)
      val models = modelRegistry.get(modelManager).asInstanceOf[java.util.Map[ResourceLocation, BakedModel]]
      
      val matchingModels = models.entrySet().asScala.filter(entry => entry.getKey.toString.matches(pattern))
      for (entry <- matchingModels) {
        val transformedModel = transform(entry.getValue)
        models.put(entry.getKey, transformedModel)
      }
    } catch {
      case e: Exception =>
        // Log error but continue
        println(s"Failed to apply model override for pattern $pattern: ${e.getMessage}")
    }
  }

  private def createCustomBlockModel(blockName: String): BakedModel = {
    blockName match {
      case Constants.BlockName.Cable => CableModel
      case Constants.BlockName.NetSplitter => NetSplitterModel
      case Constants.BlockName.Print => PrintModel
      case Constants.BlockName.Robot => RobotModel
      case Constants.BlockName.RobotAfterimage => NullModel
      case _ => NullModel
    }
  }

  private def createCustomItemModel(blockName: String): BakedModel = {
    blockName match {
      case Constants.BlockName.Cable => CableModel
      case Constants.BlockName.NetSplitter => NetSplitterModel
      case Constants.BlockName.Print => PrintModel
      case Constants.BlockName.Robot => RobotModel
      case Constants.BlockName.RobotAfterimage => NullModel
      case _ => NullModel
    }
  }
}