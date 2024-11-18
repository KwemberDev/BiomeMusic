package biomemusic.combatutils;

import biomemusic.BiomeMusic;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

import static biomemusic.handlers.BiomeMusicConfig.bossMusicOptions;

@SideOnly(Side.CLIENT)
public class BossTargetUtils {

    @SideOnly(Side.CLIENT)
    public static String bossMusicFile(EntityPlayer player) {

        if (bossMusicOptions.enableBossMusic) {
            for (String entry : bossMusicOptions.bossMusicList) {
                String[] parts = entry.split(",");
                if (parts.length != 2) continue; // Skip any invalid entries

                String mobId = parts[0].trim();      // mob ID, e.g., "minecraft:zombie"
                String musicFile = parts[1].trim();  // music file, e.g., "spooky_music.ogg"

                AxisAlignedBB searchBox = new AxisAlignedBB(
                        player.posX - bossMusicOptions.bossMusicRange, player.posY - bossMusicOptions.bossMusicRange, player.posZ - bossMusicOptions.bossMusicRange,
                        player.posX + bossMusicOptions.bossMusicRange, player.posY + bossMusicOptions.bossMusicRange, player.posZ + bossMusicOptions.bossMusicRange
                );

                // Get all entities within the search box
                List<Entity> nearbyEntities = player.world.getEntitiesWithinAABB(Entity.class, searchBox, entity -> {
                    // Check if the entity's ID matches the specified mob ID
                    ResourceLocation entityID = EntityList.getKey(entity);
                    return entityID != null && entityID.toString().equals(mobId);
                });

                // If any matching entities were found within the radius, return the music file
                if (!nearbyEntities.isEmpty()) {
                    // Return the music file associated with the first mob found in the list
                    return musicFile;
                }
            }
        }

        // Return null if no matching mobs were found
        return null;
    }
}
