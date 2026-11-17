package com.coldfriend.item;

import java.util.regex.Pattern;

/**
 * The stable identifier of a kind of item, such as {@code firewood}. It never changes once chosen, so saved games,
 * trades and other players can refer to an item type by it. It is not a file name and says nothing about how the
 * item is drawn.
 *
 * @param value lower-case letters, digits and underscores, starting with a letter
 */
public record ItemId(String value) {

    private static final Pattern VALID = Pattern.compile("[a-z][a-z0-9_]*");

    /** Rejects identifiers that are not lower snake case. */
    public ItemId {
        if (value == null || !VALID.matcher(value).matches()) {
            throw new IllegalArgumentException("Item id must be lower_snake_case but was: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
