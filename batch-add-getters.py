#!/usr/bin/env python3
import os
import re
from pathlib import Path

def add_java_bean_getters(file_path):
    """Add JavaBean-style getters to a Java event class file."""

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Skip if already has JavaBean-style getters
    if '// JavaBean-style getters' in content:
        print(f"Skipping {file_path} - already has JavaBean-style getters")
        return False

    # Find all record-style accessors
    accessor_pattern = r'    public\s+(\S+(?:<[^>]+>)?)\s+(\w+)\(\)\s*\{\s*\n\s*return\s+(\w+);\s*\n\s*\}'
    accessors = re.findall(accessor_pattern, content)

    if not accessors:
        print(f"Skipping {file_path} - no record-style accessors found")
        return False

    # Generate JavaBean-style getters
    getters = []
    getters.append('\n    // JavaBean-style getters')

    for return_type, method_name, field_name in accessors:
        # Generate getter name
        if return_type == 'boolean' and method_name.startswith('is'):
            getter_name = method_name[0].upper() + method_name[1:]
        else:
            getter_name = 'get' + method_name[0].upper() + method_name[1:]

        getters.append(f'    public {return_type} {getter_name}() {{ return {field_name}; }}')

    # Find the last closing brace and insert getters before it
    lines = content.split('\n')

    # Find the last non-empty line
    last_brace_idx = None
    for i in range(len(lines) - 1, -1, -1):
        stripped = lines[i].strip()
        if stripped == '}':
            last_brace_idx = i
            break

    if last_brace_idx is None:
        print(f"Warning: Could not find closing brace in {file_path}")
        return False

    # Insert getters before the last closing brace
    lines.insert(last_brace_idx, '\n'.join(getters))

    # Write back
    new_content = '\n'.join(lines)
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)

    print(f"Updated {file_path}")
    return True

def main():
    base_dir = Path(r'C:\Users\Thomas\Documents\work\haven\backend\modules')

    # Define all event directories to process
    event_dirs = [
        base_dir / 'case-mgmt/src/main/java/org/haven/casemgmt/domain/events',
        base_dir / 'case-mgmt/src/main/java/org/haven/casemgmt/domain/mandatedreport/events',
        base_dir / 'service-delivery/src/main/java/org/haven/servicedelivery/domain/events',
    ]

    updated_count = 0
    total_count = 0

    for event_dir in event_dirs:
        if not event_dir.exists():
            print(f"Directory not found: {event_dir}")
            continue

        for java_file in event_dir.glob('*.java'):
            total_count += 1
            if add_java_bean_getters(java_file):
                updated_count += 1

    print(f"\nDone! Updated {updated_count} out of {total_count} files.")

if __name__ == '__main__':
    main()
