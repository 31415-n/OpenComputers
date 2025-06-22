package li.cil.oc.common.block.property

import net.minecraft.world.level.block.state.properties.IntegerProperty

/**
 * In 1.20.1, unlisted properties were removed from Forge.
 * We use regular IntegerProperty instead for block state properties.
 * For complex data that can't be stored in block states, use BlockEntity data.
 */
object UnlistedInteger {
  /**
   * Creates an IntegerProperty with the given name and range.
   * This replaces the old IUnlistedProperty system.
   */
  def create(name: String, min: Int = 0, max: Int = 15): IntegerProperty = {
    IntegerProperty.create(name, min, max)
  }
  
  /**
   * Creates an IntegerProperty with a larger range for cases where we need more values.
   */
  def createLarge(name: String, min: Int = 0, max: Int = 255): IntegerProperty = {
    IntegerProperty.create(name, min, max)
  }
}

/**
 * Legacy wrapper class for compatibility.
 * Use UnlistedInteger.create() for new code.
 */
class UnlistedInteger(val name: String) {
  def getName: String = name
  
  def isValid(value: Integer): Boolean = value != null
  
  def getType: Class[Integer] = classOf[Integer]
  
  def valueToString(value: Integer): String = value.toString
  
  /**
   * Creates the actual IntegerProperty for use in block states.
   */
  def toProperty(min: Int = 0, max: Int = 15): IntegerProperty = {
    UnlistedInteger.create(name, min, max)
  }
}
