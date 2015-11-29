package io.github.tommsy64.satchels;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SatchelsCommandExecutor implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0 || (args.length > 0 && args[0].equalsIgnoreCase("help")))
            helpMessage(sender);
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (Permissions.RELOAD.check(sender)) {
                    Satchels.reload();
                    sender.sendMessage(ChatColor.BLUE + "Satchels " + ChatColor.GREEN + "successfully" + ChatColor.BLUE + " reloaded!");
                } else
                    sendMessage(sender, Messages.getNoPermission());
            } else if (args[0].equalsIgnoreCase("clearCache")) {
                if (Permissions.CLEAR_CACHE.check(sender)) {
                    Satchels.clearCache();
                    if (sender instanceof Player) {
                        Satchels.log().log(Level.INFO, sender.getName() + " cleared the Inventory Cache!");
                        sender.sendMessage(ChatColor.BLUE + "Inventory Cache " + ChatColor.GREEN + "succesfully" + ChatColor.BLUE + " cleared!");
                    }
                } else
                    sendMessage(sender, Messages.getNoPermission());
            } else
                unknownMessage(sender);
        }
        return true;
    }

    public static boolean createSatchel(ItemStack item, String title, String rowsStr) {
        int size;
        try {
            size = Integer.parseInt(rowsStr);
        } catch (NumberFormatException nfe) {
            return false;
        }
        Backpack bp = Backpack.builder().title(title).rows(size).build();
        Backpack.convert(bp, item);
        return true;
    }

    private void unknownMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "That is an unknown subcommand!");
    }

    private void helpMessage(CommandSender sender) {
        if (Permissions.HELP.check(sender)) {
            sender.sendMessage(ChatColor.BLUE + "/satchels " + ChatColor.WHITE + ChatColor.UNDERLINE + "reload" + ChatColor.BLUE + " - Reloads the plugin");
            if (sender instanceof Player) {
                sender.sendMessage(ChatColor.BLUE + "/satchels " + ChatColor.WHITE + ChatColor.UNDERLINE + "clearcache" + ChatColor.BLUE
                        + " - Clears the in memory inventory cache.");
            }
        } else
            sendMessage(sender, Messages.getNoPermission());
    }

    private void sendMessage(CommandSender sender, String msg) {
        if (!msg.isEmpty())
            sender.sendMessage(msg);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> tab = new LinkedList<>();
        if (Permissions.HELP.check(sender)) {
            if (Permissions.CLEAR_CACHE.check(sender))
                tab.add("clearcache");
            if (Permissions.RELOAD.check(sender))
                tab.add("reload");
        }
        if (args.length > 0)
            tab = tab.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        return tab;
    }

    private static enum Permissions {
        RELOAD("satchels.reload"), HELP("satchels.help"), CLEAR_CACHE("satchels.clearCache");

        public final String permission;

        private Permissions(String permission) {
            this.permission = permission;
        }

        public boolean check(CommandSender sender) {
            return sender.hasPermission(permission);
        }
    }
}
