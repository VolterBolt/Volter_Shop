package dev.shop.economy;

import org.bukkit.entity.Player;

/** Anything the shop can charge / pay with. Add your own by implementing this. */
public interface EconomyProvider {
    boolean isReady();
    String name();
    double balance(Player p);
    boolean withdraw(Player p, double amount);
    boolean deposit(Player p, double amount);
}
