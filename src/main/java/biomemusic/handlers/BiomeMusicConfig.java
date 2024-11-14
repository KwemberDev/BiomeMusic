package biomemusic.handlers;

import biomemusic.BiomeMusic;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Config(modid = BiomeMusic.MODID)
public class BiomeMusicConfig {

	@Config.Comment("What music to play on the main menu. DEFAULT: [default_music]")
	@Config.Name("Main Menu Music")
	public static String acmainMenuMusic = "default_music";

	@Config.Name("Available Music Files")
	@Config.Comment("List of recognized .ogg music files. \nWhen setting up custom music, include .ogg extension and make sure to spell the name of the file correctly. \nThe mod will work even if the music you specify is not in this list if the music is in the correct folder, \nas this is just a second verification step to make sure the music file is correctly placed.")
	public static String[] abavailableMusicFiles = new String[0]; // Start with an empty array

	@Config.Name("Biome Music Map")
	@Config.Comment("Biome Music Mapping. List of all recognised biomes and a corresponding input field for the music. Music specified in here will overwrite the music from biome tags.")
	public static Map<String, String> biomeMusicMap = new HashMap<>();

	@Config.Name("Biome Tag Music Map")
	@Config.Comment("Biome Tag Music Mapping. List of all Biome Tags. \nExample: If you want a certain music to play in all biomes with snow, use the snowy tag.")
	public static Map<String, String> biomeTagMusicMap = new HashMap<>();

	@Config.Name("Fade Options")
	@Config.Comment("Fade Options. These control the music fading. \nDO NOT TOUCH UNLESS YOU KNOW EXACTLY WHAT YOU ARE DOING, THIS CAN AND WILL BREAK THE MOD IF GIVEN INCORRECT VALES. DEFAULTS: [20000,20000,140,10000]")
	public static final FadeOptions fadeOptions = new FadeOptions();

	@Config.Name("Ambient Mode")
	@Config.Comment("Enable or Disable Ambient mode. In this mode vanilla music will not be turned off when a custom music is set for a biome." +
					"\nEnable if you want to use this mod for ambience sound tracks instead of music.")
	public static boolean adambientMode = false;

	@Config.Name("Combat Music Options")
	@Config.Comment("Combat options to toggle or set.")
	public static final CombatOptions combatOptions = new CombatOptions();

	@Config.Comment("Link normal music to battle music here!")
	public static Map<String, String> musicLink = new HashMap<>();

	@Config.Name("Cavern Music Options")
	@Config.Comment("Cavern Music Options.")
	public static final UndergroundOptions cpundergroundOptions = new UndergroundOptions();

	@Config.Name("Boss Music Options")
	@Config.Comment("Boss Music Options.")
	public static final BossMusicOptions bossMusicOptions = new BossMusicOptions();

	@Mod.EventBusSubscriber(modid = BiomeMusic.MODID)
	public static class EventHandler {

