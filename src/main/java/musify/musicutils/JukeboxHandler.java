package musify.musicutils;

import net.minecraft.block.BlockJukebox;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static musify.handlers.BiomeMusicConfig.miscOptions;
import static musify.musicplayer.CustomMusicPlayer.*;

@SideOnly(Side.CLIENT)
public class JukeboxHandler {
    private static final double RANGE = miscOptions.jukeboxRange; // Range to check for jukeboxes
    public static boolean isJukeboxPlaying = false; // Track if a jukebox is playing
    private static int countFalse = 0;

    public static void findJukebox(EntityPlayer player) {
        World world = player.getEntityWorld();

        BlockPos playerPos = new BlockPos(player.posX, player.posY, player.posZ);
//        Musify.LOGGER.info("POSITIONS: {}, {}, {}",player.posX,player.posY,player.posZ);
        boolean foundJukebox = false;

        // Iterate through all positions in the range
        for (BlockPos pos : BlockPos.getAllInBox(
                playerPos.add(-RANGE, -RANGE, -RANGE),
                playerPos.add(RANGE, RANGE, RANGE)
        )) {
            TileEntity tile = world.getTileEntity(pos);

            if (tile instanceof BlockJukebox.TileEntityJukebox) {
                BlockJukebox.TileEntityJukebox jukebox = (BlockJukebox.TileEntityJukebox) tile;

                // Check if the jukebox contains a record
                if (jukebox.getRecord() != ItemStack.EMPTY) {
//                    Musify.LOGGER.info("Found jukebox! Record: {}", jukebox.getRecord().getItem().getRegistryName());
                    foundJukebox = true;
                    break;
                }
            }
        }

        // Handle state changes
        if (foundJukebox && !isJukeboxPlaying && !isFading) {
            isJukeboxPlaying = true;
            silenceMusic();
        } else if (!foundJukebox && isJukeboxPlaying && !isFading) {
            countFalse++;
            if (countFalse >= 5) {
                isJukeboxPlaying = false;
                resumeMusicWithFadeIn();
                countFalse = 0;
            }
        }
    }
}

