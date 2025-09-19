package main.models;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TileMapLoader {

    public static final Map<String, TileType> TILE_TYPES = Map.ofEntries(
            Map.entry("GR", new TileType("Grass", "/images/tiles/grass.png", true)),
            Map.entry("SO", new TileType("Soil", "/images/tiles/soil.png", true)),
            Map.entry("FX", new TileType("Fence1", "/images/tiles/fence1.png", false)),
            Map.entry("FY", new TileType("Fence2", "/images/tiles/fence2.png", false)),
            Map.entry("H1", new TileType("House 1", "/images/house/house1.png", false)),
            Map.entry("H2", new TileType("House 2", "/images/house/house2.png", false)),
            Map.entry("H3", new TileType("House 3", "/images/house/house3.png", false)),
            Map.entry("BN", new TileType("Barn", "/images/tiles/barn.png", false)),
            Map.entry("CH", new TileType("Chicken Coop", "/images/tiles/chicken_coop.png", false)),
            Map.entry("CP", new TileType("Cow Pen", "/images/tiles/cow_pen.png", false)),
            Map.entry("GP", new TileType("Goat Pen", "/images/tiles/goat_pen.png", false)),
            Map.entry("TP", new TileType("Turkey Pen", "/images/tiles/turkey_pen.png", false)),
            Map.entry("HP", new TileType("Horse Pen", "/images/tiles/horse_pen.png", false)),
            Map.entry("BA", new TileType("Buying Animal Store", "/images/market/buying_animal_Shop.png", false)),
            Map.entry("BC", new TileType("Buying Crop Store", "/images/market/buying_crop_Shop.png", false)),
            Map.entry("BF", new TileType("Buying Fish Store", "/images/market/buying_fish_Shop.png", false)),
            Map.entry("BS", new TileType("Buying Flower Store", "/images/market/buying_flower_Shop.png", false)),
            Map.entry("SA", new TileType("Selling Animal Store", "/images/market/Selling_animals_Shop.png", false)),
            Map.entry("SC", new TileType("Selling Crop Store", "/images/market/selling_crop_Shop.png", false)),
            Map.entry("SF", new TileType("Selling Fish Store", "/images/market/Selling_fish_Shop.png", false)),
            Map.entry("SS", new TileType("Selling Flower Store", "/images/market/Selling_flower_Shop.png", false)),
            Map.entry("MB", new TileType("Buy", "/images/market/buy.png", false)),
            Map.entry("MS", new TileType("Sell", "/images/market/sell.png", false)),
            Map.entry("FP", new TileType("Fish Pond", "/images/tiles/fish_pond.png", true)),
            Map.entry("PN", new TileType("Pond", "/images/tiles/fish_pond.png", false)),
            Map.entry("FR", new TileType("Forest", "/images/tiles/forestt.png", false)),
            Map.entry("BR", new TileType("Bridge", "/images/tiles/bridge.png", true)),
            Map.entry("RV", new TileType("River", "/images/tiles/river.png", false)),
            Map.entry("RO", new TileType("Road", "/images/tiles/road.png", true)),
            Map.entry("CO", new TileType("Cow", "/images/animals/cow.png", false)),
            Map.entry("HR", new TileType("Horse", "/images/animals/horse.png", false)),
            Map.entry("GO", new TileType("Goat", "/images/animals/goat.png", false)),
            Map.entry("CN", new TileType("Chicken", "/images/animals/chicken.png", false)),
            Map.entry("TU", new TileType("Turkey", "/images/animals/turkey.png", false)),
            Map.entry("TR", new TileType("Tree", "/images/trees_and_bushes/tree.png", false)),
            Map.entry("FL", new TileType("Tree", "/images/trees_and_bushes/flowers.png", false)),
            Map.entry("CT", new TileType("Tree", "/images/trees_and_bushes/casuarina_pine.png", false)),
            Map.entry("TT", new TileType("Tomatoes and Carrots", "/images/crops/crops_carrot_tomato.png", false)),
            Map.entry("PP", new TileType("Corn", "/images/crops/corn.png", false)),
            Map.entry("QQ", new TileType("Wheat", "/images/crops/wheat.png", false)),
            Map.entry("XX", new TileType("Potato", "/images/crops/potato.png", false)),
            Map.entry("AR", new TileType("Apple tree", "/images/trees_and_bushes/apple_tree.png", false)),
            Map.entry("PA", new TileType("Path", "/images/tiles/soil.png", true)),
            Map.entry("WT", new TileType("Water", "/images/tiles/water.png", false)),
            Map.entry("MK", new TileType("Market", "/images/market/market_stall.png", false)),
            Map.entry("--", new TileType("Empty", "/images/tiles/empty.png", true))
    );

    public static main.models.TileMap loadFromCSV(String csvPath, int tileSize) throws Exception {
        InputStream in = TileMapLoader.class.getResourceAsStream(csvPath);
        if (in == null) throw new FileNotFoundException("TileMap CSV not found: " + csvPath);

        List<String[]> rows = new ArrayList<>();
        int expectedCols = -1;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.trim().split(",");
                if (expectedCols == -1) expectedCols = parts.length;
                else if (parts.length != expectedCols)
                    throw new IOException("Malformed row: expected " + expectedCols + " columns, found " + parts.length);
                rows.add(parts);
            }
        }

        int numRows = rows.size();
        int numCols = expectedCols;
        main.models.TileMap map = new main.models.TileMap(numRows, numCols, tileSize);

        for (int r = 0; r < numRows; r++) {
            String[] row = rows.get(r);
            for (int c = 0; c < numCols; c++) {
                String code = c < row.length ? row[c] : "--";
                TileType type = TILE_TYPES.getOrDefault(code, TILE_TYPES.get("--"));
                Image img = safeLoadImage(type.imagePath, code);
                map.setTile(r, c, new main.models.Tile(code, img, type.walkable));
            }
        }

        return map;
    }

    public static Image safeLoadImage(String path, String code) {
        InputStream is = TileMapLoader.class.getResourceAsStream(path);
        if (is != null) return new Image(is);

        System.err.println("WARNING: Image not found for code: " + code + " at " + path + " — using fallback.");

        InputStream fallback = TileMapLoader.class.getResourceAsStream("/images/tiles/empty.png");
        if (fallback != null) return new Image(fallback);

        return new WritableImage(16, 16);
    }

    public static class TileType {
        public final String name;
        public final String imagePath;
        public final boolean walkable;

        public TileType(String name, String imagePath, boolean walkable) {
            this.name = name;
            this.imagePath = imagePath;
            this.walkable = walkable;
        }
    }
}