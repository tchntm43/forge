# ⚔️  Forge: The Magic: The Gathering Rules Engine | Justin's Mods!

Hi, all. Here are source files for my mods to Forge. There are a lot of changes, new files, etc. A brief summary of what is different in my mod:

1. Added random map events. These include:
- An enemy guards a hoard of valuable spells. Defeat this enemy to get card rewards. Enemy gets to start with a random card from their deck on the battlefield. Reward is rares/mythics, or rarely a random restricted card.
- A merchant sells stolen goods. This is a shop that exclusively sells rares and mythics, at price discount. No refresh available.
- An enemy challenges you to a duel. Fair fight, no starting cards, both at 20 life. Win 3000 gold on win, lose half of gold on loss (penalty probably too heavy and may be changed later)
- Nomad's Bazaar: Buy any non-restricted card. Prices are higher than shops.
- Random gold reward
- Random non-restricted card reward
- River event, inflicts a minor penalty on player, but you have choice over the penalty.
- Duplicate card event, trade shards for duplicating any card you own. The cost is 25 shards for non-restricted cards, and 200 shards for restricted cards.
2. Added Trading: 1 in 5 monsters on the world map want to trade instead of dueling. It does not apply to monsters in dungeons. Cards they have available for trade are influenced by what you offer. If you have a restricted list card for trade, 50% chance they offer 1 restricted list card as well.
3. Added Trade Binder: In deck editor, this is located below the Auto-sell. You can move cards from your inventory to the trade binder. You can still trade with an empty trade binder, but it's a lot more clunky.
4. Modified chest/book rewards. Each has a low chance of instead offering 1 random restricted card.
5. Modified shops to make rares more rare. Shops sell 1 rare/mythic, 2 uncommons, and the rest are commons. This only applies to standard shops with 8 cards.
6. Banned digital-only cards. This is a work-in-progress. The ban-list is located in config.json right below the restricted list. If you want the digital cards, just delete all the entries and leave one line with something that isn't a real card name, like gibberish, just to make sure the loader doesn't fail to find anything in it. Note that there are still quite a few in the game I haven't gotten around to removing yet.
7. Many decks have been modified to replace digital-only cards. If you want enemies to use them, you'll have to modify them back. Sorry.
8. Added unique card prices for cards that are considered "good". Commons can be worth 120 gold at the most, uncommons worth 250 gold at the most, and black lotus is worth 100000. Non-restricted rares tend to top out at around 20000 gold for the very best (things like Jace, the Mind Sculptor). This is also a work in progress, I haven't done all the expansion sets yet, and haven't done any core sets aside from ABUR.

Note, my mods do make the game easier because of faster access to cards you want. This set of mods is best for players who want to get through a "run" in a shorter period of time, being able to quickly build up the deck they've chosen to pilot.

To-do:
1. Finish adding custom card prices for remaining sets
2. Hopefully better AI when facing Counterbalance.
3. Hopefully better AI for avoiding soft-lock with repeated-use cards that return cards from the graveyard to the top of the library

The rest of this page is unedited from the original Forge github page.

Join the **Forge community** on [Discord](https://discord.gg/HcPJNyD66a)!

[![Test build](https://github.com/Card-Forge/forge/actions/workflows/test-build.yaml/badge.svg)](https://github.com/Card-Forge/forge/actions/workflows/test-build.yaml)

---

## ✨ Introduction

**Forge** is a dynamic and open-source **Rules Engine** tailored for **Magic: The Gathering** enthusiasts. Developed by a community of passionate programmers, Forge allows players to explore the rich universe of MTG through a flexible, engaging platform. 

**Note:** Forge operates independently and is not affiliated with Wizards of the Coast.

---

## 🌟 Key Features

- **🌐 Cross-Platform Support:** Play on **Windows, Mac, Linux,** and **Android**.
- **🔧 Extensible Architecture:** Built in **Java**, Forge encourages developers to contribute by adding features and cards.
- **🎮 Versatile Gameplay:** Dive into single-player modes or challenge opponents online!

---

## 🛠️ Installation Guide

### 📥 Desktop Installation
1. **Latest Releases:** Download the latest version [here](https://github.com/Card-Forge/forge/releases/latest).
2. **Snapshot Build:** For the latest development version, grab the `forge-gui-desktop` tarball from our [Snapshot Build](https://github.com/Card-Forge/forge/releases/tag/daily-snapshots).
   - **Tip:** Extract to a new folder to prevent version conflicts.
3. **User Data Management:** Previous players’ data is preserved during upgrades.
4. **Java Requirement:** Ensure you have **Java 17 or later** installed.

### 📱 Android Installation
- _(Note: **Android 11** is the minimum requirement with at least **6GB RAM** to run smoothly. You need to enable **"Install unknown apps"** for Forge to initialize and update itself)_
- Download the **APK** from the [Snapshot Build](https://github.com/Card-Forge/forge/releases/tag/daily-snapshots). On the first launch, Forge will automatically download all necessary assets.

---

## 🎮 Modes of Play

Forge offers various exciting gameplay options:

### 🌍 Adventure Mode
Embark on a thrilling single-player journey where you can:
- Explore an overworld map.
- Challenge diverse AI opponents.
- Collect cards and items to boost your abilities.

<img width="1282" height="752" alt="Shandalar World" src="https://github.com/user-attachments/assets/9af31471-d688-442f-9418-9807d8635b72" />

### 🔍 Quest Modes
Engage in focused gameplay without the overworld exploration—perfect for quick sessions!

<img width="1282" height="752" alt="Quest Duels" src="https://github.com/user-attachments/assets/b9613b1c-e8c3-4320-8044-6922c519aad4" />

### 🤖 AI Formats
Test your skills against AI in multiple formats:
- **Sealed**
- **Draft**
- **Commander**
- **Cube**

For comprehensive gameplay instructions, visit our [User Guide](https://github.com/Card-Forge/forge/wiki/User-Guide).

<img width="1282" height="752" alt="Sealed" src="https://github.com/user-attachments/assets/ae603dbd-4421-4753-a333-87cb0a28d772" />

---

## 💬 Support & Community

Need help? Join our vibrant Discord community! 
- 📜 Read the **#rules** and explore the **FAQ**.
- ❓ Ask your questions in the **#help** channel for assistance.

---

## 🤝 Contributing to Forge

We love community contributions! Interested in helping? Check out our [Contributing Guidelines](CONTRIBUTING.md) for details on how to get started.

---

## ℹ️ About Forge

Forge aims to deliver an immersive and customizable Magic: The Gathering experience for fans around the world. 

### 📊 Repository Statistics

| Metric         | Count                                                       |
|----------------|-------------------------------------------------------------|
| **⭐ Stars:**   | [![GitHub stars](https://img.shields.io/github/stars/Card-Forge/forge?style=flat-square)](https://github.com/Card-Forge/forge/stargazers) |
| **🍴 Forks:**   | [![GitHub forks](https://img.shields.io/github/forks/Card-Forge/forge?style=flat-square)](https://github.com/Card-Forge/forge/network) |
| **👥 Contributors:** | [![GitHub contributors](https://img.shields.io/github/contributors/Card-Forge/forge?style=flat-square)](https://github.com/Card-Forge/forge/graphs/contributors) |

---

**📄 License:** [GPL-3.0](LICENSE)
<div align="center" style="display: flex; align-items: center; justify-content: center;">
    <div style="margin-left: auto;">
        <a href="#top">
            <img src="https://img.shields.io/badge/Back%20to%20Top-000000?style=for-the-badge&logo=github&logoColor=white" alt="Back to Top">
        </a>
    </div>
</div>
