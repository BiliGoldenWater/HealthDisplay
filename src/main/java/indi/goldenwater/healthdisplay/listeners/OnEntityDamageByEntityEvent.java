package indi.goldenwater.healthdisplay.listeners;

import indi.goldenwater.healthdisplay.HealthDisplay;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class OnEntityDamageByEntityEvent implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event){
        final Entity damager = event.getDamager();
        if (damager instanceof Player){
            final Player player = (Player) damager;
            final HealthDisplay instance = HealthDisplay.getInstance();
            final Configuration config = instance.getConfig();
            final boolean isBlacklist = config.getBoolean("settings.isBlacklist");
            final boolean isJsonMessage = config.getBoolean("message.useJson");

            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent());
        }
    }
}
