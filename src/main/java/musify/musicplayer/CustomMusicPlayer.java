package musify.musicplayer;

import musify.Musify;
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

import static musify.handlers.BiomeMusicConfig.*;
import static musify.handlers.BiomeMusicEventHandler.*;


@SideOnly(Side.CLIENT)
public class CustomMusicPlayer {

    public static Clip musicClip;
    public static Clip combatMusicClip;
    private static boolean isMusicPlaying = false;
    private static boolean isPaused = false;
    private static long pausePosition = 0;
    private static long combatPausePosition = 0;
    private static final int FADE_IN_DURATION_MS = fadeOptions.customMusicFadeInTime;
    private static final int FADE_OUT_DURATION_MS = fadeOptions.customMusicFadeOutTime;
    public static final int COMBAT_FADE_IN_DURATION = fadeOptions.combatMusicFadeInTime;
    private static final long CHUNK_DURATION_MS = 10_000;
    public static boolean isFading = false;
    private static AudioInputStream pcmStream;
    private static AudioInputStream combatpcmStream;
    public static String currentFile = "";
    public static boolean isBackgroundCombatMusicPlaying = false;
    public static String hasEndFile = "";
    public static boolean isSilent = false;

    @SideOnly(Side.CLIENT)
    public static void playCustomMusic(String musicFile) {

        new Thread(() -> {
            try {
                if (musicClip != null && isMusicPlaying) {
                    if (!currentFile.equals(musicFile)) {
                        fadeOutMusicAndPlayNew(musicFile);
                    }
                    return;
                }
                    loadAndPlay(musicFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @SideOnly(Side.CLIENT)
    public static void loadAndPlay(String music) throws Exception {
        loadAndPlayMusicInChunks(music);
        if (combatOptions.enableCombatMusic) {
            loadAndPlayCombatMusicInChunks(music, false);
        }
        fadeInMusic(FADE_IN_DURATION_MS);
    }

    @SideOnly(Side.CLIENT)
    public static void loadAndPlayMusicInChunks(String music) throws Exception {
        stopMusic(); // Ensure no music is playing before starting a new track

        String filePath = Musify.musicFolder.getPath() + "/" + music;

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

        FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
        float minVolume = volumeControl.getMinimum();
        volumeControl.setValue(minVolume);

        musicClip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP && !isPaused && !isFading) {
                if (musicClip.getMicrosecondPosition() + CHUNK_DURATION_MS * 1000 < musicClip.getMicrosecondLength()) {
                    loadNextChunk();
                } else {
                    if (musicClip != null) {
                        musicClip.stop();
                        musicClip.close();
                    }
                    isMusicPlaying = false;
                    isPaused = false;
                    pausePosition = 0;
                    hasEndFile = currentFile;
                    isFading = false;
                    stopMusic();
                }
            }
        });

        isMusicPlaying = true;
        playChunk(); // Start the first chunk
        currentFile = music;
        isLoading = false;
    }


    @SideOnly(Side.CLIENT)
    public static void loadAndPlayCombatMusicInChunks(String music, boolean sole) throws Exception {

        String linkedMusic = null;
        if (!sole) {
            linkedMusic = musicLink.get(music);
            if (Objects.equals(linkedMusic, "") || Objects.equals(linkedMusic, null)) {
                if (Objects.equals(combatOptions.combatMusicList, "default_music")) {
                    Musify.LOGGER.warn("No combat music specified. If you do not plan on using combat music, please disable it in the config.");
                    return;
                }
                linkedMusic = getRandomSongForCombat();
            }
        }
        if (sole) {
            linkedMusic = getRandomSongForCombat();
        }

        String filePath = Musify.musicFolder.getPath() + "/" + linkedMusic;

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

        DataLine.Info info = new DataLine.Info(Clip.class, decodedFormat);
        combatMusicClip = (Clip) AudioSystem.getLine(info);
        combatMusicClip.open(combatpcmStream);

        FloatControl volumeControl = (FloatControl) combatMusicClip.getControl(FloatControl.Type.MASTER_GAIN);
        float minVolume = volumeControl.getMinimum();
        volumeControl.setValue(minVolume);

        combatMusicClip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP && !isPaused && !isFading) {
                if (combatMusicClip.getMicrosecondPosition() + CHUNK_DURATION_MS * 1000 < combatMusicClip.getMicrosecondLength()) {
                    loadNextChunkCombat();
                } else {
                    combatMusicClip.setMicrosecondPosition(0);
                    combatMusicClip.start();
                }
            }
        });

