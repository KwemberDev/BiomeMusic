package biomemusic.handlers;

import biomemusic.BiomeMusic;
import biomemusic.combatutils.TargetingUtils;
import biomemusic.musicplayer.CustomMusicPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.achievement.GuiStats;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static biomemusic.combatutils.TargetingUtils.countHistory;
import static biomemusic.handlers.MainMenuMusicHandler.isMainMenuMusicPlaying;
import static biomemusic.handlers.MainMenuMusicHandler.isMainMenuScreen;
import static biomemusic.musicplayer.CustomMusicPlayer.*;

@Mod.EventBusSubscriber
@SideOnly(Side.CLIENT)
public class PauseEventHandler {

    private boolean hasQueueBeenReset = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();

        if (mc.world == null && !hasQueueBeenReset) {
            TargetingUtils.resetQueue();
            hasQueueBeenReset = true;
        } else if (mc.world != null) {
            hasQueueBeenReset = false;
        }

        if (mc.world == null && !isMainMenuMusicPlaying) {
            if (CustomMusicPlayer.isMusicPlaying()) {
                CustomMusicPlayer.stopMusic();

            }
            return;
        }

        if (isPauseMenuOpen(mc)) {
            if (CustomMusicPlayer.isMusicPlaying() && !CustomMusicPlayer.isPaused()) {
                CustomMusicPlayer.pauseMusic();
            } else if (combatMusicClip != null) {
                if (combatMusicClip.isRunning() && !CustomMusicPlayer.isPaused()) {
                    CustomMusicPlayer.pauseCombatMusic();
                }
            }
        } else {

            if (CustomMusicPlayer.isPaused() && !isMainMenuScreen(mc)) {
                if (!CustomMusicPlayer.isFading) {
                    CustomMusicPlayer.adjustVolume();
                }
                CustomMusicPlayer.resumeMusic();
            } else if (CustomMusicPlayer.isPaused() && isMainMenuScreen(mc)) {
                if (!CustomMusicPlayer.isFading) {
                    CustomMusicPlayer.adjustVolume();
                }
                resumeMusic();
            }
        }
    }

    private boolean isPauseMenuOpen(Minecraft mc) {
        if (mc.currentScreen == null) {
            return false;
        }

        boolean isKnownPauseMenu = mc.currentScreen instanceof GuiIngameMenu
                || mc.currentScreen instanceof GuiOptions
                || mc.currentScreen instanceof GuiStats
                || mc.currentScreen instanceof GuiVideoSettings
                || mc.currentScreen instanceof GuiControls
                || mc.currentScreen instanceof GuiLanguage
                || mc.currentScreen instanceof GuiScreenOptionsSounds
                || mc.currentScreen instanceof GuiConfig
                || mc.currentScreen instanceof GuiGameOver
                || mc.currentScreen instanceof GuiScreenResourcePacks
                || mc.currentScreen instanceof GuiShareToLan
                || mc.currentScreen instanceof GuiModList
                || mc.currentScreen instanceof GuiCustomizeSkin
                || mc.currentScreen instanceof GuiSnooper
                || mc.currentScreen instanceof ScreenChatOptions
                ;

        if (mc.currentScreen.getClass().getName().contains("Advancement")) {
            return true;
        }

        if (mc.currentScreen.getClass().getName().contains("LockIconButton")) {
            return true;
        }

        return isKnownPauseMenu;
    }

}
