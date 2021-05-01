package indi.goldenwater.healthdisplay;

import indi.goldenwater.healthdisplay.utils.ConfigWatchService;
import org.bukkit.plugin.java.JavaPlugin;

public final class HealthDisplay extends JavaPlugin {
    private static HealthDisplay instance;
    private ConfigWatchService watchService;


    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        saveDefaultConfig();

        if (getConfig().getBoolean("fileWatchService")) {
            watchService = new ConfigWatchService(this);
            watchService.register("fileWatchService");
        }

        getLogger().info("Enabled.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        watchService.unregister();
        getLogger().info("Disabled.");
    }

    public static HealthDisplay getInstance() {
        return instance;
    }
}
