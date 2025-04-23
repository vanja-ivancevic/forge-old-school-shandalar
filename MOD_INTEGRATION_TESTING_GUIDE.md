# Guide: Testing Old-School Shandalar Mod Integration

This guide outlines the steps to test the integration of the "Old-School Shandalar" mod into a clean fork of the official Forge repository, using the conditional loading strategy.

## Prerequisites

*   You have your current modded fork available locally (let's call its path `~/forge-mod`).
*   You have the URL of the official Forge repository (`https://github.com/Card-Forge/forge`).
*   You have Git installed.

## Part 1: Setting Up the New Testing Fork

1.  **Clone the Official Repository:** Create a new directory for your testing environment and clone the official Forge repository into it.
    ```bash
    mkdir ~/forge-integration-test
    cd ~/forge-integration-test
    git clone https://github.com/Card-Forge/forge .
    ```

2.  **Add Your Mod Fork as a Remote:** Add your existing mod fork as a remote repository. This allows you to easily pull files from it. Replace `~/forge-mod` with the actual path to your mod fork directory.
    ```bash
    # Navigate to the new testing fork directory if you aren't already there
    cd ~/forge-integration-test

    # Add the mod fork as a remote named 'mod-fork'
    git remote add mod-fork ~/forge-mod
    ```
    *Note: You can verify this with `git remote -v`.*

3.  **Create an Integration Branch:** Create a new branch in the testing fork where you'll perform the integration work. This keeps the main branch clean.
    ```bash
    git checkout -b shandalar-oldschool-integration
    ```

## Part 2: Bringing Mod Files into the Testing Fork

This section details how to copy your modified files from the `mod-fork` remote into your current `shandalar-oldschool-integration` branch.

**Method 1: Manual File Copying (Using Git)**

This is precise but requires listing each file. You can use the `MOD_CHANGED_FILES.txt` generated earlier as a reference.

*   **Fetch from Mod Fork:** Ensure you have the latest data from your mod fork remote.
    ```bash
    git fetch mod-fork
    ```
*   **Checkout Specific Files:** For each file you modified in your mod, use `git checkout` to pull that specific file *from your mod fork's branch* (assuming it's `master`) into your current working directory.
    *   **Important:** Rename data files during checkout to include `_oldschool` or place them in a new `common_oldschool` directory structure as you check them out.

    ```bash
    # Example for a data file (renaming):
    git checkout mod-fork/master -- forge-gui/res/adventure/common/world/enemies.json
    mv forge-gui/res/adventure/common/world/enemies.json forge-gui/res/adventure/common/world/enemies_oldschool.json

    # Example for a data file (new directory):
    mkdir -p forge-gui/res/adventure/common_oldschool/world
    git checkout mod-fork/master -- forge-gui/res/adventure/common/world/items.json
    mv forge-gui/res/adventure/common/world/items.json forge-gui/res/adventure/common_oldschool/world/items.json

    # Example for a deck file (renaming):
    git checkout mod-fork/master -- forge-gui/res/adventure/common/decks/boss/akroma.dck
    mv forge-gui/res/adventure/common/decks/boss/akroma.dck forge-gui/res/adventure/common/decks/boss/akroma_oldschool.dck

    # Example for a Java file (no rename needed yet):
    git checkout mod-fork/master -- forge-gui-mobile/src/forge/adventure/data/RewardData.java

    # ... repeat for ALL modified files listed in MOD_CHANGED_FILES.txt ...

    # Stage the newly added/copied files
    git add .
    ```
    *   This is tedious for many files but gives maximum control.

**Method 2: Scripting the File Copy (Recommended for many files)**

You can automate the checkout and renaming process using a script.

1.  **Create a Script:** Create a shell script (e.g., `copy_mod_files.sh`) in the root of your `forge-integration-test` directory.
2.  **Populate the Script:** Use the `MOD_CHANGED_FILES.txt` list to generate the necessary `git checkout` and `mv` commands. You might need some text processing (like `awk` or Python) to help generate this script based on the diff output.

    *Example `copy_mod_files.sh` structure:*
    ```bash
    #!/bin/bash

    # Ensure we are fetching the latest from the mod fork
    git fetch mod-fork

    # Define the source branch on the mod fork
    MOD_BRANCH="master" # Or whatever branch your mod is on

    echo "Checking out and renaming data/deck/map files..."

    # --- Data Files ---
    # Example: enemies.json -> enemies_oldschool.json
    git checkout mod-fork/$MOD_BRANCH -- forge-gui/res/adventure/common/world/enemies.json && \
      mv forge-gui/res/adventure/common/world/enemies.json forge-gui/res/adventure/common/world/enemies_oldschool.json

    # Example: items.json -> items_oldschool.json
    git checkout mod-fork/$MOD_BRANCH -- forge-gui/res/adventure/common/world/items.json && \
      mv forge-gui/res/adventure/common/world/items.json forge-gui/res/adventure/common/world/items_oldschool.json

    # ... Add entries for ALL .json, .txt files ...

    # --- Deck Files ---
    # Example: akroma.dck -> akroma_oldschool.dck
    git checkout mod-fork/$MOD_BRANCH -- forge-gui/res/adventure/common/decks/boss/akroma.dck && \
      mv forge-gui/res/adventure/common/decks/boss/akroma.dck forge-gui/res/adventure/common/decks/boss/akroma_oldschool.dck

    # ... Add entries for ALL .dck files ...

    # --- Map Files ---
    # Example: aerie_0.tmx -> aerie_0_oldschool.tmx (if layout changed)
    # Decide if maps need renaming or if data interpretation changes suffice
    # git checkout mod-fork/$MOD_BRANCH -- forge-gui/res/adventure/common/maps/map/aerie/aerie_0.tmx && \
    #   mv forge-gui/res/adventure/common/maps/map/aerie/aerie_0.tmx forge-gui/res/adventure/common/maps/map/aerie/aerie_0_oldschool.tmx

    # ... Add entries for necessary .tmx files ...


    echo "Checking out Java files (will be modified later)..."
    # --- Java Files (No rename needed at this stage) ---
    git checkout mod-fork/$MOD_BRANCH -- forge-game/src/main/java/forge/game/GameFormat.java
    git checkout mod-fork/$MOD_BRANCH -- forge-gui-mobile/src/forge/adventure/data/RewardData.java
    git checkout mod-fork/$MOD_BRANCH -- forge-gui-mobile/src/forge/adventure/util/CardUtil.java

    echo "Staging changes..."
    git add .

    echo "Done copying mod files."
    ```
