# Proposal: Integrate "Old-School Shandalar" Mod into Forge

## 1. Goal

To integrate the community-developed "Old-School Shandalar" mod (currently maintained in a fork) into the official Forge codebase as an optional game mode for the Shandalar adventure campaign.

## 2. Motivation

The mod provides an authentic "golden-age" Magic: The Gathering experience within Shandalar, restricting the card pool primarily to sets from Alpha through Scourge (pre-Modern borders, up to May 2003). This offers a distinct and popular way to play for users who enjoy classic Magic. Integrating it officially makes it easily accessible to all players without needing a separate installation.

## 3. Proposed User Experience

Introduce a new checkbox option on the Shandalar Adventure Mode setup screen:

```
[ ] Enable Pre-2003 Old-School Mod
```

When checked, the game loads the mod's rules, card pool, decks, and configurations. When unchecked, standard Shandalar loads.

## 4. Summary of Changes (Based on `git diff upstream/master...master`)

The mod introduces changes across several areas:

*   **Core Code:** Modifications identified in:
    *   `forge-game/src/main/java/forge/game/GameFormat.java` (Likely for reprint handling)
    *   `forge-gui-mobile/src/forge/adventure/data/RewardData.java` (Handles reward logic)
    *   `forge-gui-mobile/src/forge/adventure/util/CardUtil.java` (Utility functions)
*   **Configuration Data:** Significant modifications and additions to JSON and TXT files defining game elements:
    *   `forge-gui/res/adventure/common/world/enemies.json`
    *   `forge-gui/res/adventure/common/world/items.json`
    *   `forge-gui/res/adventure/common/world/validcards.json` (Added)
    *   `forge-gui/res/blockdata/starters.txt`
    *   `forge-gui/res/adventure/common/config.json`
    *   `forge-gui/res/adventure/Shandalar/world/shops.json`
    *   `forge-gui/res/blockdata/blocks.txt`
*   **Decks:** A very large number of `.dck` files (standard, boss, miniboss, starter) were modified or added within `forge-gui/res/adventure/common/decks/`.
*   **Maps:** A very large number of `.tmx` map files were modified within `forge-gui/res/adventure/common/maps/`.
*   **Build/Packaging:** Modifications to `pom.xml`, `distribution.xml`, and launch scripts (`forge-adventure.command`, `.sh`).
*   **Documentation:** Added/modified `README.md`, `MODDING_GUIDE.md` (for context, not direct integration).

## 5. Proposed Implementation Strategy

The core principle is **conditional loading** based on the new mod setting, avoiding the need for a separate compile target or launcher.

*   **5.1. Mod Setting:**
    *   Implement the "Enable Pre-2003 Old-School Mod" checkbox in the Adventure Mode UI.
    *   Store this setting persistently (e.g., in preferences).
    *   Provide a way for game logic to easily check if the mod is enabled (e.g., `AdventureModePreferences.isOldSchoolModEnabled()`).

*   **5.2. Conditional Data Loading:**
    *   **Recommendation:** Create parallel versions of all modified data files, using a consistent naming convention (e.g., `enemies_oldschool.json`, `my_deck_oldschool.dck`) or place them in a parallel directory structure (e.g., `res/adventure/common_oldschool/`).
    *   Modify the game's adventure loading logic:
        *   If `isOldSchoolModEnabled()` is true, load the `*_oldschool.*` files (or files from the `common_oldschool` directory).
        *   Otherwise, load the standard files.
    *   This applies to: `enemies.json`, `items.json`, `validcards.json`, `starters.txt`, `config.json`, `shops.json`, `blocks.txt`, and all `.dck` files.

*   **5.3. Code Handling (Single Codebase):**
    *   **`GameFormat.java` / `CardUtil.java`:** Analyze the specific changes.
        *   If the changes represent general bug fixes or improvements (like the reprint handling), propose integrating them directly into the core code for everyone's benefit.
        *   If the changes are strictly necessary *only* for the old-school ruleset, wrap the modified code sections within conditional blocks: `if (AdventureModePreferences.isOldSchoolModEnabled()) { /* mod logic */ } else { /* standard logic */ }`.
    *   **`RewardData.java`:**
        *   **Priority:** Investigate refactoring the reward logic changes to be driven by the conditionally loaded data files (e.g., different item pools defined in `items_oldschool.json`, different weights in `config_oldschool.json`).
        *   **Fallback:** If data-driven changes are insufficient, wrap the mod-specific reward logic within conditional blocks (`if (isOldSchoolMode) { ... }`). **Avoid creating separate classes or requiring a different compile.**

*   **5.4. Map Handling (`.tmx`):**
    *   **Acknowledge Complexity:** The large number of modified maps requires careful handling.
    *   **Investigate:** Determine if the map modifications only affect data references (e.g., which enemy deck to use, which reward table to roll on). If so, the standard `.tmx` files might be usable, with the game *interpreting* them differently based on the mod setting (loading `enemy_deck_oldschool.dck` instead of `enemy_deck.dck`).
    *   **Fallback:** If map layouts or fundamental structures were changed, create parallel `*_oldschool.tmx` files and load them conditionally based on the mod setting.

*   **5.5. Build Adjustments:**
    *   The official `pom.xml` and build/packaging scripts may need minor adjustments to include the new `*_oldschool.*` data files and potentially the conditional logic.

## 6. Key Code Pointers (Illustrative - Requires Actual Forge Codebase Analysis)

*   Adventure Mode setup UI class (e.g., `NewGameScreen.java` or similar) - Add checkbox and preference saving.
*   Adventure data loading classes (e.g., `AdventureDataManager.java`, `DeckIO.java`, `WorldIO.java`) - Implement conditional file path logic.
*   `GameFormat.java` - Locate modified methods for reprint handling.
*   `RewardData.java` - Locate modified methods for reward generation.
*   `CardUtil.java` - Locate modified utility methods.
*   Map loading/rendering classes (e.g., `MapLoader.java`, `AdventureMapScreen.java`) - Investigate conditional data interpretation or conditional TMX loading.

## 7. Benefits

*   Integrates a popular mod seamlessly into the main game.
*   Maintains a single codebase and single download package for users.
*   Provides player choice via a simple setting.
*   Leverages existing game structures for loading data.

## 8. Open Questions / Further Investigation Needed

*   Can the changes in `RewardData.java` be fully driven by configuration data, or is conditional Java logic required?
*   What is the exact nature of the `.tmx` map file changes? Can standard maps be used with conditional data interpretation, or are parallel `*_oldschool.tmx` files necessary?
*   Are the changes in `GameFormat.java` / `CardUtil.java` universally beneficial or mod-specific?