package main.models;

import javafx.scene.image.Image;

/**
 * Represents a generic entity on the game map, such as the player, an animal, or a tree.
 * This class holds basic information like type, position (row, col), and visual representation.
 */
public class Entity {

    /**
     * The visual image of the entity to be drawn on the canvas.
     */
    public Image image;

    /**
     * An enumeration to define the category of the entity.
     */
    public enum Type {
        PLAYER,
        ANIMAL,
        CROP,
        TREE,
        PET,
        ITEM
    }

    public Type type;
    public int row, col;

    /**
     * A secondary image reference, often used for UI portraits or icons.
     * Can be the same as the main 'image'.
     */
    public Image avatar;

    /**
     * Constructs a new Entity.
     *
     * @param type   The type of the entity (e.g., PLAYER, ANIMAL).
     * @param row    The grid row position of the entity.
     * @param col    The grid column position of the entity.
     * @param avatar The image to use for this entity's appearance.
     */
    public Entity(Type type, int row, int col, Image avatar) {
        this.type = type;
        this.row = row;
        this.col = col;
        this.avatar = avatar;
        this.image = avatar; // By default, the main image is the same as the avatar.
    }
}
