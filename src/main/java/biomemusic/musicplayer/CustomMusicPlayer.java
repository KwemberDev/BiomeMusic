package biomemusic.musicplayer;

import biomemusic.BiomeMusic;
import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

import static biomemusic.handlers.BiomeMusicConfig.fadeOptions;


@SideOnly(Side.CLIENT)
public class CustomMusicPlayer {

    private static Clip musicClip;
    private static boolean isMusicPlaying = false;
    private static boolean isPaused = false;
    private static long pausePosition = 0;
    private static final int FADE_IN_DURATION_MS = fadeOptions.customMusicFadeInTime;
    private static final int FADE_OUT_DURATION_MS = fadeOptions.customMusicFadeOutTime;
    private static final long CHUNK_DURATION_MS = 10_000; // 10 seconds in milliseconds
    public static boolean isFading = false;
    private static String currentMusicFilePath = "";
    private static AudioInputStream pcmStream;

    @SideOnly(Side.CLIENT)
    public static void playCustomMusic(String filePath) {
        try {
            if (musicClip != null && isMusicPlaying) {
                if (currentMusicFilePath.equals(filePath)) {
                    return;
                } else {
                    fadeOutMusicAndPlayNew(filePath);
                    return;
                }
            }
            loadAndPlayMusicInChunks(filePath);
            fadeInMusic(FADE_IN_DURATION_MS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load and play the music file in chunks
    @SideOnly(Side.CLIENT)
    public static void loadAndPlayMusicInChunks(String filePath) throws Exception {
        stopMusic();

        File musicFile = new File(filePath);
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
        pcmStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream);

        DataLine.Info info = new DataLine.Info(Clip.class, decodedFormat);
        musicClip = (Clip) AudioSystem.getLine(info);
        musicClip.open(pcmStream);

        musicClip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP && !isPaused && !isFading) {
                if (musicClip.getMicrosecondPosition() + CHUNK_DURATION_MS * 1000 < musicClip.getMicrosecondLength()) {
                    loadNextChunk();
                } else {
                    musicClip.setMicrosecondPosition(0); // Loop to start
                    musicClip.start();
                }
            }
        });

        playChunk();
        isMusicPlaying = true;
        isPaused = false;
        currentMusicFilePath = filePath;
    }

    // Load and play the next chunk
    private static void loadNextChunk() {
        if (musicClip != null && pcmStream != null) {
            try {
                musicClip.stop();
                long nextPosition = musicClip.getMicrosecondPosition() + CHUNK_DURATION_MS * 1000;
                if (nextPosition < musicClip.getMicrosecondLength()) {
                    musicClip.setMicrosecondPosition(nextPosition);
                } else {
                    musicClip.setMicrosecondPosition(0); // Start over at end of track
                }
                playChunk();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Play the current chunk in the clip
    private static void playChunk() {
        if (musicClip != null) {
            musicClip.start();
        }
    }

    @SideOnly(Side.CLIENT)
    private static void fadeOutMusicAndPlayNew(String newFilePath) {
        if (musicClip == null) {
            try {
                loadAndPlayMusicInChunks(newFilePath);
                fadeInMusic(FADE_IN_DURATION_MS);
                currentMusicFilePath = newFilePath;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        isFading = true;
        FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);

        Minecraft mc = Minecraft.getMinecraft();
        float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);
        float minVolume = volumeControl.getMinimum();
        float maxVolume = volumeControl.getMaximum();
        float currentVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (maxVolume - minVolume));

        new Thread(() -> {
            try {
                int fadeSteps = 100;
                long fadeInterval = FADE_OUT_DURATION_MS / fadeSteps;
                float volumeStep = (currentVolume - minVolume) / fadeSteps;

                for (int i = fadeSteps; i >= 0; i--) {
                    float newVolume = minVolume + (i * volumeStep);
                    volumeControl.setValue(newVolume);
                    Thread.sleep(fadeInterval);
                }

                stopMusic();
                loadAndPlayMusicInChunks(newFilePath);
                fadeInMusic(FADE_IN_DURATION_MS);
                currentMusicFilePath = newFilePath;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @SideOnly(Side.CLIENT)
    private static void fadeInMusic(int durationMs) {
        if (musicClip != null) {
            try {
                isFading = true;
                FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);

                Minecraft mc = Minecraft.getMinecraft();
                float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);
                float minVolume = volumeControl.getMinimum();
                float maxVolume = volumeControl.getMaximum();
                float targetVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (maxVolume - minVolume));

                volumeControl.setValue(minVolume);

                int fadeSteps = 100;
                long fadeInterval = durationMs / fadeSteps;
                float volumeStep = (targetVolume - minVolume) / fadeSteps;

                new Thread(() -> {
                    try {
                        for (int i = 0; i <= fadeSteps; i++) {
                            float currentVolume = minVolume + (i * volumeStep);
                            volumeControl.setValue(currentVolume);
                            Thread.sleep(fadeInterval);
                        }
                        volumeControl.setValue(targetVolume);
                        isFading = false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
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
            currentMusicFilePath = "";
            isFading = false;
        }
        if (pcmStream != null) {
            try {
                pcmStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static void pauseMusic() {
        if (musicClip != null && musicClip.isRunning()) {
            pausePosition = musicClip.getMicrosecondPosition();
            musicClip.stop();
            isPaused = true;
        }
    }

    @SideOnly(Side.CLIENT)
    public static void resumeMusic() {
        if (musicClip != null && isPaused) {
            musicClip.setMicrosecondPosition(pausePosition);
            musicClip.start();
            isPaused = false;
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
                Minecraft mc = Minecraft.getMinecraft();
                float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);
                FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
                float min = volumeControl.getMinimum();
                float max = volumeControl.getMaximum();
                float newVolume = (float) (min + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (max - min));
                volumeControl.setValue(newVolume);
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            e.printStackTrace();
        }
    }

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
}