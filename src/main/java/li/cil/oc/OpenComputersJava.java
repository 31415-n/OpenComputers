package li.cil.oc;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main mod class for OpenComputers 1.20.1 port.
 * This is a Java version of the original Scala OpenComputers class
 * to allow the mod to load while Scala compilation issues are being resolved.
 */
@Mod("opencomputers")
public class OpenComputersJava {
    public static final String ID = "opencomputers";
    public static final String NAME = "OpenComputers";
    public static final String VERSION = "1.9.0-snapshot";
    
    private static final Logger LOGGER = LogManager.getLogger(NAME);
    
    public OpenComputersJava() {
        LOGGER.info("OpenComputers Java main class initializing...");
        
        // Register event listeners
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::serverSetup);
        
        LOGGER.info("OpenComputers Java main class initialized.");
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("OpenComputers common setup phase starting...");
        // TODO: Initialize common components when Scala compilation is fixed
        LOGGER.info("OpenComputers common setup phase completed.");
    }
    
    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("OpenComputers client setup phase starting...");
        // TODO: Initialize client components when Scala compilation is fixed
        LOGGER.info("OpenComputers client setup phase completed.");
    }
    
    private void serverSetup(final FMLDedicatedServerSetupEvent event) {
        LOGGER.info("OpenComputers server setup phase starting...");
        // TODO: Initialize server components when Scala compilation is fixed
        LOGGER.info("OpenComputers server setup phase completed.");
    }
    
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("OpenComputers server starting...");
        // TODO: Register commands and initialize server components when Scala compilation is fixed
    }
    
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        LOGGER.info("OpenComputers server stopped.");
        // TODO: Cleanup server components when Scala compilation is fixed
    }
    
    public static Logger getLogger() {
        return LOGGER;
    }
}