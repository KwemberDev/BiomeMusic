package biomemusic.handlers;

import biomemusic.BiomeMusic;
import biomemusic.combatutils.TargetingUtils;
import biomemusic.musicplayer.CustomMusicPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import static biomemusic.combatutils.TargetingUtils.countHistory;
import static biomemusic.handlers.MainMenuMusicHandler.isMainMenuMusicPlaying;
import static biomemusic.handlers.MainMenuMusicHandler.isMainMenuScreen;
import static biomemusic.musicplayer.CustomMusicPlayer.*;

@Mod.EventBusSubscriber
public class PauseEventHandler {

    private boolean hasQueueBeenReset = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world == null && !hasQueueBeenReset) {
            TargetingUtils.resetQueue();
            hasQueueBeenReset = true;
        } else if (mc.world != null) {
            hasQueueBeenReset = false;
        }

        // Check if the player is in a world or not
        if (mc.world == null && !isMainMenuMusicPlaying) {
            // If no world is loaded (i.e., player is in main menu), stop any playing music
            if (CustomMusicPlayer.isMusicPlaying()) {
                CustomMusicPlayer.stopMusic();

            }
            return;  // Exit early, no need to check further
        }

        // Check if any pause-related GUI is open, including submenus from the pause screen
        if (isPauseMenuOpen(mc)) {
            // If a pause menu is open, pause the music
            if (CustomMusicPlayer.isMusicPlaying() && !CustomMusicPlayer.isPaused()) {
                CustomMusicPlayer.pauseMusic();
                BiomeMusic.LOGGER.info("Paused Music");
            } else if (combatMusicClip.isRunning() && !CustomMusicPlayer.isPaused()) {
                CustomMusicPlayer.pauseCombatMusic();
                BiomeMusic.LOGGER.info("Paused Combat Music");
            }
        } else {
            // If no pause-related menu is open, resume the music if it was paused

            if (CustomMusicPlayer.isPaused() && !isMainMenuScreen(mc)) {
                if (!CustomMusicPlayer.isFading) {  // Use the class's field for fading check
                    CustomMusicPlayer.adjustVolume();
                }
                CustomMusicPlayer.resumeMusic();
            }
        }
    }

    private boolean isPauseMenuOpen(Minecraft mc) {
        if (mc.currentScreen == null) {
            return false;  // No screen is open, game is not paused
        }

        // Check for known pause-related GUIs
        boolean isKnownPauseMenu = mc.currentScreen instanceof GuiIngameMenu
                || mc.currentScreen instanceof GuiOptions
                || mc.currentScreen instanceof GuiStats
                || mc.currentScreen instanceof GuiVideoSettings
                || mc.currentScreen instanceof GuiControls
                || mc.currentScreen instanceof GuiLanguage
                || mc.currentScreen instanceof GuiScreenOptionsSounds
                || mc.currentScreen instanceof GuiConfig
                || mc.currentScreen instanceof GuiGameOver
                || mc.currentScreen instanceof GuiScreenResourcePacks
                || mc.currentScreen instanceof GuiShareToLan
                || mc.currentScreen instanceof GuiModList
                || mc.currentScreen instanceof GuiCustomizeSkin
                || mc.currentScreen instanceof GuiSnooper
                || mc.currentScreen instanceof ScreenChatOptions
                ;

        // Additional checks for GUIs that do not extend GuiScreen
        if (mc.currentScreen.getClass().getName().contains("Advancement")) {
            return true;  // Pause for advancement GUI
        }

        if (mc.currentScreen.getClass().getName().contains("LockIconButton")) {
            return true;  // Pause for lock icon GUI
        }

        // If any of the known or custom conditions are met, return true
        return isKnownPauseMenu;
    }

}
