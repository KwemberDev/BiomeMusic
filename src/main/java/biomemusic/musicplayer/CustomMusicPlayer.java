package biomemusic.musicplayer;

import biomemusic.BiomeMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.sound.sampled.*;
import java.io.File;

import static biomemusic.handlers.BiomeMusicConfig.fadeOptions;


@SideOnly(Side.CLIENT)
public class CustomMusicPlayer {

    private static Clip musicClip;
    private static boolean isMusicPlaying = false;
    private static boolean isPaused = false; // To track pause state
    private static long pausePosition = 0;   // To store clip's position when paused
    private static final int FADE_IN_DURATION_MS = fadeOptions.customMusicFadeInTime; // 20 seconds fade-in
    private static final int FADE_OUT_DURATION_MS = fadeOptions.customMusicFadeOutTime; // 20 seconds fade-out
    public static boolean isFading = false;

    private static String currentMusicFilePath = ""; // Store the currently playing music file path

    // Play custom music with fade-in
    @SideOnly(Side.CLIENT)
    public static void playCustomMusic(String filePath) {
        try {
            // If music is already playing, fade it out first
            if (musicClip != null && isMusicPlaying) {
                if (currentMusicFilePath.equals(filePath)) {
                    // If the current music is already playing, don't restart it
                    return;
                } else {
                    fadeOutMusicAndPlayNew(filePath);
                    return; // Exit as the fade-out will handle stopping the current music
                }
            }

            // Load and start the new music
            loadAndPlayMusic(filePath);
            fadeInMusic(FADE_IN_DURATION_MS); // Apply fade-in to the new track
//            currentMusicFilePath = filePath;  // Update the currently playing file path
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load and play a new music file
    @SideOnly(Side.CLIENT)
    public static void loadAndPlayMusic(String filePath) throws Exception {
        stopMusic(); // Stop any currently playing music

        // Load the new music file
        File musicFile = new File(filePath);
        BiomeMusic.LOGGER.info("Loaded file on path: {}", musicFile.getAbsolutePath());

        // Open and decode the AudioInputStream
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicFile);
        AudioFormat baseFormat = audioStream.getFormat();
        AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(),
                16,
                baseFormat.getChannels(),
                baseFormat.getChannels() * 2,
                baseFormat.getSampleRate(),
                false
        );
        AudioInputStream pcmStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream);

        // Get a data line (Clip) that matches the decoded format
        DataLine.Info info = new DataLine.Info(Clip.class, decodedFormat);
        musicClip = (Clip) AudioSystem.getLine(info);
        musicClip.open(pcmStream);

