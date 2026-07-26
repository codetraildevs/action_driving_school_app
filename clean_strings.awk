BEGIN {
    # Read the list of names to delete from safe_to_delete.txt
    while ((getline name < "/tmp/safe_to_delete.txt") > 0) {
        delete_names[name] = 1
    }
    close("/tmp/safe_to_delete.txt")
    
    in_deleted = 0
    current_name = ""
}

{
    # If we're inside a deleted multi-line string, check for closing tag
    if (in_deleted) {
        if ($0 ~ /<\/string>/) {
            in_deleted = 0
            current_name = ""
        }
        next
    }
    
    # Check if this line starts a string definition we want to delete
    if (match($0, /<string name="([^"]+)">/, arr)) {
        if (arr[1] in delete_names) {
            # Check if it's a single-line string (opening and closing on same line)
            if ($0 ~ /<\/string>/) {
                # Single-line - just skip this one line
                next
            } else {
                # Multi-line - set flag to skip until closing tag
                in_deleted = 1
                current_name = arr[1]
                next
            }
        }
    }
    
    # Print lines that aren't being deleted
    print
}

END {
    if (in_deleted) {
        print "[WARN] Unclosed string block for '" current_name "' at EOF" > "/dev/stderr"
    }
}
