# Volter Shop

A Minecraft Paper plugin providing a GUI-based shop system with support for multiple economy providers.

## Features

- GUI-based item shop
- Configurable shop items and prices
- Economy provider abstraction
- Vault economy support
- PlayerPoints support
- Command-based economy support
- Configurable messages and settings
- Paper API support
- Maven-based Java project

## Project Structure
```
Volter_Shop/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── dev/shop/
│       │       ├── ShopGUI.java
│       │       ├── VolterShop.java
│       │       └── economy/
│       │           ├── CommandEconomy.java
│       │           ├── EconomyProvider.java
│       │           ├── PlayerPointsEconomy.java
│       │           └── VaultEconomy.java
│       └── resources/
│           ├── config.yml
│           └── plugin.yml
│
│
└── .gitignore
```

## Requirements

- Minecraft Java Edition
- Paper 1.20.4
- Java 17+
- Maven for building from source

Depending on the selected economy mode, an appropriate economy provider may also be required.

## Installation

1. Download the plugin JAR from the builds directory or a GitHub release.
2. Place Volter_Shop-1.0.0.jar into the server's plugins folder.
3. Install and configure the required economy provider if your selected economy mode needs one.
4. Start or restart the Paper server.
5. Configure the plugin through its configuration files.

## Building From Source

Clone the repository and build it with Maven:

    git clone git@github.com:VolterBolt/Volter_Shop.git
    cd Volter_Shop
    mvn clean package

The compiled JAR will normally be generated inside the target directory.

## Economy Support

Volter Shop uses an economy abstraction so different economy systems can be supported.

Available implementations include:

- Vault
- PlayerPoints
- Command-based economy

Important: Vault itself is only a bridge/API. Installing Vault alone does not provide an economy. A compatible economy provider must be installed and registered with Vault when using the Vault economy mode.

## Development

This project is maintained as a Maven-based Java project and is intended for Paper servers.

## License

This project is licensed under the MIT License. See the LICENSE file for details.

## Status

Development / testing project.
