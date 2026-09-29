package dev.shop.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/** Talks to PlayerPoints through reflection, so there is no compile-time dependency. */
public class PlayerPointsEconomy implements EconomyProvider {

    private Object api() {
        try {
            Plugin pp = Bukkit.getPluginManager().getPlugin("PlayerPoints");
            if (pp == null || !pp.isEnabled()) return null;
            return pp.getClass().getMethod("getAPI").invoke(pp);
        } catch (Exception e) {
            return null;
        }
    }

    private Object call(String method, Player p, Integer amount) {
        try {
            Object api = api();
            if (api == null) return null;
            return amount == null
                    ? api.getClass().getMethod(method, UUID.class).invoke(api, p.getUniqueId())
                    : api.getClass().getMethod(method, UUID.class, int.class).invoke(api, p.getUniqueId(), amount);
        } catch (Exception e) {
            return null;
        }
    }

    @Override public boolean isReady() { return api() != null; }
    @Override public String name() { return "PlayerPoints"; }

    @Override public double balance(Player p) {
        Object r = call("look", p, null);
        return r instanceof Integer i ? i : 0;
    }

    // Points are whole numbers: round in the server's favour.
    @Override public boolean withdraw(Player p, double a) { return Boolean.TRUE.equals(call("take", p, (int) Math.ceil(a))); }
    @Override public boolean deposit(Player p, double a) { return Boolean.TRUE.equals(call("give", p, (int) Math.floor(a))); }
}
