import os
import re
import sys
import xml.etree.ElementTree as ET

def modify_reward_string(reward_string):
    """Applies modifications to the extracted reward string."""
    if not reward_string:
        return reward_string

    modified_string = reward_string

    # Use simpler string manipulation now that we have the isolated value
    lines = modified_string.splitlines()
    cleaned_lines = []
    for line in lines:
        stripped_line = line.strip()
        # Remove lines containing editions or cardName keys
        # Use " to match the XML escaped quotes
        if '"editions"' in stripped_line or '"cardName"' in stripped_line:
            continue
        cleaned_lines.append(line)

    modified_string = "\n".join(cleaned_lines)

    # Replace Mythic Rare
    modified_string = modified_string.replace('"Mythic Rare"', '"Rare"')

    # Remove duplicate Rare entries (using regex for flexibility)
    modified_string = re.sub(r'("Rare")(\s*,\s*"Rare")+', r'\1', modified_string, flags=re.DOTALL)

    # Cleanup commas (more robustly on the processed lines)
    lines = modified_string.splitlines()
    cleaned_lines = []
    for i, line in enumerate(lines):
        stripped_line = line.strip()
        if not stripped_line: continue # Skip empty lines

        # Remove trailing comma if it's the last element before a closing brace/bracket on the next line
        if stripped_line.endswith(',') and i + 1 < len(lines) and lines[i+1].strip() in ('}', ']'):
            cleaned_lines.append(line.rstrip().rstrip(','))
        else:
            cleaned_lines.append(line)

    modified_string = "\n".join(cleaned_lines)

    # Final dangling comma cleanup before closing bracket/brace
    modified_string = re.sub(r',\s*([}\]])', r'\1', modified_string, flags=re.DOTALL)
    # Remove comma after opening bracket/brace
    modified_string = re.sub(r'([{\[])\s*,', r'\1', modified_string, flags=re.DOTALL)
     # Remove consecutive commas
    while re.search(r',\s*,', modified_string):
         modified_string = re.sub(r',\s*,', ',', modified_string, flags=re.DOTALL)
    # Remove empty objects {}
    modified_string = re.sub(r'\{\s*\},?', '', modified_string, flags=re.DOTALL)
    modified_string = re.sub(r',\s*([}\]])', r'\1', modified_string, flags=re.DOTALL) # Clean again

    return modified_string.strip() # Return stripped string

def process_tmx_file_xml(filepath):
    """Processes a single TMX file using XML parsing."""
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
        modified = False

        for obj in root.findall('.//object'):
            properties = obj.find('properties')
            if properties is not None:
                for prop in properties.findall('property'):
                    if prop.get('name') == 'reward':
                        original_reward_text = prop.text
                        if original_reward_text:
                            modified_reward_text = modify_reward_string(original_reward_text)

                            # Update the element's text only if it actually changed
                            if modified_reward_text != original_reward_text:
                                # Avoid writing empty content if all lines were removed
                                if modified_reward_text and not modified_reward_text.isspace():
                                     prop.text = modified_reward_text
                                     modified = True
                                else:
                                     # If modification results in empty, maybe remove the property?
                                     # For now, we just don't update it, leaving original content.
                                     print(f"Warning: Modification resulted in empty content for reward in {filepath}. Original kept.", file=sys.stderr)


        if modified:
            # Write the modified XML tree back to the file
            # Use xml_declaration=True to keep the <?xml ...?> header
            tree.write(filepath, encoding='utf-8', xml_declaration=True)
            print(f"Modified: {filepath}")
            return True
        else:
            return False

    except ET.ParseError as e:
        print(f"XML Parse Error processing file {filepath}: {e}", file=sys.stderr)
        return False
    except Exception as e:
        print(f"Error processing file {filepath}: {e}", file=sys.stderr)
        return False

def main():
    """Main function to walk through directories and process TMX files."""
    start_dir = 'forge-gui/res/adventure/common/maps/map'
    modified_count = 0
    processed_count = 0
    if not os.path.isdir(start_dir):
        print(f"Error: Directory not found - {start_dir}", file=sys.stderr)
        return

    print(f"Starting final processing run in: {os.path.abspath(start_dir)}")
    for root, _, files in os.walk(start_dir):
        for filename in files:
            if filename.endswith('.tmx'):
                filepath = os.path.join(root, filename)
                processed_count += 1
                if process_tmx_file_xml(filepath): # Use the XML processing function
                    modified_count += 1

    print(f"\nFinished processing.")
    print(f"Processed {processed_count} .tmx files.")
    print(f"Modified {modified_count} files this run.")

if __name__ == "__main__":
    main()