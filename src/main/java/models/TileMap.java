package main.models;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import main.models.Tile;

public class TileMap {
    private final Tile[][] tiles;
    private final int rows, cols, tileSize;
    private static final HashMap<String, Image> imageCache = new HashMap<>();

    public TileMap(int rows, int cols, int tileSize) {
        this.rows = rows;
        this.cols = cols;
        this.tileSize = tileSize;
        tiles = new Tile[rows][cols];
    }

    public boolean isInBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    public String getTileCode(int row, int col) {
        Tile t = getTile(row, col);
        return (t != null) ? t.getType() : "--";
    }

    public void setTile(int row, int col, Tile tile) {
        tiles[row][col] = tile;
    }

    public Tile getTile(int row, int col) {
        return tiles[row][col];
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getTileSize() { return tileSize; }

    public static Image getCachedImage(String path) {
        if (!imageCache.containsKey(path)) {
            try (var is = TileMap.class.getResourceAsStream(path)) {
                if (is != null) {
                    Image img = new Image(is);
                    imageCache.put(path, img);
                }
            } catch (Exception e) {
                System.err.println("Error loading image: " + path);
            }
        }
        return imageCache.getOrDefault(path, null);
    }

    public void render(GraphicsContext gc, int cameraRow, int cameraCol, int viewRows, int viewCols) {
        for (int r = 0; r < viewRows; r++) {
            for (int c = 0; c < viewCols; c++) {
                int mapR = cameraRow + r, mapC = cameraCol + c;
                Image img;
                if (isInBounds(mapR, mapC)) {
                    Tile tile = tiles[mapR][mapC];
                    img = (tile != null && tile.getImage() != null)
                            ? tile.getImage()
                            : getCachedImage("/images/tiles/empty.png");
                } else {
                    img = getCachedImage("/images/tiles/empty.png");
                }
                if (img != null)
                    gc.drawImage(img, c * tileSize, r * tileSize, tileSize, tileSize);
            }
        }
    }

    public void saveToCSV(String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (int r = 0; r < getRows(); r++) {
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < getCols(); c++) {
                    sb.append(getTileCode(r, c));
                    if (c < getCols() - 1) sb.append(",");
                }
                writer.write(sb.toString());
                writer.newLine();
            }
        }
    }

    public void replaceTile(int row, int col, String code) {
        if (!isInBounds(row, col)) return;
        main.models.TileMapLoader.TileType type = main.models.TileMapLoader.TILE_TYPES.getOrDefault(code, main.models.TileMapLoader.TILE_TYPES.get("--"));
        Image img = getCachedImage(type.imagePath);
        setTile(row, col, new Tile(code, img, type.walkable));
    }

    public void clearTile(int row, int col) {
        replaceTile(row, col, "--");
    }

    public boolean isWalkable(int row, int col) {
        if (!isInBounds(row, col)) return false;
        Tile t = tiles[row][col];
        return t != null && t.isWalkable();
    }
}