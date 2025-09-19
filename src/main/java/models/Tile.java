package main.models;

import javafx.scene.image.Image;

public class Tile {
    private final String type;
    private final Image image;
    private final boolean walkable;

    public Tile(String type, Image image, boolean walkable) {
        this.type = type;
        this.image = image;
        this.walkable = walkable;
    }
    public String getType() { return type; }
    public Image getImage() { return image; }
    public boolean isWalkable() { return walkable; }
}

