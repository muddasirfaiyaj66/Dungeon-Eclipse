package com.dungeon.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Inventory {
    private List<Item> items;
    private final int maxSize;

    public Inventory(int maxSize) {
        this.items = new ArrayList<>();
        this.maxSize = maxSize;
    }

    public boolean addItem(Item item) {
        if (items.size() < maxSize) {
            items.add(item);
            return true;
        }
        return false; // Inventory full
    }

    public boolean removeItem(Item item) {
        return items.remove(item);
    }

    public List<Item> getItems() {
        return new ArrayList<>(items);
    }

    public int getMaxSize() {
        return maxSize;
    }

    // Add hasItem method
    public boolean hasItem(Item.ItemType type) {
        for (Item item : items) {
            if (item.getType() == type) {
                return true;
            }
        }
        return false;
    }

    // Add removeItem method
    public void removeItem(Item.ItemType type, int count) {
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext() && count > 0) {
            Item item = iterator.next();
            if (item.getType() == type) {
                iterator.remove();
                count--;
            }
        }
    }
}
