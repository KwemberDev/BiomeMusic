package musify.handlers;

import musify.Musify;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class MusicFileHandler {

    public static List<String> getAvailableMusicFiles() {
        List<String> musicFiles = new ArrayList<>();

        File folder = Musify.musicFolder;

        if (folder != null && folder.exists() && folder.isDirectory()) {
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