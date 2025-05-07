# Forge: Old-School Shandalar - Modding Guide

This document provides technical specifications for developers looking to modify or understand the core components of the Old-School Shandalar mod for Forge.

## Overview

This mod primarily modifies the Shandalar adventure mode by restricting the card pool to older sets (generally Alpha through Scourge) and adjusting game elements like enemy decks, shops, and rewards to fit this theme. Most modifications involve editing configuration files within the `forge-gui/res/` directory.

## Key Files and Directories

### 1. Global Configuration & Restrictions

*   **File:** `forge-gui/res/adventure/common/config.json`
*   **Purpose:** Central configuration for the adventure mode.
*   **Key Sections:**
    *   `restrictedCards`: A list of specific card names banned from the mode.
    *   `restrictedEditions`: A list of edition codes (e.g., "M21", "NEO") completely banned from the mode. **This is the primary mechanism for enforcing the old-border theme.**
    *   `difficulties`: Defines settings for each difficulty level (Easy, Normal, Hard, Insane). This includes:
        *   Starting life, gold, mana shards.
        *   Enemy life scaling (`enemyLifeFactor`).
        *   Reward scaling (`rewardMaxFactor`).
        *   Penalties (`goldLoss`, `lifeLoss`).
        *   Card sell values (`sellFactor`, `shardSellRatio`).
        *   Paths to starter deck files (`starterDecks`, `constructedStarterDecks`, `pileDecks`) for each color and difficulty.
        *   Starting items (`startItems`).
    *   `starterEditions`: List of edition codes used for generating starter card pools or decks.

### 2. Enemy Definitions

*   **File:** `forge-gui/res/adventure/common/world/enemies.json`
*   **Purpose:** Defines all roaming enemies and bosses encountered on the world map.
*   **Key Sections per Enemy:**
    *   `name`: Internal identifier.
    *   `sprite`: Path to the enemy's visual representation.
    *   `deck`: An array listing paths to the `.dck` or `.json` deck files this enemy can use. `randomizeDeck: true` means one is chosen randomly if multiple are listed.
    *   `life`, `speed`, `difficulty`, `spawnRate`, `flying`, `boss`: Enemy stats and behavior flags.
    *   `rewards`: An array defining potential drops upon defeat. Rewards can be:
        *   `deckCard`: Cards specifically from the enemy's deck, filterable by rarity.
        *   `card`: Cards drawn from the allowed pool (respecting `config.json` restrictions), filterable by color, type, subtype, rarity, card text regex, etc.
        *   `gold`, `shards`, `life`.
        *   `item`: Specific items defined in `items.json`.
        *   Each reward type has `probability` and `count` (often with `addMaxCount` for randomization).
    *   `questTags`: Used for linking enemies to quests.

### 3. Item Definitions

*   **File:** `forge-gui/res/adventure/common/world/items.json`
*   **Purpose:** Defines all usable items, equipment, and quest items.
*   **Key Sections per Item:**
    *   `name`, `description`, `iconName`.
    *   `equipmentSlot`: (e.g., "Left", "Right", "Neck", "Body", "Boots", "Ability1", "Ability2").
    *   `cost`: Price if sold in shops.
    *   `questItem`: Boolean flag.
    *   `effect`: Defines passive effects (e.g., `lifeModifier`, `moveSpeed`, `startBattleWithCard`, `startBattleWithCardInCommandZone`, `cardRewardBonus`).
    *   `usableOnWorldMap`, `usableInPoi`: Flags for active use.
    *   `commandOnUse`, `dialogOnUse`: Defines actions triggered when used (often requires `shardsNeeded`).

### 4. Shop Inventories

*   **File:** `forge-gui/res/adventure/Shandalar/world/shops.json`
*   **Purpose:** Defines the different types of shops found in the world and their inventories.
*   **Key Sections per Shop:**
    *   `name`, `description`, `sprite`, `overlaySprite`.
    *   `rewards`: Array defining the shop's stock. Uses similar filtering logic as enemy rewards (`cardName`, `colors`, `cardTypes`, `subTypes`, `superTypes`, `cardText` regex) to determine which cards are sold.
    *   Can also define shops selling specific `item` types or basic lands (`unlimited: true`).
    *   Booster pack shops (`type: "cardPackShop"`) can filter packs by release date (`startDate`, `endDate`).

### 5. Deck Files

*   **Directory:** `forge-gui/res/adventure/common/decks/`
*   **Purpose:** Contains the actual decklists used by enemies and potentially for starter decks.
*   **Structure:**
    *   Organized into subdirectories (e.g., `boss/`, `miniboss/`, `standard/`, `starter/`).
    *   `.dck` files: Standard Forge deck format (one card name per line, optionally prefixed with quantity like `2x Card Name`). These are referenced directly in `enemies.json`.
    *   `.json` files: Often used as templates for generating decks or card pools based on rules (e.g., starter decks referenced in `config.json`). The structure defines criteria for card selection (colors, rarity, types, etc.).

