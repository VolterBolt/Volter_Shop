package dev.shop.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomy implements EconomyProvider {

    // Looked up on every call so it still works if the economy plugin loads late or is reloaded.
    private Economy eco() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) return null;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : rsp.getProvider();
    }

    @Override public boolean isReady() { return eco() != null; }

    @Override public String name() {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) return "Vault (not installed)";
        Economy e = eco();
        return e == null ? "Vault (no economy plugin found)" : "Vault -> " + e.getName();
    }

    @Override public double balance(Player p) { return eco().getBalance(p); }
    @Override public boolean withdraw(Player p, double a) { return eco().withdrawPlayer(p, a).transactionSuccess(); }
    @Override public boolean deposit(Player p, double a) { return eco().depositPlayer(p, a).transactionSuccess(); }
}
