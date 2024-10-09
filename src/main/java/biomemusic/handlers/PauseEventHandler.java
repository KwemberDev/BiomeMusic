package biomemusic.handlers;

import biomemusic.BiomeMusic;
import biomemusic.musicplayer.CustomMusicPlayer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import static biomemusic.musicplayer.CustomMusicPlayer.adjustVolume;
import static biomemusic.musicplayer.CustomMusicPlayer.isFading;

@Mod.EventBusSubscriber
public class PauseEventHandler {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();

        // Check if the player is in a world or not
        if (mc.world == null) {
            // If no world is loaded (i.e., player is in main menu), stop any playing music
            if (CustomMusicPlayer.isMusicPlaying()) {
                CustomMusicPlayer.stopMusic();
            }
            return;  // Exit early, no need to check further
        }

        // If the game is paused, pause the music
        if (mc.isGamePaused()) {
            if (CustomMusicPlayer.isMusicPlaying() && !CustomMusicPlayer.isPaused()) {
                CustomMusicPlayer.pauseMusic();
                BiomeMusic.LOGGER.info("Paused Music");
            }
        } else {
            // If the game is resumed and music was paused, resume it
            if (CustomMusicPlayer.isPaused()) {
                if (!isFading) {
                    adjustVolume();
                }
                CustomMusicPlayer.resumeMusic();
            }
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        // Check if this is the client side (since we only care about the client)
        if (event.getWorld().isRemote) {
            // Stop any custom music that is playing
            if (CustomMusicPlayer.isMusicPlaying()) {
                CustomMusicPlayer.stopMusic();
                BiomeMusic.LOGGER.info("Stopped music due to world unload");
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Check if the event is triggered on the client side
        if (event.player.world.isRemote) {
            if (CustomMusicPlayer.isMusicPlaying()) {
                CustomMusicPlayer.stopMusic();
                BiomeMusic.LOGGER.info("Stopped music due to player logout");
            }
        }
    }
}
