package indi.goldenwater.healthdisplay.utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.file.*;

public class ConfigWatchService {
    private final JavaPlugin plugin;
    private WatchService watchService;
    private BukkitRunnable watchServiceRunnable;

    public ConfigWatchService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @param configKey config key of this feature(bool) if configKey is null then not monitor that.
     */
    public void register(String configKey, CheckFile checkFile, DoSomeThing doSomeThing) {
        if (configKey != null && !plugin.getConfig().getBoolean(configKey)) {
            return;
        }
        try {
            Path dataFolder = plugin.getDataFolder().toPath();
            watchService = FileSystems.getDefault().newWatchService();
            dataFolder.register(watchService, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
            watchServiceRunnable = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        WatchKey key;
                        while ((key = watchService.take()) != null && !this.isCancelled()) {
                            for (WatchEvent<?> event : key.pollEvents()) {
                                if (event.kind().equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                                    if (checkFile.check(event.context().toString())) {
                                        doSomeThing.release();
                                    }
                                } else if (event.kind().equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                                    if (checkFile.check(event.context().toString())) {
                                        doSomeThing.reload();
                                        if (configKey != null && !plugin.getConfig().getBoolean(configKey)) {
                                            this.cancel();
                                        }
                                    }
                                }
                            }
                            key.reset();
                        }
                        try {
                            watchService.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            watchServiceRunnable.runTaskAsynchronously(plugin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unregister() {
        if (!watchServiceRunnable.isCancelled()) {
            watchServiceRunnable.cancel();
        }
    }

    public interface DoSomeThing {
        void reload();

        void release();
    }

    public interface CheckFile {
        boolean check(String name);
    }
}
