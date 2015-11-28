package io.github.tommsy64.satchels;

import org.bukkit.command.CommandSender;

public enum Permissions {
    RELOAD("satchels.reload"), HELP("satchels.help"), CREATE("satchels.create"), CLEAR_CACHE("satchels.clearCache");

    public final String permission;

    private Permissions(String permission) {
        this.permission = permission;
    }

    public boolean check(CommandSender sender) {
        return sender.hasPermission(permission);
    }
}
