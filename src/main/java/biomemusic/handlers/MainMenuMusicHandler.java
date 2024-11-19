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
@SideOnly(Side.CLIENT)
public class MainMenuMusicHandler {

    public static boolean isMainMenuMusicPlaying = false;
    public static String currentMusic = "";

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) throws Exception {

        String mainMenuMusicPath = BiomeMusicConfig.acmainMenuMusic;

        if (mainMenuMusicPath != null && !mainMenuMusicPath.equals("default_music")) {

            Minecraft mc = Minecraft.getMinecraft();

            if (Loader.isModLoaded("custommainmenu")) {
                if (mc.currentScreen != null && mc.currentScreen.getClass().getName().contains("lumien.custommainmenu")) {
                    if (combatMusicClip != null && combatMusicClip.isRunning()) {
                        stopCombatMusic();
                    }
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

            if (isMainMenuScreen(mc) && !Loader.isModLoaded("lumien.custommainmenu")) {
                if (combatMusicClip != null && combatMusicClip.isRunning()) {
                    stopCombatMusic();
                }
                stopVanillaMusicMainMenu();

                try {
                    playMainMenuMusic();
                } catch (Exception e) {
                    BiomeMusic.LOGGER.error("Failed to play main menu music. File not found or invalid: {}", mainMenuMusicPath, e);
                }

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

    public static boolean isMainMenuScreen(Minecraft mc) {
        return mc.currentScreen instanceof GuiMainMenu
                || mc.currentScreen instanceof GuiWorldSelection
                || mc.currentScreen instanceof GuiCreateWorld
                || mc.currentScreen instanceof GuiMultiplayer;
    }

    private static void playMainMenuMusic() throws Exception {
        String mainMenuMusicPath = BiomeMusicConfig.acmainMenuMusic;

        if (isMainMenuMusicPlaying && currentMusic.equals(mainMenuMusicPath)) {
            return;
        }

        CustomMusicPlayer.stopMusic();
        isMainMenuMusicPlaying = true;
        CustomMusicPlayer.loadAndPlayMusicInChunks(mainMenuMusicPath);
        isCombatMusicPlaying = false;
        CustomMusicPlayer.adjustVolume();
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
