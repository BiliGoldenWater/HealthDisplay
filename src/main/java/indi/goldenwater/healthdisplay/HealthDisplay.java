package indi.goldenwater.healthdisplay;

import indi.goldenwater.healthdisplay.listeners.OnEntityDamageByEntityEvent;
import indi.goldenwater.healthdisplay.listeners.OnPlayerQuitEvent;
import indi.goldenwater.healthdisplay.utils.ConfigWatchService;
import indi.goldenwater.healthdisplay.utils.ConfigWatchService.DoSomeThing;
import indi.goldenwater.healthdisplay.utils.ConfigWatchService.CheckFile;
import indi.goldenwater.healthdisplay.utils.I18nManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public final class HealthDisplay extends JavaPlugin {
    private static HealthDisplay instance;
    private I18nManager i18nManager;
    private final Map<String, BukkitRunnable> playerRunnable = new HashMap<>();
    private ConfigWatchService watchService;


    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;
        saveDefaultConfig();

        i18nManager = new I18nManager(getDataFolder(), "langs", "en_US");
        i18nManager.releaseDefaultLangFile(this, "langs", "langList.json", false);

        if (getConfig().getBoolean("fileWatchService")) {
            watchService = new ConfigWatchService(this);
            DoSomeThing doSomeThing = new DoSomeThing() {
                @Override
                public void reload() {
                    reloadConfig();
                    i18nManager.reload();
                }

                @Override
                public void release() {
                    saveDefaultConfig();
                    i18nManager.releaseDefaultLangFile(HealthDisplay.getInstance(), "langs", "langList.json", false);
                }
            };

            CheckFile checkFile = name -> name.endsWith(".yml") || name.endsWith(".json");
            watchService.register("fileWatchService", checkFile, doSomeThing);
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

    public I18nManager getI18nManager() {
        return i18nManager;
    }
}
