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

    /*
     * Modern Volter Shop GUI
     *
     * Trading/economy logic is intentionally kept compatible with the
     * existing VolterShop.java and EconomyProvider system.
     */

    private static final int GUI_SIZE = 54;
    private static final int MAX_ITEMS = 28;

    // Category item positions: 4 rows x 7 columns.
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final int BALANCE_SLOT = 4;
    private static final int BACK_SLOT = 45;
    private static final int CLOSE_SLOT = 49;

    private record Entry(Material mat, double buy, double sell) {}
    private record Category(String name, Material icon, List<Entry> entries) {}

    private static class Holder implements InventoryHolder {
        final Category cat; // null = main menu
        Inventory inv;

        Holder(Category cat) {
            this.cat = cat;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private final VolterShop plugin;
    private final Map<Integer, Category> mainSlots = new HashMap<>();
    private int rows = 6;

    public ShopGUI(VolterShop plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- config

    public void load() {
        mainSlots.clear();

        FileConfiguration cfg = plugin.getConfig();

        // The modern menu needs enough room for a clean 6-row layout.
        rows = Math.max(3, Math.min(6, cfg.getInt("gui.rows", 6)));

        ConfigurationSection root = cfg.getConfigurationSection("categories");
        if (root == null) {
            plugin.getLogger().warning("No categories section found in config.yml");
            return;
        }

        int auto = 0;

        for (String id : root.getKeys(false)) {
            ConfigurationSection c = root.getConfigurationSection(id);
            if (c == null) continue;

            List<Entry> entries = new ArrayList<>();

            for (Map<?, ?> m : c.getMapList("items")) {
                Material mat = Material.matchMaterial(String.valueOf(m.get("material")));

                if (mat == null || !mat.isItem()) {
                    plugin.getLogger().warning(
                            "Unknown material '" + m.get("material") +
                            "' in category " + id
                    );
                    continue;
                }

                entries.add(new Entry(
                        mat,
                        num(m.get("buy")),
                        num(m.get("sell"))
                ));
            }

            Material icon = Material.matchMaterial(
                    c.getString("icon", "CHEST")
            );

            Category cat = new Category(
                    c.getString("name", id),
                    icon == null ? Material.CHEST : icon,
                    entries
            );

            int slot = c.getInt("slot", -1);

            if (slot < 0) {
                while (mainSlots.containsKey(auto)) {
                    auto++;
                }
                slot = auto;
            }

            if (slot >= 0 && slot < rows * 9) {
                mainSlots.put(slot, cat);
            }
        }
    }

    private static double num(Object o) {
        return o instanceof Number n ? n.doubleValue() : -1;
    }

    // ---------------------------------------------------------------- main menu

    public void openMain(Player p) {
        Holder h = new Holder(null);

        String title = plugin.getConfig().getString(
                "gui.title",
                "&8&lVolter Shop"
        );

        Inventory inv = Bukkit.createInventory(
                h,
                rows * 9,
                color(title)
        );

        h.inv = inv;

        fillBackground(inv);

        // Balance / economy information.
        inv.setItem(BALANCE_SLOT, balanceItem(p));

        // Category buttons.
        mainSlots.forEach((slot, cat) -> {
            if (slot >= 0 && slot < inv.getSize()) {
                inv.setItem(slot, categoryItem(cat));
            }
        });

        // Bottom controls.
        inv.setItem(49, named(
                Material.BARRIER,
                "&c&lClose",
                List.of("&7Close the shop.")
        ));

        p.openInventory(inv);
        openSound(p);
    }

    private ItemStack categoryItem(Category cat) {
        String loreText = plugin.getConfig().getString(
                "lore.browse",
                "&7Click to browse this category."
        );

        List<String> lore = List.of(
                color(loreText),
                "",
                color("&e▸ Click to open")
        );

        return named(cat.icon(), cat.name(), lore);
    }

    // ---------------------------------------------------------------- category menu

    private void openCategory(Player p, Category cat) {
        Holder h = new Holder(cat);

        Inventory inv = Bukkit.createInventory(
                h,
                GUI_SIZE,
                color(cat.name())
        );

        h.inv = inv;

        fillBackground(inv);

        // Small balance panel at the top.
        inv.setItem(BALANCE_SLOT, balanceItem(p));

        // Items.
        int shown = Math.min(cat.entries().size(), MAX_ITEMS);

        for (int i = 0; i < shown; i++) {
            inv.setItem(
                    ITEM_SLOTS[i],
                    display(cat.entries().get(i))
            );
        }

        // Navigation.
        inv.setItem(
                BACK_SLOT,
                named(
                        Material.ARROW,
                        "&e&lBack",
                        List.of("&7Return to categories.")
                )
        );

        inv.setItem(
                CLOSE_SLOT,
                named(
                        Material.BARRIER,
                        "&c&lClose",
                        List.of("&7Close the shop.")
                )
        );

        p.openInventory(inv);
        openSound(p);
    }

    private ItemStack display(Entry en) {
        FileConfiguration c = plugin.getConfig();

        int stack = en.mat().getMaxStackSize();

        List<String> lore = new ArrayList<>();

        lore.add(color("&8━━━━━━━━━━━━━━━━"));
        lore.add("");

        if (en.buy() >= 0) {
            lore.add(color(
                    c.getString("lore.buy", "&aBuy: &f{price}")
                            .replace("{price}", plugin.money(en.buy()))
            ));

            if (stack > 1) {
                lore.add(color(
                        c.getString(
                                "lore.buy-stack",
                                "&aBuy stack: &f{stack_price}"
                        ).replace(
                                "{stack_price}",
                                plugin.money(en.buy() * stack)
                        )
                ));
            }
        } else {
            lore.add(color("&c✘ Not for sale"));
        }

        if (en.sell() >= 0) {
            lore.add(color(
                    c.getString("lore.sell", "&6Sell: &f{price}")
                            .replace("{price}", plugin.money(en.sell()))
            ));

            lore.add(color(
                    c.getString(
                            "lore.sell-all",
                            "&6Sell all: &fRight-click"
                    )
            ));
        } else {
            lore.add(color("&c✘ Cannot be sold"));
        }

        lore.add("");
        lore.add(color("&8━━━━━━━━━━━━━━━━"));
        lore.add("");

        if (en.buy() >= 0) {
            lore.add(color("&aLeft-click &7→ Buy 1"));
            lore.add(color("&aShift + Left-click &7→ Buy a stack"));
        }

        if (en.sell() >= 0) {
            lore.add(color("&6Right-click &7→ Sell 1"));
            lore.add(color("&6Shift + Right-click &7→ Sell all"));
        }

        ItemStack s = new ItemStack(en.mat());
        ItemMeta m = s.getItemMeta();

        if (m != null) {
            m.setDisplayName(color("&f" + pretty(en.mat())));
            m.setLore(lore);
            s.setItemMeta(m);
        }

        return s;
    }

    // ---------------------------------------------------------------- clicks

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder h)) return;

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (e.getClickedInventory() == null ||
                e.getClickedInventory() != e.getInventory()) {
            return;
        }

        int slot = e.getSlot();

        // Main menu.
        if (h.cat == null) {
            Category cat = mainSlots.get(slot);

            if (cat != null) {
                openCategory(p, cat);
                return;
            }

            if (slot == CLOSE_SLOT) {
                p.closeInventory();
                closeSound(p);
            }

            return;
        }

        // Category menu.
        if (slot == BACK_SLOT) {
            openMain(p);
            return;
        }

        if (slot == CLOSE_SLOT) {
            p.closeInventory();
            closeSound(p);
            return;
        }

        // Ignore decorative slots.
        int itemIndex = slotIndex(slot);

        if (itemIndex < 0 ||
                itemIndex >= Math.min(h.cat.entries().size(), MAX_ITEMS)) {
            return;
        }

        Entry en = h.cat.entries().get(itemIndex);

        switch (e.getClick()) {
            case LEFT -> buy(p, en, 1);
            case SHIFT_LEFT -> buy(p, en, en.mat().getMaxStackSize());
            case RIGHT -> sell(p, en, 1);
            case SHIFT_RIGHT -> sell(p, en, Integer.MAX_VALUE);
            default -> {
                // Other click types intentionally do nothing.
            }
        }
    }

    private int slotIndex(int slot) {
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            if (ITEM_SLOTS[i] == slot) {
                return i;
            }
        }

        return -1;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Holder) {
            e.setCancelled(true);
        }
    }

    // ---------------------------------------------------------------- trading
    // Existing trading logic intentionally preserved.

    private void buy(Player p, Entry en, int amount) {
        if (en.buy() < 0) {
            p.sendMessage(plugin.msg("not-for-sale"));
            return;
        }

        EconomyProvider eco = plugin.economy();

        if (eco == null || !eco.isReady()) {
            p.sendMessage(plugin.msg("economy-unavailable"));
            return;
        }

        if (!hasSpace(p, en.mat(), amount)) {
            p.sendMessage(plugin.msg("inventory-full"));
            fail(p);
            return;
        }

        double cost = en.buy() * amount;
        double bal = eco.balance(p);

        if (bal < cost) {
            p.sendMessage(plugin.msg(
                    "not-enough-money",
                    "{price}", plugin.money(cost),
                    "{balance}", plugin.money(bal)
            ));
            fail(p);
            return;
        }

        if (!eco.withdraw(p, cost)) {
            p.sendMessage(plugin.msg("transaction-failed"));
            fail(p);
            return;
        }

        p.getInventory()
                .addItem(new ItemStack(en.mat(), amount))
                .values()
                .forEach(i ->
                        p.getWorld().dropItemNaturally(
                                p.getLocation(),
                                i
                        )
                );

        p.sendMessage(plugin.msg(
                "bought",
                "{amount}", String.valueOf(amount),
                "{item}", pretty(en.mat()),
                "{price}", plugin.money(cost)
        ));

        ok(p);
    }

    private void sell(Player p, Entry en, int amount) {
        if (en.sell() < 0) {
            p.sendMessage(plugin.msg("not-sellable"));
            return;
        }

        EconomyProvider eco = plugin.economy();

        if (eco == null || !eco.isReady()) {
            p.sendMessage(plugin.msg("economy-unavailable"));
            return;
        }

        int qty = Math.min(amount, count(p, en.mat()));

        if (qty <= 0) {
            p.sendMessage(plugin.msg(
                    "nothing-to-sell",
                    "{item}", pretty(en.mat())
            ));
            fail(p);
            return;
        }

        remove(p, en.mat(), qty);

        double earned = en.sell() * qty;

        if (!eco.deposit(p, earned)) {
            p.getInventory()
                    .addItem(new ItemStack(en.mat(), qty))
                    .values()
                    .forEach(i ->
                            p.getWorld().dropItemNaturally(
                                    p.getLocation(),
                                    i
                            )
                    );

            p.sendMessage(plugin.msg("transaction-failed"));
            fail(p);
            return;
        }

        p.sendMessage(plugin.msg(
                "sold",
                "{amount}", String.valueOf(qty),
                "{item}", pretty(en.mat()),
                "{price}", plugin.money(earned)
        ));

        ok(p);
    }

    // ---------------------------------------------------------------- helpers

    private void fillBackground(Inventory inv) {
        ItemStack filler = named(
                Material.GRAY_STAINED_GLASS_PANE,
                " ",
                null
        );

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        // Clear the central item area so categories/items stand out.
        for (int slot : ITEM_SLOTS) {
            inv.setItem(slot, null);
        }
    }

    private ItemStack balanceItem(Player p) {
        EconomyProvider eco = plugin.economy();

        if (eco == null || !eco.isReady()) {
            return named(
                    Material.EMERALD,
                    "&a&lBalance",
                    List.of(
                            "&7Economy: &cUnavailable",
                            "",
                            "&8Install/configure an economy provider."
                    )
            );
        }

        double balance = eco.balance(p);

        return named(
                Material.EMERALD,
                "&a&lYour Balance",
                List.of(
                        "&7Available: &f" + plugin.money(balance),
                        "",
                        "&8Economy: &f" + eco.name()
                )
        );
    }

    private boolean hasSpace(Player p, Material m, int amount) {
        int room = 0;

        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (s == null || s.getType().isAir()) {
                room += m.getMaxStackSize();
            } else if (s.getType() == m) {
                room += Math.max(
                        0,
                        m.getMaxStackSize() - s.getAmount()
                );
            }
        }

        return room >= amount;
    }

    /**
     * Only counts plain items, so renamed / enchanted / damaged
     * items are never sold.
     */
    private int count(Player p, Material m) {
        int n = 0;

        for (ItemStack s : p.getInventory().getStorageContents()) {
            if (s != null &&
                    s.getType() == m &&
                    !s.hasItemMeta()) {
                n += s.getAmount();
            }
        }

        return n;
    }

    private void remove(Player p, Material m, int amount) {
        ItemStack[] contents =
                p.getInventory().getStorageContents();

        for (int i = 0;
             i < contents.length && amount > 0;
             i++) {

            ItemStack s = contents[i];

            if (s == null ||
                    s.getType() != m ||
                    s.hasItemMeta()) {
                continue;
            }

            int take = Math.min(amount, s.getAmount());

            if (take == s.getAmount()) {
                contents[i] = null;
            } else {
                s.setAmount(s.getAmount() - take);
            }

            amount -= take;
        }

        p.getInventory().setStorageContents(contents);
    }

    private void ok(Player p) {
        p.playSound(
                p.getLocation(),
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                1f,
                1.2f
        );
    }

    private void fail(Player p) {
        p.playSound(
                p.getLocation(),
                Sound.ENTITY_VILLAGER_NO,
                1f,
                1f
        );
    }

    private void openSound(Player p) {
        p.playSound(
                p.getLocation(),
                Sound.BLOCK_CHEST_OPEN,
                0.7f,
                1.1f
        );
    }

    private void closeSound(Player p) {
        p.playSound(
                p.getLocation(),
                Sound.BLOCK_CHEST_CLOSE,
                0.7f,
                1.0f
        );
    }

    private static ItemStack named(
            Material m,
            String name,
            List<String> lore
    ) {
        ItemStack s = new ItemStack(m);
        ItemMeta meta = s.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(color(name));

            if (lore != null) {
                meta.setLore(lore);
            }

            s.setItemMeta(meta);
        }

        return s;
    }

    private static String color(String s) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                s == null ? "" : s
        );
    }

    private static String pretty(Material m) {
        StringBuilder sb = new StringBuilder();

        for (String w :
                m.name().toLowerCase(Locale.ROOT).split("_")) {

            sb.append(
                    Character.toUpperCase(w.charAt(0))
            ).append(
                    w.substring(1)
            ).append(' ');
        }

        return sb.toString().trim();
    }
}
