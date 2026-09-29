package dev.shop;

import dev.shop.economy.CommandEconomy;
import dev.shop.economy.EconomyProvider;
import dev.shop.economy.PlayerPointsEconomy;
import dev.shop.economy.VaultEconomy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public class VolterShop extends JavaPlugin {
    private EconomyProvider economy;
    private ShopGUI gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        gui = new ShopGUI(this);
        gui.load();
        getServer().getPluginManager().registerEvents(gui, this);
        // One tick later, so every economy plugin has finished registering itself.
        Bukkit.getScheduler().runTask(this, this::loadEconomy);
    }

    public EconomyProvider economy() { return economy; }

    private void loadEconomy() {
        String type = getConfig().getString("economy.provider", "VAULT").trim().toUpperCase(Locale.ROOT);
        try {
            economy = switch (type) {
                case "PLAYERPOINTS" -> new PlayerPointsEconomy();
                case "COMMAND" -> new CommandEconomy(
                        getConfig().getString("economy.command.balance-placeholder"),
                        getConfig().getString("economy.command.deposit"),
                        getConfig().getString("economy.command.withdraw"));
                default -> new VaultEconomy();
            };
            getLogger().info("Economy provider: " + economy.name() + (economy.isReady() ? "" : "  ** NOT READY **"));
        } catch (Throwable t) {
            economy = null;
            getLogger().severe("Could not load economy provider '" + type + "': " + t);
        }
    }

    public void reload() {
        reloadConfig();
        gui.load();
        loadEconomy();
    }

    /** Coloured, prefixed message from config. Extra args are {placeholder}, value pairs. */
    public String msg(String key, String... kv) {
        String s = getConfig().getString("messages." + key, key);
        for (int i = 0; i + 1 < kv.length; i += 2) s = s.replace(kv[i], kv[i + 1]);
        return ChatColor.translateAlternateColorCodes('&', getConfig().getString("messages.prefix", "") + s);
    }

    public String money(double v) {
        try {
            return String.format(Locale.US, getConfig().getString("economy.format", "$%.2f"), v);
        } catch (Exception e) {
            return String.valueOf(v);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("shop.reload")) { sender.sendMessage(msg("no-permission")); return true; }
            reload();
            sender.sendMessage(msg("reloaded"));
            return true;
        }
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
        if (!p.hasPermission("shop.use")) { p.sendMessage(msg("no-permission")); return true; }
        gui.openMain(p);
        return true;
    }
}
