package li.cil.oc

import li.cil.oc.common.IMC
import li.cil.oc.common.Proxy
import li.cil.oc.server.command.CommandHandler
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle._
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.InterModComms
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import li.cil.oc.util.ThreadPoolFactory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(OpenComputers.ID)
object OpenComputers {
  final val ID = "opencomputers"

  final val Name = "OpenComputers"

  final val McVersion = "1.12.2-forge"

  final val Version = "@VERSION@"

  def log: Logger = logger.getOrElse(LogManager.getLogger(Name))

  var logger: Option[Logger] = None

  var proxy: Proxy = _

  // Initialize proxy based on dist
  if (net.minecraftforge.api.distmarker.Dist.CLIENT.isClient) {
    proxy = new li.cil.oc.client.Proxy()
  } else {
    proxy = new li.cil.oc.server.Proxy()
  }

  FMLJavaModLoadingContext.get().getModEventBus.addListener(this.commonSetup)
  FMLJavaModLoadingContext.get().getModEventBus.addListener(this.clientSetup)
  FMLJavaModLoadingContext.get().getModEventBus.addListener(this.serverSetup)

  def commonSetup(event: FMLCommonSetupEvent): Unit = {
    logger = Option(LogManager.getLogger(Name))
    proxy.preInit(event)
    OpenComputers.log.info("Done with common setup phase.")
  }

  def clientSetup(event: FMLClientSetupEvent): Unit = {
    proxy.init(event)
    OpenComputers.log.info("Done with client setup phase.")
  }

  def serverSetup(event: FMLDedicatedServerSetupEvent): Unit = {
    proxy.postInit(event)
    OpenComputers.log.info("Done with server setup phase.")
  }

  @SubscribeEvent
  def serverStart(e: ServerStartingEvent): Unit = {
    CommandHandler.register(e)
    ThreadPoolFactory.safePools.foreach(_.newThreadPool())

    if (Settings.get.internetAccessConfigured()) {
      if (Settings.get.internetFilteringRulesInvalid()) {
        OpenComputers.log.warn("####################################################")
        OpenComputers.log.warn("#                                                  #")
        OpenComputers.log.warn("#  Could not parse Internet Card filtering rules!  #")
        OpenComputers.log.warn("#  Review the server log and adjust the filtering  #")
        OpenComputers.log.warn("#  list to ensure it is appropriately configured.  #")
        OpenComputers.log.warn("#   (config/OpenComputers.cfg => filteringRules)   #")
        OpenComputers.log.warn("# Internet access has been automatically disabled. #")
        OpenComputers.log.warn("#                                                  #")
        OpenComputers.log.warn("####################################################")
      } else if (!Settings.get.internetFilteringRulesObserved && e.getServer.isDedicatedServer) {
        OpenComputers.log.warn("####################################################")
        OpenComputers.log.warn("#                                                  #")
        OpenComputers.log.warn("#    It appears that you're running a dedicated    #")
        OpenComputers.log.warn("#  server with OpenComputers installed! Make sure  #")
        OpenComputers.log.warn("#  to review the Internet Card address filtering   #")
        OpenComputers.log.warn("#  list to ensure it is appropriately configured.  #")
        OpenComputers.log.warn("#   (config/OpenComputers.cfg => filteringRules)   #")
        OpenComputers.log.warn("#                                                  #")
        OpenComputers.log.warn("####################################################")
      } else {
        OpenComputers.log.info(f"Successfully applied ${Settings.get.internetFilteringRules.length} Internet Card filtering rules.")
      }
    }
  }

  @SubscribeEvent
  def serverStop(e: ServerStoppedEvent): Unit = {
    ThreadPoolFactory.safePools.foreach(_.waitForCompletion())
  }

  @SubscribeEvent
  def imc(event: net.minecraftforge.fml.event.lifecycle.InterModProcessEvent): Unit = IMC.handleEvent(event)
}
