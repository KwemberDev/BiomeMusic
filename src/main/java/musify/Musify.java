package musify;

import musify.handlers.BiomeMusicConfig;
import musify.handlers.PauseEventHandler;
import musify.proxy.CommonProxy;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = Musify.MODID, version = Musify.VERSION, name = Musify.NAME, clientSideOnly = true)
public class Musify {
    public static final String MODID = "musify";
    public static final String VERSION = "Beta 0.1.10";
    public static final String NAME = "BiomeMusic!";
    public static final Logger LOGGER = LogManager.getLogger();
	
    @SidedProxy(clientSide = "musify.proxy.ClientProxy", serverSide = "musify.proxy.CommonProxy")
    public static CommonProxy PROXY;
	
	@Instance(MODID)
	public static Musify instance;

    public static File musicFolder;
	
	@Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {

        File minecraftDir = event.getModConfigurationDirectory().getParentFile();
        musicFolder = new File(minecraftDir, "biomemusic");

        if (!musicFolder.exists()) {
            musicFolder.mkdirs();
        }

    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new PauseEventHandler());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        BiomeMusicConfig.updateBiomeList();
        BiomeMusicConfig.updateBiomeTagList();
        BiomeMusicConfig.updateMusicList();
    }
}