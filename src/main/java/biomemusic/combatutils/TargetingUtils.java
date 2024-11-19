package biomemusic.combatutils;

import biomemusic.BiomeMusic;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@SideOnly(Side.CLIENT)
public class TargetingUtils {

    private static final int SMOOTHING_WINDOW = 5;
    public static final Queue<Integer> countHistory = new LinkedList<>();

    /**
     * Counts the number of hostile mobs targeting the specified player within a given radius,
     * applying a rolling average to smooth out fluctuations.
     *
     * @param player The player to check for mobs targeting.
     * @param radius The radius to search for mobs.
     * @return The smoothed count of hostile mobs currently targeting the player.
     */
    @SideOnly(Side.CLIENT)
    public static int countMobsTargetingPlayer(EntityPlayer player, double radius) {
        World world = player.getEntityWorld();
        AxisAlignedBB searchArea = new AxisAlignedBB(
                player.posX - radius, player.posY - radius, player.posZ - radius,
                player.posX + radius, player.posY + radius, player.posZ + radius
        );
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, searchArea);
        int targetingCount = 0;
        for (EntityLivingBase entity : entities) {
            if ((entity instanceof IAnimals || entity instanceof IEntityOwnable) && entity instanceof EntityLiving) {
                EntityLiving hostileMob = (EntityLiving) entity;
                if (hostileMob.getAttackTarget() == player || hostileMob.getRevengeTarget() == player) {
                    targetingCount++;
                }
            }
        }

        if (countHistory.size() >= SMOOTHING_WINDOW) {
            countHistory.poll();
        }
        countHistory.offer(targetingCount);
        int smoothedCount = countHistory.stream().mapToInt(Integer::intValue).sum() / countHistory.size();
        entities.clear();
        return smoothedCount;
    }

    public static void resetQueue() {
        countHistory.clear();
        for (int i = 0; i < SMOOTHING_WINDOW; i++) {
            countHistory.add(0);
        }
    }
}
