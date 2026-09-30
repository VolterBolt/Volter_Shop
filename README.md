# ✦ Volter Shop

> A configurable, GUI-first shop plugin for Paper servers — built for simple player trading and flexible economy integrations.

[![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Paper](https://img.shields.io/badge/Paper-1.20.4-blue?logo=minecraft)](https://papermc.io/)
[![Version](https://img.shields.io/badge/version-1.2.0-6f42c1)](https://github.com/VolterBolt/Volter_Shop)
[![License](https://img.shields.io/badge/license-Custom-red)](LICENSE)

Volter Shop turns your server shop into an intuitive inventory interface. Players can browse categories, buy individual items or full stacks, sell items from their inventory, and view their current balance.

> **Project status:** Development / testing project. Always test economy and transaction settings on a staging server before using them in production.

---

## ✨ Highlights

- **Modern inventory GUI** with category navigation and balance display
- **Buy and sell interactions** using familiar Minecraft click controls
- **Stack purchasing** with `Shift + Left-click`
- **Sell-all support** with `Shift + Right-click`
- **Configurable categories, slots, icons, prices, messages, lore, and GUI title**
- **Multiple economy modes** through a common provider abstraction
- **Transaction safeguards** for unavailable economies, insufficient funds, and full inventories
- **Reload support** without restarting the server
- **Paper API and Maven-based Java project** for straightforward development

## 🧭 In-game controls

| Action | Control |
| --- | --- |
| Open the shop | `/shop` or `/store` |
| Browse a category | Click a category item |
| Buy one item | Left-click an item |
| Buy a full stack | Shift + Left-click |
| Sell one item | Right-click an item |
| Sell all matching items | Shift + Right-click |
| Return to categories | Click **Back** |
| Close the shop | Click **Close** or close the inventory normally |

## 🚀 Installation

### Requirements

- Minecraft Java Edition
- A Paper `1.20.x` server; the project is compiled against Paper `1.20.4`
- Java 17 or newer
- One configured economy option (see [Economy providers](#-economy-providers))

### Install the plugin

1. Download the latest `Volter_Shop` JAR from your build output or a GitHub release.
2. Place the JAR in your server's `plugins/` directory.
3. Install and configure the economy dependency required by your selected provider.
4. Start the Paper server once to generate `plugins/Volter_Shop/config.yml`.
5. Edit the configuration and run `/shop reload`, or restart the server.
6. Open the shop with `/shop`.

## 💰 Economy providers

Set `economy.provider` in `config.yml` to one of the following:

| Provider | Value | Notes |
| --- | --- | --- |
| Vault-backed economy | `VAULT` | Requires Vault plus a compatible economy plugin, such as EssentialsX Economy. Vault alone does not provide money. |
| PlayerPoints | `PLAYERPOINTS` | Requires the PlayerPoints plugin. |
| Command-based economy | `COMMAND` | Uses configurable balance, deposit, and withdraw commands. PlaceholderAPI may be required by the selected balance placeholder. |

Example:

```yaml
economy:
  provider: VAULT
  format: "$%.2f"
```

For command-based economies:

```yaml
economy:
  provider: COMMAND
  command:
    balance-placeholder: "%vault_eco_balance%"
    deposit: "eco give {player} {amount}"
    withdraw: "eco take {player} {amount}"
```

## ⚙️ Configuration at a glance

The generated `config.yml` is the main customization point.

### Add or edit a category

```yaml
categories:
  building:
    name: "&a&lBuilding Blocks"
    icon: GRASS_BLOCK
    slot: 11
    items:
      - { material: STONE, buy: 10, sell: 2 }
      - { material: GLASS, buy: 12, sell: 3 }
      - { material: GOLDEN_APPLE, buy: 200 }
```

- `buy: -1` or an omitted buy value makes an item unavailable for purchase.
- `sell: -1` or an omitted sell value makes an item unavailable for selling.
- Category slots use normal inventory slot indexes.
- The GUI supports between 3 and 6 rows; the default category layout uses 6 rows.
- Material names must be valid Bukkit/Paper `Material` names.

### Customize the interface

```yaml
gui:
  title: "&8✦ &6Volter Shop"
  rows: 6

lore:
  browse: "&7Click to browse"
  buy: "&aLeft-click &7→ Buy 1 &8• &f{price}"
  buy-stack: "&aShift + Left-click &7→ Buy a stack &8• &f{stack_price}"
  sell: "&cRight-click &7→ Sell 1 &8• &f{price}"
  sell-all: "&cShift + Right-click &7→ Sell all"
```

Messages support placeholders such as `{amount}`, `{item}`, `{price}`, and `{balance}`. Color codes use the `&` format.

## 🔐 Commands and permissions

| Command / permission | Default | Purpose |
| --- | --- | --- |
| `/shop` | — | Open the shop GUI |
| `/store` | — | Alias for `/shop` |
| `/shop reload` | — | Reload configuration and economy provider |
| `shop.use` | Everyone | Allows a player to use the shop |
| `shop.reload` | Operators | Allows configuration reloads |

## 🛠️ Build from source

```bash
git clone https://github.com/VolterBolt/Volter_Shop.git
cd Volter_Shop
mvn clean package
```

The compiled plugin JAR is created in `target/`. Copy it to the test server's `plugins/` directory.

### Development stack

- **Language:** Java
- **Build:** Maven
- **Runtime API:** Paper API `1.20.4-R0.1-SNAPSHOT`
- **Java release:** 17
- **Optional integrations:** Vault, PlayerPoints, PlaceholderAPI

## 📁 Project structure

```text
Volter_Shop/
├── pom.xml
├── src/main/java/dev/shop/
│   ├── ShopGUI.java              # Inventory menus, controls, and transactions
│   ├── VolterShop.java           # Plugin lifecycle, commands, reloads
│   └── economy/
│       ├── EconomyProvider.java  # Economy abstraction
│       ├── VaultEconomy.java
│       ├── PlayerPointsEconomy.java
│       └── CommandEconomy.java
└── src/main/resources/
    ├── config.yml                # Shop, economy, GUI, and message settings
    └── plugin.yml                 # Plugin metadata, commands, and permissions
```

## 🧪 Troubleshooting

<details>
<summary><strong>The shop says the economy is unavailable</strong></summary>

Confirm that the provider in `config.yml` matches an installed and enabled economy plugin. For `VAULT`, install both Vault and a Vault-compatible economy implementation. Check the server console for error messages.
</details>

<details>
<summary><strong>Players cannot open the shop</strong></summary>

Check that the player has `shop.use` and that the plugin loaded successfully. Console commands cannot open the GUI because the shop is player-only.
</details>

<details>
<summary><strong>An item does not appear</strong></summary>

Verify the material name and category slot in `config.yml`. Invalid materials are skipped and reported in the server log.
</details>

<details>
<summary><strong>Configuration changes are not visible</strong></summary>

Run `/shop reload` with `shop.reload`, or restart the server. Review the console for YAML or provider errors.
</details>

## 🤝 Contributing

1. Fork the repository.
2. Create a focused branch for your change.
3. Build and test against a Paper `1.20.4` server.
4. Keep configuration examples and user-facing messages up to date.
5. Open a pull request with a clear description of the change and test results.

Bug reports and feature suggestions are welcome through [GitHub Issues](https://github.com/VolterBolt/Volter_Shop/issues).

## 📄 License

Volter Shop is distributed under a custom license. See [LICENSE](LICENSE) for the complete license text.

---

<p align="center">
  Made for Paper servers • Configure it once • Let players trade with confidence
</p>
