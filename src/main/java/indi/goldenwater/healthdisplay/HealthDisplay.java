package indi.goldenwater.healthdisplay;

import indi.goldenwater.healthdisplay.listeners.OnEntityDamageByEntityEvent;
import indi.goldenwater.healthdisplay.listeners.OnPlayerQuitEvent;
import indi.goldenwater.healthdisplay.utils.ConfigWatchService;
import indi.goldenwater.healthdisplay.utils.ConfigWatchService.DoSomeThing;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public final class HealthDisplay extends JavaPlugin {
    private static HealthDisplay instance;
    private final Map<String, BukkitRunnable> playerRunnable = new HashMap<>();
    private ConfigWatchService watchService;


    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        saveDefaultConfig();

        if (getConfig().getBoolean("fileWatchService")) {
            watchService = new ConfigWatchService(this);
            DoSomeThing doSomeThing = new DoSomeThing() {
                @Override
                public void reload() {
                    reloadConfig();
                }

                @Override
                public void release() {
                    saveDefaultConfig();
                }
            };
            watchService.register("fileWatchService", doSomeThing);
        }

        getServer().getPluginManager().registerEvents(new OnEntityDamageByEntityEvent(), this);
        getServer().getPluginManager().registerEvents(new OnPlayerQuitEvent(), this);

        getLogger().info("Enabled.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        watchService.unregister();
        getLogger().info("Disabled.");
    }

    public Map<String, BukkitRunnable> getPlayerRunnable() {
        return playerRunnable;
    }

    public static HealthDisplay getInstance() {
        return instance;
    }
}
