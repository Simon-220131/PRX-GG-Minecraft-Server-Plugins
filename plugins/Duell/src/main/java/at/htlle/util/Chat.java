package at.htlle.util;

import org.bukkit.ChatColor;

public class Chat {
    public static String ok(String s) { return ChatColor.GREEN + "✔ " + ChatColor.RESET + s; }
    public static String err(String s) { return ChatColor.RED + "✖ " + ChatColor.RESET + s; }
    public static String info(String s) { return ChatColor.AQUA + s + ChatColor.RESET; }
    public static String warn(String s) { return ChatColor.GOLD + "⚠ " + ChatColor.RESET + s; }
    public static String cmd(String s) { return ChatColor.YELLOW + s + ChatColor.RESET; }
    public static String gray(String s) { return ChatColor.DARK_GRAY + s + ChatColor.RESET; }
    public static String opponent(String name) { return ChatColor.RED + "Gegner: " + name + ChatColor.RESET; }
    public static String announce(String s) { return ChatColor.LIGHT_PURPLE + s + ChatColor.RESET; }
}