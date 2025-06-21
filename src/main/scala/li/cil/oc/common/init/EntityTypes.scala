package li.cil.oc.common.init

import li.cil.oc.OpenComputers
import li.cil.oc.common.entity.Drone
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

/**
 * Entity type registration for OpenComputers 1.20.1.
 * Replaces the old EntityRegistry.registerModEntity system from 1.12.2.
 */
object EntityTypes {
  
  val ENTITIES: DeferredRegister[EntityType[_]] = 
    DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, OpenComputers.ID)

  /**
   * Drone entity type registration.
   * Equivalent to: EntityRegistry.registerModEntity(classOf[Drone], "Drone", 0, OpenComputers, 80, 1, true)
   */
  val DRONE: RegistryObject[EntityType[Drone]] = 
    ENTITIES.register("drone", () => 
      EntityType.Builder.of[Drone](
        (entityType, level) => new Drone(level),
        MobCategory.MISC
      )
      .sized(12f / 16f, 6f / 16f) // setSize(12 / 16f, 6 / 16f) from original
      .setTrackingRange(80) // tracking range from original registration
      .setUpdateInterval(1) // update interval from original registration
      .setShouldReceiveVelocityUpdates(true) // velocity updates from original registration
      .fireImmune() // isImmuneToFire = true from original
      .build("drone")
    )
}