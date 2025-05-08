# Forge Adventure Mode: Reward & Shop System Enhancements Report

This report details modifications made to the Forge adventure mode codebase (compared against `upstream/master`) focusing on card generation, filtering, and shop inventories, particularly concerning basic lands. Code snippets from the modified (local) version are included for precision.

## 1. Basic Land Shop Revamp

**Goal:** Provide dedicated shops for basic lands, separating standard, Snow-Covered, and utility lands.

**Implementation:**

*   **New Shop Definitions (`shops.json`):**
    *   Added five new shop entries (absent upstream) specifically for "Plains", "Island", "Swamp", "Mountain", and "Forest". Example ("Plains"):
        ```json
          {
            "name":"Plains",
            "description":"The Cartographer's Guild",
            "spriteAtlas":"maps/tileset/buildings.atlas",
            "sprite":"LandShop",
               "restockPrice": 1,
              "rewards": [
                   { "count": 5, "cardTypes": ["Land"], "subTypes": ["Plains"] },
                   { "count": 1, "cardName": "Snow-Covered Plains" },
                   { "count": 2, "cardTypes": ["Land"], "subTypes": ["Plains"], "rarity": ["C", "U", "R"] }
                 ]
          }
        ```
    *   Each shop uses a three-part reward structure:
        1.  `{ "count": 5, "cardTypes": ["Land"], "subTypes": ["<Type>"] }`: Generates 5 standard basics. Relies on the filtering change below.
        2.  `{ "count": 1, "cardName": "Snow-Covered <Type>" }`: Generates 1 specific Snow-Covered basic.
        3.  `{ "count": 2, "cardTypes": ["Land"], "subTypes": ["<Type>"], "rarity": ["C", "U", "R"] }`: Generates 2 non-basic utility lands with the basic land subtype.

*   **Filtering Adjustment (`CardUtil.java`):**
    *   Modified `CardUtil.getPredicateResult()` to explicitly skip cards named "Snow-Covered..." when the filter criteria include `cardTypes: ["Land"]` and a basic land `subType`. This ensures the first reward slot yields only standard basics.
        ```java
            // Inside the loop iterating through cards:
            if (shouldExcludeSnowCovered && item.getName().startsWith("Snow-Covered")) {
                continue; // Skip Snow-Covered lands when filtering by Land type/subtype
            }
        ```

## 2. Card Generation & Filtering Enhancements (`CardUtil.java`)

**Goal:** Improve card variety in rewards and provide consistent, diverse printings, especially for basic lands.

**Implementation:**

*   **New `generateCards()` Method:**
    *   Introduced a new core method `CardUtil.generateCards()` to handle card list generation for rewards.
    *   **Basic Land Diversity:** If `filterData.cardName` specifies a basic land (checked via new helper `isBasicLandName()`), it fetches *all* allowed printings of that land to use as the source pool:
        ```java
            if (filterData.cardName != null && isBasicLandName(filterData.cardName)) {
                List<PaperCard> allPrintingsOfBasicLand = FModel.getMagicDb().getCommonCards().getAllCards(filterData.cardName);

                ConfigData config = Config.instance().getConfigData();
                final List<String> restrictedEditions = (config != null && config.restrictedEditions != null) ? Arrays.asList(config.restrictedEditions) : Collections.emptyList();

                List<PaperCard> globallyAllowedPrintings = allPrintingsOfBasicLand.stream()
                    .filter(p -> !restrictedEditions.contains(p.getEdition()))
                    .collect(Collectors.toList());

                if (!globallyAllowedPrintings.isEmpty()) {
                    sourceCardPrintings = globallyAllowedPrintings;
                }
            }
        ```
    *   **Selection Process:** Selects unique card *names* first from the filtered pool, then randomly picks a specific *printing* for each chosen name. This increases the variety of distinct cards received in multi-card rewards. (See lines [`406-440`](forge-gui-mobile/src/forge/adventure/util/CardUtil.java:406) in local `CardUtil.java` for full loop logic).

*   **Modified `getCardByName()` Method:**
    *   Enhanced `CardUtil.getCardByName()` to specifically handle basic lands by fetching all printings and filtering based on global `allowedEditions`/`restrictedEditions` from config:
        ```java
            // Inside getCardByName(String cardName):
            boolean isBasicLand = isBasicLandName(cardName);
            if (isBasicLand) {
                // For basic lands, get all printings but filter based on config restrictions
                validCards = FModel.getMagicDb().getCommonCards().getAllCards(cardName);
                // Filter to allowedEditions or remove restrictedEditions
                ConfigData configData = Config.instance().getConfigData();
                if (configData != null) {
                    // ... filtering logic based on allowed/restricted editions ...
                }
                // If no valid cards after filtering, fall back to default behavior
                if (validCards.isEmpty()) {
                    validCards = FModel.getMagicDb().getCommonCards().getUniqueCardsNoAlt(cardName);
                }
            } else {
                // Standard behavior for non-basic land cards
                // ... (uses useAllCardVariants flag, though that's forced false in Config) ...
            }
        ```
    *   Uses `new Random()` for selecting a printing, decoupling art selection from the world's main RNG seed:
        ```java
            return validCards.get(new Random().nextInt(validCards.size()));
        ```

