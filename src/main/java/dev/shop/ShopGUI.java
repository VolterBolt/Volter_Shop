package dev.shop;

import dev.shop.economy.EconomyProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShopGUI implements Listener {
    private static final int BACK_SLOT = 49, MAX_ITEMS = 45;

    private record Entry(Material mat, double buy, double sell) {}
    private record Category(String name, Material icon, List<Entry> entries) {}

    private static class Holder implements InventoryHolder {
        final Category cat;   // null = main menu
        Inventory inv;
        Holder(Category cat) { this.cat = cat; }
        @Override public Inventory getInventory() { return inv; }
    }

    private final VolterShop plugin;
    private final Map<Integer, Category> mainSlots = new HashMap<>();
    private int rows = 3;

    public ShopGUI(VolterShop plugin) { this.plugin = plugin; }

    // ---------------------------------------------------------------- config

    public void load() {
        mainSlots.clear();
        FileConfiguration cfg = plugin.getConfig();
        rows = Math.max(1, Math.min(6, cfg.getInt("gui.rows", 3)));
        ConfigurationSection root = cfg.getConfigurationSection("categories");
        if (root == null) return;

        int auto = 0;
        for (String id : root.getKeys(false)) {
            ConfigurationSection c = root.getConfigurationSection(id);
            if (c == null) continue;

            List<Entry> entries = new ArrayList<>();
            for (Map<?, ?> m : c.getMapList("items")) {
                Material mat = Material.matchMaterial(String.valueOf(m.get("material")));
                if (mat == null || !mat.isItem()) {
                    plugin.getLogger().warning("Unknown material '" + m.get("material") + "' in category " + id);
                    continue;
                }
                entries.add(new Entry(mat, num(m.get("buy")), num(m.get("sell"))));
            }

            Material icon = Material.matchMaterial(c.getString("icon", "CHEST"));
            Category cat = new Category(c.getString("name", id), icon == null ? Material.CHEST : icon, entries);

            int slot = c.getInt("slot", -1);
            if (slot < 0) {
                while (mainSlots.containsKey(auto)) auto++;
                slot = auto;
            }
            if (slot < rows * 9) mainSlots.put(slot, cat);
        }
    }

    private static double num(Object o) { return o instanceof Number n ? n.doubleValue() : -1; }

    // ---------------------------------------------------------------- menus

    public void openMain(Player p) {
        Holder h = new Holder(null);
        Inventory inv = Bukkit.createInventory(h, rows * 9, color(plugin.getConfig().getString("gui.title", "&8Shop")));
        h.inv = inv;

        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        String browse = color(plugin.getConfig().getString("lore.browse", ""));
        mainSlots.forEach((slot, cat) -> inv.setItem(slot, named(cat.icon(), cat.name(), List.of(browse))));
        p.openInventory(inv);
    }

    private void openCategory(Player p, Category cat) {
        Holder h = new Holder(cat);
        Inventory inv = Bukkit.createInventory(h, 54, color(cat.name()));
        h.inv = inv;

        for (int i = 0; i < Math.min(cat.entries().size(), MAX_ITEMS); i++) inv.setItem(i, display(cat.entries().get(i)));
        inv.setItem(BACK_SLOT, named(Material.ARROW, "&cBack", null));
        p.openInventory(inv);
    }

    private ItemStack display(Entry en) {
        FileConfiguration c = plugin.getConfig();
        int stack = en.mat().getMaxStackSize();
        List<String> lore = new ArrayList<>();
        if (en.buy() >= 0) {
            lore.add(color(c.getString("lore.buy", "").replace("{price}", plugin.money(en.buy()))));
            if (stack > 1)
                lore.add(color(c.getString("lore.buy-stack", "").replace("{stack_price}", plugin.money(en.buy() * stack))));
        }
        if (en.sell() >= 0) {
            lore.add(color(c.getString("lore.sell", "").replace("{price}", plugin.money(en.sell()))));
            lore.add(color(c.getString("lore.sell-all", "")));
        }
        ItemStack s = new ItemStack(en.mat());
        ItemMeta m = s.getItemMeta();
        m.setLore(lore);
        s.setItemMeta(m);
        return s;
    }

    // ---------------------------------------------------------------- clicks

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getInventory()) return;

        int slot = e.getSlot();
        if (h.cat == null) {
            Category cat = mainSlots.get(slot);
            if (cat != null) openCategory(p, cat);
            return;
        }
        if (slot == BACK_SLOT) { openMain(p); return; }
        if (slot < 0 || slot >= Math.min(h.cat.entries().size(), MAX_ITEMS)) return;

        Entry en = h.cat.entries().get(slot);
        switch (e.getClick()) {
            case LEFT -> buy(p, en, 1);
            case SHIFT_LEFT -> buy(p, en, en.mat().getMaxStackSize());
            case RIGHT -> sell(p, en, 1);
            case SHIFT_RIGHT -> sell(p, en, Integer.MAX_VALUE);
            default -> { }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }

    // ---------------------------------------------------------------- trading

    private void buy(Player p, Entry en, int amount) {
        if (en.buy() < 0) { p.sendMessage(plugin.msg("not-for-sale")); return; }
        EconomyProvider eco = plugin.economy();
        if (eco == null || !eco.isReady()) { p.sendMessage(plugin.msg("economy-unavailable")); return; }
        if (!hasSpace(p, en.mat(), amount)) { p.sendMessage(plugin.msg("inventory-full")); fail(p); return; }

        double cost = en.buy() * amount;
        double bal = eco.balance(p);
        if (bal < cost) {
            p.sendMessage(plugin.msg("not-enough-money", "{price}", plugin.money(cost), "{balance}", plugin.money(bal)));
            fail(p);
            return;
        }
        if (!eco.withdraw(p, cost)) { p.sendMessage(plugin.msg("transaction-failed")); fail(p); return; }

        p.getInventory().addItem(new ItemStack(en.mat(), amount))
                .values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
        p.sendMessage(plugin.msg("bought", "{amount}", String.valueOf(amount),
                "{item}", pretty(en.mat()), "{price}", plugin.money(cost)));
        ok(p);
    }

    private void sell(Player p, Entry en, int amount) {
        if (en.sell() < 0) { p.sendMessage(plugin.msg("not-sellable")); return; }
        EconomyProvider eco = plugin.economy();
        if (eco == null || !eco.isReady()) { p.sendMessage(plugin.msg("economy-unavailable")); return; }

        int qty = Math.min(amount, count(p, en.mat()));
        if (qty <= 0) { p.sendMessage(plugin.msg("nothing-to-sell", "{item}", pretty(en.mat()))); fail(p); return; }

        remove(p, en.mat(), qty);
        double earned = en.sell() * qty;
        if (!eco.deposit(p, earned)) {
            p.getInventory().addItem(new ItemStack(en.mat(), qty))   // give the items back
                    .values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
            p.sendMessage(plugin.msg("transaction-failed"));
            fail(p);
            return;
        }
        p.sendMessage(plugin.msg("sold", "{amount}", String.valueOf(qty),
                "{item}", pretty(en.mat()), "{price}", plugin.money(earned)));
        ok(p);
    }

    // ---------------------------------------------------------------- helpers

    private boolean hasSpace(Player p, Material m, int amount) {
        int room = 0;
        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (s == null || s.getType().isAir()) room += m.getMaxStackSize();
            else if (s.getType() == m) room += Math.max(0, m.getMaxStackSize() - s.getAmount());
        }
        return room >= amount;
    }

    /** Only counts plain items, so renamed / enchanted / damaged ones are never sold. */
    private int count(Player p, Material m) {
        int n = 0;
        for (ItemStack s : p.getInventory().getStorageContents())
            if (s != null && s.getType() == m && !s.hasItemMeta()) n += s.getAmount();
        return n;
    }

    private void remove(Player p, Material m, int amount) {
        ItemStack[] contents = p.getInventory().getStorageContents();
        for (int i = 0; i < contents.length && amount > 0; i++) {
            ItemStack s = contents[i];
            if (s == null || s.getType() != m || s.hasItemMeta()) continue;
            int take = Math.min(amount, s.getAmount());
            if (take == s.getAmount()) contents[i] = null;
            else s.setAmount(s.getAmount() - take);
            amount -= take;
        }
        p.getInventory().setStorageContents(contents);
    }

    private void ok(Player p) { p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f); }
    private void fail(Player p) { p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f); }

    private static ItemStack named(Material m, String name, List<String> lore) {
        ItemStack s = new ItemStack(m);
        ItemMeta meta = s.getItemMeta();
        meta.setDisplayName(color(name));
        if (lore != null) meta.setLore(lore);
        s.setItemMeta(meta);
        return s;
    }

    private static String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    private static String pretty(Material m) {
        StringBuilder sb = new StringBuilder();
        for (String w : m.name().toLowerCase(Locale.ROOT).split("_"))
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        return sb.toString().trim();
    }
}
