package li.cil.oc.common

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import li.cil.oc.api.network.ManagedEnvironment
import net.minecraft.world.level.Level
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

import scala.jdk.CollectionConverters._
import scala.collection.mutable

/**
 * Keeps track of loaded components by ID. Used to send messages between
 * component representation on server and client without knowledge of their
 * containers. For now this is only used for screens / text buffer components.
 */
abstract class ComponentTracker {
  private val worlds = mutable.Map.empty[Int, Cache[String, ManagedEnvironment]]

  private def components(world: Level) = {
    worlds.getOrElseUpdate(world.dimension().location().hashCode(),
      com.google.common.cache.CacheBuilder.newBuilder().
        weakValues().
        asInstanceOf[CacheBuilder[String, ManagedEnvironment]].
        build[String, ManagedEnvironment]())
  }

  def add(world: Level, address: String, component: ManagedEnvironment): Unit = {
    this.synchronized {
      components(world).put(address, component)
    }
  }

  def remove(world: Level, component: ManagedEnvironment): Unit = {
    this.synchronized {
      components(world).invalidateAll(components(world).asMap().filter(_._2 == component).keys.asJava)
      components(world).cleanUp()
    }
  }

  def get(world: Level, address: String): Option[ManagedEnvironment] = this.synchronized {
    components(world).cleanUp()
    Option(components(world).getIfPresent(address))
  }

  @SubscribeEvent
  def onWorldUnload(e: LevelEvent.Unload): Unit = clear(e.getLevel)

  protected def clear(world: Level): Unit = this.synchronized {
    components(world).invalidateAll()
    components(world).cleanUp()
  }
}