		@SubscribeEvent
		public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if (event.getModID().equals(BiomeMusic.MODID)) {
				ConfigManager.sync(BiomeMusic.MODID, Config.Type.INSTANCE);
				updateBiomeList();  // Call this method to update the biome list in the config
				updateBiomeTagList();
				updateMusicList();  // Call this method to update the music list in the config
			}
		}
	}

	public static void updateBiomeList() {
		IForgeRegistry<Biome> biomeRegistry = RegistryManager.ACTIVE.getRegistry(Biome.class);

		// Loop through all biomes
		for (Biome biome : biomeRegistry) {
			String biomeName = biome.getRegistryName().toString();

			// If the biome is not in the config, add it with a default value
			biomeMusicMap.putIfAbsent(biomeName, "default_music");
		}

		// Save any changes to the config (new biomes added)
		ConfigManager.sync(BiomeMusic.MODID, Config.Type.INSTANCE);
	}

	public static void updateBiomeTagList() {
		IForgeRegistry<Biome> biomeRegistry = RegistryManager.ACTIVE.getRegistry(Biome.class);

		// Collect all unique tags across registered biomes
		Set<String> allTags = biomeRegistry.getValues().stream()
				.flatMap(biome -> BiomeDictionary.getTypes(biome).stream())
				.map(type -> type.getName().toLowerCase())
				.collect(Collectors.toSet());

		// Add each unique tag to the biomeTagMusicMap with a default value, if not already present
		for (String tag : allTags) {
			biomeTagMusicMap.putIfAbsent(tag, "default_music");
		}

		// Save any changes to the config (new tags added)
		ConfigManager.sync(BiomeMusic.MODID, Config.Type.INSTANCE);
	}


	public static void updateMusicList() {
		// Fetch available music files from the custom folder
		String[] musicFiles = MusicFileHandler.getAvailableMusicFiles().toArray(new String[0]);

		// Update the config with the recognized music files
		abavailableMusicFiles = musicFiles;

		for (String music : musicFiles) {
			musicLink.putIfAbsent(music, "");
		}

		// Sync the config after updating the music list
		ConfigManager.sync(BiomeMusic.MODID, Config.Type.INSTANCE);
	}

	public static class FadeOptions {

		@Config.Name("Polling Rate")
		@Config.Comment("Polling rate for the music change. Default: 140 | [INT / TICKS]")
		public int pollingRate = 140;

		@Config.Name("Vanilla Fade-out")
		@Config.Comment("Vanilla Music Fade-out Time. Default: 10000 | [INT / MS]")
		public int vanillaMusicFadeOutTime = 10000;

		@Config.Name("Biome Music Fade-in")
		@Config.Comment("Custom Music Fade-in Time. Default: 20000 | [INT / MS]")
		public int customMusicFadeInTime = 20000;

		@Config.Name("Biome Music Fade-out")
		@Config.Comment("Custom Music Fade-out Time. Default: 20000 | [INT / MS]")
		public int customMusicFadeOutTime = 20000;

		@Config.Name("Combat Music Fade-in Time")
		@Config.Comment("Custom Combat Music Fade-in Time. Default: 10000 | [INT / MS]")
		public int combatMusicFadeInTime = 10000;

		@Config.Name("Music Volume Multiplier")
		@Config.Comment("The Volume Multiplier for custom music.")
		public double musicVolumeMultiplier = 0.8;
	}

	public static class CombatOptions {

		@Config.Name("Combat Music")
		@Config.Comment("Enable or Disable Combat Music.")
		public boolean enableCombatMusic = false;

		@Config.Name("Combat Music List")
		@Config.Comment("Put any music you want to be played during combat encounters in here.")
		public String combatMusicList = "default_music";

		@Config.Name("Combat Music Range")
		@Config.Comment("Range radius in which aggro'd mobs are counted for combat music trigger")
		public int combatRadius = 15;

		@Config.Name("Combat Music Start Number")
		@Config.Comment("The amount of mobs needed to start the combat music. always put higher than stop amount.")
		public int combatStartNumber = 5;

		@Config.Name("Combat Music Stop Number")
		@Config.Comment("The amount of mobs that should be left before the combat music stops. always put lower than start number.")
		public int combatStopNumber = 2;

	}

	public static class UndergroundOptions {
		@Config.Name("Cavern Music")
		@Config.Comment("Enable for music under a certain Y level. Good for cavern music.")
		public boolean enableUndergroundMusic = false;

		@Config.Name("Cavern Music Start Level")
		@Config.Comment("The Y level at which cavern music starts.")
		public int undergroundMusicYLevelStart = 40;

		@Config.Name("Cavern Music Stop Level")
		@Config.Comment("The Y Level at which Cavern Music Stops. To prevent frequent switching when at the start level, please set this value higher then the start Y level.")
		public int undergroundMusicYLevelStop = 50;

		@Config.Name("Cavern Music list:")
		@Config.Comment("The Cavern Music that will play underground.")
		public String CavernMusic = "default_music";
	}

	public static class BossMusicOptions {

		@Config.Name("Boss Music")
		@Config.Comment("Enable or Disable Boss Music!")
		public boolean enableBossMusic = false;

		@Config.Name("Boss Music List")
		@Config.Comment("Set A boss mob and its music here! boss mob followed by a comma for its music e.g. [lycanitesmobs:rahovart,doomfear.ogg]")
		public String[] bossMusicList = new String[0];

		@Config.Name("Boss mob detection range.")
		@Config.Comment("The range in which the mod will check for a boss mob.")
		public int bossMusicRange = 50;
	}

}