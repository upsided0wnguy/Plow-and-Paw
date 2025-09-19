package main.models;

public class MapSource {
    private int id;
    private String username;
    private int row, col;
    private String code, type, name;
    private int level;

    public MapSource(int id, String username, int row, int col, String code, String type, String name, int level) {
        this.id = id; this.username = username; this.row = row; this.col = col;
        this.code = code; this.type = type; this.name = name; this.level = level;
    }
    // Overloaded for convenience (without id)
    public MapSource(String username, int row, int col, String code, String type, String name, int level) {
        this(0, username, row, col, code, type, name, level);
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public String getCode() { return code; }
    public String getType() { return type; }
    public String getName() { return name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
}
