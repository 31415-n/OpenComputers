package li.cil.oc.common.tileentity

import li.cil.oc.api
import li.cil.oc.api.network.Visibility
import li.cil.oc.common
import li.cil.oc.Constants
import li.cil.oc.util.Color
import net.minecraft.world.item.DyeColor
import li.cil.oc.util.ItemColorizer
import net.minecraft.world.item.ItemStack
import net.minecraftforge.client.model.data.ModelData
import net.minecraftforge.client.model.data.ModelDataManager

class Cable extends traits.Environment with traits.NotAnalyzable with traits.ImmibisMicroblock with traits.Colored {
  val node = api.Network.newNode(this, Visibility.None).create()

  setColor(Color.rgbValues(DyeColor.LIGHT_GRAY))

  def createItemStack() = {
    val stack = api.Items.get(Constants.BlockName.Cable).createItemStack(1)
    if (getColor != Color.rgbValues(DyeColor.LIGHT_GRAY)) {
      ItemColorizer.setColor(stack, getColor)
    }
    stack
  }

  def fromItemStack(stack: ItemStack): Unit = {
    if (ItemColorizer.hasColor(stack)) {
      setColor(ItemColorizer.getColor(stack))
    }
  }

  override def controlsConnectivity = true

  override def consumesDye = true

  override protected def onColorChanged() {
    super.onColorChanged()
    if (getWorld != null && isServer) {
      api.Network.joinOrCreateNetwork(this)
    }
  }

  override def getRenderBoundingBox = common.block.Cable.bounds(getLevel, getBlockPos)
  
  // Provide ModelData for rendering
  override def getModelData(): ModelData = {
    import li.cil.oc.client.renderer.block.CableModel
    
    val neighbors = common.block.Cable.neighbors(getLevel, getBlockPos)
    val color = getColor
    var isCableMask = 0
    
    for (side <- net.minecraft.core.Direction.values()) {
      if (getLevel.getBlockEntity(getBlockPos.relative(side)).isInstanceOf[Cable]) {
        isCableMask = common.block.Cable.mask(side, isCableMask)
      }
    }
    
    ModelData.builder()
      .`with`(CableModel.NEIGHBORS_PROPERTY, Integer.valueOf(neighbors))
      .`with`(CableModel.COLOR_PROPERTY, Integer.valueOf(color))
      .`with`(CableModel.IS_SIDE_CABLE_PROPERTY, Integer.valueOf(isCableMask))
      .build()
  }
  
  override def requestModelDataUpdate(): Unit = {
    super.requestModelDataUpdate()
    ModelDataManager.requestRefresh(this)
  }
}
