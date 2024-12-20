package biomemusic;

import biomemusic.handlers.BiomeMusicConfig;
import biomemusic.handlers.PauseEventHandler;
import biomemusic.proxy.CommonProxy;
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

@Mod(modid = BiomeMusic.MODID, version = BiomeMusic.VERSION, name = BiomeMusic.NAME, clientSideOnly = true)
public class BiomeMusic {
    public static final String MODID = "biomemusic";
    public static final String VERSION = "Beta 0.1.9";
    public static final String NAME = "BiomeMusic!";
    public static final Logger LOGGER = LogManager.getLogger();
	
    @SidedProxy(clientSide = "biomemusic.proxy.ClientProxy", serverSide = "biomemusic.proxy.CommonProxy")
    public static CommonProxy PROXY;
	
	@Instance(MODID)
	public static BiomeMusic instance;

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