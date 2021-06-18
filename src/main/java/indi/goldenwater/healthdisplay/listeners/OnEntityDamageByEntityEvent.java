package indi.goldenwater.healthdisplay.listeners;

import indi.goldenwater.healthdisplay.HealthDisplay;
import indi.goldenwater.healthdisplay.utils.I18nManager;
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

                while (System.currentTimeMillis() < time_start + (durationSecond * 1000L) &&
                        !this.isCancelled() &&
                        playerRunnable.get(playerName) == this) {
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

    public void showHealth(HealthDisplay plugin, Configuration config, Player targetPlayer, Entity entity) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!(entity instanceof LivingEntity)) return;

                final LivingEntity livingEntity = (LivingEntity) entity;
                final I18nManager.L10nGetter l = plugin.getI18nManager().getL10nGetter(targetPlayer.getLocale());

                final String originMessage = config.getString("message.text","{{entityName}}§7: [§r{{healthNotEmpty}}{{healthEmpty}}§7]§r §r{{healthNum}}§7/§r{{healthNumMax}}§r");
                final int healthBarLength = config.getInt("message.healthBarLength", 20);
                final String healthBarNotEmpty = config.getString("message.healthBarNotEmpty");
                final String healthBarEmpty = config.getString("message.healthBarEmpty");
                final int healthNumDecimalPlaces = config.getInt("message.healthNumDecimalPlaces");
                final boolean healthBarRoundUp = config.getBoolean("message.roundUp", true);

                final double maxHealth = livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                final double health = livingEntity.getHealth();
                final double healthPercent = health / maxHealth;

                final int healthNotEmptyLength = (int) (healthBarRoundUp ?
                        Math.ceil(healthBarLength * healthPercent) :
                        (healthBarLength * healthPercent));
                final int healthEmptyLength = healthBarLength - healthNotEmptyLength;

                final String healthNotEmptyStr = repeatString(healthNotEmptyLength, healthBarNotEmpty);
                final String healthEmptyStr = repeatString(healthEmptyLength, healthBarEmpty);

                assert originMessage != null;
                String finalMessage = originMessage.replace("{{entityName}}",
                        entity.getCustomName() == null ?
                                getName(l, entity) :
                                entity.getCustomName())
                        .replace("{{healthNotEmpty}}", healthNotEmptyStr)
                        .replace("{{healthEmpty}}", healthEmptyStr)
                        .replace("{{healthNum}}", String.format("%." + healthNumDecimalPlaces + "f", health))
                        .replace("{{healthNumMax}}", String.format("%." + healthNumDecimalPlaces + "f", maxHealth));

                BaseComponent[] message;

                message = TextComponent.fromLegacyText(finalMessage);

                targetPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, message);
            }
        }.runTaskLaterAsynchronously(plugin, 1);
    }

    public String getName(I18nManager.L10nGetter l, Entity entity) {
        String entityName = entity.getName();
        if (l.getLanguage().equalsIgnoreCase("en_us")) {
            return entityName;
        } else {
            String entityType = entity.getType().toString().toLowerCase();
            String result = l.l(entityType);
            if (result.equals(entityType)) {
                return entityName;
            } else {
                return result;
            }
        }
    }

    public String repeatString(int times, String str) {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < times; i++) {
            stringBuilder.append(str);
        }

        return stringBuilder.toString();
    }
}
