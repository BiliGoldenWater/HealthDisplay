package indi.goldenwater.healthdisplay.utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.nio.file.*;

public class ConfigWatchService {
    private final JavaPlugin plugin;
    private WatchService watchService;
    private BukkitRunnable watchServiceRunnable;
    private WatchKey key;
    private boolean isRunning;

    public ConfigWatchService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @param configKey config key of this feature(bool) if configKey is null then not monitor that.
     */
    public void register(String configKey, CheckFile checkFile, DoSomeThing doSomeThing) {
        isRunning = true;
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
                        System.out.println("1");
                        while (isRunning && (key = watchService.take()) != null) {
                            System.out.println("2");
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
                            System.out.println("3");
                            key.reset();
                        }
                        System.out.println("4");
                        try {
                            watchService.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("5");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ClosedWatchServiceException ignored){

                    }
                }
            };
            watchServiceRunnable.runTaskAsynchronously(plugin);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unregister() {
        isRunning = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