3.  **Run the Script:**
    ```bash
    chmod +x copy_mod_files.sh
    ./copy_mod_files.sh
    ```
4.  **Commit:** After running the script (or manual checkout), commit the initial state.
    ```bash
    git commit -m "Integrate: Initial copy of Old-School mod files"
    ```

## Part 3: Implementing the Integration Logic

Now, modify the code in the testing fork (`shandalar-oldschool-integration` branch) to make the mod optional.

1.  **Implement UI Setting:**
    *   Locate the Adventure Mode setup screen UI code (e.g., `NewGameScreen.java`).
    *   Add a checkbox: `[ ] Enable Pre-2003 Old-School Mod`.
    *   Save its state to preferences (e.g., using `ForgePreferences`).
    *   Create a static method to easily check the preference (e.g., `AdventureModePreferences.isOldSchoolModEnabled()`).

2.  **Implement Conditional Data Loading:**
    *   Find the classes responsible for loading adventure data (`enemies.json`, `.dck` files, `items.json`, `config.json`, etc.).
    *   Modify the loading logic: Before loading a file, check `AdventureModePreferences.isOldSchoolModEnabled()`.
    *   If true, construct the path to the `_oldschool` version of the file (e.g., `enemies_oldschool.json`). Check if it exists; if so, load it. If not, potentially fall back to the standard file or log an error.
    *   If false, load the standard file path.

3.  **Implement Conditional Map Loading (If Needed):**
    *   If you determined map layouts changed significantly, apply similar conditional logic to map loading, looking for `_oldschool.tmx` files when the mod is enabled.
    *   If only data *within* maps changed, ensure the data loading logic (Step 2) correctly provides the old-school data *to* the standard map rendering logic.

4.  **Implement Conditional Java Logic:**
    *   **`GameFormat.java` / `CardUtil.java`:**
        *   Review the changes you checked out from the mod fork.
        *   If they are universal fixes, leave them as is (they become part of the official code).
        *   If they are mod-specific, wrap the changed sections:
            ```java
            if (AdventureModePreferences.isOldSchoolModEnabled()) {
                // Your mod-specific logic from the fork
            } else {
                // Original logic from the official Forge repo
            }
            ```
    *   **`RewardData.java`:**
        *   **Priority:** Refactor the reward logic. Instead of hardcoding values/logic, make it read from the conditionally loaded `config_oldschool.json` or `items_oldschool.json`. Example: Replace `if (someCondition) { value = 10; }` with `value = config.getInt("oldSchoolValue", defaultValue);`.
        *   **Fallback:** If refactoring isn't fully possible, use conditional blocks as shown above:
            ```java
            if (AdventureModePreferences.isOldSchoolModEnabled()) {
                // Your mod's reward generation logic
            } else {
                // Standard Forge reward generation logic
            }
            ```

5.  **Commit Changes:** Make incremental commits as you implement these changes (e.g., "Implement UI setting", "Implement conditional deck loading", "Refactor RewardData for config").

## Part 4: Testing

1.  **Build:** Build the testing fork.
2.  **Run & Test (Mod Disabled):** Start Forge, go to Adventure Mode setup, ensure the "Old-School Mod" checkbox is **unchecked**. Start a game and verify that standard Shandalar loads correctly (standard decks, card pool, rewards).
3.  **Run & Test (Mod Enabled):** Restart Forge (or start a new game), go to Adventure Mode setup, **check** the "Old-School Mod" checkbox. Start a game and verify:
    *   The card pool is restricted (check shops, rewards).
    *   Enemy decks match your old-school versions.
    *   Starter decks are the old-school ones.
    *   Reward logic behaves as expected in your mod.
    *   Map encounters use the correct decks/data.
4.  **Iterate:** Debug any issues, make corrections, commit, and re-test.

## Part 5: Proposing the Changes (Later)

Once testing is successful in your `shandalar-oldschool-integration` branch, you can clean up the commits and create a Pull Request against the official Forge repository, using the `MOD_INTEGRATION_PROPOSAL.md` as the description.