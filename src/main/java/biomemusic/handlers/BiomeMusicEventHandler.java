package biomemusic.handlers;

import biomemusic.BiomeMusic;
import biomemusic.musicplayer.CustomMusicPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeRiver;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

import static biomemusic.handlers.BiomeMusicConfig.ambientMode;
import static biomemusic.handlers.BiomeMusicConfig.fadeOptions;
import biomemusic.handlers.MainMenuMusicHandler.*;
import static biomemusic.musicplayer.CustomMusicPlayer.*;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class BiomeMusicEventHandler {

    private static float originalMusicVolume; // To store the original music volume
    private static boolean isVanillaMusicFading = false; // Flag to check if fading
    private static int tickCounter = 0; // Counter for ticks

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        tickCounter++;

        if (fadeOptions.pollingRate == 0) {
            if (tickCounter % 200 == 0) {
                BiomeMusic.LOGGER.warn("POLLING RATE IS SET TO 0 IN BIOMEMUSIC CONFIG. THIS WILL BREAK THE MOD. (forcibly stopped the mod to prevent crash.)");
            }
            return;
        }

        // every 7 seconds
        if (tickCounter % fadeOptions.pollingRate == 0) {

            if (event.player != null && event.player.world != null) {
                if (MainMenuMusicHandler.isMainMenuMusicPlaying) {
                    MainMenuMusicHandler.isMainMenuMusicPlaying = false;
                }
                BlockPos pos = event.player.getPosition();
                Biome biome = event.player.world.getBiome(pos);
                String biomeName = biome.getBiomeName();
                BiomeMusic.LOGGER.info("Current biome: {}", biomeName);

                // Call the method to handle biome-specific music
                handleBiomeMusic(event.player, biome);
            }
                tickCounter = 0;
        }
    }

    @SideOnly(Side.CLIENT)
    private static void handleBiomeMusic(EntityPlayer player, Biome biome) {
        // Get the biome's registry name instead of the human-readable name
        ResourceLocation biomeRegistryName = biome.getRegistryName();

        if (biomeRegistryName != null) {
            BiomeMusic.LOGGER.info("Got to the handleBiomeMusic method. Attempting to get biome config value.");

            // Use the registry name (e.g., "minecraft:extreme_hills") as the key
            String musicFile = BiomeMusicConfig.biomeMusicMap.get(biomeRegistryName.toString());

            if (musicFile != null && !musicFile.equals("default_music") && biome != Biomes.RIVER) {
                // Construct the path to the .ogg file in the biomemusic folder
                String filePath = BiomeMusic.musicFolder.getPath() + "/" + musicFile;
                // Check if the current music playing is the same as the one for this biome
                if ((!CustomMusicPlayer.isMusicPlaying() || !CustomMusicPlayer.isCurrentTrack(filePath)) && !isFading) {
                    // If not playing or the wrong track is playing, stop and start the new one

                    if (!isVanillaMusicFading && !ambientMode) {
                        fadeOutVanillaMusic();
                    }
                    BiomeMusic.LOGGER.error("PLAYED NEW CUSTOM MUSIC.");
                    CustomMusicPlayer.playCustomMusic(filePath);
                    BiomeMusic.LOGGER.info("Tried to set up custom music for {}", biomeRegistryName);
                }
                // if the vanilla music isnt fading, and if the correct custom music is playing. once every 10 seconds fade out vanilla music to prevent it coming back.
                if (!isVanillaMusicFading && CustomMusicPlayer.isMusicPlaying() && !ambientMode) {
                    stopVanillaMusic();
                    BiomeMusic.LOGGER.info("TRIED TO STOP VANILLA MUSIC WHILE CUSTOM IS PLAYING AGAIN");
                }
            } else {

                // Play vanilla music (or stop custom music if needed)
                if (CustomMusicPlayer.isMusicPlaying() && !isFading && !isVanillaMusicFading && biome != Biomes.RIVER) {
                    CustomMusicPlayer.stopMusicWithFadeOut();
                    BiomeMusic.LOGGER.info("No custom music found for biome: {}. Playing vanilla music.", biomeRegistryName);
                }
            }
        } else {
            BiomeMusic.LOGGER.error("Biome registry name was null for biome: {}", biome.getBiomeName());
        }
    }

    @SideOnly(Side.CLIENT)
    private static void fadeOutVanillaMusic() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            originalMusicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC); // Store the original volume
            MusicTicker musicTicker = mc.getMusicTicker();

            // Use reflection to access the currentMusic field in MusicTicker
            Field currentMusicField = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147678_c");
            currentMusicField.setAccessible(true);

            // Get the current ISound instance (vanilla music)
            ISound currentMusic = (ISound) currentMusicField.get(musicTicker);

            if (currentMusic != null && !isVanillaMusicFading) {
                isVanillaMusicFading = true; // Start fading

                // Fade out over 2 seconds (2000ms)
                new Thread(() -> {
                    try {
                        float volume = 1.0f; // Start at full volume
                        float fadeDuration = (float) fadeOptions.vanillaMusicFadeOutTime; // Fade over 10 seconds
                        float fadeSteps = 100; // How many steps to fade
                        float stepTime = fadeDuration / fadeSteps; // Time between steps
                        float volumeStep = volume / fadeSteps; // Volume decrease per step

                        // Gradually reduce the volume
                        for (int i = 0; i < fadeSteps; i++) {
                            volume -= volumeStep;
                            setMusicVolume(SoundCategory.MUSIC, Math.max(0, volume));
                            Thread.sleep((long) stepTime);
                        }

                        // Stop the music once faded out
                        mc.getSoundHandler().stopSound(currentMusic);
                        isVanillaMusicFading = false; // Reset fading flag
                        BiomeMusic.LOGGER.info("Vanilla music faded out and stopped.");

                        // Restore music volume after stopping
                        restoreMusicVolume();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    // Method to set the volume for the MUSIC category
    @SideOnly(Side.CLIENT)
    private static void setMusicVolume(SoundCategory category, float volume) {
        Minecraft mc = Minecraft.getMinecraft();

        // Set the volume in the game's settings (this ensures consistency with the UI)
        mc.gameSettings.setSoundLevel(category, volume);

        try {
            // Access the private sndManager field via reflection
            Field sndManagerField = ObfuscationReflectionHelper.findField(SoundHandler.class, "field_147694_f");
            sndManagerField.setAccessible(true); // Make the field accessible

            // Get the SoundManager instance from SoundHandler
            SoundManager soundManager = (SoundManager) sndManagerField.get(mc.getSoundHandler());

            // Use the setVolume method to adjust the volume for the specific category
            soundManager.setVolume(category, volume);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    // Method to restore the original volume after stopping the sound
    @SideOnly(Side.CLIENT)
    private static void restoreMusicVolume() {
        Minecraft mc = Minecraft.getMinecraft();
        setMusicVolume(SoundCategory.MUSIC, originalMusicVolume);
        BiomeMusic.LOGGER.info("Vanilla music volume restored to: {}", originalMusicVolume);
    }

    @SideOnly(Side.CLIENT)
    public static void stopVanillaMusic() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            originalMusicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);
            MusicTicker musicTicker = mc.getMusicTicker();
            Field currentMusicField = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147678_c");
            currentMusicField.setAccessible(true);
            ISound currentMusic = (ISound) currentMusicField.get(musicTicker);

            if (currentMusic != null && !isVanillaMusicFading) {
                isVanillaMusicFading = true;
                new Thread(() -> {
                    mc.getSoundHandler().stopSound(currentMusic);
                    isVanillaMusicFading = false; // Reset fading flag
                    restoreMusicVolume();
                }).start();
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
