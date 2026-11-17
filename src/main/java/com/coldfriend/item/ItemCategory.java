package com.coldfriend.item;

/** The broad kind of an item. Used for grouping and display; behaviour is never decided by category alone. */
public enum ItemCategory {
    /** Raw material such as firewood, stones and branches. */
    MATERIAL,
    /** Something to eat. */
    FOOD,
    /** A container, for example for water. */
    CONTAINER,
    /** Survival supplies such as bandages and matches. */
    SUPPLY,
    /** A tool. */
    TOOL
}
