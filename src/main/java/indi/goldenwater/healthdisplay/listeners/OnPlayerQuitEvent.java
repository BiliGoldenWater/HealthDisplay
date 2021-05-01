package indi.goldenwater.healthdisplay.listeners;

import indi.goldenwater.healthdisplay.HealthDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

public class OnPlayerQuitEvent implements Listener {
    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event){
        final Map<String, BukkitRunnable> playerRunnable = HealthDisplay.getInstance().getPlayerRunnable();
        final String playerName = event.getPlayer().getName();
        final BukkitRunnable runnable = playerRunnable.get(playerName);

        if (runnable!=null && !runnable.isCancelled()) {
            runnable.cancel();
        }

        playerRunnable.remove(playerName);
    }
}
