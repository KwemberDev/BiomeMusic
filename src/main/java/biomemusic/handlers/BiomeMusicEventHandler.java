package biomemusic.handlers;

import biomemusic.BiomeMusic;
import biomemusic.combatutils.BossTargetUtils;
import biomemusic.combatutils.TargetingUtils;
import biomemusic.musicplayer.CustomMusicPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;
import java.util.*;

import static biomemusic.handlers.BiomeMusicConfig.*;
import static biomemusic.musicplayer.CustomMusicPlayer.*;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class BiomeMusicEventHandler {

    private static EntityPlayer player;

    private static float originalMusicVolume;
    private static boolean isVanillaMusicFading = false;
    private static int tickCounter = 0;
    private static final Random random = new Random();
    public static boolean isCombatMusicPlaying = false;
    public static boolean isLoading = false;
    public static boolean isCavernMusicPlaying = false;
    public static boolean isBossMusicPlaying = false;
    private static String currentBossMusic = "";

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) throws Exception {
        if (MainMenuMusicHandler.isMainMenuMusicPlaying && event.player.world != null) {
            MainMenuMusicHandler.isMainMenuMusicPlaying = false;
            stopMusic();
        }

        if (player == null || player != event.player) {
            player = event.player;
        }

        tickCounter++;

        if (fadeOptions.pollingRate == 0) {
            if (tickCounter % 800 == 0) {
                BiomeMusic.LOGGER.error("POLLING RATE IS SET TO 0 IN BIOMEMUSIC CONFIG. THIS WILL BREAK THE MOD. (forcibly stopped the mod to prevent crash.)");
            }
            return;
        }

        if (tickCounter % fadeOptions.pollingRate == 0 && event.player != null && event.player.world != null) {

            if (bossMusicOptions.enableBossMusic) {
                String bossMusic = BossTargetUtils.bossMusicFile(event.player);
                if (bossMusic != null) {
                    handleBossMusic(bossMusic);
                    return;
                }
            }

            if (isBossMusicPlaying) {
                isBossMusicPlaying = false;
            }

            if (combatOptions.enableCombatMusic) {
                int aggrocount = TargetingUtils.countMobsTargetingPlayer(player, combatOptions.combatRadius);
                if (aggrocount >= combatOptions.combatStartNumber) {
                    handleCombatMusic();
                    return;
                } else if (isCombatMusicPlaying && aggrocount > combatOptions.combatStopNumber) {
                    if (!isVanillaMusicFading) {
                        stopVanillaMusic();
                    }
                    return;
                }
            }

            if (isCombatMusicPlaying) {
                if (musicClip.isRunning()) {
                    switchToBiomeMusic();
                    isCombatMusicPlaying = false;
                } else if (!musicClip.isRunning() && combatMusicClip.isRunning() && !isFading) {
                    stopCombatMusicWithFadeOut();
                    isCombatMusicPlaying = false;
                }
            }

            if (cpundergroundOptions.enableUndergroundMusic) {
                if (event.player.posY <= cpundergroundOptions.undergroundMusicYLevelStart && event.player.world.provider.getDimension() == 0) {
                    handleCavernMusic();
                    return;
                } else if (event.player.posY <= cpundergroundOptions.undergroundMusicYLevelStop && isCavernMusicPlaying) {
                    if (!isVanillaMusicFading) {
                        stopVanillaMusic();
                    }
                    return;
                }
            }

            if (isCavernMusicPlaying) {
                isCavernMusicPlaying = false;
            }

            BlockPos pos = event.player.getPosition();
            Biome biome = event.player.world.getBiome(pos);
            String biomeName = biome.getBiomeName();
            handleBiomeMusic(biome);

            tickCounter = 0;
        }
    }

    @SideOnly(Side.CLIENT)
    private static void handleBossMusic(String musicFile) {

        if (!isBossMusicPlaying && !isFading) {
            isBossMusicPlaying = true;
            if (!isVanillaMusicFading && !adambientMode) {
                fadeOutVanillaMusic();
            }
            playCustomMusic(musicFile);
            currentBossMusic = musicFile;
        } else if (isBossMusicPlaying && !isFading && !Objects.equals(currentBossMusic, musicFile)) {
            if (!isVanillaMusicFading && !adambientMode) {
                fadeOutVanillaMusic();
            }
            playCustomMusic(musicFile);
            currentBossMusic = musicFile;
        } else if (isBossMusicPlaying && !isVanillaMusicFading) {
            stopMusic();
        }

    }

    @SideOnly(Side.CLIENT)
    private static void handleCavernMusic() {

        if (!isCavernMusicPlaying && !isFading) {
            String musicFile = getRandomSongForCavern();
            if (!musicFile.equals("default_music") && musicFile != null) {
                isCavernMusicPlaying = true;
                if (!isVanillaMusicFading) {
                    fadeOutVanillaMusic();
                }
                playCustomMusic(musicFile);
            }
        }
        if (isCavernMusicPlaying && !isVanillaMusicFading) {
            stopVanillaMusic();
        }
    }

    @SideOnly(Side.CLIENT)
    private static void handleCombatMusic() throws Exception {

        if (!isCombatMusicPlaying && isMusicPlaying() && isBackgroundCombatMusicPlaying && !isFading) {
            isCombatMusicPlaying = true;
            fadeOutVanillaMusic();
            switchToCombatMusic();
        }
        if (!isCombatMusicPlaying && !isBackgroundCombatMusicPlaying && !isFading) {
            isCombatMusicPlaying = true;
            fadeOutVanillaMusic();
            loadAndPlayCombatMusicInChunks("", true);
            fadeInCombatMusic();
        }
        if (isCombatMusicPlaying && !isBackgroundCombatMusicPlaying && !isVanillaMusicFading) {
            stopVanillaMusic();
        }

    }

    @SideOnly(Side.CLIENT)
    private static void handleBiomeMusic(Biome biome) {
        ResourceLocation biomeRegistryName = biome.getRegistryName();

        if (biomeRegistryName != null) {

            String configSet = BiomeMusicConfig.biomeMusicMap.get(biomeRegistryName.toString());
            String musicFile = null;

            if (configSet != null) {
                musicFile = getRandomSongForBiome(configSet);
            }

            if (musicFile != null && configSet != null && !configSet.equals("default_music") && biome != Biomes.RIVER) {
                if ((!CustomMusicPlayer.isMusicPlaying() || !CustomMusicPlayer.isCurrentTrackIncluded(configSet)) && !isFading && !isLoading) {
                    isLoading = true;

                    if (!isVanillaMusicFading && !adambientMode) {
                        fadeOutVanillaMusic();
                    }
                    CustomMusicPlayer.playCustomMusic(musicFile);
                }
                if (!isVanillaMusicFading && CustomMusicPlayer.isMusicPlaying() && !adambientMode) {
                    stopVanillaMusic();
                }
            } else {
                Set<BiomeDictionary.Type> biomeTags = BiomeDictionary.getTypes(biome);
                if (!biomeTags.isEmpty() && biome != Biomes.RIVER && !isLoading) {
                    String randomTagMusicFile = biomeTags.stream()
                            .map(type -> getRandomSongForBiomeTag(type.getName().toLowerCase()))
                            .filter(song -> !song.equals("default_music"))
                            .findFirst()
                            .orElse("default_music");

                    if (!randomTagMusicFile.equals("default_music")) {

                        Set<String> possibleSongs = new HashSet<>();

                        for (BiomeDictionary.Type type : biomeTags) {
                            String possibleSongList = BiomeMusicConfig.biomeTagMusicMap.getOrDefault(type.getName().toLowerCase(), "default_music");
                            String[] songList = possibleSongList.split(",");
                            possibleSongs.addAll(Arrays.asList(songList));
                        }
                        if ((!CustomMusicPlayer.isMusicPlaying() || !possibleSongs.contains(currentFile) && !isFading)) {
                            isLoading = true;
                            if (!isVanillaMusicFading && !adambientMode) {
                                fadeOutVanillaMusic();
                            }
                            playCustomMusic(randomTagMusicFile);
                        }
                        if (!isVanillaMusicFading && CustomMusicPlayer.isMusicPlaying() && !adambientMode) {
                            stopVanillaMusic();
                        }
                    } else {
                        if (CustomMusicPlayer.isMusicPlaying() && !isFading && !isVanillaMusicFading) {
                            CustomMusicPlayer.stopMusicWithFadeOut();
                        }
                    }
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
            originalMusicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);
            MusicTicker musicTicker = mc.getMusicTicker();

            Field currentMusicField = ObfuscationReflectionHelper.findField(MusicTicker.class, "field_147678_c");
            currentMusicField.setAccessible(true);

            ISound currentMusic = (ISound) currentMusicField.get(musicTicker);

            if (currentMusic != null && !isVanillaMusicFading) {
                isVanillaMusicFading = true;

                new Thread(() -> {
                    try {
                        float volume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);
                        float fadeDuration = (float) fadeOptions.vanillaMusicFadeOutTime;
                        float fadeSteps = 100;
                        float stepTime = fadeDuration / fadeSteps;
                        float volumeStep = volume / fadeSteps;

                        for (int i = 0; i < fadeSteps; i++) {
                            volume -= volumeStep;
                            setMusicVolume(SoundCategory.MUSIC, Math.max(0, volume));
                            Thread.sleep((long) stepTime);
                        }

                        mc.getSoundHandler().stopSound(currentMusic);
                        isVanillaMusicFading = false;

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

    @SideOnly(Side.CLIENT)
    private static void setMusicVolume(SoundCategory category, float volume) {
        Minecraft mc = Minecraft.getMinecraft();

        mc.gameSettings.setSoundLevel(category, volume);

        try {
            Field sndManagerField = ObfuscationReflectionHelper.findField(SoundHandler.class, "field_147694_f");
            sndManagerField.setAccessible(true);

            SoundManager soundManager = (SoundManager) sndManagerField.get(mc.getSoundHandler());

            soundManager.setVolume(category, volume);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @SideOnly(Side.CLIENT)
    private static void restoreMusicVolume() {
        Minecraft mc = Minecraft.getMinecraft();
        setMusicVolume(SoundCategory.MUSIC, originalMusicVolume);
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
                    isVanillaMusicFading = false;
                    restoreMusicVolume();
                }).start();
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetches a random song for a given biome tag.
     *
     * @param biomeTag The tag to look up in biomeTagMusicMap.
     * @return The file name of a randomly chosen song, or "default_music" if none is set.
     */
    public static String getRandomSongForBiomeTag(String biomeTag) {
        String songList = BiomeMusicConfig.biomeTagMusicMap.getOrDefault(biomeTag, "default_music");

        List<String> songs = Arrays.asList(songList.split(","));

        return songs.get(random.nextInt(songs.size())).trim();
    }

    public static String getRandomSongForCombat() {
        String songList = (combatOptions.combatMusicList);

        List<String> songs = Arrays.asList(songList.split(","));

        return songs.get(random.nextInt(songs.size())).trim();
    }

    public static String getRandomSongForBiome(String musiclist) {
        List<String> list = Arrays.asList(musiclist.split(","));
        return list.get(random.nextInt(list.size())).trim();
    }

    public static String getRandomSongForCavern() {
        String songList = (cpundergroundOptions.CavernMusic);

        List<String> songs = Arrays.asList(songList.split(","));

        return songs.get(random.nextInt(songs.size())).trim();
    }
}
