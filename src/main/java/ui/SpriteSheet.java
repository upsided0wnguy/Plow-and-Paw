package main.ui;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

public class SpriteSheet {
    private final Image sheetImage;
    private final int frameWidth;
    private final int frameHeight;

    public SpriteSheet(Image sheetImage, int frameWidth, int frameHeight) {
        this.sheetImage = sheetImage;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
    }

    public Image getFrame(int colIndex, int rowIndex) {
        return new WritableImage(sheetImage.getPixelReader(),
                colIndex * frameWidth,
                rowIndex * frameHeight,
                frameWidth,
                frameHeight);
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }
}