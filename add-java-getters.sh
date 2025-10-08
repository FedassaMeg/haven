#!/bin/bash

# Script to add JavaBean-style getters to event classes

process_file() {
    local file="$1"

    # Skip if already has JavaBean-style getters
    if grep -q "// JavaBean-style getters" "$file"; then
        echo "Skipping $file - already has JavaBean-style getters"
        return
    fi

    # Check if file has record-style accessors
    if ! grep -q "public.*().*{$" "$file"; then
        echo "Skipping $file - no accessors found"
        return
    fi

    echo "Processing $file..."

    # Create a temporary file for the new content
    temp_file=$(mktemp)

    # Read the file and identify accessors
    awk '
    BEGIN {
        in_accessors = 0
        accessor_count = 0
        indent = "    "
    }

    # Detect record-style accessor
    /^[[:space:]]+public[[:space:]]+[^(]+[[:space:]]+[a-z][a-zA-Z0-9]*\(\)[[:space:]]*\{/ {
        # Store the current line
        current_line = $0

        # Get the method signature
        match($0, /public[[:space:]]+([^[:space:]]+)[[:space:]]+([a-z][a-zA-Z0-9]*)\(\)/, parts)
        return_type = parts[1]
        method_name = parts[2]

        # Get the next line to find the return statement
        getline next_line
        if (match(next_line, /return[[:space:]]+([a-z][a-zA-Z0-9]+);/, ret_parts)) {
            field_name = ret_parts[1]

            # Store accessor info
            accessor_types[accessor_count] = return_type
            accessor_methods[accessor_count] = method_name
            accessor_fields[accessor_count] = field_name
            accessor_count++
        }

        # Print both lines
        print current_line
        print next_line
        next
    }

    # Detect closing brace of class
    /^}$/ && accessor_count > 0 {
        print ""
        print indent "// JavaBean-style getters"

        for (i = 0; i < accessor_count; i++) {
            rt = accessor_types[i]
            mn = accessor_methods[i]
            fn = accessor_fields[i]

            # Generate getter name
            if (rt == "boolean" && match(mn, /^is/)) {
                getter = toupper(substr(mn, 1, 1)) substr(mn, 2)
            } else {
                getter = "get" toupper(substr(mn, 1, 1)) substr(mn, 2)
            }

            print indent "public " rt " " getter "() { return " fn "; }"
        }

        print $0
        accessor_count = 0
        next
    }

    # Print all other lines
    { print }
    ' "$file" > "$temp_file"

    # Replace original file with temp file
    mv "$temp_file" "$file"
    echo "Updated $file"
}

# Process all event files
for module in "client-profile" "program-enrollment" "case-mgmt" "service-delivery"; do
    base_dir="C:/Users/Thomas/Documents/work/haven/backend/modules"

    case $module in
        "client-profile")
            dir="$base_dir/client-profile/src/main/java/org/haven/clientprofile/domain"
            find "$dir" -name "*.java" -path "*/events/*" | while read -r file; do
                process_file "$file"
            done
            find "$dir/consent/events" -name "*.java" 2>/dev/null | while read -r file; do
                process_file "$file"
            done
            ;;
        "program-enrollment")
            dir="$base_dir/program-enrollment/src/main/java/org/haven/programenrollment/domain/events"
            find "$dir" -name "*.java" | while read -r file; do
                process_file "$file"
            done
            ;;
        "case-mgmt")
            dir="$base_dir/case-mgmt/src/main/java/org/haven/casemgmt/domain"
            find "$dir/events" -name "*.java" | while read -r file; do
                process_file "$file"
            done
            find "$dir/mandatedreport/events" -name "*.java" 2>/dev/null | while read -r file; do
                process_file "$file"
            done
            ;;
        "service-delivery")
            dir="$base_dir/service-delivery/src/main/java/org/haven/servicedelivery/domain/events"
            find "$dir" -name "*.java" | while read -r file; do
                process_file "$file"
            done
            ;;
    esac
done

echo "Done!"
