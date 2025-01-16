package musify.proxy;

import musify.handlers.BiomeMusicEventHandler;
import musify.handlers.MainMenuMusicHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;


@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent e) {
        super.preInit(e);
        MinecraftForge.EVENT_BUS.register(new BiomeMusicEventHandler());
        MinecraftForge.EVENT_BUS.register(new MainMenuMusicHandler());
    }


}