### 6. Limited Format Definitions (Draft/Sealed)

*   **File:** `forge-gui/res/blockdata/blocks.txt`
*   **Purpose:** Defines Magic blocks and the sets included in them for Draft and Sealed formats.
*   **Format:** `Block Name, DraftPacks/SealedPacks/LandSetCode, SetCode1 SetCode2 ...`
*   **Relevance:** Controls which sets are used if Draft/Sealed events are encountered within the mod. Ensure this aligns with the `restrictedEditions` in `config.json` for consistency.

### 7. Token Selection Logic

*   **File Modified:** `forge-core/src/main/java/forge/token/TokenDb.java`
*   **Purpose:** To ensure a consistent "old-school" feel, the game now prioritizes the oldest available art/printing for any token being created.
*   **Selection Order:**
    1.  **Oldest Printing:** The game searches all sets containing the required token and selects the one with the earliest release date. This naturally includes sets like Unglued (UGL) if they are the oldest available source for a specific token.
    2.  **Original Hint:** As a fallback, if the token cannot be resolved by finding the oldest printing (e.g., if a token script is new and only exists in a very recent set or if dates are missing/inconsistent), the game may use an edition hint originally provided by the calling code.
*   **Impact:** This change primarily affects the visual representation of tokens in-game, ensuring they align with the pre-2003 aesthetic of the mod. It does not change the functional characteristics of the tokens themselves (e.g., a 1/1 Saproling is still a 1/1 Saproling, regardless of which set's art is used).
*   **Modding Implication:** When creating new cards or effects that generate tokens, be aware that the game will automatically attempt to find the oldest printing. If a specific token appearance from a newer set is desired for a particular new card (which would be unusual for this mod's theme), this core logic would need to be considered. However, for maintaining the old-school theme, this new logic is beneficial.

### 8. Core Java File Modifications (from Original Mod)

The original "Old-School Shandalar" mod included changes to a few core Java files. While the integration proposal suggests making these conditional or data-driven, understanding their original intent is useful for modders.

*   **File Modified:** [`forge-game/src/main/java/forge/game/GameFormat.java`](forge-game/src/main/java/forge/game/GameFormat.java)
    *   **Likely Purpose (as per proposal):** Modifications related to handling card reprints. This could involve how the game determines if a card (even a newer printing) is legal based on its original printing being within the mod's allowed sets (Alpha through Scourge). This is crucial for ensuring the correct card pool is enforced.
    *   **Modding Implication:** Changes here affect the fundamental legality of cards within the game format. If further format adjustments are needed, this file might be relevant.

*   **File Modified:** [`forge-gui-mobile/src/forge/adventure/data/RewardData.java`](forge-gui-mobile/src/forge/adventure/data/RewardData.java)
    *   **Likely Purpose (as per proposal):** Adjustments to how rewards (cards, items, etc.) are generated and distributed in the Shandalar adventure mode. This would be to ensure rewards align with the restricted, old-school card pool and theme. For example, preventing post-Scourge cards from appearing as rewards.
    *   **Modding Implication:** If you're altering reward tables, drop rates, or the types of items/cards that can be rewarded, this file (or the data files it might now conditionally load) would be the place to investigate.

*   **File Modified:** [`forge-gui-mobile/src/forge/adventure/util/CardUtil.java`](forge-gui-mobile/src/forge/adventure/util/CardUtil.java)
    *   **Likely Purpose (as per proposal):** Utility functions related to cards, potentially modified to support the mod's specific needs. This could include functions for checking card legality against the old-school format, filtering card lists, or retrieving card information in a way that respects the mod's restrictions.
    *   **Modding Implication:** If you are writing new script conditions, UI elements, or any logic that needs to query or manipulate card data with respect to the mod's rules, this utility class might contain helpful functions or require adjustments.

**Note:** The integration proposal aims to make many of these Java changes data-driven (e.g., by loading different versions of `items.json` or `config.json`) or conditional based on whether the "Old-School Mod" is enabled. This reduces direct Java code changes for the mod itself, making it more maintainable. However, understanding these original points of modification is key if deeper changes to the mod's core behavior are considered.

## Modding Workflow Example (Changing an Enemy Deck)

1.  **Identify Enemy:** Find the enemy entry in `forge-gui/res/adventure/common/world/enemies.json`.
2.  **Locate Deck File:** Note the path(s) listed in the `deck` array for that enemy (e.g., `decks/standard/some_deck.dck`).
3.  **Edit Deck:** Open the corresponding `.dck` file in `forge-gui/res/adventure/common/decks/standard/` (or relevant subdirectory) and modify the card list. Ensure added cards are allowed by the restrictions in `config.json`.
4.  **Test:** Run the game and encounter the enemy to verify the deck changes.

By understanding these key files and their interactions, developers can effectively modify card pools, enemy encounters, rewards, shops, and other core aspects of the Old-School Shandalar experience.