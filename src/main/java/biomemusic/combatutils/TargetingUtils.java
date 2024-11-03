package biomemusic.combatutils;

import biomemusic.BiomeMusic;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityOwnable;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import java.util.LinkedList;
import java.util.Queue;

@SideOnly(Side.CLIENT)
public class TargetingUtils {

    // Define the size of the rolling average window
    private static final int SMOOTHING_WINDOW = 5;
    private static final Queue<Integer> countHistory = new LinkedList<>();

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

        // Define the search area around the player using an AxisAlignedBB.
        AxisAlignedBB searchArea = new AxisAlignedBB(
                player.posX - radius, player.posY - radius, player.posZ - radius,
                player.posX + radius, player.posY + radius, player.posZ + radius
        );

        // Retrieve all EntityLivingBase instances within the specified search area.
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, searchArea);

        int targetingCount = 0;
        //TODO FIX THIS SHIT, IT KEEPS PUTTING 0 IN AND NOT RECOGNIZING THE MOBS OR SOME SHIT
        for (EntityLivingBase entity : entities) {
            // Check if the entity is a hostile mob (implements IMob) and is an instance of EntityLiving.
            if ((entity instanceof IAnimals || entity instanceof IEntityOwnable) && entity instanceof EntityLiving) {
                EntityLiving hostileMob = (EntityLiving) entity;
                // Check if the hostile mob's current attack target or revenge target is the player.
                if (hostileMob.getAttackTarget() == player || hostileMob.getRevengeTarget() == player) {
                    targetingCount++;
                }
            }
        }

        // Update the count history for smoothing
        if (countHistory.size() >= SMOOTHING_WINDOW) {
            countHistory.poll();  // Remove the oldest count if we exceed the window size
        }
        countHistory.offer(targetingCount);  // Add the new count to the history

        // Calculate the rolling average from the history
        int smoothedCount = countHistory.stream().mapToInt(Integer::intValue).sum() / countHistory.size();

        // Log and return the smoothed targeting count
        BiomeMusic.LOGGER.info("CURRENT AGGRO MOB COUNT (Smoothed): {}", smoothedCount);
        entities.clear();
        return smoothedCount;
    }
}
