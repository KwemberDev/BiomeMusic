package biomemusic.handlers;

import biomemusic.BiomeMusic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MusicFileHandler {

    // Method to get all .ogg files from the custom folder
    public static List<String> getAvailableMusicFiles() {
        List<String> musicFiles = new ArrayList<>();

        File folder = BiomeMusic.musicFolder;

        // Make sure the folder exists
        if (folder != null && folder.exists() && folder.isDirectory()) {
            // Filter for .ogg files
            File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".ogg"));

            if (files != null) {
                for (File file : files) {
                    musicFiles.add(file.getName());
                }
            }
        }

        return musicFiles;
    }
}