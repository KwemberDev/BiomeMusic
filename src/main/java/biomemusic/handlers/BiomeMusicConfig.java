package biomemusic.handlers;

import biomemusic.BiomeMusic;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryManager;

import java.util.HashMap;
import java.util.Map;


@Config(modid = BiomeMusic.MODID)
public class BiomeMusicConfig {

	@Config.Comment("What music to play on the main menu.")
	@Config.Name("Main Menu Music")
	public static String mainMenuMusic = "default_music";

	@Config.Comment("List of recognized .ogg music files")
	public static String[] availableMusicFiles = new String[0]; // Start with an empty array

	@Config.Comment("Biome Music Mapping")
	public static Map<String, String> biomeMusicMap = new HashMap<>();

	// List of available music files (now using a String array)

	@Config.Comment("Fade Options")
	@Config.Name("Fade Options")
	public static final FadeOptions fadeOptions = new FadeOptions();

	@Config.Name("Ambient Mode")
	@Config.Comment("Enable or Disable Ambient mode. In this mode vanilla music will not be turned off when a custom music is set for a biome." +
					"\nEnable if you want to use this mod for ambience sound tracks instead of music.")
	public static boolean ambientMode = false;



	@Mod.EventBusSubscriber(modid = BiomeMusic.MODID)
	public static class EventHandler {

		@SubscribeEvent
		public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if (event.getModID().equals(BiomeMusic.MODID)) {
				ConfigManager.sync(BiomeMusic.MODID, Config.Type.INSTANCE);
				updateBiomeList();  // Call this method to update the biome list in the config
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


	public static void updateMusicList() {
		// Fetch available music files from the custom folder
		String[] musicFiles = MusicFileHandler.getAvailableMusicFiles().toArray(new String[0]);

		// Update the config with the recognized music files
		availableMusicFiles = musicFiles;

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

		@Config.Name("Fade-in")
		@Config.Comment("Custom Music Fade-in Time. Default: 20000 | [INT / MS]")
		public int customMusicFadeInTime = 20000;

		@Config.Name("Fade-out")
		@Config.Comment("Custom Music Fade-out Time. Default: 20000 | [INT / MS]")
		public int customMusicFadeOutTime = 20000;
	}
}