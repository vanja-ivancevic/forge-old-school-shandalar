import json
import os

ITEMS_FILE = 'forge-gui/res/adventure/common/world/items.json'
VALID_CARDS_FILE = 'forge-gui/res/adventure/common/world/validcards.json'

def validate_card_names():
    invalid_references = {}

    # --- Load Valid Cards ---
    if not os.path.exists(VALID_CARDS_FILE):
        print(f"ERROR: Valid cards file not found at '{VALID_CARDS_FILE}'")
        return

    try:
        with open(VALID_CARDS_FILE, 'r', encoding='utf-8') as f:
            valid_cards_data = json.load(f)
            # Expecting a list of dictionaries, each with a "name" key
            if isinstance(valid_cards_data, list):
                 valid_card_names = set()
                 for card_obj in valid_cards_data:
                     if isinstance(card_obj, dict) and "name" in card_obj:
                         valid_card_names.add(card_obj["name"])
                     else:
                         print(f"WARNING: Skipping invalid entry in '{VALID_CARDS_FILE}': {card_obj}")
            else:
                 print(f"ERROR: Unexpected format in '{VALID_CARDS_FILE}'. Expected a list of card objects.")
                 return
    except json.JSONDecodeError as e:
        print(f"ERROR: Could not decode JSON from '{VALID_CARDS_FILE}': {e}")
        return
    except Exception as e:
        print(f"ERROR: Could not read valid cards file '{VALID_CARDS_FILE}': {e}")
        return

    print(f"Loaded {len(valid_card_names)} valid card names.")

    # --- Load Items ---
    if not os.path.exists(ITEMS_FILE):
        print(f"ERROR: Items file not found at '{ITEMS_FILE}'")
        return

    try:
        with open(ITEMS_FILE, 'r', encoding='utf-8') as f:
            items_data = json.load(f)
    except json.JSONDecodeError as e:
        print(f"ERROR: Could not decode JSON from '{ITEMS_FILE}': {e}")
        return
    except Exception as e:
        print(f"ERROR: Could not read items file '{ITEMS_FILE}': {e}")
        return

    # --- Validate Items ---
    for item in items_data:
        item_name = item.get("name", "Unnamed Item")
        invalid_cards_for_item = []

        effect = item.get("effect", {})
        if not isinstance(effect, dict):
            continue # Skip if effect is not a dictionary

        card_lists_to_check = []
        if "startBattleWithCard" in effect:
            card_lists_to_check.append(effect["startBattleWithCard"])
        if "startBattleWithCardInCommandZone" in effect:
            card_lists_to_check.append(effect["startBattleWithCardInCommandZone"])

        # Check opponent effects too
        opponent_effect = effect.get("opponent", {})
        if isinstance(opponent_effect, dict):
             if "startBattleWithCard" in opponent_effect:
                 card_lists_to_check.append(opponent_effect["startBattleWithCard"])
             if "startBattleWithCardInCommandZone" in opponent_effect:
                 card_lists_to_check.append(opponent_effect["startBattleWithCardInCommandZone"])


        for card_list in card_lists_to_check:
            if isinstance(card_list, list):
                for card_name in card_list:
                    if card_name not in valid_card_names:
                        invalid_cards_for_item.append(card_name)

        if invalid_cards_for_item:
            if item_name not in invalid_references:
                invalid_references[item_name] = []
            # Avoid adding duplicates for the same item
            for card in invalid_cards_for_item:
                if card not in invalid_references[item_name]:
                     invalid_references[item_name].append(card)


    # --- Print Results ---
    if not invalid_references:
        print("\nValidation Complete: All card references in items.json are valid according to validcards.json.")
    else:
        print("\nValidation Complete: Found invalid card references in items.json:")
        print("-------------------------------------------------------------")
        for item_name, invalid_cards in invalid_references.items():
            print(f"- Item: \"{item_name}\" references invalid card(s): {', '.join(invalid_cards)}")
        print("-------------------------------------------------------------")
        print("Please correct these card names in items.json.")

if __name__ == "__main__":
    validate_card_names()