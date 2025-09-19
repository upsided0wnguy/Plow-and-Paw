package main.models;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the user's barn, which stores all farm elements and products.
 */
public class Barn {
    private int level;
    private int maxCapacity;
    private final List<main.models.FarmElement> elements = new ArrayList<>();

    public Barn(int level) {
        this.level = Math.max(1, level);
        this.maxCapacity = 50 + (level - 1) * 25;
    }

    public Barn() { this(1); }

    /**
     * Adds an element (or a stack of products) to the barn.
     * If an identical item already exists (same type, name, level), it stacks.
     * Otherwise, it adds a new entry.
     *
     * @return true if the item was added successfully, false if the barn is full.
     */
    public boolean addElement(main.models.FarmElement.Type type, String name, int amount, int level) {
        if (getUsedCapacity() + amount > getMaxCapacity() && type != main.models.FarmElement.Type.ANIMAL && type != main.models.FarmElement.Type.CROP) {
            // Capacity check, but allow adding core producers even if full.
            return false;
        }

        // Try to find an existing stack.
        for (main.models.FarmElement elem : elements) {
            if (elem.getType() == type && elem.getName().equalsIgnoreCase(name) && elem.getLevel() == level) {
                elem.setAmount(elem.getAmount() + amount);
                return true;
            }
        }

        // If no stack found, add a new element.
        // This part dynamically creates the right object, though a simple FarmElement works for most cases.
        main.models.FarmElement newElement;
        switch (type) {
            case ANIMAL: newElement = new main.models.Animal(name, level, amount); break;
            case CROP:   newElement = new main.models.Crop(name, level, amount); break;
            case FISH:   newElement = new main.models.Fish(name, level, amount); break;
            case FLOWER: newElement = new main.models.Flower(name, level, amount); break;
            case FRUIT:  newElement = new main.models.FruitTree(name, level, amount); break;
            default:     newElement = new main.models.FarmElement(type, name, level, amount); break;
        }
        elements.add(newElement);
        return true;
    }

    /**
     * Removes an element or a specified amount of a product from the barn.
     */
    public boolean removeElement(main.models.FarmElement.Type type, String name, int level, int amountToRemove) {
        Iterator<main.models.FarmElement> it = elements.iterator();
        while (it.hasNext()) {
            main.models.FarmElement elem = it.next();
            if (elem.getType() == type && elem.getName().equalsIgnoreCase(name) && elem.getLevel() == level) {
                if (elem.getAmount() < amountToRemove) return false; // Not enough to remove.
                elem.setAmount(elem.getAmount() - amountToRemove);
                if (elem.getAmount() <= 0) {
                    it.remove(); // Remove the element if the stack is empty.
                }
                return true;
            }
        }
        return false; // Element not found.
    }

    // --- Getters and Setters ---
    public List<main.models.FarmElement> getAllElements() { return new ArrayList<>(elements); }
    public List<main.models.FarmElement> getElements(main.models.FarmElement.Type type) {
        return elements.stream().filter(e -> e.getType() == type).collect(Collectors.toList());
    }
    public int getLevel() { return level; }
    public void setLevel(int level) {
        this.level = level;
        this.maxCapacity = 50 + (level - 1) * 25; // Recalculate capacity on level up.
    }
    public int getMaxCapacity() { return maxCapacity; }
    public int getUsedCapacity() {
        return elements.stream().mapToInt(main.models.FarmElement::getAmount).sum();
    }
    public int getUpgradeCost() { return 150 * level; }
    public boolean upgradeBarn(main.models.User user) {
        int cost = getUpgradeCost();
        if (user.getCoins() >= cost) {
            user.setCoins(user.getCoins() - cost);
            setLevel(this.level + 1);
            return true;
        }
        return false;
    }
    public void clearAll() { elements.clear(); }
}