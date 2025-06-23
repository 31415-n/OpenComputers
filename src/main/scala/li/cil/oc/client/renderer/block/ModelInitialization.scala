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
    registeredModels += blockLocation -> createCustomBlockModel(blockName)
    registeredModels += itemLocation -> createCustomItemModel(blockName)
  }

  /**
   * Register additional models that need to be loaded.
   */
  private def registerAdditionalModels(event: ModelEvent.RegisterAdditional): Unit = {
    // Register all special model locations
    event.register(CableBlockLocation)
    event.register(CableItemLocation)
    event.register(NetSplitterBlockLocation)
    event.register(NetSplitterItemLocation)
    event.register(PrintBlockLocation)
    event.register(PrintItemLocation)
    event.register(RobotBlockLocation)
    event.register(RobotItemLocation)
    event.register(RobotAfterimageBlockLocation)
    event.register(RobotAfterimageItemLocation)
    event.register(RackBlockLocation)

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
   * Register regular items with dynamic model resolution using 1.20.1 system.
   */
  private def registerItems(): Unit = {
    // Store dynamic models for later injection during model baking
    for (item <- meshableItems) {
      val baseLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, item.toString), "inventory")
      registeredModels += baseLocation -> createDynamicItemModel()
    }
    meshableItems.clear()
  }

  /**
   * Register delegate sub-items using 1.20.1 model system.
   */
  private def registerSubItems(): Unit = {
    // Create a single dynamic model for the delegator item that handles all sub-items
    if (itemDelegates.nonEmpty) {
      val delegatorItem = itemDelegates.head._2.parent
      val baseLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, delegatorItem.toString), "inventory")
      registeredModels += baseLocation -> createDelegateModel()
      
      // Register individual model locations for each delegate
      for ((id, item) <- itemDelegates) {
        val location = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, id), "inventory")
        // These will be resolved dynamically by the delegate model
      }
    }
    itemDelegates.clear()
  }

  /**
   * Register custom model delegates using 1.20.1 system.
   */
  private def registerSubItemsCustom(): Unit = {
    // Create dynamic models for custom delegates
    if (itemDelegatesCustom.nonEmpty) {
      val delegatorItem = itemDelegatesCustom.head.parent
      val baseLocation = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, delegatorItem.toString), "inventory")
      registeredModels += baseLocation -> createCustomDelegateModel()
    }
  }

  /**
   * Replace models with custom implementations using 1.20.1 model system.
   */
  private def replaceCustomModels(modelManager: net.minecraft.client.resources.model.ModelManager, modelBakery: net.minecraft.client.resources.model.ModelBakery): Unit = {
    val modelRegistry = getModelRegistry(modelManager)
    
    // Replace special block/item models
    modelRegistry.put(CableBlockLocation, CableModel)
    modelRegistry.put(CableItemLocation, CableModel)
    modelRegistry.put(NetSplitterBlockLocation, NetSplitterModel)
    modelRegistry.put(NetSplitterItemLocation, NetSplitterModel)
    modelRegistry.put(PrintBlockLocation, PrintModel)
    modelRegistry.put(PrintItemLocation, PrintModel)
    modelRegistry.put(RobotBlockLocation, RobotModel)
    modelRegistry.put(RobotItemLocation, RobotModel)
    modelRegistry.put(RobotAfterimageBlockLocation, NullModel)
    modelRegistry.put(RobotAfterimageItemLocation, NullModel)
    
    // Inject our dynamic models
    for ((location, model) <- registeredModels) {
      modelRegistry.put(location, model)
    }
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

  /**
   * Create a dynamic model that resolves OpenComputers items based on their descriptor.
   */
  private def createDynamicItemModel(): BakedModel = {
    new BakedModel {
      override def getQuads(state: BlockState, side: Direction, rand: RandomSource): java.util.List[BakedQuad] = java.util.Collections.emptyList()
      override def getQuads(state: BlockState, side: Direction, rand: RandomSource, data: ModelData, renderType: RenderType): java.util.List[BakedQuad] = getQuads(state, side, rand)
      override def useAmbientOcclusion(): Boolean = true
      override def isGui3d: Boolean = true
      override def usesBlockLight(): Boolean = false
      override def isCustomRenderer: Boolean = false
      override def getParticleIcon: TextureAtlasSprite = Minecraft.getInstance().getModelManager.getMissingModel.getParticleIcon
      override def getParticleIcon(data: ModelData): TextureAtlasSprite = getParticleIcon
      override def getTransforms: net.minecraft.client.renderer.block.model.ItemTransforms = net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS
      
      override def getOverrides: ItemOverrides = new ItemOverrides {
        override def resolve(originalModel: BakedModel, stack: ItemStack, world: net.minecraft.world.level.Level, entity: net.minecraft.world.entity.LivingEntity, seed: Int): BakedModel = {
          Option(api.Items.get(stack)) match {
            case Some(descriptor) =>
              val location = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, descriptor.name()), "inventory")
              Minecraft.getInstance().getModelManager.getModel(location)
            case _ => originalModel
          }
        }
      }
    }
  }

  /**
   * Create a dynamic model for delegate items that resolves based on item damage/NBT.
   */
  private def createDelegateModel(): BakedModel = {
    new BakedModel {
      override def getQuads(state: BlockState, side: Direction, rand: RandomSource): java.util.List[BakedQuad] = java.util.Collections.emptyList()
      override def getQuads(state: BlockState, side: Direction, rand: RandomSource, data: ModelData, renderType: RenderType): java.util.List[BakedQuad] = getQuads(state, side, rand)
      override def useAmbientOcclusion(): Boolean = true
      override def isGui3d: Boolean = true
      override def usesBlockLight(): Boolean = false
      override def isCustomRenderer: Boolean = false
      override def getParticleIcon: TextureAtlasSprite = Minecraft.getInstance().getModelManager.getMissingModel.getParticleIcon
      override def getParticleIcon(data: ModelData): TextureAtlasSprite = getParticleIcon
      override def getTransforms: net.minecraft.client.renderer.block.model.ItemTransforms = net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS
      
      override def getOverrides: ItemOverrides = new ItemOverrides {
        override def resolve(originalModel: BakedModel, stack: ItemStack, world: net.minecraft.world.level.Level, entity: net.minecraft.world.entity.LivingEntity, seed: Int): BakedModel = {
          Delegator.subItem(stack) match {
            case Some(subItem) =>
              val location = new ModelResourceLocation(new ResourceLocation(Settings.resourceDomain, subItem.unlocalizedName), "inventory")
              Minecraft.getInstance().getModelManager.getModel(location)
            case _ => originalModel
          }
        }
      }
    }
  }

  /**
   * Create a custom model for delegates with CustomModel trait.
   */
  private def createCustomDelegateModel(): BakedModel = {
    new BakedModel {
      override def getQuads(state: BlockState, side: Direction, rand: RandomSource): java.util.List[BakedQuad] = java.util.Collections.emptyList()
      override def getQuads(state: BlockState, side: Direction, rand: RandomSource, data: ModelData, renderType: RenderType): java.util.List[BakedQuad] = getQuads(state, side, rand)
      override def useAmbientOcclusion(): Boolean = true
      override def isGui3d: Boolean = true
      override def usesBlockLight(): Boolean = false
      override def isCustomRenderer: Boolean = false
      override def getParticleIcon: TextureAtlasSprite = Minecraft.getInstance().getModelManager.getMissingModel.getParticleIcon
      override def getParticleIcon(data: ModelData): TextureAtlasSprite = getParticleIcon
      override def getTransforms: net.minecraft.client.renderer.block.model.ItemTransforms = net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS
      
      override def getOverrides: ItemOverrides = new ItemOverrides {
        override def resolve(originalModel: BakedModel, stack: ItemStack, world: net.minecraft.world.level.Level, entity: net.minecraft.world.entity.LivingEntity, seed: Int): BakedModel = {
          Delegator.subItem(stack) match {
            case Some(subItem: CustomModel) =>
              val location = subItem.getModelLocation(stack)
              Minecraft.getInstance().getModelManager.getModel(location)
            case _ => originalModel
          }
        }
      }
    }
  }

  /**
   * Get the model registry from the model manager using reflection.
   * This is necessary because the model registry is private in 1.20.1.
   */
  private def getModelRegistry(modelManager: net.minecraft.client.resources.model.ModelManager): java.util.Map[ResourceLocation, BakedModel] = {
    try {
      // Try to access the models field through reflection
      val modelRegistryField = modelManager.getClass.getDeclaredField("models")
      modelRegistryField.setAccessible(true)
      modelRegistryField.get(modelManager).asInstanceOf[java.util.Map[ResourceLocation, BakedModel]]
    } catch {
      case _: Exception =>
        // If reflection fails, try alternative field names used in different versions
        try {
          val modelRegistryField = modelManager.getClass.getDeclaredField("bakedRegistry")
          modelRegistryField.setAccessible(true)
          modelRegistryField.get(modelManager).asInstanceOf[java.util.Map[ResourceLocation, BakedModel]]
        } catch {
          case _: Exception =>
            try {
              val modelRegistryField = modelManager.getClass.getDeclaredField("modelRegistry")
              modelRegistryField.setAccessible(true)
              modelRegistryField.get(modelManager).asInstanceOf[java.util.Map[ResourceLocation, BakedModel]]
            } catch {
              case _: Exception =>
                // Create a temporary map - this should not happen in normal operation
                new java.util.HashMap[ResourceLocation, BakedModel]()
            }
        }
    }
  }

  private def applyModelOverrideToManager(pattern: String, transform: BakedModel => BakedModel, modelManager: net.minecraft.client.resources.model.ModelManager): Unit = {
    val modelRegistry = getModelRegistry(modelManager)
    
    val matchingModels = modelRegistry.entrySet().asScala.filter(entry => entry.getKey.toString.matches(pattern))
    for (entry <- matchingModels) {
      val transformedModel = transform(entry.getValue)
      modelRegistry.put(entry.getKey, transformedModel)
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