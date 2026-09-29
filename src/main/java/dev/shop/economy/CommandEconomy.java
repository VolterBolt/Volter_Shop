package dev.shop.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Universal fallback: pays/charges by running console commands and reads the
 * balance through a PlaceholderAPI placeholder. Works with any economy plugin
 * that has commands and a placeholder.
 */
public class CommandEconomy implements EconomyProvider {
    private final String placeholder, depositCmd, withdrawCmd;

    public CommandEconomy(String placeholder, String depositCmd, String withdrawCmd) {
        this.placeholder = placeholder == null ? "" : placeholder;
        this.depositCmd = depositCmd == null ? "" : depositCmd;
        this.withdrawCmd = withdrawCmd == null ? "" : withdrawCmd;
    }

    @Override public boolean isReady() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
                && !placeholder.isBlank() && !depositCmd.isBlank() && !withdrawCmd.isBlank();
    }

    @Override public String name() {
        return "Command-based" + (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") ? "" : " (PlaceholderAPI missing)");
    }

    @Override public double balance(Player p) {
        try {
            Class<?> c = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            String out = (String) c.getMethod("setPlaceholders", OfflinePlayer.class, String.class).invoke(null, p, placeholder);
            return Double.parseDouble(out.replaceAll("[^0-9.\\-]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    @Override public boolean withdraw(Player p, double a) { return run(withdrawCmd, p, a); }
    @Override public boolean deposit(Player p, double a) { return run(depositCmd, p, a); }

    private boolean run(String template, Player p, double amount) {
        String cmd = template.replace("{player}", p.getName())
                             .replace("{amount}", String.format(Locale.US, "%.2f", amount));
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }
}