*   **Modified `getFullCardPool()` Method:**
    *   Changed `CardUtil.getFullCardPool()` to *always* return all printings, ensuring subsequent filtering logic operates on a complete dataset:
        ```java
        public static Collection<PaperCard> getFullCardPool(boolean allCardVariants) {
            // Always return all cards initially; filtering by edition happens later.
            // The allCardVariants flag might be used elsewhere, but for initial pool generation, we need all printings.
            return FModel.getMagicDb().getCommonCards().getAllCards();
        }
        ```

## 3. Reward Data Handling (`RewardData.java`)

**Goal:** Align reward generation with the enhanced `CardUtil` logic.

**Implementation:**

*   **`generate()` Method (Card Case):**
    *   When generating multiple copies of a specific `cardName`, it now calls the enhanced `CardUtil.getCardByName()` *inside* the loop. This allows each generated instance to potentially be a different printing, leveraging the diversity logic in `CardUtil`. (Upstream called `getCardByName` once outside the loop).
        ```java
                    // Inside generate(), case "card":
                    if( cardName != null && !cardName.isEmpty() ) {
                        if (allCardVariants) { // Note: allCardVariants is forced false by Config.java
                            for (int i = 0; i < count + addedCount; i++) {
                                // Fetch a new random printing for each card instance
                                PaperCard chosenPrinting = CardUtil.getCardByName(cardName); // Called inside loop
                                ret.add(new Reward(chosenPrinting, isNoSell));
                            }
                        } else { // This branch is effectively always taken due to Config override
                           // ... (fallback logic) ...
                        }
                    } // ...
        ```

*   **`rewardsToCards()` Method:**
    *   Simplified `RewardData.rewardsToCards()` to directly extract the `PaperCard` from each `Reward` object. Removed upstream logic that attempted to homogenize basic land editions, as printing selection is now handled more robustly earlier in the process (by `CardUtil.generateCards`).
        ```java
        static public List<PaperCard> rewardsToCards(Iterable<Reward> dataList) {
            ArrayList<PaperCard> ret=new ArrayList<PaperCard>();

            // Directly add the card from the reward without homogenizing basic land editions
            for (Reward data : dataList) {
                PaperCard card = data.getCard();
                if (card != null) { // Ensure card exists
                    ret.add(card);
                }
            }
            // The 'else' block for !allCardVariants is removed as it's effectively covered by the simplified loop above now.
            return ret; // Ensure the list is returned
        }
        ```

## 4. Configuration & Token Handling

*   **`Config.java`:**
    *   The constructor now forcibly sets `settingsData.useAllCardVariants = false;`. This enforces a mod-specific setting to exclude newer/alternate card variants, aligning with a likely pre-2003 theme.
        ```java
            // Inside Config() constructor:
            // Always ensure useAllCardVariants is false to prevent post-2003 cards from appearing in mod
            settingsData.useAllCardVariants = false;
        ```

*   **`TokenDb.java`:**
    *   The `getToken()` method was refactored to prioritize finding actual token printings across all editions (defaulting to the oldest) rather than relying solely on an edition hint. (See lines [`79-108`](forge-core/src/main/java/forge/token/TokenDb.java:79) in local `TokenDb.java`).

## 5. Shop Curation (`shops.json`)

*   **Content Reduction:** The local `shops.json` is significantly smaller than upstream, indicating removal of many shops (e.g., Phyrexian-themed) to curate the experience for the mod's theme.
*   **Booster Filtering:** Booster pack shops consistently use `startDate` and `endDate` filters to limit available packs to the mod's intended era. Example:
    ```json
      {
        "name": "BoosterPackShop",
        // ...
        "rewards": [ { "type": "cardPackShop", "count": 8, "startDate": 1996, "endDate": 2003 } ]
      }
    ```

## Conclusion

These modifications provide a more controlled and diverse card acquisition experience within the adventure mode, specifically addressing basic land availability and improving the variety of card printings and unique card names offered in rewards and shops, while curating the overall content to fit the mod's specific design goals.