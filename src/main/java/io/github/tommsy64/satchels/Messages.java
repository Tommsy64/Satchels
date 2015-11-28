package io.github.tommsy64.satchels;

import org.bukkit.ChatColor;

import lombok.Getter;

public class Messages {
    @Getter
    private static String noPermission;

    public static void setNoPermission(String noPermission) {
        Messages.noPermission = ChatColor.translateAlternateColorCodes('&', noPermission);
    }
}
