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
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static biomemusic.handlers.BiomeMusicEventHandler.stopVanillaMusic;

@Mod.EventBusSubscriber
public class MainMenuMusicHandler {

    public static boolean isMainMenuMusicPlaying = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();

        // Check if the current screen is one of the main menus
        if (isMainMenuScreen(mc)) {
            // Stop any vanilla music and play custom music
            stopVanillaMusicMainMenu();
            playMainMenuMusic();
        } else {
            // If no longer in the main menu, stop the custom music
            if (isMainMenuMusicPlaying) {
                CustomMusicPlayer.stopMusic();
                isMainMenuMusicPlaying = false;
            }
        }
    }

    // Check if the current screen is one where you want to stop the vanilla music and play custom music
    private static boolean isMainMenuScreen(Minecraft mc) {
        return mc.currentScreen instanceof GuiMainMenu
                || mc.currentScreen instanceof GuiWorldSelection
                || mc.currentScreen instanceof GuiCreateWorld
                || mc.currentScreen instanceof GuiMultiplayer;
    }

    // Play custom music defined in the config
    private static void playMainMenuMusic() throws Exception {
        if (!isMainMenuMusicPlaying) {
            String mainMenuMusicPath = BiomeMusicConfig.mainMenuMusic; // Get the music from the config
            String filePath = BiomeMusic.musicFolder.getPath() + "/" + mainMenuMusicPath;
            CustomMusicPlayer.loadAndPlayMusic(filePath); // Play the custom music
            isMainMenuMusicPlaying = true;
        }
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
