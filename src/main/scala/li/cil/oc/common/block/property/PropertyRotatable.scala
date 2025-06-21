package li.cil.oc.common.block.property

import com.google.common.base.Predicate
import com.google.common.base.Predicates
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.core.Direction

import scala.jdk.CollectionConverters._

object PropertyRotatable {
  final val Facing = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL.asInstanceOf[Predicate[Direction]])
  final val Pitch = DirectionProperty.create("pitch", Predicates.in(java.util.Arrays.asList(Direction.DOWN, Direction.UP, Direction.NORTH)))
  final val Yaw = DirectionProperty.create("yaw", Direction.Plane.HORIZONTAL.asInstanceOf[Predicate[Direction]])
}
