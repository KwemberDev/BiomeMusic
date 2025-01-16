package musify.combatutils;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

import static musify.handlers.BiomeMusicConfig.bossMusicOptions;

@SideOnly(Side.CLIENT)
public class BossTargetUtils {

    @SideOnly(Side.CLIENT)
    public static String bossMusicFile(EntityPlayer player) {
        if (bossMusicOptions.enableBossMusic) {
            for (String entry : bossMusicOptions.bossMusicList) {
                String[] parts = entry.split(",");
                if (parts.length != 2) continue;
                String mobId = parts[0].trim();
                String musicFile = parts[1].trim();
                AxisAlignedBB searchBox = new AxisAlignedBB(
                        player.posX - bossMusicOptions.bossMusicRange, player.posY - bossMusicOptions.bossMusicRange, player.posZ - bossMusicOptions.bossMusicRange,
                        player.posX + bossMusicOptions.bossMusicRange, player.posY + bossMusicOptions.bossMusicRange, player.posZ + bossMusicOptions.bossMusicRange
                );
                List<Entity> nearbyEntities = player.world.getEntitiesWithinAABB(Entity.class, searchBox, entity -> {
                    ResourceLocation entityID = EntityList.getKey(entity);
                    return entityID != null && entityID.toString().equals(mobId);
                });
                if (!nearbyEntities.isEmpty()) {
                    return musicFile;
                }
            }
        }
        return null;
    }
}
