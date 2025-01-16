package musify.combatutils;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;

@SideOnly(Side.CLIENT)
public class TargetingUtils {

    private static final Map<UUID, Long> targetingMobs = new HashMap<>();
    private static final int CACHE_TIMEOUT = 200;

    /**
     * Counts the number of hostile mobs targeting the specified player within a given radius.
     *
     * - Mobs with the player as their attack target are added to the cache.
     * - If a mob is already in the cache, its timeout is renewed.
     * - Mobs that have not updated within the CACHE_TIMEOUT period are removed.
     *
     * @param player The player to check for mobs targeting.
     * @param radius The radius to search for mobs.
     * @return The count of hostile mobs currently targeting the player.
     */
    public static int countMobsTargetingPlayer(EntityPlayer player, double radius) {
        World world = player.getEntityWorld();
        long currentTime = world.getTotalWorldTime();

        AxisAlignedBB searchArea = new AxisAlignedBB(
                player.posX - radius, player.posY - radius, player.posZ - radius,
                player.posX + radius, player.posY + radius, player.posZ + radius
        );
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, searchArea);

        for (EntityLivingBase entity : entities) {
            if ((entity instanceof IAnimals || entity instanceof IEntityOwnable || entity instanceof EntityLiving)) {
                EntityLiving hostileMob = (EntityLiving) entity;

                boolean isTargetingPlayer = false;
                if (hostileMob.getAttackTarget() != null &&
                        hostileMob.getAttackTarget().getUniqueID().equals(player.getUniqueID())) {
                    isTargetingPlayer = true;
                } else if (hostileMob.getRevengeTarget() != null &&
                        hostileMob.getRevengeTarget().getUniqueID().equals(player.getUniqueID())) {
                    isTargetingPlayer = true;
                }

                if (isTargetingPlayer) {
                    targetingMobs.put(hostileMob.getUniqueID(), currentTime);
                }
            }
        }

        targetingMobs.entrySet().removeIf(entry -> currentTime - entry.getValue() > CACHE_TIMEOUT);

        return targetingMobs.size();
    }

    /**
     * Resets the targeting cache, clearing all tracked mobs.
     */
    public static void resetCache() {
        targetingMobs.clear();
    }
}
