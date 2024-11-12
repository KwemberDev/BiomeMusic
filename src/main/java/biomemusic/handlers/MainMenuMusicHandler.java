package biomemusic.handlers;

import biomemusic.BiomeMusic;
import biomemusic.musicplayer.CustomMusicPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiWorldSelection;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

import static biomemusic.handlers.BiomeMusicEventHandler.isCombatMusicPlaying;
import static biomemusic.musicplayer.CustomMusicPlayer.*;

@Mod.EventBusSubscriber
public class MainMenuMusicHandler {

    public static boolean isMainMenuMusicPlaying = false;
    public static String currentMusic = ""; // Track the currently playing music file

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) throws Exception {

        String mainMenuMusicPath = BiomeMusicConfig.mainMenuMusic; // Get the music from the config

        // Check if the music is set and not equal to the default placeholder
        if (mainMenuMusicPath != null && !mainMenuMusicPath.equals("default_music")) {

            Minecraft mc = Minecraft.getMinecraft();

            if (Loader.isModLoaded("custommainmenu")) {
                if (mc.currentScreen != null && mc.currentScreen.getClass().getName().contains("lumien.custommainmenu")) {
                    if (combatMusicClip != null && combatMusicClip.isRunning()) {
                        stopCombatMusic();
                    }
                    // This is a custom main menu screen
                    stopVanillaMusicMainMenu();
                    playMainMenuMusic();
                }
                if (isMainMenuScreen(mc) && isMainMenuMusicPlaying) {
                    stopVanillaMusicMainMenu();
                }

                if (isMainMenuMusicPlaying && mc.currentScreen != null && !mc.currentScreen.getClass().getName().contains("lumien.custommainmenu") && !isMainMenuScreen(mc)) {
                    CustomMusicPlayer.stopMusic();

                    isMainMenuMusicPlaying = false;
                }

            }

            // Check if the current screen is one of the main menus
            if (isMainMenuScreen(mc) && !Loader.isModLoaded("lumien.custommainmenu")) {
                if (combatMusicClip != null && combatMusicClip.isRunning()) {
                    stopCombatMusic();
                }
                // Stop any vanilla music and attempt to play the custom music
                stopVanillaMusicMainMenu();

                // Wrap the custom music player in a try-catch block to handle invalid file paths
                try {
                    playMainMenuMusic();  // Pass the file path to play
                } catch (Exception e) {
                    // Log an error if the music file cannot be found or loaded
                    BiomeMusic.LOGGER.error("Failed to play main menu music. File not found or invalid: {}", mainMenuMusicPath, e);
                }

                // If no longer in the main menu, stop the custom music
                 if (isMainMenuMusicPlaying && !isMainMenuScreen(mc)) {
                    CustomMusicPlayer.stopMusic();

                     isMainMenuMusicPlaying = false;
                }
            }
        } else {
            if (mainMenuMusicPath.equals("default_music") && isMainMenuMusicPlaying) {
                CustomMusicPlayer.stopMusic();

                isMainMenuMusicPlaying = false;
            }

        }
    }


    // Check if the current screen is one where you want to stop the vanilla music and play custom music
    public static boolean isMainMenuScreen(Minecraft mc) {
        return mc.currentScreen instanceof GuiMainMenu
                || mc.currentScreen instanceof GuiWorldSelection
                || mc.currentScreen instanceof GuiCreateWorld
                || mc.currentScreen instanceof GuiMultiplayer;
    }

    // Play custom music defined in the config
    private static void playMainMenuMusic() throws Exception {
        String mainMenuMusicPath = BiomeMusicConfig.mainMenuMusic;  // Get the configured music file path

        // Check if the correct music is already playing
        if (isMainMenuMusicPlaying && currentMusic.equals(mainMenuMusicPath)) {
            // The correct music is already playing, no need to change
            return;
        }

        BiomeMusic.LOGGER.info("Wrong custom menu music. or no music playing");
        // If the wrong music is playing or no music is playing, stop the current music and play the correct one
        CustomMusicPlayer.stopMusic();  // Stop any currently playing music

        isMainMenuMusicPlaying = true;
        CustomMusicPlayer.loadAndPlayMusicInChunks(mainMenuMusicPath);  // Play the correct music
        //TODO figure out why if i quit to main menu while combat music is playing, the main menu music aint playin
        isCombatMusicPlaying = false;
        CustomMusicPlayer.adjustVolume();
        BiomeMusic.LOGGER.info("Tried to load: {}", mainMenuMusicPath);
        // Update the state to indicate music is playing and store the current track
        currentMusic = mainMenuMusicPath;
    }

    @SideOnly(Side.CLIENT)
    public static void stopVanillaMusicMainMenu() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            MusicTicker musicTicker = mc.getMusicTicker();
            Field currentMusicField = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147678_c");
            currentMusicField.setAccessible(true);
            ISound currentMusic = (ISound) currentMusicField.get(musicTicker);

            if (currentMusic != null) {
                new Thread(() -> {
                    mc.getSoundHandler().stopSound(currentMusic);
                }).start();
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