        isBackgroundCombatMusicPlaying = true;

        playChunkCombat();
    }

    private static void loadNextChunk() {
        if (musicClip != null && pcmStream != null) {
            try {
                musicClip.stop();
                long nextPosition = musicClip.getMicrosecondPosition() + CHUNK_DURATION_MS * 1000;
                if (nextPosition < musicClip.getMicrosecondLength()) {
                    musicClip.setMicrosecondPosition(nextPosition);
                } else {
                    musicClip.setMicrosecondPosition(0);
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
                    combatMusicClip.setMicrosecondPosition(0);
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

        float volumeReductionFactor = (float) fadeOptions.musicVolumeMultiplier;
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

                float volumeReductionFactor = (float) fadeOptions.musicVolumeMultiplier;
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
            hasEndFile = "";
            isFading = false;
            isBackgroundCombatMusicPlaying = false;
            isCombatMusicPlaying = false;
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
    public static void stopCombatMusic() {
        isCombatMusicPlaying = false;
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
        if (musicClip != null && musicClip.isRunning() && !isSilent) {
            isPaused = true;
            pausePosition = musicClip.getMicrosecondPosition();
            musicClip.stop();
            pauseCombatMusic();
        }
    }

    @SideOnly(Side.CLIENT)
    public static void pauseCombatMusic() {
        if (combatMusicClip != null && combatMusicClip.isRunning()) {
            isPaused = true;
            combatPausePosition = combatMusicClip.getMicrosecondPosition();
            combatMusicClip.stop();
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
        } else if (combatMusicClip != null && isPaused) {
            combatMusicClip.setMicrosecondPosition(combatPausePosition);
            combatMusicClip.start();
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
                FloatControl combatVolumeControl = null;
                if (isBackgroundCombatMusicPlaying) {
                    combatVolumeControl = (FloatControl) combatMusicClip.getControl(FloatControl.Type.MASTER_GAIN);
                }
                float min = volumeControl.getMinimum();
                float max = volumeControl.getMaximum();

                float volumeReductionFactor = (float) fadeOptions.musicVolumeMultiplier;
                float adjustedMaxVolume = min + (volumeReductionFactor * (max - min));

                float newVolume = (float) (min + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (adjustedMaxVolume - min));
                if (!isCombatMusicPlaying) {
                    volumeControl.setValue(newVolume);
                }
                if (isBackgroundCombatMusicPlaying && !isMusicPlaying) {
                    combatVolumeControl.setValue(newVolume);
                }
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

            Minecraft mc = Minecraft.getMinecraft();
            float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);

            float minVolume = volumeControl.getMinimum();
            float maxVolume = volumeControl.getMaximum();

            float volumeReductionFactor = (float) fadeOptions.musicVolumeMultiplier;
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

        float minVolume = volumeControl.getMinimum();
        float maxVolume = volumeControl.getMaximum();

        float volumeReductionFactor = (float) fadeOptions.musicVolumeMultiplier;
        float adjustedMaxVolume = minVolume + (volumeReductionFactor * (maxVolume - minVolume));

        float currentVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (adjustedMaxVolume - minVolume));

        new Thread(() -> {
            try {
                int fadeSteps = 100;
                long fadeInterval = COMBAT_FADE_IN_DURATION / fadeSteps;
                float volumeStep = (currentVolume - minVolume) / fadeSteps;

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

        float minVolume = volumeControl.getMinimum();
        float maxVolume = volumeControl.getMaximum();

        float volumeReductionFactor = (float) fadeOptions.musicVolumeMultiplier;
        float adjustedMaxVolume = minVolume + (volumeReductionFactor * (maxVolume - minVolume));

        float currentVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (adjustedMaxVolume - minVolume));

        new Thread(() -> {
            try {
                int fadeSteps = 100;
                long fadeInterval = COMBAT_FADE_IN_DURATION / fadeSteps;
                float volumeStep = (currentVolume - minVolume) / fadeSteps;

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
        long fadeInterval = (long) ((COMBAT_FADE_IN_DURATION) / fadeSteps);
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

    @SideOnly(Side.CLIENT)
    public static void stopCombatMusicWithFadeOut() {
        if (combatMusicClip != null && combatMusicClip.isRunning()) {
            isFading = true;
            FloatControl volumeControl = (FloatControl) combatMusicClip.getControl(FloatControl.Type.MASTER_GAIN);

            Minecraft mc = Minecraft.getMinecraft();
            float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);

            float minVolume = volumeControl.getMinimum();
            float maxVolume = volumeControl.getMaximum();

            float volumeReductionFactor = (float) fadeOptions.musicVolumeMultiplier;
            float adjustedMaxVolume = minVolume + (volumeReductionFactor * (maxVolume - minVolume));

            float currentVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (adjustedMaxVolume - minVolume));

            new Thread(() -> {
                try {
                    int fadeSteps = 100;
                    long fadeInterval = COMBAT_FADE_IN_DURATION / fadeSteps;
                    float volumeStep = (currentVolume - minVolume) / fadeSteps;

                    for (int i = fadeSteps; i >= 0; i--) {
                        float newVolume = minVolume + (i * volumeStep);
                        volumeControl.setValue(newVolume);
                        Thread.sleep(fadeInterval);
                    }

                    stopMusic();
                    isFading = false;

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @SideOnly(Side.CLIENT)
    public static void fadeInCombatMusic() {
        if (combatMusicClip != null) {
            try {
                isFading = true;

                Minecraft mc = Minecraft.getMinecraft();
                FloatControl volumeControl = (FloatControl) combatMusicClip.getControl(FloatControl.Type.MASTER_GAIN);
                float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);
                float minVolume = volumeControl.getMinimum();
                float maxVolume = volumeControl.getMaximum();

                float volumeReductionFactor = (float) fadeOptions.musicVolumeMultiplier;
                float adjustedMaxVolume = minVolume + (volumeReductionFactor * (maxVolume - minVolume));

                float targetVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (adjustedMaxVolume - minVolume));

                volumeControl.setValue(minVolume);

                int fadeSteps = 100;
                long fadeInterval = COMBAT_FADE_IN_DURATION / fadeSteps;
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
    public static void silenceMusic() {
        if (musicClip != null && musicClip.isRunning() && !isCombatMusicPlaying && !isFading && !isSilent) {

            isFading = true;
            isSilent = true;
            FloatControl volumeControl = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);

            Minecraft mc = Minecraft.getMinecraft();
            float musicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);

            float minVolume = volumeControl.getMinimum();
            float maxVolume = volumeControl.getMaximum();

            float volumeReductionFactor = (float) fadeOptions.musicVolumeMultiplier;
            float adjustedMaxVolume = minVolume + (volumeReductionFactor * (maxVolume - minVolume));

            float currentVolume = (float) (minVolume + (Math.log10(musicVolume * 149 + 1) / Math.log10(150)) * (adjustedMaxVolume - minVolume));

            new Thread(() -> {
                try {
                    int fadeSteps = 100;
                    long fadeInterval = (FADE_OUT_DURATION_MS / 2) / fadeSteps;
                    float volumeStep = (currentVolume - minVolume) / fadeSteps;

                    for (int i = fadeSteps; i >= 0; i--) {
                        float newVolume = minVolume + (i * volumeStep);
                        volumeControl.setValue(newVolume);
                        Thread.sleep(fadeInterval);
                    }

                    isFading = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @SideOnly(Side.CLIENT)
    public static void resumeMusicWithFadeIn() {
        if (isSilent && musicClip != null && musicClip.isRunning() && !isFading) {
            fadeInMusic(FADE_IN_DURATION_MS/2);
            isSilent = false;
        }
    }

}