        // Add a listener to repeat the track when it ends
        musicClip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP && !isPaused && !isFading) {
                if (musicClip.getMicrosecondPosition() >= musicClip.getMicrosecondLength()) {
                    // Music has ended, restart from the beginning
                    musicClip.setMicrosecondPosition(0); // Rewind to the start
                    musicClip.start(); // Play again
                }
            }
        });

        // Start playing the music
        musicClip.start();
        isMusicPlaying = true;
        isPaused = false;
        currentMusicFilePath = filePath;
        BiomeMusic.LOGGER.info("Started music: {}", filePath);
    }
    @SideOnly(Side.CLIENT)
    private static void fadeOutMusicAndPlayNew(String newFilePath) {
        if (musicClip == null) {
            try {
                loadAndPlayMusic(newFilePath);
                fadeInMusic(FADE_IN_DURATION_MS);
                currentMusicFilePath = newFilePath;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        isFading = true;
        FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);

        // Get the current volume based on Minecraft's sound settings
        Minecraft mc = Minecraft.getMinecraft();
        float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);

        // Get the minimum and maximum possible volume
        float minVolume = volumeControl.getMinimum(); // Usually -80 dB
        float maxVolume = volumeControl.getMaximum(); // Usually 6 dB

        // Calculate the current volume in decibels based on Minecraft's music volume setting
        float currentVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (maxVolume - minVolume));

        new Thread(() -> {
            try {
                int fadeSteps = 100;
                long fadeInterval = FADE_OUT_DURATION_MS / fadeSteps;
                float volumeStep = (currentVolume - minVolume) / fadeSteps;

                // Gradually decrease the volume from the current volume to the minimum volume
                for (int i = fadeSteps; i >= 0; i--) {
                    float newVolume = minVolume + (i * volumeStep);
                    volumeControl.setValue(newVolume);
                    Thread.sleep(fadeInterval);
                }

                // After fade-out, stop the current music and load/play the new music
                stopMusic();
                loadAndPlayMusic(newFilePath);
                fadeInMusic(FADE_IN_DURATION_MS);
                currentMusicFilePath = newFilePath;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Fade in the music over a given duration (in milliseconds)
    @SideOnly(Side.CLIENT)
    private static void fadeInMusic(int durationMs) {
        if (musicClip != null) {
            try {
                isFading = true;
                FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);

                // Get the current volume based on Minecraft's sound settings
                Minecraft mc = Minecraft.getMinecraft();
                float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);

                // Get the minimum and maximum possible volume
                float minVolume = volumeControl.getMinimum(); // Usually -80 dB
                float maxVolume = volumeControl.getMaximum(); // Usually 6 dB

                // Calculate the target volume (maximum for fade-in) based on Minecraft's music volume setting
                float targetVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (maxVolume - minVolume));

                // Set the volume to minimum initially
                volumeControl.setValue(minVolume);

                int fadeSteps = 100;
                long fadeInterval = durationMs / fadeSteps;
                float volumeStep = (targetVolume - minVolume) / fadeSteps;

                new Thread(() -> {
                    try {
                        // Gradually increase the volume from the minimum to the target volume
                        for (int i = 0; i <= fadeSteps; i++) {
                            float currentVolume = minVolume + (i * volumeStep);
                            volumeControl.setValue(currentVolume);
                            Thread.sleep(fadeInterval);
                        }
                        volumeControl.setValue(targetVolume);  // Ensure it's at target volume at the end
                        isFading = false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (IllegalArgumentException e) {
                BiomeMusic.LOGGER.warn("Volume control not supported: {}", e.getMessage());
            }
        }
    }

    // Fade out the music over a given duration (in milliseconds) and stop it
    @SideOnly(Side.CLIENT)
    public static void stopMusicWithFadeOut() {
        if (musicClip != null && isMusicPlaying) {
            isFading = true;
            FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);

            // Get the current volume based on Minecraft's sound settings
            Minecraft mc = Minecraft.getMinecraft();
            float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);

            // Get the minimum and maximum possible volume
            float minVolume = volumeControl.getMinimum(); // Usually -80 dB
            float maxVolume = volumeControl.getMaximum(); // Usually 6 dB

            // Calculate the current volume in decibels based on Minecraft's music volume setting
            float currentVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (maxVolume - minVolume));

            new Thread(() -> {
                try {
                    int fadeSteps = 100;
                    long fadeInterval = FADE_OUT_DURATION_MS / fadeSteps;
                    float volumeStep = (currentVolume - minVolume) / fadeSteps;

                    // Gradually decrease the volume from the current volume to the minimum volume
                    for (int i = fadeSteps; i >= 0; i--) {
                        float newVolume = minVolume + (i * volumeStep);
                        volumeControl.setValue(newVolume);
                        Thread.sleep(fadeInterval);
                    }

                    stopMusic(); // Stop the music after fading out

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @SideOnly(Side.CLIENT)
    public static void stopMusic() {
        if (musicClip != null && musicClip.isRunning()) {
            musicClip.stop();
            musicClip.close();
            isMusicPlaying = false;
            isPaused = false;
            pausePosition = 0;
            currentMusicFilePath = ""; // Reset current music file
            isFading = false;
        }
    }

    // Pause the music
    @SideOnly(Side.CLIENT)
    public static void pauseMusic() {
        if (musicClip != null && musicClip.isRunning()) {
            pausePosition = musicClip.getMicrosecondPosition(); // Save the position
            musicClip.stop();
            isPaused = true;
            BiomeMusic.LOGGER.info("Music paused at position: {}", pausePosition);
        }
    }

    // Resume the music from the last paused position
    @SideOnly(Side.CLIENT)
    public static void resumeMusic() {
        if (musicClip != null && isPaused) {
            musicClip.setMicrosecondPosition(pausePosition); // Resume from where paused
            musicClip.start();
            isPaused = false;
            BiomeMusic.LOGGER.info("Music resumed from position: {}", pausePosition);
        }
    }

    public static boolean isMusicPlaying() {
        return isMusicPlaying;
    }

    public static boolean isPaused() {
        return isPaused;
    }

    public static boolean isCurrentTrack(String filePath) {
        return currentMusicFilePath.equals(filePath);
    }

    public static void adjustVolume() {
        try {
            if (musicClip != null) {
                // Get the current music volume from Minecraft settings
                Minecraft mc = Minecraft.getMinecraft();
                float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC); // Value between 0.0 and 1.0

                // Adjust the volume using FloatControl (MASTER_GAIN)
                FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);

                // Convert the volume to decibels using logarithmic scaling for perceptual linearity
                float min = volumeControl.getMinimum(); // Usually -80.0 dB or so
                float max = volumeControl.getMaximum(); // Usually 6.0 dB or so

                // Apply logarithmic scaling to make volume adjustments more linear to perception
                float newVolume = (float) (min + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (max - min));

                // Set the new volume
                volumeControl.setValue(newVolume);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
        }
    }
}
