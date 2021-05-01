package indi.goldenwater.healthdisplay.utils;

import org.bukkit.command.CommandSender;

public final class CheckPermissions {
    public static boolean hasPermissions(CommandSender sender, String perm) {
        if (sender.hasPermission(perm) || sender.hasPermission("*")) return true;

        String[] nodes = perm.split("\\.");
        int perm_long = nodes.length;

        while (perm_long > 1) {
            StringBuilder perm2 = new StringBuilder(nodes[0]);
            for (int i = 1; i < perm_long; ++i) {
                if (i == perm_long - 1) {
                    perm2.append(".*");
                } else {
                    perm2.append(".").append(nodes[i]);
                }
            }
            if (sender.hasPermission(perm2.toString())) return true;
            perm_long--;
        }

        return false;
    }

    public static boolean hasPermissions_Tips(CommandSender sender, String perm, String messageOnNoPerm) {
        boolean hasPerm;
        hasPerm = hasPermissions(sender, perm);
        if (!hasPerm) {
            sender.sendMessage(messageOnNoPerm);
        }
        return hasPerm;
    }

    public static boolean hasPermissions_Tips(CommandSender sender, String perm) {
        boolean hasPerm;
        hasPerm = hasPermissions(sender, perm);
        if (!hasPerm) {
            sender.sendMessage("§cYou don't have permission.");
        }
        return hasPerm;
    }
}
