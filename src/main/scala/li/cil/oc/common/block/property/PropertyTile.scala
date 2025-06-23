package li.cil.oc.common.block.property

import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.client.model.data.ModelProperty

object PropertyTile {
  // ModelProperty for passing block entities to renderers in 1.20.1
  final val TILE_ENTITY_PROPERTY = new ModelProperty[BlockEntity]()
}
