package biomemusic.musicplayer;

import biomemusic.BiomeMusic;
import biomemusic.handlers.BiomeMusicConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static biomemusic.handlers.BiomeMusicConfig.*;
import static biomemusic.handlers.BiomeMusicEventHandler.*;


@SideOnly(Side.CLIENT)
public class CustomMusicPlayer {

    private static Clip musicClip;
    private static Clip combatMusicClip;
    private static boolean isMusicPlaying = false;
    private static boolean isPaused = false;
    private static long pausePosition = 0;
    private static long combatPausePosition = 0;
    private static final int FADE_IN_DURATION_MS = fadeOptions.customMusicFadeInTime;
    private static final int FADE_OUT_DURATION_MS = fadeOptions.customMusicFadeOutTime;
    private static final int COMBAT_FADE_IN_DURATION = fadeOptions.combatMusicFadeInTime;
    private static final long CHUNK_DURATION_MS = 10_000; // 10 seconds in milliseconds
    public static boolean isFading = false;
    private static AudioInputStream pcmStream;
    private static AudioInputStream combatpcmStream;
    public static String currentFile = "";
    public static boolean isBackgroundCombatMusicPlaying = false;

    @SideOnly(Side.CLIENT)
    public static void playCustomMusic(String musicFile) {

        // Start loading the music in a background thread to prevent game lag
        new Thread(() -> {
            try {
                if (musicClip != null && isMusicPlaying) {
                    if (!currentFile.equals(musicFile)) {
                        fadeOutMusicAndPlayNew(musicFile);
                    }
                    return;  // Already playing this track, no need to reload
                }
                    loadAndPlay(musicFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @SideOnly(Side.CLIENT)
    public static void loadAndPlay(String music) throws Exception {
        loadAndPlayMusicInChunks(music);  // Run this in a background thread
        loadAndPlayCombatMusicInChunks(music);
        fadeInMusic(FADE_IN_DURATION_MS);    // Start fade-in once loaded

    }

    @SideOnly(Side.CLIENT)
    public static void loadAndPlayMusicInChunks(String music) throws Exception {
        stopMusic();  // Stop any currently playing music

        String filePath = BiomeMusic.musicFolder.getPath() + "/" + music;

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

        // Use DataLine for audio information and create the Clip instance for playback
        DataLine.Info info = new DataLine.Info(Clip.class, decodedFormat);
        musicClip = (Clip) AudioSystem.getLine(info);
        musicClip.open(pcmStream);  // Open in this background thread

        FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
        float minVolume = volumeControl.getMinimum();
        volumeControl.setValue(minVolume);

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


        isMusicPlaying = true;
        playChunk();
        currentFile = music;
        isLoading = false;
    }

    @SideOnly(Side.CLIENT)
    public static void loadAndPlayCombatMusicInChunks(String music) throws Exception {

        if (Objects.equals(combatOptions.combatMusicList, "default_music")) {
            BiomeMusic.LOGGER.warn("No combat music specified. If you do not plan on using combat music, please disable it in the config.");
            return;
        }

        String linkedMusic = musicLink.get(music);
        if (Objects.equals(linkedMusic, "")) {
            linkedMusic = getRandomSongForCombat();
        }

        String filePath = BiomeMusic.musicFolder.getPath() + "/" + linkedMusic;

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

        combatpcmStream = AudioSystem.getAudioInputStream(decodedFormat, audioStream);

        // Use DataLine for audio information and create the Clip instance for playback
        DataLine.Info info = new DataLine.Info(Clip.class, decodedFormat);
        combatMusicClip = (Clip) AudioSystem.getLine(info);
        combatMusicClip.open(combatpcmStream);  // Open in this background thread

        FloatControl volumeControl = (FloatControl) combatMusicClip.getControl(FloatControl.Type.MASTER_GAIN);
        float minVolume = volumeControl.getMinimum();
        volumeControl.setValue(minVolume);

        combatMusicClip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP && !isPaused && !isFading) {
                if (combatMusicClip.getMicrosecondPosition() + CHUNK_DURATION_MS * 1000 < combatMusicClip.getMicrosecondLength()) {
                    loadNextChunkCombat();
                } else {
                    combatMusicClip.setMicrosecondPosition(0); // Loop to start
                    combatMusicClip.start();
                }
            }
        });


        isBackgroundCombatMusicPlaying = true;
        BiomeMusic.LOGGER.info("COMBAT MUSIC PLAYING IN BACKGROUND!");

        playChunkCombat();
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

    private static void loadNextChunkCombat() {
        if (combatMusicClip != null && combatpcmStream != null) {
            try {
                combatMusicClip.stop();
                long nextPosition = combatMusicClip.getMicrosecondPosition() + CHUNK_DURATION_MS * 1000;
                if (nextPosition < combatMusicClip.getMicrosecondLength()) {
                    combatMusicClip.setMicrosecondPosition(nextPosition);
                } else {
                    combatMusicClip.setMicrosecondPosition(0); // Start over at end of track
                }
                playChunkCombat();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void playChunk() {
        if (musicClip != null) {
            musicClip.start();
        }
    }

    private static void playChunkCombat() {
        if (combatMusicClip != null) {
            combatMusicClip.start();
        }
    }


    @SideOnly(Side.CLIENT)
    private static void fadeOutMusicAndPlayNew(String music) {
        if (musicClip == null) {
            try {
                loadAndPlay(music);
                currentFile = music;
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

        float volumeReductionFactor = 0.8f;
        float adjustedMaxVolume = minVolume + (volumeReductionFactor * (maxVolume - minVolume));

        float currentVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (adjustedMaxVolume - minVolume));

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
                loadAndPlay(music);
                currentFile = music;

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

                Minecraft mc = Minecraft.getMinecraft();
                FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
                float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);
                float minVolume = volumeControl.getMinimum();
                float maxVolume = volumeControl.getMaximum();

                float volumeReductionFactor = 0.8f; // Adjust this as needed (0.8 = 80% of original volume)
                float adjustedMaxVolume = minVolume + (volumeReductionFactor * (maxVolume - minVolume));

                float targetVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (adjustedMaxVolume - minVolume));

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
            currentFile = "";
            isFading = false;
            isBackgroundCombatMusicPlaying = false;
        }
        if (pcmStream != null) {
            try {
                pcmStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (combatMusicClip != null && combatMusicClip.isRunning()) {
            combatMusicClip.stop();
            combatMusicClip.close();
        }
        if (combatpcmStream != null) {
            try {
                combatpcmStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static void pauseMusic() {
        if (musicClip != null && musicClip.isRunning()) {
            isPaused = true;
            pausePosition = musicClip.getMicrosecondPosition();
            musicClip.stop();
            if (combatMusicClip != null && combatMusicClip.isRunning()) {
                combatPausePosition = combatMusicClip.getMicrosecondPosition();
                combatMusicClip.stop();
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public static void resumeMusic() {
        if (musicClip != null && isPaused) {
            musicClip.setMicrosecondPosition(pausePosition);
            musicClip.start();
            if (combatMusicClip != null) {
                combatMusicClip.setMicrosecondPosition(combatPausePosition);
                combatMusicClip.start();
            }
            isPaused = false;
        }
    }

    public static boolean isMusicPlaying() {
        return isMusicPlaying;
    }

    public static boolean isPaused() {
        return isPaused;
    }

    public static boolean isCurrentTrackIncluded(String configSet) {
        List<String> list = Arrays.asList(configSet.split(","));

        for (String config : list) {
            if (config.equals(currentFile)) {
                return true;
            }
        }
        return false;
    }

    public static void adjustVolume() {
        try {
            if (musicClip != null) {
                Minecraft mc = Minecraft.getMinecraft();
                float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);
                FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
                FloatControl combatVolumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
                float min = volumeControl.getMinimum();
                float max = volumeControl.getMaximum();

                float volumeReductionFactor = 0.8f; // Adjust this as needed (0.8 = 80% of original volume)
                float adjustedMaxVolume = min + (volumeReductionFactor * (max - min));

                float newVolume = (float) (min + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (adjustedMaxVolume - min));
                volumeControl.setValue(newVolume);
                combatVolumeControl.setValue(newVolume);
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

            float volumeReductionFactor = 0.8f; // Adjust this as needed (0.8 = 80% of original volume)
            float adjustedMaxVolume = minVolume + (volumeReductionFactor * (maxVolume - minVolume));

            // Calculate the current volume in decibels based on Minecraft's music volume setting
            float currentVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (adjustedMaxVolume - minVolume));

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
    public static void switchToCombatMusic() {
        isFading = true;
        FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
        FloatControl combatVolumeControl = (FloatControl) combatMusicClip.getControl(FloatControl.Type.MASTER_GAIN);

        Minecraft mc = Minecraft.getMinecraft();
        float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);

        float minVolume = volumeControl.getMinimum(); // Usually -80 dB
        float maxVolume = volumeControl.getMaximum(); // Usually 6 dB

        float volumeReductionFactor = 0.8f; // Adjust this as needed (0.8 = 80% of original volume)
        float adjustedMaxVolume = minVolume + (volumeReductionFactor * (maxVolume - minVolume));
        float combatAdjustedMaxVolume =  minVolume + (volumeReductionFactor * (maxVolume - minVolume));

        // Calculate the current volume in decibels based on Minecraft's music volume setting
        float currentVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (adjustedMaxVolume - minVolume));

        new Thread(() -> {
            try {
                int fadeSteps = 100;
                long fadeInterval = COMBAT_FADE_IN_DURATION / fadeSteps;
                float volumeStep = (currentVolume - minVolume) / fadeSteps;

                // Gradually decrease the volume from the current volume to the minimum volume
                for (int i = fadeSteps; i >= 0; i--) {
                    float newVolume = minVolume + (i * volumeStep);
                    volumeControl.setValue(newVolume);
                    Thread.sleep(fadeInterval);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        int fadeSteps = 100;
        long fadeInterval = COMBAT_FADE_IN_DURATION / fadeSteps;
        float volumeStep = (currentVolume - minVolume) / fadeSteps;

        new Thread(() -> {
            try {
                for (int i = 0; i <= fadeSteps; i++) {
                    float Volume = minVolume + (i * volumeStep);
                    combatVolumeControl.setValue(Volume);
                    Thread.sleep(fadeInterval);
                }
                combatVolumeControl.setValue(currentVolume);
                isFading = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @SideOnly(Side.CLIENT)
    public static void switchToBiomeMusic() {
        isFading = true;
        FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
        FloatControl combatVolumeControl = (FloatControl) combatMusicClip.getControl(FloatControl.Type.MASTER_GAIN);

        Minecraft mc = Minecraft.getMinecraft();
        float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);

        float minVolume = volumeControl.getMinimum(); // Usually -80 dB
        float maxVolume = volumeControl.getMaximum(); // Usually 6 dB

        float volumeReductionFactor = 0.8f; // Adjust this as needed (0.8 = 80% of original volume)
        float adjustedMaxVolume = minVolume + (volumeReductionFactor * (maxVolume - minVolume));

        // Calculate the current volume in decibels based on Minecraft's music volume setting
        float currentVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (adjustedMaxVolume - minVolume));

        new Thread(() -> {
            try {
                int fadeSteps = 100;
                long fadeInterval = COMBAT_FADE_IN_DURATION / fadeSteps;
                float volumeStep = (currentVolume - minVolume) / fadeSteps;

                // Gradually decrease the volume from the current volume to the minimum volume
                for (int i = fadeSteps; i >= 0; i--) {
                    float newVolume = minVolume + (i * volumeStep);
                    combatVolumeControl.setValue(newVolume);
                    Thread.sleep(fadeInterval);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        int fadeSteps = 100;
        long fadeInterval = (long) ((COMBAT_FADE_IN_DURATION * 1.2) / fadeSteps);
        float volumeStep = (currentVolume - minVolume) / fadeSteps;

        new Thread(() -> {
            try {
                for (int i = 0; i <= fadeSteps; i++) {
                    float Volume = minVolume + (i * volumeStep);
                    volumeControl.setValue(Volume);
                    Thread.sleep(fadeInterval);
                }
                volumeControl.setValue(currentVolume);
                isFading = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}