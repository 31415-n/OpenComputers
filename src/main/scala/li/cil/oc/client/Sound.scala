package li.cil.oc.client

import java.net.MalformedURLException
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import java.util.Timer
import java.util.TimerTask
import java.util.UUID

import com.google.common.base.Charsets
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import net.minecraft.client.Minecraft
import net.minecraft.client.sounds.SoundManager
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.event.TickEvent.ClientTickEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import paulscode.sound.{SoundSystem, SoundSystemConfig}

import scala.collection.mutable
import scala.io.Source

object Sound {
  private val sources = mutable.Map.empty[BlockEntity, PseudoLoopingStream]

  private val commandQueue = mutable.PriorityQueue.empty[Command]

  private var lastVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.BLOCKS)

  private val updateTimer = new Timer("OpenComputers-SoundUpdater", true)
  if (Settings.get.soundVolume > 0) {
    updateTimer.scheduleAtFixedRate(new TimerTask {
      override def run(): Unit = {
        sources.synchronized {
          updateCallable = Some(() => {
          updateVolume()
          processQueue()
          })
        }
      }
    }, 500, 50)
  }

  private var updateCallable = None: Option[() => Unit]

  // Set in init event.
  var manager: SoundManager = _

  def soundSystem: SoundSystem = if (manager != null) {
    try {
      // Access the sound engine through reflection since it's not directly accessible in 1.20.1
      val field = manager.getClass.getDeclaredField("soundEngine")
      field.setAccessible(true)
      field.get(manager).asInstanceOf[SoundSystem]
    } catch {
      case _: Throwable => null
    }
  } else null

  private def updateVolume(): Unit = {
    val volume =
      if (isGamePaused) 0f
      else Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.BLOCKS)
    if (volume != lastVolume) {
      lastVolume = volume
      sources.synchronized {
        for (sound <- sources.values) {
          sound.updateVolume()
        }
      }
    }
  }

  private def isGamePaused = {
    val minecraft = Minecraft.getInstance()
    val server = minecraft.getSingleplayerServer
    // Check outside of match to avoid client side class access.
    server != null && !server.isDedicatedServer && minecraft.isPaused
  }

  private def processQueue(): Unit = {
    if (commandQueue.nonEmpty) {
      commandQueue.synchronized {
        while (commandQueue.nonEmpty && commandQueue.head.when < System.currentTimeMillis()) {
          try commandQueue.dequeue()() catch {
            case t: Throwable => OpenComputers.log.warn("Error processing sound command.", t)
          }
        }
      }
    }
  }

  def startLoop(blockEntity: BlockEntity, name: String, volume: Float = 1f, delay: Long = 0): Unit = {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new StartCommand(System.currentTimeMillis() + delay, blockEntity, name, volume)
      }
    }
  }

  def stopLoop(blockEntity: BlockEntity): Unit = {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new StopCommand(blockEntity)
      }
    }
  }

  def updatePosition(blockEntity: BlockEntity): Unit = {
    if (Settings.get.soundVolume > 0) {
      commandQueue.synchronized {
        commandQueue += new UpdatePositionCommand(blockEntity)
      }
    }
  }

  // Sound loading is handled differently in 1.20.1
  def initSoundManager(): Unit = {
    manager = Minecraft.getInstance().getSoundManager
  }

  private var hasPreloaded = Settings.get.soundVolume <= 0

  @SubscribeEvent
  def onTick(e: ClientTickEvent): Unit = {
    if (manager == null) {
      initSoundManager()
    }
    
    if (soundSystem != null) {
      if (!hasPreloaded) {
        hasPreloaded = true
        new Thread(new Runnable() {
          override def run(): Unit = {
            val preloadConfigLocation = new ResourceLocation(Settings.resourceDomain, "sounds/preload.cfg")
            try {
              val preloadConfigResource = Minecraft.getInstance().getResourceManager.getResource(preloadConfigLocation)
              for (location <- Source.fromInputStream(preloadConfigResource.get().open())(Charsets.UTF_8).getLines()) {
                val url = getClass.getClassLoader.getResource(location)
                if (url != null) try {
                  val sourceName = "preload_" + location
                  soundSystem.newSource(false, sourceName, url, location, true, 0, 0, 0, SoundSystemConfig.ATTENUATION_NONE, 16)
                  soundSystem.activate(sourceName)
                  soundSystem.removeSource(sourceName)
                } catch {
                  case t: Throwable => 
                    OpenComputers.log.debug(s"Failed to preload sound $location", t)
                }
                else OpenComputers.log.warn(s"Couldn't preload sound $location!")
              }
            } catch {
              case _: Throwable => // Resource not found or other error
            }
          }
        }).start()
      }

      sources.synchronized {
        updateCallable.foreach(_ ())
        updateCallable = None
      }
    }
  }

  @SubscribeEvent
  def onWorldUnload(event: LevelEvent.Unload): Unit = {
    commandQueue.synchronized(commandQueue.clear())
    sources.synchronized(try sources.foreach(_._2.stop()) catch {
      case _: Throwable => // Ignore.
    })
    sources.clear()
  }

  private abstract class Command(val when: Long, val blockEntity: BlockEntity) extends Ordered[Command] {
    def apply(): Unit

    override def compare(that: Command) = (that.when - when).toInt
  }

  private class StartCommand(when: Long, blockEntity: BlockEntity, val name: String, val volume: Float) extends Command(when, blockEntity) {
    override def apply(): Unit = {
      sources.synchronized {
        sources.getOrElseUpdate(blockEntity, new PseudoLoopingStream(blockEntity, volume)).play(name)
      }
    }
  }

  private class StopCommand(blockEntity: BlockEntity) extends Command(System.currentTimeMillis() + 1, blockEntity) {
    override def apply(): Unit = {
      sources.synchronized {
        sources.remove(blockEntity) match {
          case Some(sound) => sound.stop()
          case _ =>
        }
      }
      commandQueue.synchronized {
        // Remove all other commands for this block entity from the queue. This
        // is inefficient, but we generally don't expect the command queue to
        // be very long, so this should be OK.
        commandQueue ++= commandQueue.dequeueAll.filter(_.blockEntity != blockEntity)
      }
    }
  }

  private class UpdatePositionCommand(blockEntity: BlockEntity) extends Command(System.currentTimeMillis(), blockEntity) {
    override def apply(): Unit = {
      sources.synchronized {
        sources.get(blockEntity) match {
          case Some(sound) => sound.updatePosition()
          case _ =>
        }
      }
    }
  }

  private class PseudoLoopingStream(val blockEntity: BlockEntity, val volume: Float, val source: String = UUID.randomUUID.toString) {
    var initialized = false

    def updateVolume(): Unit = {
      if (soundSystem != null) {
        soundSystem.setVolume(source, lastVolume * volume * Settings.get.soundVolume)
      }
    }

    def updatePosition(): Unit = {
      if (soundSystem != null) {
        if (blockEntity != null) soundSystem.setPosition(source, blockEntity.getBlockPos.getX, blockEntity.getBlockPos.getY, blockEntity.getBlockPos.getZ)
        else soundSystem.setPosition(source, 0, 0, 0)
      }
    }

    def play(name: String): Unit = {
      if (soundSystem != null) {
        val resourceName = s"${Settings.resourceDomain}:$name"
        val resourceLocation = new ResourceLocation(resourceName)
        
        if (!initialized) {
          initialized = true
          try {
            val url = toUrl(resourceLocation)
            if (blockEntity != null) {
              soundSystem.newSource(false, source, url, resourceLocation.toString, true, 
                blockEntity.getBlockPos.getX.toFloat, 
                blockEntity.getBlockPos.getY.toFloat, 
                blockEntity.getBlockPos.getZ.toFloat, 
                SoundSystemConfig.ATTENUATION_LINEAR, 16)
            } else {
              soundSystem.newSource(false, source, url, resourceLocation.toString, false, 
                0f, 0f, 0f, 
                SoundSystemConfig.ATTENUATION_NONE, 0)
            }
            updateVolume()
            soundSystem.activate(source)
          } catch {
            case t: Throwable => 
              OpenComputers.log.warn(s"Failed to initialize sound source $source", t)
          }
        }
        try {
          soundSystem.play(source)
        } catch {
          case t: Throwable => 
            OpenComputers.log.warn(s"Failed to play sound $source", t)
        }
      }
    }

    def stop(): Unit = {
      if (soundSystem != null) try {
        soundSystem.stop(source)
        soundSystem.removeSource(source)
      }
      catch {
        case _: Throwable =>
      }
    }
  }

  // This is copied from SoundManager.getURLForSoundResource, which is private.
  private def toUrl(resource: ResourceLocation): URL = {
    val name = s"mcsounddomain:${resource.getNamespace}:${resource.getPath}"
    try {
      new URL(null, name, new URLStreamHandler {
        protected def openConnection(url: URL): URLConnection = new URLConnection(url) {
          def connect(): Unit = {
          }

          override def getInputStream = try {
            Minecraft.getInstance().getResourceManager.getResource(resource).get().open()
          } catch {
            case t: Throwable =>
              OpenComputers.log.warn(t)
              null
          }
        }
      })
    }
    catch {
      case _: MalformedURLException => null
    }
  }
}
