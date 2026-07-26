#!/bin/bash
# Delete unused string resources from all locale files
FILES=(
  "app/src/main/res/values/strings.xml"
  "app/src/main/res/values-en/strings.xml"
  "app/src/main/res/values-fr/strings.xml"
  "app/src/main/res/values-rw/strings.xml"
)

# Read all unused string names into an array
NAMES=()
while IFS= read -r name; do
  NAMES+=("$name")
done < /tmp/unused_strings.txt

for file in "${FILES[@]}"; do
  if [ ! -f "$file" ]; then
    echo "SKIP (not found): $file"
    continue
  fi
  echo "Processing: $file"
  cp "$file" "${file}.bak"
  
  for name in "${NAMES[@]}"; do
    # Delete from <string name="name"> through </string> - handles single & multi-line
    sed -i "/<string name=\"$name\">/,/<\/string>/d" "$file" 2>/dev/null
  done
  
  echo "  Done: $file ($(wc -l < "$file") lines)"
done

echo "ALL DONE"
