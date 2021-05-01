package indi.goldenwater.healthdisplay.listeners;

import indi.goldenwater.healthdisplay.HealthDisplay;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;

import static indi.goldenwater.healthdisplay.utils.CheckPermissions.hasPermissions;

public class OnEntityDamageByEntityEvent implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        final Entity damager = event.getDamager();
        if (damager instanceof Player) {
            final Player player = (Player) damager;
            if (!hasPermissions(player, "healthdisplay.show")) return;

            final HealthDisplay instance = HealthDisplay.getInstance();
            final Configuration config = instance.getConfig();
            final Entity entity = event.getEntity();
            if (isBlackList(config, entity)) return;

            if (config.getBoolean("settings.continuousUpdate.enable")) {
                int duration = config.getInt("settings.continuousUpdate.duration");
                long periodMillis = config.getLong("settings.continuousUpdate.period");
                showHealthContinuous(config, player, entity, instance, duration, periodMillis);
            } else {
                showHealth(instance, config, player, entity);
            }
        }
    }

    public boolean isBlackList(Configuration config, Entity entity) {
        final boolean isBlacklist = config.getBoolean("settings.isBlacklist");
        final List<String> entityList = config.getStringList("settings.entityList");
        final boolean inEntityList = entityList.contains(entity.getType().toString());

        if (config.getBoolean("settings.debug")) {
            HealthDisplay.getInstance().getLogger().info("[Debug] Entity type:" +
                    entity.getType().toString());
        }

        if (isBlacklist) {
            return inEntityList;
        } else {
            return !inEntityList;
        }
    }

    public void showHealthContinuous(Configuration config, Player targetPlayer, Entity entity,
                                     HealthDisplay plugin, int durationSecond, long periodMillis) {
        final String playerName = targetPlayer.getName();
        final Map<String, BukkitRunnable> playerRunnable = plugin.getPlayerRunnable();

        BukkitRunnable oldRunnable = playerRunnable.get(playerName);

        if (oldRunnable != null && !oldRunnable.isCancelled()) {
            oldRunnable.cancel();
        }

        BukkitRunnable runnable = new BukkitRunnable() {
            private boolean end = false;
            @SuppressWarnings("BusyWait")
            @Override
            public void run() {
                long time_start = System.currentTimeMillis();

                while (System.currentTimeMillis() < time_start + (durationSecond * 1000L) && !this.isCancelled()) {
                    showHealth(plugin, config, targetPlayer, entity);
                    try {
                        Thread.sleep(periodMillis);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (!targetPlayer.isOnline() || end) break;
                    if (entity.isDead()) end = true;
                }
            }
        };

        playerRunnable.put(playerName, runnable);

        runnable.runTaskAsynchronously(plugin);
    }

    public void showHealth(JavaPlugin plugin, Configuration config, Player targetPlayer, Entity entity) {
        new BukkitRunnable(){
            @Override
            public void run(){
                if (!(entity instanceof LivingEntity)) return;

                final LivingEntity livingEntity = (LivingEntity) entity;
                final String originMessage = config.getString("message.text");
                final int healthBarLength = config.getInt("message.healthBarLength", 20);
                final String healthBarNotEmpty = config.getString("message.healthBarNotEmpty");
                final String healthBarEmpty = config.getString("message.healthBarEmpty");

                final double maxHealth = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                final double health = livingEntity.getHealth();
                final double healthPercent = health / maxHealth;

                final int healthNotEmptyLength = (int) (healthBarLength * healthPercent);
                final int healthEmptyLength = healthBarLength - healthNotEmptyLength;

                final String healthNotEmptyStr = repeatString(healthNotEmptyLength, healthBarNotEmpty);
                final String healthEmptyStr = repeatString(healthEmptyLength, healthBarEmpty);

                String finalMessage = originMessage.replace("{{entityName}}",
                        entity.getCustomName() == null ? entity.getName() : entity.getCustomName())
                        .replace("{{healthNotEmpty}}", healthNotEmptyStr)
                        .replace("{{healthEmpty}}", healthEmptyStr)
                        .replace("{{healthNum}}", String.format("%.1f", health))
                        .replace("{{healthNumMax}}", String.valueOf(maxHealth));

                BaseComponent[] message;

                message = TextComponent.fromLegacyText(finalMessage);

                targetPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
            }
        }.runTaskLaterAsynchronously(plugin, 1);
    }

    public String repeatString(int times, String str) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < times; i++) {
            stringBuilder.append(str);
        }

        return stringBuilder.toString();
    }
}
