package main.ui;

import javafx.scene.image.Image;

public class AnimatedAvatar {
    private final main.ui.SpriteSheet sheet;
    private final int columns;
    private final int fps;
    private int rowIndex;
    private int currentFrame;
    private long lastUpdateTime;

    public AnimatedAvatar(main.ui.SpriteSheet spriteSheet, int initialRowIndex, int columns, int fps) {
        this.sheet = spriteSheet;
        this.rowIndex = initialRowIndex;
        this.columns = columns;
        this.fps = fps;
        this.currentFrame = 0;
        this.lastUpdateTime = 0;
    }

    public void update(long now) {
        if (now - lastUpdateTime >= 1_000_000_000L / fps) {
            currentFrame = (currentFrame + 1) % columns;
            lastUpdateTime = now;
        }
    }

    public void setRowIndex(int row) {
        if (this.rowIndex != row) {
            this.rowIndex = row;
            resetAnimation();
        }
    }

    public void resetAnimation() {
        currentFrame = 0;
        lastUpdateTime = System.nanoTime();
    }

    public Image getCurrentFrame() {
        return sheet.getFrame(rowIndex, currentFrame);
    }

    public int getRowIndex() {
        return rowIndex;
    }
}